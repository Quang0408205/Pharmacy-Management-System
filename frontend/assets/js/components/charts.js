import { html } from '../core/html.js';
import { errorState } from './states.js';

let libraryPromise = null;
const registry = new Map();

const loadLibrary = () => {
  if (window.Chart) return Promise.resolve(window.Chart);
  if (!libraryPromise) {
    libraryPromise = new Promise((resolve, reject) => {
      const script = document.createElement('script');
      script.src = new URL('../../vendor/chart.umd.js', import.meta.url).href;
      script.onload = () => resolve(window.Chart);
      script.onerror = () => {
        libraryPromise = null;
        reject(new Error('Không tải được thư viện biểu đồ.'));
      };
      document.head.append(script);
    });
  }
  return libraryPromise;
};

export const cssVar = (name) => getComputedStyle(document.documentElement).getPropertyValue(name).trim();

export const chartTheme = () => ({
  text: cssVar('--color-text-secondary'),
  grid: cssVar('--color-border'),
  primary: cssVar('--color-primary'),
  secondary: cssVar('--color-secondary'),
  surface: cssVar('--color-surface'),
  success: cssVar('--color-success'),
  warning: cssVar('--color-warning'),
  danger: cssVar('--color-danger'),
  info: cssVar('--color-info'),
});

const draw = async (container, build) => {
  try {
    const Chart = await loadLibrary();
    const previous = registry.get(container);
    if (previous && previous.chart) previous.chart.destroy();
    const theme = chartTheme();
    const reduceMotion = window.matchMedia('(prefers-reduced-motion: reduce)').matches;
    Chart.defaults.font.family = cssVar('--font-family');
    Chart.defaults.color = theme.text;
    container.innerHTML = '<canvas role="img"></canvas>';
    const canvas = container.querySelector('canvas');
    const config = build(theme);
    canvas.setAttribute('aria-label', config.ariaLabel || 'Biểu đồ');
    const chart = new Chart(canvas, {
      ...config.chart,
      options: {
        responsive: true,
        maintainAspectRatio: false,
        animation: reduceMotion ? false : { duration: 400 },
        ...config.chart.options,
      },
    });
    registry.set(container, { chart, build });
  } catch (error) {
    container.innerHTML = errorState(error, { retry: false }).value;
  }
};

export const renderChart = (container, build) => {
  registry.set(container, { chart: null, build });
  return draw(container, build);
};

window.addEventListener('pms:theme-changed', () => {
  registry.forEach((entry, container) => {
    if (container.isConnected) draw(container, entry.build);
    else registry.delete(container);
  });
});

export const chartBox = (id, label, size = '') =>
  html`<div class="chart-box ${size}" id="${id}" aria-label="${label}"></div>`;

export const categoricalPalette = [
  '#0d9488',
  '#2563eb',
  '#f59e0b',
  '#a855f7',
  '#ef4444',
  '#0ea5e9',
  '#84cc16',
  '#ec4899',
];
