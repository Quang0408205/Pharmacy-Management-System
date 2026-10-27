import { html } from '../core/html.js';
import { icon } from '../core/icons.js';
import { errorMessage } from '../core/errors.js';

export const skeletonTable = (
  rows = 6,
) => html`<div class="skeleton-table" aria-busy="true" aria-label="Đang tải dữ liệu">
  ${Array.from({ length: rows }, () => html`<span class="skeleton skeleton--line"></span>`)}
</div>`;

export const skeletonBlock = () => html`<div class="skeleton skeleton--block" aria-busy="true"></div>`;

export const skeletonCards = (count = 6) => html`<div class="med-grid" aria-busy="true" aria-label="Đang tải dữ liệu">
  ${Array.from({ length: count }, () => html`<span class="skeleton skeleton--block"></span>`)}
</div>`;

export const emptyState = ({
  title = 'Chưa có dữ liệu',
  message = '',
  iconName = 'inbox',
  action = '',
} = {}) => html`<div class="empty-state">
  <span class="empty-state__icon">${icon(iconName, 'icon--lg')}</span>
  <strong>${title}</strong>
  ${message ? html`<p>${message}</p>` : ''}
  ${action}
</div>`;

export const errorState = (
  error,
  { retry = true } = {},
) => html`<div class="empty-state empty-state--error" role="alert">
  <span class="empty-state__icon">${icon('alertCircle', 'icon--lg')}</span>
  <strong>Không thể tải dữ liệu</strong>
  <p>${typeof error === 'string' ? error : errorMessage(error)}</p>
  ${retry ? html`<button type="button" class="btn btn--secondary btn--sm" data-retry>${icon('reset', 'icon--sm')} Thử lại</button>` : ''}
</div>`;

export const cardLoading = () => html`<div class="card__body">${skeletonTable(4)}</div>`;
