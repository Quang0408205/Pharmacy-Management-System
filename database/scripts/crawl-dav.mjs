import { mkdir, readFile, writeFile, access } from 'node:fs/promises';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

const here = dirname(fileURLToPath(import.meta.url));
const endpoint = 'https://dichvucong.dav.gov.vn/api/services/app/soDangKy/GetAllPublicServerPaging';
const pageSize = 100;
const maxPagesPerKeyword = Number(process.env.MAX_PAGES || 3);
const delayMs = Number(process.env.DELAY_MS || 1200);
const maxRetries = 5;
const runDate = process.env.RUN_DATE || new Date().toISOString().slice(0, 10);
const outputDirectory = resolve(here, '../raw_data/dav', runDate);
const userAgent = 'pharmacy-management-student-project/1.0 (drug registration lookup, low rate)';

const sleep = (milliseconds) => new Promise((resolveSleep) => setTimeout(resolveSleep, milliseconds));

const slugify = (value) =>
  value
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd')
    .replace(/[^a-zA-Z0-9]+/g, '-')
    .replace(/^-|-$/g, '')
    .toLowerCase();

const exists = async (path) => {
  try {
    await access(path);
    return true;
  } catch {
    return false;
  }
};

const requestPage = async (keyword, page) => {
  const payload = {
    filterText: keyword,
    SoDangKyThuoc: {},
    KichHoat: true,
    skipCount: page * pageSize,
    maxResultCount: pageSize,
    sorting: null,
  };
  for (let attempt = 1; attempt <= maxRetries; attempt += 1) {
    try {
      const response = await fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/json', Accept: 'application/json', 'User-Agent': userAgent },
        body: JSON.stringify(payload),
        signal: AbortSignal.timeout(60000),
      });
      if (response.status === 429 || response.status >= 500) throw new Error(`HTTP ${response.status}`);
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const body = await response.json();
      if (!body.success || !body.result) throw new Error('empty result');
      return { payload, body };
    } catch (error) {
      if (attempt === maxRetries) throw error;
      const backoff = delayMs * 2 ** attempt;
      process.stdout.write(`  retry ${attempt} for "${keyword}" page ${page}: ${error.message} (wait ${backoff}ms)\n`);
      await sleep(backoff);
    }
  }
  return null;
};

const ingredients = JSON.parse(await readFile(resolve(here, 'data/ingredients.json'), 'utf8'));
const keywords = [...new Set(ingredients.map((item) => item.ingredient))];
const only = process.argv.slice(2);
const selected = only.length ? keywords.filter((keyword) => only.includes(keyword)) : keywords;

await mkdir(outputDirectory, { recursive: true });
let requests = 0;
let skipped = 0;

for (const keyword of selected) {
  const slug = slugify(keyword);
  let total = Infinity;
  for (let page = 0; page < maxPagesPerKeyword && page * pageSize < total; page += 1) {
    const file = resolve(outputDirectory, `${slug}-p${page + 1}.json`);
    if (await exists(file)) {
      const saved = JSON.parse(await readFile(file, 'utf8'));
      total = saved.response.result.totalCount;
      skipped += 1;
      continue;
    }
    const { payload, body } = await requestPage(keyword, page);
    total = body.result.totalCount;
    const record = {
      meta: { source: 'DAV', endpoint, keyword, page: page + 1, fetchedAt: new Date().toISOString(), payload },
      response: body,
    };
    await writeFile(file, JSON.stringify(record));
    requests += 1;
    process.stdout.write(`${keyword} p${page + 1}: ${body.result.items.length}/${total}\n`);
    await sleep(delayMs);
  }
}

process.stdout.write(`done requests=${requests} skipped=${skipped} directory=${outputDirectory}\n`);
