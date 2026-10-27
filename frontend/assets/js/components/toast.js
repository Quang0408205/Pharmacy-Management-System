import { html } from '../core/html.js';
import { icon } from '../core/icons.js';

const iconByType = { success: 'checkCircle', error: 'alertCircle', warning: 'alertTriangle', info: 'info' };
const durations = { success: 3500, info: 4000, warning: 5500, error: 6500 };

let region = null;

const ensureRegion = () => {
  if (region && document.body.contains(region)) return region;
  region = document.createElement('div');
  region.className = 'toast-region';
  region.setAttribute('role', 'region');
  region.setAttribute('aria-label', 'Thông báo');
  region.setAttribute('aria-live', 'polite');
  document.body.append(region);
  return region;
};

const dismiss = (element) => {
  if (!element.isConnected) return;
  element.classList.add('is-leaving');
  setTimeout(() => element.remove(), 240);
};

export const showToast = ({ type = 'info', title, message = '', duration }) => {
  const element = document.createElement('div');
  element.className = `toast toast--${type}`;
  element.setAttribute('role', type === 'error' ? 'alert' : 'status');
  element.innerHTML = html`
    <span class="toast__icon">${icon(iconByType[type], 'icon--lg')}</span>
    <div class="toast__text">
      <strong>${title}</strong>
      ${message ? html`<span class="text-secondary text-small">${message}</span>` : ''}
    </div>
    <button type="button" class="icon-button icon-button--sm" aria-label="Đóng thông báo">${icon('x')}</button>
  `.value;
  element.querySelector('button').addEventListener('click', () => dismiss(element));
  ensureRegion().append(element);
  setTimeout(() => dismiss(element), duration || durations[type]);
  return element;
};

export const toast = {
  success: (title, message) => showToast({ type: 'success', title, message }),
  error: (title, message) => showToast({ type: 'error', title, message }),
  warning: (title, message) => showToast({ type: 'warning', title, message }),
  info: (title, message) => showToast({ type: 'info', title, message }),
};
