import { database } from './database.js';
import { addDaysKey, todayKey, toDateKey } from '../core/format.js';
import { badRequest } from '../core/errors.js';

export const batchStatusOf = (batch, settings, today = todayKey()) => {
  if (batch.expiryDate < today) return 'EXPIRED';
  if (batch.remainingQty <= 0) return 'OUT_OF_STOCK';
  if (batch.expiryDate <= addDaysKey(today, settings.expiryWarningDays)) return 'EXPIRING_SOON';
  if (batch.remainingQty <= batch.initialQty * 0.2) return 'LOW_STOCK';
  return 'AVAILABLE';
};

export const decorateBatch = (batch) => {
  const { data } = database;
  const medicine = data.medicines.find((item) => item.id === batch.medicineId);
  const supplier = data.suppliers.find((item) => item.id === batch.supplierId);
  const receipt = data.receipts.find((item) => item.id === batch.receiptId);
  return {
    ...batch,
    status: batchStatusOf(batch, data.settings),
    medicineCode: medicine ? medicine.code : '',
    medicineName: medicine ? medicine.name : '',
    unit: medicine ? medicine.unit : '',
    supplierName: supplier ? supplier.name : '',
    receiptCode: receipt ? receipt.code : '',
  };
};

export const buildStockIndex = () => {
  const { data } = database;
  const today = todayKey();
  const index = new Map();
  data.medicines.forEach((medicine) => {
    index.set(medicine.id, {
      totalQty: 0,
      availableQty: 0,
      expiredQty: 0,
      batchCount: 0,
      nearestExpiry: null,
      expiringQty: 0,
    });
  });
  const warningKey = addDaysKey(today, data.settings.expiryWarningDays);
  data.batches.forEach((batch) => {
    const entry = index.get(batch.medicineId);
    if (!entry || batch.remainingQty <= 0) return;
    entry.totalQty += batch.remainingQty;
    entry.batchCount += 1;
    if (batch.expiryDate < today) {
      entry.expiredQty += batch.remainingQty;
      return;
    }
    entry.availableQty += batch.remainingQty;
    if (!entry.nearestExpiry || batch.expiryDate < entry.nearestExpiry) entry.nearestExpiry = batch.expiryDate;
    if (batch.expiryDate <= warningKey) entry.expiringQty += batch.remainingQty;
  });
  return index;
};

export const stockStatusOf = (medicine, stock, settings) => {
  if (stock.availableQty <= 0) return 'OUT_OF_STOCK';
  if (stock.availableQty <= medicine.minStock * settings.lowStockMultiplier) return 'LOW_STOCK';
  return 'IN_STOCK';
};

export const decorateMedicine = (medicine, index) => {
  const { data } = database;
  const stock = index.get(medicine.id);
  const category = data.categories.find((item) => item.id === medicine.categoryId);
  return {
    ...medicine,
    categoryName: category ? category.name : '',
    totalQty: stock.totalQty,
    availableQty: stock.availableQty,
    expiredQty: stock.expiredQty,
    batchCount: stock.batchCount,
    nearestExpiry: stock.nearestExpiry,
    stockStatus: stockStatusOf(medicine, stock, data.settings),
  };
};

export const recordMovement = (entry) => {
  const { data } = database;
  data.movements.push({
    id: database.nextId('mov', 'movements', 5),
    createdAt: new Date().toISOString(),
    ...entry,
  });
};

export const planFefo = (medicineId, quantity, at = new Date()) => {
  const { data } = database;
  const atKey = toDateKey(at);
  const usable = data.batches
    .filter((batch) => batch.medicineId === medicineId && batch.remainingQty > 0 && batch.expiryDate >= atKey)
    .sort((a, b) => a.expiryDate.localeCompare(b.expiryDate) || a.receivedDate.localeCompare(b.receivedDate));
  const available = usable.reduce((sum, batch) => sum + batch.remainingQty, 0);
  if (available < quantity) return { ok: false, available, allocations: [] };
  const allocations = [];
  let needed = quantity;
  for (const batch of usable) {
    if (needed === 0) break;
    const take = Math.min(batch.remainingQty, needed);
    allocations.push({ batch, quantity: take });
    needed -= take;
  }
  return { ok: true, available, allocations };
};

export const requireFields = (body, rules) => {
  const fieldErrors = {};
  Object.entries(rules).forEach(([field, label]) => {
    const value = body[field];
    if (value === undefined || value === null || String(value).trim() === '')
      fieldErrors[field] = `${label} là bắt buộc.`;
  });
  if (Object.keys(fieldErrors).length)
    throw badRequest('Dữ liệu chưa hợp lệ. Vui lòng kiểm tra lại các trường đánh dấu.', fieldErrors);
};

export const publicUser = (employee) => ({
  id: employee.id,
  name: employee.name,
  email: employee.email,
  username: employee.username,
  role: employee.role,
  phone: employee.phone,
  status: employee.status,
});
