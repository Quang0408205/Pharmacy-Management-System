import { database } from '../database.js';
import { buildStockIndex, decorateBatch, recordMovement, requireFields, stockStatusOf } from '../domain.js';
import { inDateRange, matchesQuery, respondList } from '../query.js';
import { badRequest, conflict, notFound } from '../../core/errors.js';
import { addDaysKey, todayKey } from '../../core/format.js';
import { route } from '../router.js';

const adminWarehouse = ['ADMIN', 'WAREHOUSE'];
const warehouseOnly = ['WAREHOUSE'];
const admin = ['ADMIN'];

const nextReceiptCode = (data) => {
  const max = data.receipts.reduce(
    (highest, receipt) => Math.max(highest, Number(receipt.code.split('-').pop()) || 0),
    0,
  );
  return `PN-${todayKey().slice(0, 4)}-${String(max + 1).padStart(4, '0')}`;
};

const receiptSummary = ({ items, ...receipt }) => ({ ...receipt, itemCount: items.length });

route('GET', '/receipts/next-code', warehouseOnly, ({ data }) => ({ code: nextReceiptCode(data) }));

route('GET', '/receipts', adminWarehouse, ({ query, data }) => {
  let list = data.receipts;
  if (query.status) list = list.filter((receipt) => receipt.status === query.status);
  if (query.supplierId) list = list.filter((receipt) => receipt.supplierId === query.supplierId);
  if (query.from || query.to) list = list.filter((receipt) => inDateRange(receipt.receivingDate, query.from, query.to));
  if (query.q)
    list = list.filter((receipt) => matchesQuery([receipt.code, receipt.supplierName, receipt.createdByName], query.q));
  return respondList(list.map(receiptSummary), query, {
    sortable: ['code', 'receivingDate', 'supplierName', 'totalQuantity', 'totalAmount', 'status', 'createdByName'],
    defaultSort: 'receivingDate,desc',
  });
});

route('GET', '/receipts/:id', adminWarehouse, ({ params, data }) => {
  const receipt = data.receipts.find((item) => item.id === params.id);
  if (!receipt) throw notFound('Không tìm thấy phiếu nhập.');
  return receipt;
});

const buildReceiptItems = (body, data, { strict, existingId }) => {
  if (!Array.isArray(body.items) || !body.items.length) throw badRequest('Phiếu nhập chưa có dòng thuốc nào.');
  const today = todayKey();
  const receivingDate = body.receivingDate || today;
  const fieldErrors = {};
  const seenBatches = new Set();
  const items = body.items.map((item, index) => {
    const medicine = data.medicines.find((entry) => entry.id === item.medicineId);
    if (!medicine) throw badRequest(`Dòng ${index + 1}: thuốc không tồn tại.`);
    const quantity = Number(item.quantity);
    const price = Number(item.purchasePrice);
    const batchCode = String(item.batchCode || '').trim();
    const mark = (field, message) => {
      fieldErrors[`items.${index}.${field}`] = message;
    };
    if (!Number.isInteger(quantity) || quantity <= 0) mark('quantity', 'Số lượng phải là số nguyên lớn hơn 0.');
    if (!Number.isFinite(price) || price < 0) mark('purchasePrice', 'Giá nhập không hợp lệ.');
    if (strict) {
      if (!batchCode) mark('batchCode', 'Nhập số lô.');
      if (!item.expiryDate) mark('expiryDate', 'Nhập hạn sử dụng.');
      else if (item.expiryDate <= receivingDate || item.expiryDate <= today)
        mark('expiryDate', 'Hạn sử dụng phải sau ngày nhập và chưa hết hạn.');
      if (item.manufactureDate && item.expiryDate && item.manufactureDate >= item.expiryDate)
        mark('manufactureDate', 'Ngày sản xuất phải trước hạn sử dụng.');
      if (item.manufactureDate && item.manufactureDate > receivingDate)
        mark('manufactureDate', 'Ngày sản xuất không được sau ngày nhập.');
      const key = `${medicine.id}|${batchCode.toLowerCase()}`;
      if (batchCode && seenBatches.has(key)) mark('batchCode', 'Số lô bị trùng trong phiếu.');
      seenBatches.add(key);
      if (
        batchCode &&
        data.batches.some(
          (batch) => batch.medicineId === medicine.id && batch.code.toLowerCase() === batchCode.toLowerCase(),
        )
      ) {
        mark('batchCode', 'Số lô này đã tồn tại cho thuốc này.');
      }
    }
    return {
      medicineId: medicine.id,
      medicineCode: medicine.code,
      medicineName: medicine.name,
      batchCode,
      manufactureDate: item.manufactureDate || '',
      expiryDate: item.expiryDate || '',
      quantity: Number.isFinite(quantity) ? quantity : 0,
      unit: medicine.unit,
      purchasePrice: Number.isFinite(price) ? price : 0,
      subtotal: (Number.isFinite(quantity) ? quantity : 0) * (Number.isFinite(price) ? price : 0),
    };
  });
  const quantityErrors = Object.keys(fieldErrors).filter(
    (key) => key.endsWith('quantity') || key.endsWith('purchasePrice'),
  );
  if (quantityErrors.length || (strict && Object.keys(fieldErrors).length)) {
    throw badRequest('Phiếu nhập có dòng chưa hợp lệ. Vui lòng kiểm tra các dòng được đánh dấu.', fieldErrors);
  }
  return { items, receivingDate, existingId };
};

const applyReceiptFields = (receipt, body, data, user, strict) => {
  requireFields(body, { supplierId: 'Nhà cung cấp' });
  const supplier = data.suppliers.find((item) => item.id === body.supplierId);
  if (!supplier) throw badRequest('Nhà cung cấp không tồn tại.', { supplierId: 'Nhà cung cấp không tồn tại.' });
  if (strict && supplier.status !== 'ACTIVE')
    throw badRequest('Nhà cung cấp đã ngừng hợp tác.', { supplierId: 'Nhà cung cấp đã ngừng hợp tác.' });
  const { items, receivingDate } = buildReceiptItems(body, data, { strict, existingId: receipt.id });
  Object.assign(receipt, {
    supplierId: supplier.id,
    supplierName: supplier.name,
    receivingDate,
    items,
    totalQuantity: items.reduce((sum, item) => sum + item.quantity, 0),
    totalAmount: items.reduce((sum, item) => sum + item.subtotal, 0),
    note: String(body.note || '').trim(),
    createdById: receipt.createdById || user.id,
    createdByName: receipt.createdByName || user.name,
  });
};

const confirmReceipt = (receipt, data, user) => {
  receipt.items.forEach((item) => {
    const medicine = data.medicines.find((entry) => entry.id === item.medicineId);
    const batch = {
      id: database.nextId('bat', 'batches'),
      code: item.batchCode,
      medicineId: item.medicineId,
      supplierId: receipt.supplierId,
      receiptId: receipt.id,
      receivedDate: receipt.receivingDate,
      manufactureDate: item.manufactureDate,
      expiryDate: item.expiryDate,
      initialQty: item.quantity,
      remainingQty: item.quantity,
      purchasePrice: item.purchasePrice,
    };
    data.batches.push(batch);
    item.batchId = batch.id;
    if (medicine && item.purchasePrice > 0) medicine.costPrice = item.purchasePrice;
    recordMovement({
      type: 'IN',
      medicineId: item.medicineId,
      medicineName: item.medicineName,
      batchId: batch.id,
      batchCode: batch.code,
      quantity: item.quantity,
      referenceType: 'RECEIPT',
      referenceCode: receipt.code,
      performedById: user.id,
      performedByName: user.name,
      note: `Nhập từ ${receipt.supplierName}`,
    });
  });
  receipt.status = 'CONFIRMED';
  receipt.confirmedAt = new Date().toISOString();
  database.save('receipts', 'batches', 'movements', 'medicines');
  database.log(
    user,
    'CONFIRM_RECEIPT',
    'RECEIPT',
    receipt.code,
    `Xác nhận phiếu nhập ${receipt.code} (${receipt.totalQuantity} đơn vị)`,
  );
};

route('POST', '/receipts', warehouseOnly, ({ body, data, user }) => {
  const receipt = {
    id: database.nextId('rec', 'receipts', 3),
    code: nextReceiptCode(data),
    status: 'DRAFT',
    confirmedAt: null,
    createdById: user.id,
    createdByName: user.name,
  };
  applyReceiptFields(receipt, body, data, user, Boolean(body.confirm));
  data.receipts.push(receipt);
  if (body.confirm) confirmReceipt(receipt, data, user);
  else {
    database.save('receipts');
    database.log(user, 'SAVE_RECEIPT_DRAFT', 'RECEIPT', receipt.code, `Lưu nháp phiếu nhập ${receipt.code}`);
  }
  return receipt;
});

route('PUT', '/receipts/:id', warehouseOnly, ({ params, body, data, user }) => {
  const receipt = data.receipts.find((item) => item.id === params.id);
  if (!receipt) throw notFound('Không tìm thấy phiếu nhập.');
  if (receipt.status !== 'DRAFT') throw conflict('Phiếu nhập đã xác nhận, không thể chỉnh sửa.');
  applyReceiptFields(receipt, body, data, user, Boolean(body.confirm));
  if (body.confirm) confirmReceipt(receipt, data, user);
  else database.save('receipts');
  return receipt;
});

route('DELETE', '/receipts/:id', warehouseOnly, ({ params, data, user }) => {
  const receipt = data.receipts.find((item) => item.id === params.id);
  if (!receipt) throw notFound('Không tìm thấy phiếu nhập.');
  if (receipt.status !== 'DRAFT') throw conflict('Chỉ được xóa phiếu nháp.');
  data.receipts.splice(data.receipts.indexOf(receipt), 1);
  database.save('receipts');
  database.log(user, 'DELETE_RECEIPT_DRAFT', 'RECEIPT', receipt.code, `Xóa phiếu nháp ${receipt.code}`);
  return { success: true };
});

route('GET', '/batches', ['ADMIN', 'WAREHOUSE', 'SALES'], ({ query, data, user }) => {
  let list = data.batches.map(decorateBatch);
  if (user.role === 'SALES') list = list.filter((batch) => batch.remainingQty > 0);
  if (query.medicineId) list = list.filter((batch) => batch.medicineId === query.medicineId);
  if (query.supplierId) list = list.filter((batch) => batch.supplierId === query.supplierId);
  if (query.status) list = list.filter((batch) => batch.status === query.status);
  if (query.expiryWithin) {
    const limit = addDaysKey(todayKey(), Number(query.expiryWithin));
    list = list.filter(
      (batch) => batch.remainingQty > 0 && batch.expiryDate >= todayKey() && batch.expiryDate <= limit,
    );
  }
  if (query.hasStock === 'true') list = list.filter((batch) => batch.remainingQty > 0);
  if (query.q)
    list = list.filter((batch) =>
      matchesQuery(
        [batch.code, batch.medicineName, batch.medicineCode, batch.supplierName, batch.receiptCode],
        query.q,
      ),
    );
  return respondList(list, query, {
    sortable: [
      'code',
      'medicineName',
      'supplierName',
      'receivedDate',
      'expiryDate',
      'initialQty',
      'remainingQty',
      'status',
    ],
    defaultSort: 'expiryDate,asc',
  });
});

route('GET', '/batches/:id', adminWarehouse, ({ params, data }) => {
  const batch = data.batches.find((item) => item.id === params.id);
  if (!batch) throw notFound('Không tìm thấy lô thuốc.');
  const movements = data.movements
    .filter((movement) => movement.batchId === batch.id)
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt));
  return { ...decorateBatch(batch), movements };
});

route('POST', '/batches/:id/write-off', warehouseOnly, ({ params, body, data, user }) => {
  const batch = data.batches.find((item) => item.id === params.id);
  if (!batch) throw notFound('Không tìm thấy lô thuốc.');
  requireFields(body, { reason: 'Lý do hủy' });
  if (batch.expiryDate >= todayKey()) throw conflict('Chỉ được hủy lô đã quá hạn sử dụng.');
  if (batch.remainingQty <= 0) throw conflict('Lô này không còn tồn để hủy.');
  const medicine = data.medicines.find((item) => item.id === batch.medicineId);
  const quantity = batch.remainingQty;
  batch.remainingQty = 0;
  recordMovement({
    type: 'WRITE_OFF',
    medicineId: medicine.id,
    medicineName: medicine.name,
    batchId: batch.id,
    batchCode: batch.code,
    quantity: -quantity,
    referenceType: 'WRITE_OFF',
    referenceCode: `HUY-${batch.code}`,
    performedById: user.id,
    performedByName: user.name,
    note: body.reason.trim(),
  });
  database.save('batches', 'movements');
  database.log(
    user,
    'WRITE_OFF_BATCH',
    'BATCH',
    batch.code,
    `Hủy ${quantity} ${medicine.unit} lô hết hạn ${batch.code}`,
  );
  return decorateBatch(batch);
});

route('GET', '/movements', adminWarehouse, ({ query, data }) => {
  let list = data.movements;
  if (query.type) list = list.filter((movement) => movement.type === query.type);
  if (query.medicineId) list = list.filter((movement) => movement.medicineId === query.medicineId);
  if (query.batchId) list = list.filter((movement) => movement.batchId === query.batchId);
  if (query.from || query.to) list = list.filter((movement) => inDateRange(movement.createdAt, query.from, query.to));
  if (query.q)
    list = list.filter((movement) =>
      matchesQuery(
        [movement.medicineName, movement.batchCode, movement.referenceCode, movement.performedByName],
        query.q,
      ),
    );
  return respondList(list, query, {
    sortable: ['createdAt', 'medicineName', 'batchCode', 'quantity', 'type', 'referenceCode', 'performedByName'],
    defaultSort: 'createdAt,desc',
  });
});

route('GET', '/inventory/summary', adminWarehouse, ({ data }) => {
  const index = buildStockIndex();
  const today = todayKey();
  const warning = addDaysKey(today, data.settings.expiryWarningDays);
  const withStock = data.batches.filter((batch) => batch.remainingQty > 0);
  let lowStock = 0;
  let outOfStock = 0;
  data.medicines
    .filter((medicine) => medicine.status === 'ACTIVE')
    .forEach((medicine) => {
      const status = stockStatusOf(medicine, index.get(medicine.id), data.settings);
      if (status === 'LOW_STOCK') lowStock += 1;
      if (status === 'OUT_OF_STOCK') outOfStock += 1;
    });
  return {
    totalMedicines: data.medicines.length,
    activeMedicines: data.medicines.filter((medicine) => medicine.status === 'ACTIVE').length,
    totalBatches: withStock.length,
    totalUnits: withStock.reduce((sum, batch) => sum + batch.remainingQty, 0),
    stockValue: withStock.reduce((sum, batch) => sum + batch.remainingQty * batch.purchasePrice, 0),
    lowStock,
    outOfStock,
    expiringBatches: withStock.filter((batch) => batch.expiryDate >= today && batch.expiryDate <= warning).length,
    expiredBatches: withStock.filter((batch) => batch.expiryDate < today).length,
  };
});

route('GET', '/expiry/summary', adminWarehouse, ({ data }) => {
  const today = todayKey();
  const withStock = data.batches.filter((batch) => batch.remainingQty > 0);
  const tiers = data.settings.expiryTiers.map((days) => ({
    days,
    count: withStock.filter((batch) => batch.expiryDate >= today && batch.expiryDate <= addDaysKey(today, days)).length,
  }));
  return { tiers, expired: withStock.filter((batch) => batch.expiryDate < today).length };
});

const stocktakeSummary = ({ lines, ...stocktake }) => ({
  ...stocktake,
  lineCount: lines.length,
  differenceCount: lines.filter((line) => line.actualQty !== null && line.difference !== 0).length,
});

const findStocktake = (data, id) => {
  const stocktake = data.stocktakes.find((item) => item.id === id);
  if (!stocktake) throw notFound('Không tìm thấy phiếu kiểm kê.');
  return stocktake;
};

route('GET', '/stocktakes', adminWarehouse, ({ query, data }) => {
  let list = data.stocktakes;
  if (query.status) list = list.filter((item) => item.status === query.status);
  if (query.q) list = list.filter((item) => matchesQuery([item.code, item.scopeLabel, item.createdByName], query.q));
  return respondList(list.map(stocktakeSummary), query, {
    sortable: ['code', 'createdAt', 'scopeLabel', 'status', 'createdByName'],
    defaultSort: 'createdAt,desc',
  });
});

route('GET', '/stocktakes/:id', adminWarehouse, ({ params, data }) => findStocktake(data, params.id));

route('POST', '/stocktakes', warehouseOnly, ({ body, data, user }) => {
  const category =
    body.categoryId && body.categoryId !== 'ALL' ? data.categories.find((item) => item.id === body.categoryId) : null;
  if (body.categoryId && body.categoryId !== 'ALL' && !category) throw badRequest('Danh mục không tồn tại.');
  const medicineIds = new Set(
    data.medicines
      .filter((medicine) => !category || medicine.categoryId === category.id)
      .map((medicine) => medicine.id),
  );
  const medicineById = Object.fromEntries(data.medicines.map((medicine) => [medicine.id, medicine]));
  const lines = data.batches
    .filter((batch) => batch.remainingQty > 0 && medicineIds.has(batch.medicineId))
    .sort(
      (a, b) =>
        medicineById[a.medicineId].name.localeCompare(medicineById[b.medicineId].name, 'vi') ||
        a.expiryDate.localeCompare(b.expiryDate),
    )
    .slice(0, 60)
    .map((batch) => ({
      batchId: batch.id,
      batchCode: batch.code,
      medicineId: batch.medicineId,
      medicineName: medicineById[batch.medicineId].name,
      systemQty: batch.remainingQty,
      actualQty: null,
      difference: 0,
      reason: '',
    }));
  if (!lines.length) throw conflict('Không có lô nào còn tồn trong phạm vi đã chọn.');
  const max = data.stocktakes.reduce((highest, item) => Math.max(highest, Number(item.code.split('-').pop()) || 0), 0);
  const stocktake = {
    id: database.nextId('stk', 'stocktakes', 3),
    code: `KK-${todayKey().slice(0, 4)}-${String(max + 1).padStart(3, '0')}`,
    status: 'DRAFT',
    scope: category ? category.id : 'ALL',
    scopeLabel: category ? category.name : 'Toàn bộ kho',
    note: String(body.note || '').trim(),
    createdAt: new Date().toISOString(),
    createdById: user.id,
    createdByName: user.name,
    submittedAt: null,
    reviewedById: null,
    reviewedByName: null,
    reviewedAt: null,
    reviewNote: '',
    lines,
  };
  data.stocktakes.push(stocktake);
  database.save('stocktakes');
  database.log(user, 'CREATE_STOCKTAKE', 'STOCKTAKE', stocktake.code, `Tạo phiên kiểm kê ${stocktake.code}`);
  return stocktake;
});

const applyCounts = (stocktake, counts) => {
  const byBatch = new Map(counts.map((entry) => [entry.batchId, entry]));
  stocktake.lines.forEach((line) => {
    const entry = byBatch.get(line.batchId);
    if (!entry) return;
    const hasValue = entry.actualQty !== null && entry.actualQty !== '' && entry.actualQty !== undefined;
    const actual = hasValue ? Number(entry.actualQty) : null;
    if (hasValue && (!Number.isInteger(actual) || actual < 0))
      throw badRequest(`Số lượng thực tế của lô ${line.batchCode} phải là số nguyên không âm.`);
    line.actualQty = actual;
    line.difference = actual === null ? 0 : actual - line.systemQty;
    line.reason = String(entry.reason || '').trim();
  });
};

route('PUT', '/stocktakes/:id', warehouseOnly, ({ params, body, data }) => {
  const stocktake = findStocktake(data, params.id);
  if (stocktake.status !== 'DRAFT') throw conflict('Chỉ phiếu nháp mới được chỉnh sửa.');
  applyCounts(stocktake, body.lines || []);
  if (body.note !== undefined) stocktake.note = String(body.note).trim();
  database.save('stocktakes');
  return stocktake;
});

route('DELETE', '/stocktakes/:id', warehouseOnly, ({ params, data }) => {
  const stocktake = findStocktake(data, params.id);
  if (stocktake.status !== 'DRAFT') throw conflict('Chỉ được xóa phiếu kiểm kê nháp.');
  data.stocktakes.splice(data.stocktakes.indexOf(stocktake), 1);
  database.save('stocktakes');
  return { success: true };
});

route('POST', '/stocktakes/:id/submit', warehouseOnly, ({ params, body, data, user }) => {
  const stocktake = findStocktake(data, params.id);
  if (stocktake.status !== 'DRAFT') throw conflict('Phiếu kiểm kê không ở trạng thái nháp.');
  if (body.lines) applyCounts(stocktake, body.lines);
  const missing = stocktake.lines.filter((line) => line.actualQty === null);
  if (missing.length) throw badRequest(`Còn ${missing.length} dòng chưa nhập số lượng thực tế.`);
  const noReason = stocktake.lines.filter((line) => line.difference !== 0 && !line.reason);
  if (noReason.length) throw badRequest(`Còn ${noReason.length} dòng chênh lệch chưa ghi lý do.`);
  stocktake.status = 'PENDING_APPROVAL';
  stocktake.submittedAt = new Date().toISOString();
  database.save('stocktakes');
  database.log(user, 'SUBMIT_STOCKTAKE', 'STOCKTAKE', stocktake.code, `Gửi duyệt phiếu kiểm kê ${stocktake.code}`);
  return stocktake;
});

route('POST', '/stocktakes/:id/approve', admin, ({ params, body, data, user }) => {
  const stocktake = findStocktake(data, params.id);
  if (stocktake.status !== 'PENDING_APPROVAL') throw conflict('Phiếu kiểm kê không ở trạng thái chờ duyệt.');
  stocktake.lines
    .filter((line) => line.difference !== 0)
    .forEach((line) => {
      const batch = data.batches.find((item) => item.id === line.batchId);
      if (!batch) return;
      const newQty = Math.max(0, batch.remainingQty + line.difference);
      const delta = newQty - batch.remainingQty;
      batch.remainingQty = newQty;
      recordMovement({
        type: 'ADJUSTMENT',
        medicineId: line.medicineId,
        medicineName: line.medicineName,
        batchId: batch.id,
        batchCode: batch.code,
        quantity: delta,
        referenceType: 'STOCKTAKE',
        referenceCode: stocktake.code,
        performedById: user.id,
        performedByName: user.name,
        note: line.reason || 'Điều chỉnh sau kiểm kê',
      });
    });
  Object.assign(stocktake, {
    status: 'APPROVED',
    reviewedById: user.id,
    reviewedByName: user.name,
    reviewedAt: new Date().toISOString(),
    reviewNote: String(body.note || 'Đã duyệt').trim(),
  });
  database.save('stocktakes', 'batches', 'movements');
  database.log(user, 'APPROVE_STOCKTAKE', 'STOCKTAKE', stocktake.code, `Duyệt phiếu kiểm kê ${stocktake.code}`);
  return stocktake;
});

route('POST', '/stocktakes/:id/reject', admin, ({ params, body, data, user }) => {
  const stocktake = findStocktake(data, params.id);
  if (stocktake.status !== 'PENDING_APPROVAL') throw conflict('Phiếu kiểm kê không ở trạng thái chờ duyệt.');
  requireFields(body, { note: 'Lý do từ chối' });
  Object.assign(stocktake, {
    status: 'REJECTED',
    reviewedById: user.id,
    reviewedByName: user.name,
    reviewedAt: new Date().toISOString(),
    reviewNote: body.note.trim(),
  });
  database.save('stocktakes');
  database.log(user, 'REJECT_STOCKTAKE', 'STOCKTAKE', stocktake.code, `Từ chối phiếu kiểm kê ${stocktake.code}`);
  return stocktake;
});
