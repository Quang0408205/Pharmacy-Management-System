import { readFile, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const seedRoot = resolve(here, '../seed_data');
const medicines = JSON.parse(await readFile(resolve(seedRoot, 'thuoc.json'), 'utf8'));
const suppliers = JSON.parse(await readFile(resolve(seedRoot, 'nha_cung_cap.json'), 'utf8'));

const RECEIPT_COUNT = 10;
const MEDICINES_PER_CATEGORY = 20;
const SECOND_BATCH_RATE = 0.3;
const EXPIRING_BATCHES = 18;
const LOW_STOCK_MEDICINES = 15;
const BATCH_LETTERS = 'ABCDEFGHJKLMNPQRSTUVWXYZ';

function createRandom(seed) {
  let state = seed >>> 0;
  return () => {
    state = (state + 0x6d2b79f5) >>> 0;
    let value = state;
    value = Math.imul(value ^ (value >>> 15), value | 1);
    value ^= value + Math.imul(value ^ (value >>> 7), value | 61);
    return ((value ^ (value >>> 14)) >>> 0) / 4294967296;
  };
}

const random = createRandom(20261031);
const between = (min, max) => min + Math.floor(random() * (max - min + 1));
const shuffle = (items) => {
  const copy = [...items];
  for (let index = copy.length - 1; index > 0; index--) {
    const swap = Math.floor(random() * (index + 1));
    [copy[index], copy[swap]] = [copy[swap], copy[index]];
  }
  return copy;
};
const roundTo = (value, step) => Math.max(step, Math.round(value / step) * step);

const activeSuppliers = suppliers.filter((supplier) => supplier.trang_thai === 'HOAT_DONG');
const activeMedicines = medicines.filter((medicine) => medicine.trang_thai === 'DANG_KINH_DOANH');

const byCategory = new Map();
for (const medicine of activeMedicines) {
  if (!byCategory.has(medicine.id_danh_muc)) byCategory.set(medicine.id_danh_muc, []);
  byCategory.get(medicine.id_danh_muc).push(medicine);
}
const selected = [...byCategory.keys()]
  .sort()
  .flatMap((categoryId) => shuffle(byCategory.get(categoryId)).slice(0, MEDICINES_PER_CATEGORY));

const shuffledSelection = shuffle(selected);
const lowStockCodes = new Set(shuffledSelection.slice(0, LOW_STOCK_MEDICINES).map((medicine) => medicine.ma_thuoc));

const batches = [];
const usedBatchCodes = new Set();
const batchCodeFor = (medicine) => {
  let code;
  do {
    code = `${BATCH_LETTERS[between(0, BATCH_LETTERS.length - 1)]}${BATCH_LETTERS[between(0, BATCH_LETTERS.length - 1)]}${between(2024, 2026) % 100}${String(between(1, 9999)).padStart(4, '0')}`;
  } while (usedBatchCodes.has(`${medicine.ma_thuoc}|${code}`));
  usedBatchCodes.add(`${medicine.ma_thuoc}|${code}`);
  return code;
};

for (const medicine of selected) {
  const lowStock = lowStockCodes.has(medicine.ma_thuoc);
  const batchCount = !lowStock && random() < SECOND_BATCH_RATE ? 2 : 1;
  const minStock = medicine.muc_ton_toi_thieu || 20;
  for (let index = 0; index < batchCount; index++) {
    const quantity = lowStock
      ? between(Math.max(1, Math.floor(minStock * 0.2)), Math.max(2, Math.floor(minStock * 0.7)))
      : roundTo(minStock * (between(15, 45) / 10), 10);
    batches.push({
      medicine,
      quantity,
      expiresInDays: between(240, 900),
      receiptIndex: between(0, RECEIPT_COUNT - 1),
    });
  }
}

for (const batch of shuffle(batches.filter((item) => !lowStockCodes.has(item.medicine.ma_thuoc))).slice(
  0,
  EXPIRING_BATCHES,
)) {
  batch.expiresInDays = between(12, 80);
}

const receiptDays = Array.from({ length: RECEIPT_COUNT }, (_, index) => 95 - index * 9);
const receipts = receiptDays.map((daysAgo, index) => ({
  ma_nha_cung_cap: activeSuppliers[index % activeSuppliers.length].ma_nha_cung_cap,
  ngay_nhap_truoc_so_ngay: daysAgo,
  ghi_chu: 'Nhập hàng tồn kho ban đầu',
  chi_tiet_phieu_nhap: [],
}));

for (const batch of batches) {
  const receipt = receipts[batch.receiptIndex];
  const sameMedicine = receipt.chi_tiet_phieu_nhap.some((line) => line.ma_thuoc === batch.medicine.ma_thuoc);
  const target = sameMedicine ? receipts[(batch.receiptIndex + 1) % RECEIPT_COUNT] : receipt;
  target.chi_tiet_phieu_nhap.push({
    ma_thuoc: batch.medicine.ma_thuoc,
    so_lo: batchCodeFor(batch.medicine),
    san_xuat_truoc_ngay_nhap: between(20, 240),
    han_dung_sau_so_ngay: batch.expiresInDays,
    so_luong: batch.quantity,
    don_gia_nhap: batch.medicine.gia_nhap_tham_khao,
  });
}

for (const receipt of receipts) {
  receipt.chi_tiet_phieu_nhap.sort((left, right) => left.ma_thuoc.localeCompare(right.ma_thuoc));
}

await writeFile(resolve(seedRoot, 'phieu_nhap.json'), `${JSON.stringify(receipts, null, 2)}\n`, 'utf8');

const lineCount = receipts.reduce((total, receipt) => total + receipt.chi_tiet_phieu_nhap.length, 0);
console.log(
  `phieu_nhap.json: ${receipts.length} receipts, ${lineCount} batches, ${selected.length} medicines, ${lowStockCodes.size} low stock, ${EXPIRING_BATCHES} expiring`,
);
