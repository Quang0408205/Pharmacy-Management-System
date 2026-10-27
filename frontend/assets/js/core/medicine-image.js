const cache = new Map();

const hashOf = (text) => {
  let hash = 0;
  for (let index = 0; index < text.length; index += 1) hash = (hash * 31 + text.charCodeAt(index)) >>> 0;
  return hash;
};

const shapes = {
  tablet: (accent) =>
    `<circle cx="40" cy="48" r="17" fill="#fff" stroke="${accent}" stroke-width="3"/><path d="M26 48h28" stroke="${accent}" stroke-width="3"/><circle cx="72" cy="66" r="14" fill="#fff" stroke="${accent}" stroke-width="3"/><path d="M62 66h20" stroke="${accent}" stroke-width="3"/>`,
  capsule: (accent) =>
    `<g transform="rotate(-35 60 50)"><rect x="24" y="36" width="72" height="28" rx="14" fill="#fff" stroke="${accent}" stroke-width="3"/><path d="M60 36v28" stroke="${accent}" stroke-width="3"/><rect x="60" y="36" width="36" height="28" rx="14" fill="${accent}" opacity=".85"/></g>`,
  bottle: (accent) =>
    `<rect x="48" y="14" width="24" height="12" rx="3" fill="${accent}"/><path d="M50 26h20v8c10 4 14 10 14 20v32a6 6 0 0 1-6 6H42a6 6 0 0 1-6-6V54c0-10 4-16 14-20z" fill="#fff" stroke="${accent}" stroke-width="3"/><rect x="42" y="52" width="36" height="26" rx="3" fill="${accent}" opacity=".18"/><path d="M48 62h24M48 70h16" stroke="${accent}" stroke-width="3" stroke-linecap="round"/>`,
  tube: (accent) =>
    `<path d="M30 32h60l-6 56H36z" fill="#fff" stroke="${accent}" stroke-width="3" stroke-linejoin="round"/><rect x="38" y="16" width="44" height="16" rx="3" fill="${accent}"/><path d="M42 52h36M44 64h32" stroke="${accent}" stroke-width="3" stroke-linecap="round" opacity=".6"/>`,
  sachet: (accent) =>
    `<path d="M26 24h68v72H26z" fill="#fff" stroke="${accent}" stroke-width="3"/><path d="M26 34h68M26 86h68" stroke="${accent}" stroke-width="3" stroke-dasharray="4 4"/><path d="M48 56h24M48 68h16" stroke="${accent}" stroke-width="3" stroke-linecap="round"/>`,
  device: (accent) =>
    `<rect x="26" y="26" width="68" height="68" rx="12" fill="#fff" stroke="${accent}" stroke-width="3"/><circle cx="60" cy="56" r="16" fill="none" stroke="${accent}" stroke-width="3"/><path d="M60 46v10l7 5" stroke="${accent}" stroke-width="3" stroke-linecap="round" fill="none"/><path d="M42 82h36" stroke="${accent}" stroke-width="3" stroke-linecap="round"/>`,
  box: (accent) =>
    `<path d="M20 44 60 28l40 16v44L60 104 20 88z" fill="#fff" stroke="${accent}" stroke-width="3" stroke-linejoin="round"/><path d="M20 44l40 16 40-16M60 60v44" stroke="${accent}" stroke-width="3" fill="none"/><rect x="66" y="68" width="22" height="12" rx="3" fill="${accent}" opacity=".35" transform="rotate(14 66 68)"/>`,
};

const shapeFor = (form, unit) => {
  const text = `${form} ${unit}`.toLowerCase();
  if (/thiết bị|cái|que thử|băng|bông|khẩu trang/.test(text)) return 'device';
  if (/kem|gel|mỡ|tuýp|dầu gội/.test(text)) return 'tube';
  if (/siro|dung dịch|nhỏ|xịt|tiêm|chai|lọ|hỗn dịch uống/.test(text)) return 'bottle';
  if (/cốm|bột|gói/.test(text)) return 'sachet';
  if (/nang/.test(text)) return 'capsule';
  if (/viên|nén|sủi|ngậm/.test(text)) return 'tablet';
  return 'box';
};

export const medicineImage = (medicine) => {
  if (medicine.imageUrl) return medicine.imageUrl;
  const key = `${medicine.dosageForm}|${medicine.unit}|${medicine.name}`;
  if (cache.has(key)) return cache.get(key);
  const hue = hashOf(medicine.genericName || medicine.name) % 360;
  const accent = `hsl(${hue} 55% 38%)`;
  const background = `hsl(${hue} 70% 94%)`;
  const shape = shapes[shapeFor(medicine.dosageForm || '', medicine.unit || '')](accent);
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120"><rect width="120" height="120" fill="${background}"/>${shape}</svg>`;
  const uri = `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
  cache.set(key, uri);
  return uri;
};
