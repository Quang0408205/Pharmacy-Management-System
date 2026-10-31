import { mkdir, readdir, readFile, writeFile } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const rawRoot = resolve(here, '../raw_data/dav');
const outputRoot = resolve(here, '../seed_data');
const targetCount = Number(process.env.TARGET || 2000);
const sourceUrl = 'https://dichvucong.dav.gov.vn/congbothuoc/index';
const excludedForms = /tiêm|truyền|đông khô|cấy ghép/i;
const salesUnits = new Set(['Hộp', 'Chai', 'Lọ', 'Tuýp', 'Vỉ', 'Gói', 'Ống']);

const normalizeText = (value) =>
  String(value ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'd')
    .toLowerCase()
    .replace(/\s+/g, ' ')
    .trim();

const hashOf = (text) => {
  let hash = 2166136261;
  for (const char of text) {
    hash ^= char.charCodeAt(0);
    hash = Math.imul(hash, 16777619) >>> 0;
  }
  return hash >>> 0;
};

const unitInterval = (text, salt) => (hashOf(`${salt}:${text}`) % 10000) / 10000;
const roundTo = (value, step) => Math.round(value / step) * step;
const pad = (value, size) => String(value).padStart(size, '0');

const latestRunDirectory = async () => {
  const runs = (await readdir(rawRoot, { withFileTypes: true }))
    .filter((entry) => entry.isDirectory())
    .map((entry) => entry.name)
    .sort();
  if (!runs.length) throw new Error('No raw data found. Run crawl-dav.mjs first.');
  return resolve(rawRoot, process.env.RUN_DATE || runs.at(-1));
};

const ingredients = JSON.parse(await readFile(resolve(here, 'data/ingredients.json'), 'utf8'));
const categories = JSON.parse(await readFile(resolve(here, 'data/categories.json'), 'utf8'));
const categoryIds = Object.fromEntries(
  categories.map((category, index) => [category.code, `cat-${pad(index + 1, 2)}`]),
);

const stemOf = (word) => word.slice(0, 8);

const ingredientParts = (name) =>
  normalizeText(String(name ?? ''))
    .replace(/moi [^:]*(chua|co)\s*:?/g, ' ')
    .replace(/\([^)]*\)/g, ' ')
    .replace(/(tuong duong|duoi dang)[^,;+]*/g, ' ')
    .replace(/\d[\d.,]*\s*(mg|mcg|µg|ug|g|ml|%|iu|ui|kl\/tt)?/g, ' ')
    .split(/\s*(?:\+|,|;|\/| va )\s*/)
    .map((part) => part.trim().split(/\s+/)[0])
    .filter((word) => word && word.length > 2)
    .map(stemOf)
    .sort();

const ingredientByKey = new Map(
  ingredients.map((entry) => [[...new Set(ingredientParts(entry.ingredient))].join('|'), entry]),
);

const stripParentheses = (text) =>
  String(text ?? '')
    .replace(/\([^)]*\)/g, '')
    .replace(/\s+/g, ' ')
    .trim();

const salesUnitOf = (packaging, dosageForm) => {
  const first = String(packaging ?? '')
    .trim()
    .split(/\s+/)[0];
  const capitalized = first ? first.charAt(0).toUpperCase() + first.slice(1).toLowerCase() : '';
  if (salesUnits.has(capitalized)) return capitalized;
  const form = normalizeText(dosageForm);
  if (/siro|dung dich|hon dich|nhu dich/.test(form)) return 'Chai';
  if (/kem|gel|mo boi/.test(form)) return 'Tuýp';
  if (/nho|xit/.test(form)) return 'Lọ';
  if (/bot|com|cap/.test(form)) return 'Gói';
  return 'Hộp';
};

const expiresInFuture = (item, now) => {
  const expiry = item.thongTinDangKyThuoc?.ngayHetHanSoDangKy;
  return !expiry || new Date(expiry) > now;
};

const directory = await latestRunDirectory();
const files = (await readdir(directory)).filter((name) => name.endsWith('.json')).sort();
const now = new Date();
const bySoDangKy = new Map();
const rejected = { duplicate: 0, expired: 0, form: 0, combination: 0, missing: 0 };

for (const file of files) {
  const record = JSON.parse(await readFile(resolve(directory, file), 'utf8'));
  for (const item of record.response.result.items) {
    const basic = item.thongTinThuocCoBan ?? {};
    if (!item.soDangKy || !item.tenThuoc || !basic.hoatChatChinh) {
      rejected.missing += 1;
      continue;
    }
    if (item.isHetHan || item.isDeleted || item.isActive === false || !expiresInFuture(item, now)) {
      rejected.expired += 1;
      continue;
    }
    if (excludedForms.test(basic.dangBaoChe ?? '')) {
      rejected.form += 1;
      continue;
    }
    const entry = ingredientByKey.get([...new Set(ingredientParts(basic.hoatChatChinh))].join('|'));
    if (!entry) {
      rejected.combination += 1;
      continue;
    }
    if (bySoDangKy.has(item.soDangKy)) {
      rejected.duplicate += 1;
      continue;
    }
    bySoDangKy.set(item.soDangKy, { item, entry, fetchedAt: record.meta.fetchedAt });
  }
}

const rank = ({ item }) => {
  const domestic = normalizeText(item.congTySanXuat?.nuocSanXuat) === 'viet nam' ? 0 : 1;
  const expiry = new Date(item.thongTinDangKyThuoc?.ngayHetHanSoDangKy ?? '2100-01-01').getTime();
  return [domestic, -expiry, hashOf(item.soDangKy)];
};

const groups = new Map();
for (const candidate of bySoDangKy.values()) {
  const key = candidate.entry.ingredient;
  if (!groups.has(key)) groups.set(key, []);
  groups.get(key).push(candidate);
}
for (const list of groups.values()) {
  list.sort((a, b) => {
    const left = rank(a);
    const right = rank(b);
    return left[0] - right[0] || left[1] - right[1] || left[2] - right[2];
  });
}

const selected = [];
const queues = [...groups.entries()].sort(([a], [b]) => a.localeCompare(b));
for (let round = 0; selected.length < targetCount; round += 1) {
  let progressed = false;
  for (const [, list] of queues) {
    if (selected.length >= targetCount) break;
    if (round < list.length) {
      selected.push(list[round]);
      progressed = true;
    }
  }
  if (!progressed) break;
}

selected.sort(
  (a, b) =>
    categoryIds[a.entry.categoryCode].localeCompare(categoryIds[b.entry.categoryCode]) ||
    a.entry.ingredient.localeCompare(b.entry.ingredient, 'vi') ||
    a.item.tenThuoc.localeCompare(b.item.tenThuoc, 'vi'),
);

const strengthFactor = (entry, strength) => {
  const index = entry.strengths.findIndex((candidate) => normalizeText(candidate) === normalizeText(strength));
  return 1 + Math.max(index, 0) * 0.38;
};

const medicines = selected.map(({ item, entry, fetchedAt }, index) => {
  const basic = item.thongTinThuocCoBan;
  const maker = stripParentheses(item.congTySanXuat?.tenCongTySanXuat)
    ?.replace(/^[^\p{L}\p{N}]+/u, '')
    .trim();
  const country = item.congTySanXuat?.nuocSanXuat ?? null;
  const domestic = normalizeText(country) === 'viet nam';
  const costPrice = Math.max(
    1000,
    roundTo(
      entry.basePrice *
        strengthFactor(entry, basic.hamLuong) *
        (domestic ? 0.7 : 1.1) *
        (0.85 + unitInterval(item.soDangKy, 'cost') * 0.3) *
        0.7,
      100,
    ),
  );
  const price = Math.max(2000, roundTo(costPrice * (1.22 + unitInterval(item.soDangKy, 'margin') * 0.18), 500));
  const serial = index + 1;
  const strengthText = basic.hamLuong ? ` ${basic.hamLuong}` : '';
  const formText = basic.dangBaoChe ? ` dạng ${basic.dangBaoChe.toLowerCase()}` : '';
  return {
    _id: `med-${pad(serial, 4)}`,
    ma_thuoc: `T${pad(serial, 4)}`,
    ma_vach: null,
    ten_thuoc: item.tenThuoc.trim(),
    ten_hoat_chat: basic.hoatChatChinh.trim(),
    ham_luong: basic.hamLuong ?? '',
    dang_bao_che: basic.dangBaoChe ?? '',
    id_danh_muc: categoryIds[entry.categoryCode],
    nha_san_xuat: maker || '',
    nuoc_san_xuat: country ?? '',
    so_dang_ky: item.soDangKy,
    don_vi_ban: salesUnitOf(basic.dongGoi, basic.dangBaoChe),
    quy_cach_dong_goi: basic.dongGoi ?? '',
    quy_doi_don_vi: [],
    gia_ban: price,
    gia_nhap_tham_khao: costPrice,
    loai_ke_don: { OTC: 'KHONG_KE_DON', RX: 'KE_DON', CONTROLLED: 'KIEM_SOAT_DAC_BIET' }[entry.prescriptionType],
    muc_ton_toi_thieu: [10, 15, 20, 20, 30, 40, 50][hashOf(item.soDangKy) % 7],
    duong_dan_anh: null,
    mo_ta: `${basic.hoatChatChinh}${strengthText}${formText}${maker ? `, sản xuất bởi ${maker}` : ''}${country ? ` (${country})` : ''}. Số đăng ký lưu hành: ${item.soDangKy}.`,
    nguon_du_lieu: {
      loai_nguon: 'DAV',
      duong_dan_nguon: sourceUrl,
      ma_ban_ghi_nguon: String(item.id ?? ''),
      thoi_diem_thu_thap: fetchedAt,
      da_xac_minh: true,
    },
    trang_thai: unitInterval(item.soDangKy, 'status') < 0.045 ? 'NGUNG_KINH_DOANH' : 'DANG_KINH_DOANH',
  };
});

const devices = JSON.parse(await readFile(resolve(here, 'data/devices.json'), 'utf8'));
const deviceCategoryId = categoryIds.DCYT;
const sampleCollectedAt = new Date().toISOString();

devices.forEach((device) => {
  const serial = medicines.length + 1;
  const costPrice = roundTo(device.price * 0.75, 100);
  medicines.push({
    _id: `med-${pad(serial, 4)}`,
    ma_thuoc: `T${pad(serial, 4)}`,
    ma_vach: null,
    ten_thuoc: device.name,
    ten_hoat_chat: device.name,
    ham_luong: '',
    dang_bao_che: device.form,
    id_danh_muc: deviceCategoryId,
    nha_san_xuat: '',
    nuoc_san_xuat: '',
    so_dang_ky: null,
    don_vi_ban: device.unit,
    quy_cach_dong_goi: device.packaging,
    quy_doi_don_vi: [],
    gia_ban: device.price,
    gia_nhap_tham_khao: costPrice,
    loai_ke_don: 'KHONG_KE_DON',
    muc_ton_toi_thieu: [5, 10, 10, 15, 20][hashOf(device.name) % 5],
    duong_dan_anh: null,
    mo_ta: `${device.name}. Sản phẩm mẫu phục vụ demo, không phải dữ liệu đăng ký lưu hành.`,
    nguon_du_lieu: {
      loai_nguon: 'DU_LIEU_MAU',
      duong_dan_nguon: null,
      ma_ban_ghi_nguon: null,
      thoi_diem_thu_thap: sampleCollectedAt,
      da_xac_minh: false,
    },
    trang_thai: 'DANG_KINH_DOANH',
  });
});

const categoryDocuments = categories.map((category, index) => ({
  _id: `cat-${pad(index + 1, 2)}`,
  ma_danh_muc: category.code,
  ten_danh_muc: category.name,
  mo_ta: `Nhóm ${category.name.toLowerCase()} tại nhà thuốc`,
  mau_hien_thi: category.color,
  trang_thai: 'HOAT_DONG',
}));

await mkdir(outputRoot, { recursive: true });
await writeFile(resolve(outputRoot, 'danh_muc.json'), `${JSON.stringify(categoryDocuments, null, 2)}\n`);
await writeFile(resolve(outputRoot, 'thuoc.json'), `${JSON.stringify(medicines, null, 2)}\n`);

const perCategory = {};
medicines.forEach((medicine) => {
  perCategory[medicine.id_danh_muc] = (perCategory[medicine.id_danh_muc] || 0) + 1;
});
process.stdout.write(
  `${JSON.stringify({ rawFiles: files.length, candidates: bySoDangKy.size, selected: medicines.length, rejected, perCategory }, null, 2)}\n`,
);
