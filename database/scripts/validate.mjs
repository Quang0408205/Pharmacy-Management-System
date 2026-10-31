import { readFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const seedRoot = resolve(here, '../seed_data');
const medicines = JSON.parse(await readFile(resolve(seedRoot, 'thuoc.json'), 'utf8'));
const categories = JSON.parse(await readFile(resolve(seedRoot, 'danh_muc.json'), 'utf8'));

const prescriptionTypes = new Set(['KHONG_KE_DON', 'KE_DON', 'KIEM_SOAT_DAC_BIET']);
const statuses = new Set(['DANG_KINH_DOANH', 'NGUNG_KINH_DOANH']);
const requiredText = ['_id', 'ma_thuoc', 'ten_thuoc', 'ten_hoat_chat', 'id_danh_muc', 'don_vi_ban'];
const categoryIds = new Set(categories.map((category) => category._id));
const problems = [];

const seen = { _id: new Set(), ma_thuoc: new Set(), so_dang_ky: new Set() };
const sampleTypes = new Set(['DU_LIEU_MAU']);
for (const medicine of medicines) {
  for (const field of requiredText) {
    if (typeof medicine[field] !== 'string' || !medicine[field].trim())
      problems.push(`${medicine._id}: missing ${field}`);
  }
  for (const field of Object.keys(seen)) {
    if (medicine[field] === null) continue;
    if (seen[field].has(medicine[field])) problems.push(`${medicine._id}: duplicate ${field} ${medicine[field]}`);
    seen[field].add(medicine[field]);
  }
  if (!categoryIds.has(medicine.id_danh_muc))
    problems.push(`${medicine._id}: unknown category ${medicine.id_danh_muc}`);
  if (!prescriptionTypes.has(medicine.loai_ke_don)) problems.push(`${medicine._id}: bad loai_ke_don`);
  if (!statuses.has(medicine.trang_thai)) problems.push(`${medicine._id}: bad trang_thai`);
  if (!Number.isInteger(medicine.gia_ban) || medicine.gia_ban <= 0) problems.push(`${medicine._id}: bad gia_ban`);
  if (!Number.isInteger(medicine.gia_nhap_tham_khao) || medicine.gia_nhap_tham_khao <= 0)
    problems.push(`${medicine._id}: bad gia_nhap_tham_khao`);
  if (medicine.gia_ban <= medicine.gia_nhap_tham_khao) problems.push(`${medicine._id}: price not above cost`);
  const sample = sampleTypes.has(medicine.nguon_du_lieu?.loai_nguon);
  if (sample && (medicine.so_dang_ky !== null || medicine.nguon_du_lieu.da_xac_minh))
    problems.push(`${medicine._id}: sample data must not carry a registration number or verified flag`);
  if (!sample && (!medicine.so_dang_ky || !medicine.nguon_du_lieu?.da_xac_minh))
    problems.push(`${medicine._id}: official data must have a registration number and be verified`);
}

const usedCategories = new Set(medicines.map((medicine) => medicine.id_danh_muc));
const emptyCategories = categories.filter((category) => !usedCategories.has(category._id)).map((c) => c.ma_danh_muc);
const sampleCount = medicines.filter((m) => sampleTypes.has(m.nguon_du_lieu?.loai_nguon)).length;
const bySellable = medicines.filter((m) => m.trang_thai === 'DANG_KINH_DOANH').length;
const byPrescription = {};
medicines.forEach((m) => {
  byPrescription[m.loai_ke_don] = (byPrescription[m.loai_ke_don] || 0) + 1;
});

process.stdout.write(
  `${JSON.stringify({ medicines: medicines.length, sample: sampleCount, selling: bySellable, byPrescription, emptyCategories, problems: problems.length }, null, 2)}\n`,
);
if (problems.length) {
  process.stdout.write(`${problems.slice(0, 20).join('\n')}\n`);
  process.exit(1);
}
