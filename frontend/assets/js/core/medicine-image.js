const cache = new Map();

const hashOf = (text) => {
  let hash = 0;
  for (let index = 0; index < text.length; index += 1) hash = (hash * 31 + text.charCodeAt(index)) >>> 0;
  return hash;
};

const pill = (x, y, fill, stroke) =>
  `<ellipse cx="${x}" cy="${y}" rx="8" ry="6.5" fill="${fill}" stroke="${stroke}" stroke-width="1.6"/><path d="M${x - 5} ${y}h10" stroke="${stroke}" stroke-width="1.2" opacity=".5"/>`;

const capsule = (x, y, accent, light, stroke) =>
  `<g transform="rotate(-30 ${x} ${y})"><rect x="${x - 11}" y="${y - 5.5}" width="22" height="11" rx="5.5" fill="${light}" stroke="${stroke}" stroke-width="1.6"/><path d="M${x} ${y - 5.5}a5.5 5.5 0 0 1 5.5 5.5 5.5 5.5 0 0 1-5.5 5.5z" fill="${accent}"/><rect x="${x}" y="${y - 5.5}" width="11" height="11" rx="5.5" fill="${accent}" stroke="${stroke}" stroke-width="1.6"/></g>`;

const blister = (accent, light, stroke, content) =>
  `<rect x="14" y="28" width="92" height="66" rx="10" fill="#e8eef2" stroke="${stroke}" stroke-width="2"/><rect x="20" y="34" width="80" height="54" rx="7" fill="#fff" opacity=".9"/>${content}<rect x="14" y="94" width="92" height="9" rx="4" fill="${accent}" opacity=".85"/>`;

const shapes = {
  tablet: (accent, light, stroke) =>
    blister(
      accent,
      light,
      stroke,
      [42, 62, 82, 102]
        .flatMap((x, column) => [
          pill(x - 14 + 6, 50, light, stroke),
          pill(x - 14 + 6, 72, column % 2 ? light : '#fff', stroke),
        ])
        .join(''),
    ),
  capsule: (accent, light, stroke) =>
    blister(
      accent,
      light,
      stroke,
      [34, 60, 86]
        .flatMap((x) => [capsule(x, 52, accent, light, stroke), capsule(x, 74, accent, light, stroke)])
        .join(''),
    ),
  bottle: (accent, light, stroke) =>
    `<rect x="46" y="12" width="28" height="14" rx="4" fill="${stroke}"/><rect x="50" y="26" width="20" height="10" fill="#fff" stroke="${stroke}" stroke-width="2"/><path d="M50 36c-12 4-18 10-18 22v38a8 8 0 0 0 8 8h40a8 8 0 0 0 8-8V58c0-12-6-18-18-22z" fill="${accent}" opacity=".9" stroke="${stroke}" stroke-width="2"/><rect x="36" y="58" width="48" height="32" rx="4" fill="#fff"/><path d="M60 64v20M50 74h20" stroke="${accent}" stroke-width="4" stroke-linecap="round"/>`,
  dropper: (accent, light, stroke) =>
    `<path d="M54 10h12v10H54z" fill="${stroke}"/><path d="M57 20h6v16h-6z" fill="#fff" stroke="${stroke}" stroke-width="2"/><path d="M48 36h24l8 12v42a8 8 0 0 1-8 8H48a8 8 0 0 1-8-8V48z" fill="${light}" stroke="${stroke}" stroke-width="2"/><rect x="46" y="58" width="28" height="26" rx="3" fill="#fff"/><path d="M60 62c-5 7-7 11-7 15a7 7 0 0 0 14 0c0-4-2-8-7-15z" fill="${accent}"/>`,
  spray: (accent, light, stroke) =>
    `<path d="M50 16h16l8 6H56z" fill="${stroke}"/><rect x="54" y="22" width="10" height="12" fill="#fff" stroke="${stroke}" stroke-width="2"/><rect x="38" y="34" width="44" height="64" rx="10" fill="${light}" stroke="${stroke}" stroke-width="2"/><rect x="44" y="52" width="32" height="30" rx="3" fill="#fff"/><path d="M60 58v18M51 67h18" stroke="${accent}" stroke-width="4" stroke-linecap="round"/><path d="M84 18l12-4M86 26l14 0M84 34l12 4" stroke="${accent}" stroke-width="3" stroke-linecap="round"/>`,
  tube: (accent, light, stroke) =>
    `<path d="M24 34h72l-8 62H32z" fill="${accent}" opacity=".92" stroke="${stroke}" stroke-width="2" stroke-linejoin="round"/><rect x="24" y="22" width="72" height="14" rx="3" fill="${stroke}"/><path d="M44 30h32" stroke="#fff" stroke-width="2" opacity=".5"/><rect x="38" y="52" width="44" height="28" rx="4" fill="#fff"/><path d="M60 58v16M52 66h16" stroke="${accent}" stroke-width="4" stroke-linecap="round"/><path d="M34 104c8-6 16 6 26 0s18 6 26 0" stroke="${accent}" stroke-width="4" fill="none" stroke-linecap="round"/>`,
  sachet: (accent, light, stroke) =>
    `<path d="M26 22h68v78H26z" fill="#fff" stroke="${stroke}" stroke-width="2"/><path d="M26 22l4 4-4 4 4 4-4 4M94 22l-4 4 4 4-4 4 4 4" fill="none" stroke="${stroke}" stroke-width="2"/><rect x="26" y="46" width="68" height="30" fill="${accent}" opacity=".9"/><path d="M60 52v18M51 61h18" stroke="#fff" stroke-width="4" stroke-linecap="round"/><path d="M26 92l4-4-4-4 4-4-4-4M94 92l-4-4 4-4-4-4 4-4" fill="none" stroke="${stroke}" stroke-width="2"/>`,
  ampoule: (accent, light, stroke) =>
    `<path d="M52 14h16l-2 14H54z" fill="#fff" stroke="${stroke}" stroke-width="2"/><path d="M54 28h12c6 8 8 14 8 24v36a14 14 0 0 1-28 0V52c0-10 2-16 8-24z" fill="${light}" stroke="${stroke}" stroke-width="2"/><rect x="46" y="62" width="28" height="22" rx="3" fill="#fff" opacity=".9"/><path d="M60 66v14M53 73h14" stroke="${accent}" stroke-width="3" stroke-linecap="round"/>`,
  device: (accent, light, stroke) =>
    `<rect x="30" y="16" width="60" height="88" rx="14" fill="#fff" stroke="${stroke}" stroke-width="2"/><rect x="40" y="26" width="40" height="30" rx="6" fill="${light}" stroke="${stroke}" stroke-width="1.6"/><path d="M46 44l7-8 6 6 8-10 6 12" fill="none" stroke="${accent}" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/><circle cx="60" cy="76" r="10" fill="${accent}"/><path d="M60 71v10M55 76h10" stroke="#fff" stroke-width="3" stroke-linecap="round"/><path d="M42 94h36" stroke="${stroke}" stroke-width="3" stroke-linecap="round"/>`,
  box: (accent, light, stroke) =>
    `<path d="M18 42 60 26l42 16v46L60 106 18 88z" fill="${light}" stroke="${stroke}" stroke-width="2" stroke-linejoin="round"/><path d="M18 42l42 16 42-16M60 58v48" stroke="${stroke}" stroke-width="2" fill="none"/><path d="M60 58l42-16v46L60 106z" fill="${accent}" opacity=".35"/><rect x="26" y="62" width="26" height="18" rx="3" fill="#fff" transform="rotate(14 26 62)"/><path d="M36 68v8M32 72h8" stroke="${accent}" stroke-width="3" stroke-linecap="round" transform="rotate(14 26 62)"/>`,
};

const rules = [
  ['device', /thiết bị|vật tư|que thử|khẩu trang|băng |bông |nhiệt kế|máy |găng/],
  ['tube', /kem|gel|thuốc mỡ|mỡ bôi|tuýp|dầu xoa|cao xoa/],
  ['spray', /xịt|phun mù|khí dung/],
  ['dropper', /nhỏ mắt|nhỏ mũi|nhỏ tai|thuốc nhỏ|lọ nhỏ giọt/],
  ['ampoule', /tiêm|ống uống|(^|\s)ống(\s|$)|truyền/],
  ['bottle', /siro|sirô|dung dịch|hỗn dịch uống|chai|lọ|cồn|sát khuẩn|nước/],
  ['sachet', /cốm|bột|gói/],
  ['capsule', /nang/],
  ['tablet', /viên|nén|vỉ|sủi|nhai|ngậm|đạn/],
];

const shapeFor = (medicine) => {
  const form = (medicine.dosageForm || '').toLowerCase();
  const detail = `${form} ${medicine.packaging || ''} ${medicine.unit || ''} ${medicine.name || ''}`.toLowerCase();
  const primary = rules.find(([, pattern]) => pattern.test(form));
  if (primary) return primary[0];
  const secondary = rules.find(([, pattern]) => pattern.test(detail));
  return secondary ? secondary[0] : 'box';
};

export const medicineImage = (medicine) => {
  if (medicine.imageUrl) return medicine.imageUrl;
  const shape = shapeFor(medicine);
  const key = `${shape}|${medicine.genericName || medicine.name}`;
  if (cache.has(key)) return cache.get(key);
  const hue = hashOf(medicine.genericName || medicine.name || '') % 360;
  const accent = `hsl(${hue} 58% 42%)`;
  const light = `hsl(${hue} 80% 90%)`;
  const stroke = `hsl(${hue} 45% 28%)`;
  const background = `hsl(${hue} 70% 96%)`;
  const svg = `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 120 120"><rect width="120" height="120" fill="${background}"/><circle cx="104" cy="16" r="20" fill="${light}" opacity=".7"/><circle cx="12" cy="108" r="26" fill="${light}" opacity=".6"/>${shapes[shape](accent, light, stroke)}</svg>`;
  const uri = `data:image/svg+xml;utf8,${encodeURIComponent(svg)}`;
  cache.set(key, uri);
  return uri;
};
