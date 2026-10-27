import { database } from '../database.js';
import { planFefo, recordMovement, requireFields } from '../domain.js';
import { inDateRange, matchesQuery, respondList } from '../query.js';
import { ApiError, badRequest, conflict, notFound } from '../../core/errors.js';
import { toDateKey, todayKey } from '../../core/format.js';
import { route } from '../router.js';

const salesOnly = ['SALES'];
const adminSales = ['ADMIN', 'SALES'];
const admin = ['ADMIN'];

const nextInvoiceCode = () => {
  const key = todayKey().replaceAll('-', '');
  const count = database.data.invoices.filter((invoice) => invoice.code.startsWith(`HD-${key}-`)).length;
  return `HD-${key}-${String(count + 1).padStart(4, '0')}`;
};

const summarize = ({ items, ...invoice }) => ({
  ...invoice,
  itemCount: items.length,
  quantityTotal: items.reduce((sum, item) => sum + item.quantity, 0),
});

route('GET', '/invoices/next-number', salesOnly, () => ({ code: nextInvoiceCode() }));

route('GET', '/invoices', adminSales, ({ query, data, user }) => {
  let list = data.invoices;
  if (query.mine === 'true') list = list.filter((invoice) => invoice.employeeId === user.id);
  if (query.employeeId) list = list.filter((invoice) => invoice.employeeId === query.employeeId);
  if (query.status) list = list.filter((invoice) => invoice.status === query.status);
  if (query.paymentMethod) list = list.filter((invoice) => invoice.paymentMethod === query.paymentMethod);
  if (query.from || query.to) list = list.filter((invoice) => inDateRange(invoice.createdAt, query.from, query.to));
  if (query.q)
    list = list.filter((invoice) => matchesQuery([invoice.code, invoice.customerName, invoice.employeeName], query.q));
  return respondList(list.map(summarize), query, {
    sortable: ['code', 'createdAt', 'customerName', 'employeeName', 'total', 'status', 'paymentMethod'],
    defaultSort: 'createdAt,desc',
  });
});

route('GET', '/invoices/:id', adminSales, ({ params, data }) => {
  const invoice = data.invoices.find((item) => item.id === params.id);
  if (!invoice) throw notFound('Không tìm thấy hóa đơn.');
  const customer = invoice.customerId ? data.customers.find((item) => item.id === invoice.customerId) : null;
  const employee = data.employees.find((item) => item.id === invoice.employeeId);
  return {
    ...invoice,
    customer: customer ? { id: customer.id, name: customer.name, phone: customer.phone, code: customer.code } : null,
    employee: employee ? { id: employee.id, name: employee.name, role: employee.role, email: employee.email } : null,
    pharmacy: data.settings.pharmacy,
  };
});

const validateCheckout = (body, data) => {
  const fieldErrors = {};
  if (!Array.isArray(body.items) || !body.items.length)
    throw badRequest('Giỏ hàng đang trống. Vui lòng chọn thuốc trước khi thanh toán.');
  const lines = [];
  const seen = new Set();
  body.items.forEach((item) => {
    const medicine = data.medicines.find((entry) => entry.id === item.medicineId);
    if (!medicine) throw badRequest('Có thuốc trong giỏ không còn tồn tại trong hệ thống.');
    if (seen.has(medicine.id)) throw badRequest(`Thuốc ${medicine.name} bị trùng dòng trong giỏ hàng.`);
    seen.add(medicine.id);
    const quantity = Number(item.quantity);
    if (!Number.isInteger(quantity) || quantity <= 0)
      throw badRequest(`Số lượng của ${medicine.name} phải là số nguyên lớn hơn 0.`);
    if (medicine.status !== 'ACTIVE') throw conflict(`${medicine.name} đã ngừng kinh doanh, vui lòng xóa khỏi giỏ.`);
    const plan = planFefo(medicine.id, quantity);
    if (!plan.ok) {
      const expiredLeft = data.batches.some(
        (batch) => batch.medicineId === medicine.id && batch.remainingQty > 0 && batch.expiryDate < todayKey(),
      );
      const message =
        plan.available === 0 && expiredLeft
          ? `${medicine.name}: toàn bộ lô còn hàng đã hết hạn, không được phép bán.`
          : `${medicine.name}: chỉ còn ${plan.available} ${medicine.unit} có thể bán, không đủ ${quantity}.`;
      throw conflict(message);
    }
    lines.push({ medicine, quantity, plan });
  });

  const needsPrescription = lines.filter((line) => line.medicine.prescriptionType !== 'OTC');
  if (needsPrescription.length && !body.prescriptionConfirmed) {
    fieldErrors.prescriptionConfirmed = `Cần xác nhận đã kiểm tra đơn thuốc cho: ${needsPrescription.map((line) => line.medicine.name).join(', ')}.`;
  }
  const controlled = lines.filter((line) => line.medicine.prescriptionType === 'CONTROLLED');
  if (controlled.length) {
    if (!String(body.prescriptionCode || '').trim())
      fieldErrors.prescriptionCode = 'Thuốc kiểm soát đặc biệt cần nhập mã đơn thuốc.';
    if (!body.customerId) fieldErrors.customerId = 'Thuốc kiểm soát đặc biệt cần gắn thông tin khách hàng.';
  }
  if (Object.keys(fieldErrors).length)
    throw new ApiError(422, 'PRESCRIPTION_REQUIRED', 'Đơn hàng có thuốc cần kiểm soát theo quy tắc demo.', fieldErrors);
  return lines;
};

route('POST', '/invoices', salesOnly, ({ body, data, user }) => {
  const lines = validateCheckout(body, data);
  const customer = body.customerId ? data.customers.find((item) => item.id === body.customerId) : null;
  if (body.customerId && !customer) throw badRequest('Khách hàng không tồn tại.');

  const subtotal = lines.reduce((sum, line) => sum + line.quantity * line.medicine.price, 0);
  const discountValue = Number(body.discountValue || 0);
  if (!Number.isFinite(discountValue) || discountValue < 0)
    throw badRequest('Giảm giá không hợp lệ.', { discountValue: 'Giảm giá không được âm.' });
  const discountAmount =
    body.discountType === 'PERCENT'
      ? Math.round((subtotal * Math.min(discountValue, 100)) / 100)
      : Math.min(discountValue, subtotal);
  if (body.discountType === 'PERCENT' && discountValue > 100)
    throw badRequest('Giảm giá không hợp lệ.', { discountValue: 'Giảm giá tối đa 100%.' });
  if (body.discountType !== 'PERCENT' && discountValue > subtotal)
    throw badRequest('Giảm giá không hợp lệ.', { discountValue: 'Giảm giá không được lớn hơn tạm tính.' });
  const total = subtotal - discountAmount;
  const paymentMethod = ['CASH', 'TRANSFER', 'CARD'].includes(body.paymentMethod) ? body.paymentMethod : 'CASH';
  const receivedAmount = paymentMethod === 'CASH' ? Number(body.receivedAmount || 0) : total;
  if (paymentMethod === 'CASH' && receivedAmount < total) {
    throw badRequest('Tiền khách đưa chưa đủ.', {
      receivedAmount: 'Tiền khách đưa phải lớn hơn hoặc bằng tổng thanh toán.',
    });
  }

  const invoice = {
    id: database.nextId('inv', 'invoices'),
    code: nextInvoiceCode(),
    status: 'COMPLETED',
    createdAt: new Date().toISOString(),
    employeeId: user.id,
    employeeName: user.name,
    customerId: customer ? customer.id : null,
    customerName: customer ? customer.name : 'Khách lẻ',
    items: lines.map(({ medicine, quantity, plan }) => ({
      medicineId: medicine.id,
      medicineCode: medicine.code,
      medicineName: medicine.name,
      unit: medicine.unit,
      prescriptionType: medicine.prescriptionType,
      quantity,
      unitPrice: medicine.price,
      subtotal: quantity * medicine.price,
      allocations: plan.allocations.map(({ batch, quantity: taken }) => ({
        batchId: batch.id,
        batchCode: batch.code,
        expiryDate: batch.expiryDate,
        quantity: taken,
      })),
    })),
    subtotal,
    discountAmount,
    total,
    paymentMethod,
    receivedAmount,
    changeAmount: receivedAmount - total,
    prescriptionCode: String(body.prescriptionCode || '').trim(),
    note: String(body.note || '').trim(),
    cancelReason: '',
  };

  lines.forEach(({ medicine, plan }) => {
    plan.allocations.forEach(({ batch, quantity }) => {
      batch.remainingQty -= quantity;
      recordMovement({
        type: 'OUT',
        medicineId: medicine.id,
        medicineName: medicine.name,
        batchId: batch.id,
        batchCode: batch.code,
        quantity: -quantity,
        referenceType: 'INVOICE',
        referenceCode: invoice.code,
        performedById: user.id,
        performedByName: user.name,
        note: 'Bán tại quầy',
      });
    });
  });
  data.invoices.push(invoice);
  if (body.draftId) {
    const draftIndex = data.drafts.findIndex((draft) => draft.id === body.draftId && draft.employeeId === user.id);
    if (draftIndex >= 0) data.drafts.splice(draftIndex, 1);
  }
  database.save('invoices', 'batches', 'movements', 'drafts');
  database.log(
    user,
    'CREATE_INVOICE',
    'INVOICE',
    invoice.code,
    `Tạo hóa đơn ${invoice.code} - ${invoice.total.toLocaleString('vi-VN')}đ`,
  );
  return { ...invoice, pharmacy: data.settings.pharmacy };
});

route('POST', '/invoices/:id/cancel', admin, ({ params, body, data, user }) => {
  const invoice = data.invoices.find((item) => item.id === params.id);
  if (!invoice) throw notFound('Không tìm thấy hóa đơn.');
  if (invoice.status === 'CANCELLED') throw conflict('Hóa đơn đã được hủy trước đó.');
  requireFields(body, { reason: 'Lý do hủy' });
  invoice.items.forEach((item) => {
    item.allocations.forEach((allocation) => {
      const batch = data.batches.find((entry) => entry.id === allocation.batchId);
      if (!batch) return;
      batch.remainingQty += allocation.quantity;
      recordMovement({
        type: 'IN',
        medicineId: item.medicineId,
        medicineName: item.medicineName,
        batchId: batch.id,
        batchCode: batch.code,
        quantity: allocation.quantity,
        referenceType: 'INVOICE_CANCEL',
        referenceCode: invoice.code,
        performedById: user.id,
        performedByName: user.name,
        note: `Hoàn kho do hủy hóa đơn: ${body.reason.trim()}`,
      });
    });
  });
  invoice.status = 'CANCELLED';
  invoice.cancelReason = body.reason.trim();
  database.save('invoices', 'batches', 'movements');
  database.log(user, 'CANCEL_INVOICE', 'INVOICE', invoice.code, `Hủy hóa đơn ${invoice.code}`);
  return invoice;
});

route('GET', '/invoice-drafts', salesOnly, ({ data, user }) =>
  data.drafts.filter((draft) => draft.employeeId === user.id).sort((a, b) => b.savedAt.localeCompare(a.savedAt)),
);

route('POST', '/invoice-drafts', salesOnly, ({ body, data, user }) => {
  if (!Array.isArray(body.items) || !body.items.length) throw badRequest('Giỏ hàng trống, không có gì để lưu nháp.');
  const customer = body.customerId ? data.customers.find((item) => item.id === body.customerId) : null;
  const items = body.items.map((item) => {
    const medicine = data.medicines.find((entry) => entry.id === item.medicineId);
    if (!medicine) throw badRequest('Có thuốc trong giỏ không còn tồn tại.');
    return {
      medicineId: medicine.id,
      medicineName: medicine.name,
      unit: medicine.unit,
      quantity: Math.max(1, Number(item.quantity) || 1),
    };
  });
  const existing = body.id ? data.drafts.find((draft) => draft.id === body.id && draft.employeeId === user.id) : null;
  const draft = existing || { id: database.nextId('dft', 'drafts', 3), employeeId: user.id };
  Object.assign(draft, {
    savedAt: new Date().toISOString(),
    customerId: customer ? customer.id : null,
    customerName: customer ? customer.name : 'Khách lẻ',
    discountType: body.discountType === 'PERCENT' ? 'PERCENT' : 'AMOUNT',
    discountValue: Number(body.discountValue || 0),
    paymentMethod: body.paymentMethod || 'CASH',
    items,
  });
  if (!existing) data.drafts.push(draft);
  database.save('drafts');
  return draft;
});

route('DELETE', '/invoice-drafts/:id', salesOnly, ({ params, data, user }) => {
  const index = data.drafts.findIndex((draft) => draft.id === params.id && draft.employeeId === user.id);
  if (index < 0) throw notFound('Không tìm thấy đơn nháp.');
  data.drafts.splice(index, 1);
  database.save('drafts');
  return { success: true };
});

route('GET', '/dashboard/sales', salesOnly, ({ data, user }) => {
  const today = toDateKey(new Date());
  const mine = data.invoices.filter(
    (invoice) =>
      invoice.employeeId === user.id && invoice.status === 'COMPLETED' && toDateKey(invoice.createdAt) === today,
  );
  return {
    invoicesToday: mine.length,
    revenueToday: mine.reduce((sum, invoice) => sum + invoice.total, 0),
    draftCount: data.drafts.filter((draft) => draft.employeeId === user.id).length,
  };
});
