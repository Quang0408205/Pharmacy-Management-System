import { toDateKey } from '../core/format.js';

export const normalizeText = (value) =>
  String(value ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .replace(/đ/g, 'd')
    .replace(/Đ/g, 'd')
    .toLowerCase();

export const matchesQuery = (fields, query) => {
  const needle = normalizeText(query).trim();
  if (!needle) return true;
  return fields.some((field) => normalizeText(field).includes(needle));
};

const readPath = (item, path) =>
  path.split('.').reduce((value, key) => (value === null || value === undefined ? value : value[key]), item);

export const sortList = (list, sortParam, allowed, fallback) => {
  const [rawField, rawDirection] = String(sortParam || fallback || '').split(',');
  const field = allowed.includes(rawField) ? rawField : fallback.split(',')[0];
  const direction = (allowed.includes(rawField) ? rawDirection : fallback.split(',')[1]) === 'desc' ? -1 : 1;
  return [...list].sort((a, b) => {
    const left = readPath(a, field);
    const right = readPath(b, field);
    if (typeof left === 'number' && typeof right === 'number') return (left - right) * direction;
    return String(left ?? '').localeCompare(String(right ?? ''), 'vi', { numeric: true }) * direction;
  });
};

export const paginate = (list, query) => {
  const size = Math.min(Math.max(Number(query.size) || 10, 1), 200);
  const totalPages = Math.max(1, Math.ceil(list.length / size));
  const page = Math.min(Math.max(Number(query.page) || 1, 1), totalPages);
  return {
    items: list.slice((page - 1) * size, page * size),
    page,
    size,
    total: list.length,
    totalPages,
  };
};

export const respondList = (list, query, { sortable = [], defaultSort = 'id,asc' } = {}) =>
  paginate(sortList(list, query.sort, sortable, defaultSort), query);

export const inDateRange = (value, from, to) => {
  const key = value.length === 10 ? value : toDateKey(value);
  if (from && key < from) return false;
  if (to && key > to) return false;
  return true;
};
