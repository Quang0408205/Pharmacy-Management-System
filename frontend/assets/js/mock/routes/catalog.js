import { database } from '../database.js';
import { buildStockIndex, decorateBatch, decorateMedicine, requireFields } from '../domain.js';
import { matchesQuery, respondList } from '../query.js';
import { badRequest, conflict, notFound } from '../../core/errors.js';
import { route } from '../router.js';

const admin = ['ADMIN'];
const everyone = ['ADMIN', 'SALES', 'WAREHOUSE'];
const adminWarehouse = ['ADMIN', 'WAREHOUSE'];
const adminSales = ['ADMIN', 'SALES'];
const phonePattern = /^(0|\+84)[\d\s]{8,12}$/;

const findOr404 = (list, id) => {
  const item = list.find((entry) => entry.id === id);
  if (!item) throw notFound();
  return item;
};

route('GET', '/categories', everyone, ({ data }) => {
  const counts = new Map();
  data.medicines.forEach((medicine) => counts.set(medicine.categoryId, (counts.get(medicine.categoryId) || 0) + 1));
  return data.categories.map((category) => ({ ...category, medicineCount: counts.get(category.id) || 0 }));
});

const validateCategory = (body, data, selfId) => {
  requireFields(body, { name: 'Tên danh mục', code: 'Mã danh mục' });
  const duplicate = data.categories.find(
    (item) => item.id !== selfId && item.code.toLowerCase() === body.code.trim().toLowerCase(),
  );
  if (duplicate) throw conflict('Mã danh mục đã tồn tại.', { code: 'Mã danh mục đã tồn tại.' });
};

route('POST', '/categories', admin, ({ body, data, user }) => {
  validateCategory(body, data);
  const category = {
    id: database.nextId('cat', 'categories', 2),
    code: body.code.trim().toUpperCase(),
    name: body.name.trim(),
    color: body.color || '#0d9488',
    description: (body.description || '').trim(),
    status: 'ACTIVE',
  };
  data.categories.push(category);
  database.save('categories');
  database.log(user, 'CREATE_CATEGORY', 'CATEGORY', category.code, `Thêm danh mục ${category.name}`);
  return category;
});

route('PUT', '/categories/:id', admin, ({ params, body, data, user }) => {
  const category = findOr404(data.categories, params.id);
  validateCategory(body, data, category.id);
  Object.assign(category, {
    code: body.code.trim().toUpperCase(),
    name: body.name.trim(),
    color: body.color || category.color,
    description: (body.description || '').trim(),
  });
  database.save('categories');
  database.log(user, 'UPDATE_CATEGORY', 'CATEGORY', category.code, `Cập nhật danh mục ${category.name}`);
  return category;
});

route('DELETE', '/categories/:id', admin, ({ params, data, user }) => {
  const category = findOr404(data.categories, params.id);
  if (data.medicines.some((medicine) => medicine.categoryId === category.id)) {
    throw conflict('Không thể xóa danh mục đang có thuốc. Hãy chuyển thuốc sang danh mục khác trước.');
  }
  data.categories.splice(data.categories.indexOf(category), 1);
  database.save('categories');
  database.log(user, 'DELETE_CATEGORY', 'CATEGORY', category.code, `Xóa danh mục ${category.name}`);
  return { success: true };
});

route('GET', '/medicines', everyone, ({ query, data, user }) => {
  const index = buildStockIndex();
  const salesOnly = user.role === 'SALES' || query.sellable === 'true';
  let list = data.medicines
    .filter((medicine) => !salesOnly || medicine.status === 'ACTIVE')
    .map((medicine) => {
      const { costPrice, ...visible } = decorateMedicine(medicine, index);
      return user.role === 'SALES' ? visible : { ...visible, costPrice };
    });
  if (query.q)
    list = list.filter((medicine) =>
      matchesQuery(
        [medicine.name, medicine.code, medicine.barcode, medicine.genericName, medicine.manufacturer],
        query.q,
      ),
    );
  if (query.categoryId) list = list.filter((medicine) => medicine.categoryId === query.categoryId);
  if (query.status) list = list.filter((medicine) => medicine.status === query.status);
  if (query.stockStatus) list = list.filter((medicine) => medicine.stockStatus === query.stockStatus);
  if (query.prescriptionType) list = list.filter((medicine) => medicine.prescriptionType === query.prescriptionType);
  return respondList(list, query, {
    sortable: [
      'code',
      'name',
      'categoryName',
      'price',
      'totalQty',
      'availableQty',
      'nearestExpiry',
      'status',
      'stockStatus',
    ],
    defaultSort: 'name,asc',
  });
});

route('GET', '/medicines/next-code', admin, ({ data }) => {
  const max = data.medicines.reduce(
    (highest, medicine) => Math.max(highest, Number(medicine.code.replace(/\D/g, '')) || 0),
    0,
  );
  return { code: `T${String(max + 1).padStart(4, '0')}` };
});

route('GET', '/medicines/:id', everyone, ({ params, data, user }) => {
  const index = buildStockIndex();
  const medicine = findOr404(data.medicines, params.id);
  const batches = data.batches
    .filter((batch) => batch.medicineId === medicine.id && (user.role !== 'SALES' || batch.remainingQty > 0))
    .map(decorateBatch)
    .sort((a, b) => a.expiryDate.localeCompare(b.expiryDate));
  const movements =
    user.role === 'SALES'
      ? []
      : data.movements
          .filter((movement) => movement.medicineId === medicine.id)
          .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
          .slice(0, 30);
  const detail = decorateMedicine(medicine, index);
  if (user.role === 'SALES') delete detail.costPrice;
  return { ...detail, batches, movements };
});

const validateMedicine = (body, data, selfId) => {
  requireFields(body, {
    name: 'Tên thuốc',
    code: 'Mã thuốc',
    categoryId: 'Danh mục',
    unit: 'Đơn vị tính',
    manufacturer: 'Nhà sản xuất',
  });
  const price = Number(body.price);
  if (!Number.isFinite(price) || price <= 0)
    throw badRequest('Giá bán không hợp lệ.', { price: 'Giá bán phải lớn hơn 0.' });
  const costPrice = Number(body.costPrice || 0);
  if (costPrice < 0) throw badRequest('Giá nhập không hợp lệ.', { costPrice: 'Giá nhập không được âm.' });
  if (!data.categories.some((category) => category.id === body.categoryId)) {
    throw badRequest('Danh mục không tồn tại.', { categoryId: 'Danh mục không tồn tại.' });
  }
  if (data.medicines.some((item) => item.id !== selfId && item.code.toLowerCase() === body.code.trim().toLowerCase())) {
    throw conflict('Mã thuốc đã tồn tại.', { code: 'Mã thuốc đã tồn tại.' });
  }
  if (body.barcode && data.medicines.some((item) => item.id !== selfId && item.barcode === body.barcode.trim())) {
    throw conflict('Mã vạch đã tồn tại.', { barcode: 'Mã vạch đã được gán cho thuốc khác.' });
  }
};

const medicineFields = (body) => ({
  code: body.code.trim().toUpperCase(),
  barcode: (body.barcode || '').trim(),
  name: body.name.trim(),
  genericName: (body.genericName || body.name).trim(),
  strength: (body.strength || '').trim(),
  dosageForm: (body.dosageForm || '').trim(),
  unit: body.unit.trim(),
  packaging: (body.packaging || '').trim(),
  manufacturer: body.manufacturer.trim(),
  country: (body.country || '').trim(),
  categoryId: body.categoryId,
  price: Number(body.price),
  costPrice: Number(body.costPrice || 0),
  prescriptionType: body.prescriptionType || 'OTC',
  minStock: Math.max(0, Number(body.minStock || 0)),
  description: (body.description || '').trim(),
});

route('POST', '/medicines', admin, ({ body, data, user }) => {
  validateMedicine(body, data);
  const medicine = {
    id: database.nextId('med', 'medicines'),
    ...medicineFields(body),
    status: 'ACTIVE',
    imageUrl: null,
    createdAt: new Date().toISOString(),
  };
  data.medicines.push(medicine);
  database.save('medicines');
  database.log(user, 'CREATE_MEDICINE', 'MEDICINE', medicine.code, `Thêm thuốc ${medicine.name}`);
  return decorateMedicine(medicine, buildStockIndex());
});

route('PUT', '/medicines/:id', admin, ({ params, body, data, user }) => {
  const medicine = findOr404(data.medicines, params.id);
  validateMedicine(body, data, medicine.id);
  Object.assign(medicine, medicineFields(body));
  database.save('medicines');
  database.log(user, 'UPDATE_MEDICINE', 'MEDICINE', medicine.code, `Cập nhật thuốc ${medicine.name}`);
  return decorateMedicine(medicine, buildStockIndex());
});

route('PATCH', '/medicines/:id/status', admin, ({ params, body, data, user }) => {
  const medicine = findOr404(data.medicines, params.id);
  if (!['ACTIVE', 'INACTIVE'].includes(body.status)) throw badRequest('Trạng thái không hợp lệ.');
  medicine.status = body.status;
  database.save('medicines');
  database.log(
    user,
    'UPDATE_MEDICINE_STATUS',
    'MEDICINE',
    medicine.code,
    `${body.status === 'ACTIVE' ? 'Kích hoạt' : 'Ngừng kinh doanh'} thuốc ${medicine.name}`,
  );
  return decorateMedicine(medicine, buildStockIndex());
});

const validateParty = (body, data, collection, selfId, label) => {
  requireFields(body, { name: `Tên ${label}`, phone: 'Số điện thoại' });
  if (!phonePattern.test(body.phone.trim()))
    throw badRequest('Số điện thoại không hợp lệ.', { phone: 'Số điện thoại không hợp lệ (ví dụ 0901 234 567).' });
  if (body.email && !/^\S+@\S+\.\S+$/.test(body.email.trim()))
    throw badRequest('Email không hợp lệ.', { email: 'Email không hợp lệ.' });
  if (
    data[collection].some(
      (item) => item.id !== selfId && item.phone.replace(/\s/g, '') === body.phone.replace(/\s/g, ''),
    )
  ) {
    throw conflict('Số điện thoại đã tồn tại.', { phone: `Số điện thoại đã được dùng cho ${label} khác.` });
  }
};

route('GET', '/suppliers', adminWarehouse, ({ query, data }) => {
  let list = data.suppliers.map((supplier) => ({
    ...supplier,
    receiptCount: data.receipts.filter(
      (receipt) => receipt.supplierId === supplier.id && receipt.status === 'CONFIRMED',
    ).length,
  }));
  if (query.q)
    list = list.filter((supplier) =>
      matchesQuery([supplier.name, supplier.code, supplier.contactName, supplier.phone, supplier.taxCode], query.q),
    );
  if (query.status) list = list.filter((supplier) => supplier.status === query.status);
  return respondList(list, query, {
    sortable: ['code', 'name', 'contactName', 'status', 'receiptCount'],
    defaultSort: 'code,asc',
  });
});

route('GET', '/suppliers/:id', adminWarehouse, ({ params, data }) => {
  const supplier = findOr404(data.suppliers, params.id);
  const receipts = data.receipts
    .filter((receipt) => receipt.supplierId === supplier.id)
    .sort((a, b) => b.receivingDate.localeCompare(a.receivingDate))
    .map(({ items, ...receipt }) => ({ ...receipt, itemCount: items.length }));
  const totalAmount = receipts
    .filter((receipt) => receipt.status === 'CONFIRMED')
    .reduce((sum, receipt) => sum + receipt.totalAmount, 0);
  return { ...supplier, receipts: receipts.slice(0, 20), receiptCount: receipts.length, totalAmount };
});

const supplierFields = (body) => ({
  name: body.name.trim(),
  contactName: (body.contactName || '').trim(),
  phone: body.phone.trim(),
  email: (body.email || '').trim(),
  address: (body.address || '').trim(),
  taxCode: (body.taxCode || '').trim(),
  paymentTerms: body.paymentTerms || 'Thanh toán ngay',
  note: (body.note || '').trim(),
});

route('POST', '/suppliers', adminWarehouse, ({ body, data, user }) => {
  validateParty(body, data, 'suppliers', null, 'nhà cung cấp');
  const id = database.nextId('sup', 'suppliers', 2);
  const supplier = {
    id,
    code: `NCC${id.split('-')[1].padStart(3, '0')}`,
    ...supplierFields(body),
    status: 'ACTIVE',
    createdAt: new Date().toISOString(),
  };
  data.suppliers.push(supplier);
  database.save('suppliers');
  database.log(user, 'CREATE_SUPPLIER', 'SUPPLIER', supplier.code, `Thêm nhà cung cấp ${supplier.name}`);
  return supplier;
});

route('PUT', '/suppliers/:id', adminWarehouse, ({ params, body, data, user }) => {
  const supplier = findOr404(data.suppliers, params.id);
  validateParty(body, data, 'suppliers', supplier.id, 'nhà cung cấp');
  Object.assign(supplier, supplierFields(body));
  database.save('suppliers');
  database.log(user, 'UPDATE_SUPPLIER', 'SUPPLIER', supplier.code, `Cập nhật nhà cung cấp ${supplier.name}`);
  return supplier;
});

route('PATCH', '/suppliers/:id/status', adminWarehouse, ({ params, body, data, user }) => {
  const supplier = findOr404(data.suppliers, params.id);
  if (!['ACTIVE', 'INACTIVE'].includes(body.status)) throw badRequest('Trạng thái không hợp lệ.');
  supplier.status = body.status;
  database.save('suppliers');
  database.log(
    user,
    'UPDATE_SUPPLIER_STATUS',
    'SUPPLIER',
    supplier.code,
    `${body.status === 'ACTIVE' ? 'Kích hoạt' : 'Ngừng hợp tác'} nhà cung cấp ${supplier.name}`,
  );
  return supplier;
});

route('GET', '/customers', adminSales, ({ query, data }) => {
  const stats = new Map();
  data.invoices
    .filter((invoice) => invoice.customerId && invoice.status === 'COMPLETED')
    .forEach((invoice) => {
      const entry = stats.get(invoice.customerId) || { invoiceCount: 0, totalSpent: 0, lastPurchase: '' };
      entry.invoiceCount += 1;
      entry.totalSpent += invoice.total;
      if (invoice.createdAt > entry.lastPurchase) entry.lastPurchase = invoice.createdAt;
      stats.set(invoice.customerId, entry);
    });
  let list = data.customers.map((customer) => ({
    ...customer,
    ...(stats.get(customer.id) || { invoiceCount: 0, totalSpent: 0, lastPurchase: '' }),
  }));
  if (query.q)
    list = list.filter((customer) =>
      matchesQuery([customer.name, customer.code, customer.phone, customer.email], query.q),
    );
  return respondList(list, query, {
    sortable: ['code', 'name', 'phone', 'invoiceCount', 'totalSpent', 'lastPurchase'],
    defaultSort: 'code,desc',
  });
});

route('GET', '/customers/:id', adminSales, ({ params, data }) => {
  const customer = findOr404(data.customers, params.id);
  const invoices = data.invoices
    .filter((invoice) => invoice.customerId === customer.id)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .map(({ items, ...invoice }) => ({ ...invoice, itemCount: items.length }));
  const completed = invoices.filter((invoice) => invoice.status === 'COMPLETED');
  return {
    ...customer,
    invoices: invoices.slice(0, 30),
    invoiceCount: completed.length,
    totalSpent: completed.reduce((sum, invoice) => sum + invoice.total, 0),
  };
});

const customerFields = (body) => ({
  name: body.name.trim(),
  phone: body.phone.trim(),
  email: (body.email || '').trim(),
  address: (body.address || '').trim(),
  gender: body.gender || 'Nam',
  birthYear: body.birthYear ? Number(body.birthYear) : null,
  note: (body.note || '').trim(),
});

route('POST', '/customers', adminSales, ({ body, data, user }) => {
  validateParty(body, data, 'customers', null, 'khách hàng');
  const id = database.nextId('cus', 'customers', 3);
  const customer = {
    id,
    code: `KH${id.split('-')[1].padStart(4, '0')}`,
    ...customerFields(body),
    createdAt: new Date().toISOString(),
  };
  data.customers.push(customer);
  database.save('customers');
  database.log(user, 'CREATE_CUSTOMER', 'CUSTOMER', customer.code, `Thêm khách hàng ${customer.name}`);
  return { ...customer, invoiceCount: 0, totalSpent: 0, lastPurchase: '' };
});

route('PUT', '/customers/:id', adminSales, ({ params, body, data, user }) => {
  const customer = findOr404(data.customers, params.id);
  validateParty(body, data, 'customers', customer.id, 'khách hàng');
  Object.assign(customer, customerFields(body));
  database.save('customers');
  database.log(user, 'UPDATE_CUSTOMER', 'CUSTOMER', customer.code, `Cập nhật khách hàng ${customer.name}`);
  return customer;
});
