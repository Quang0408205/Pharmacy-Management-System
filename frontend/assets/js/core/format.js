const currencyFormatter = new Intl.NumberFormat('vi-VN', {
  style: 'currency',
  currency: 'VND',
  maximumFractionDigits: 0,
});
const numberFormatter = new Intl.NumberFormat('vi-VN');
const compactFormatter = new Intl.NumberFormat('vi-VN', { notation: 'compact', maximumFractionDigits: 1 });

const pad = (value) => String(value).padStart(2, '0');

export const formatCurrency = (value) => currencyFormatter.format(Number(value) || 0);
export const formatNumber = (value) => numberFormatter.format(Number(value) || 0);
export const formatCompact = (value) => compactFormatter.format(Number(value) || 0);

export const toDateKey = (input) => {
  const date = input instanceof Date ? input : new Date(input);
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())}`;
};

export const todayKey = () => toDateKey(new Date());

export const addDays = (input, days) => {
  const date = new Date(input);
  date.setDate(date.getDate() + days);
  return date;
};

export const addDaysKey = (input, days) => toDateKey(addDays(input, days));

const parseDateKey = (key) => {
  const [year, month, day] = String(key).slice(0, 10).split('-').map(Number);
  return new Date(year, month - 1, day);
};

export const daysBetween = (fromKey, toKey) => Math.round((parseDateKey(toKey) - parseDateKey(fromKey)) / 86400000);

export const daysUntil = (key) => daysBetween(todayKey(), key);

export const formatDate = (input) => {
  if (!input) return '—';
  const date = typeof input === 'string' && input.length === 10 ? parseDateKey(input) : new Date(input);
  if (Number.isNaN(date.getTime())) return '—';
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}/${date.getFullYear()}`;
};

export const formatTime = (input) => {
  const date = new Date(input);
  return `${pad(date.getHours())}:${pad(date.getMinutes())}`;
};

export const formatDateTime = (input) => (input ? `${formatTime(input)} ${formatDate(input)}` : '—');

export const formatRelative = (input) => {
  const diff = Date.now() - new Date(input).getTime();
  const minutes = Math.floor(diff / 60000);
  if (minutes < 1) return 'Vừa xong';
  if (minutes < 60) return `${minutes} phút trước`;
  const hours = Math.floor(minutes / 60);
  if (hours < 24) return `${hours} giờ trước`;
  const days = Math.floor(hours / 24);
  if (days < 30) return `${days} ngày trước`;
  return formatDate(input);
};

export const greeting = () => {
  const hour = new Date().getHours();
  if (hour < 11) return 'Chào buổi sáng';
  if (hour < 14) return 'Chào buổi trưa';
  if (hour < 18) return 'Chào buổi chiều';
  return 'Chào buổi tối';
};

export const initials = (name) =>
  String(name || '?')
    .trim()
    .split(/\s+/)
    .slice(-2)
    .map((part) => part[0])
    .join('')
    .toUpperCase();

export const weekdayLabel = (input) => {
  const labels = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'];
  return labels[new Date(input).getDay()];
};

export const shortDate = (key) => {
  const date = parseDateKey(key);
  return `${pad(date.getDate())}/${pad(date.getMonth() + 1)}`;
};
