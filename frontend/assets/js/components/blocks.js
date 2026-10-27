import { html, raw } from '../core/html.js';
import { icon } from '../core/icons.js';
import { delegate } from '../core/dom.js';
import { medicineImage } from '../core/medicine-image.js';

export const pageHeading = ({ title, subtitle = '', actions = '' }) => html`<div class="page-heading">
  <div>
    <h1>${title}</h1>
    ${subtitle ? html`<p>${subtitle}</p>` : ''}
  </div>
  ${actions ? html`<div class="page-heading__actions">${actions}</div>` : ''}
</div>`;

export const mountTablePage = ({ content, title, subtitle = '', actions = '', mount }) => {
  content.innerHTML = html`${pageHeading({ title, subtitle, actions })}<div data-table></div>`.value;
  return mount(content.querySelector('[data-table]'));
};

export const kpiCard = ({
  label,
  value,
  hint = '',
  iconName = 'chart',
  tone = '',
}) => html`<article class="card card--hover kpi-card">
  <span class="kpi-card__icon ${tone ? `kpi-card__icon--${tone}` : ''}">${icon(iconName, 'icon--lg')}</span>
  <div class="grow">
    <div class="kpi-card__label">${label}</div>
    <div class="kpi-card__value">${value}</div>
    ${hint ? html`<div class="kpi-card__hint">${hint}</div>` : ''}
  </div>
</article>`;

export const card = ({
  title,
  actions = '',
  body = '',
  flush = false,
  id = '',
  footer = '',
}) => html`<section class="card" ${id ? raw(`id="${id}"`) : ''}>
  ${title ? html`<header class="card__header"><h2>${title}</h2>${actions}</header>` : ''}
  <div class="card__body ${flush ? 'card__body--flush' : ''}" data-card-body>${body}</div>
  ${footer ? html`<footer class="card__footer">${footer}</footer>` : ''}
</section>`;

export const tabs = (items, active, idPrefix = 'tab') => html`<div class="tabs" role="tablist">
  ${items.map(
    (item) =>
      html`<button type="button" class="tabs__tab" role="tab" id="${idPrefix}-${item.key}" data-tab="${item.key}" aria-selected="${item.key === active}" aria-controls="${idPrefix}-panel" tabindex="${item.key === active ? 0 : -1}">${item.label}</button>`,
  )}
</div>`;

export const bindTabs = (root, onChange) =>
  delegate(root, 'click', '[data-tab]', (event, button) => {
    root.querySelectorAll('[data-tab]').forEach((tab) => {
      const selected = tab === button;
      tab.setAttribute('aria-selected', String(selected));
      tab.tabIndex = selected ? 0 : -1;
    });
    onChange(button.dataset.tab);
  });

export const detailList = (pairs) => html`<dl class="detail-grid">
  ${pairs.map(([label, value]) => html`<div><dt>${label}</dt><dd>${value === '' || value === null || value === undefined ? '—' : value}</dd></div>`)}
</dl>`;

export const medicineCell = (medicine, sub = '') => html`<div class="cell-main">
  <img class="med-thumb" src="${medicineImage(medicine)}" alt="" width="44" height="44" loading="lazy" />
  <div class="truncate"><strong>${medicine.name}</strong><small>${sub || `${medicine.code} · ${medicine.manufacturer || ''}`}</small></div>
</div>`;

export const progressBar = (ratio, tone = '') => {
  const percent = Math.max(0, Math.min(100, Math.round(ratio * 100)));
  return html`<progress class="progress ${tone ? `progress--${tone}` : ''}" value="${percent}" max="100">${percent}%</progress>`;
};

export const rowButton = (action, label, iconName, extraClass = '') =>
  html`<button type="button" class="icon-button icon-button--sm ${extraClass}" data-row-action="${action}" aria-label="${label}" title="${label}">${icon(iconName, 'icon--sm')}</button>`;

export const dateInputValue = (daysFromToday = 0) => {
  const date = new Date();
  date.setDate(date.getDate() + daysFromToday);
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
};
