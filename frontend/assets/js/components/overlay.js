import { html, isSafeHtml, esc } from '../core/html.js';
import { icon } from '../core/icons.js';
import { focusFirst, trapFocus, uid } from '../core/dom.js';

const closeDelay = 240;

const toMarkup = (content) => {
  if (content === null || content === undefined) return '';
  if (isSafeHtml(content)) return content.value;
  if (content instanceof Node) return '';
  return esc(content);
};

const openOverlay = ({
  kind,
  title,
  subtitle = '',
  content = '',
  footer = null,
  size = '',
  dismissible = true,
  onMount,
  labelledBy,
}) => {
  const id = uid(kind);
  const backdrop = document.createElement('div');
  backdrop.className = `${kind}-backdrop`;
  const panelClass =
    kind === 'modal' ? `modal ${size ? `modal--${size}` : ''}` : `drawer ${size ? `drawer--${size}` : ''}`;
  backdrop.innerHTML = html`
    <section class="${panelClass}" role="dialog" aria-modal="true" aria-labelledby="${labelledBy || `${id}-title`}">
      <header class="${kind}__header">
        <div>
          <h2 id="${id}-title">${title}</h2>
          ${subtitle ? html`<p class="text-secondary text-small">${subtitle}</p>` : ''}
        </div>
        ${dismissible ? html`<button type="button" class="icon-button icon-button--sm" data-overlay-close aria-label="Đóng">${icon('x')}</button>` : ''}
      </header>
      <div class="${kind}__body" data-overlay-body></div>
      <footer class="${kind}__footer ${footer ? '' : 'hidden'}" data-overlay-footer></footer>
    </section>
  `.value;

  const panel = backdrop.firstElementChild;
  const body = backdrop.querySelector('[data-overlay-body]');
  const footerElement = backdrop.querySelector('[data-overlay-footer]');
  let settle = null;
  const closed = new Promise((resolve) => {
    settle = resolve;
  });
  let isClosed = false;
  let releaseFocus = () => {};

  const setBody = (value) => {
    if (value instanceof Node) body.replaceChildren(value);
    else body.innerHTML = toMarkup(value);
  };
  const setFooter = (value) => {
    footerElement.innerHTML = toMarkup(value);
    footerElement.classList.toggle('hidden', !value);
  };

  const close = (result) => {
    if (isClosed) return;
    isClosed = true;
    releaseFocus();
    backdrop.classList.remove('is-visible');
    setTimeout(() => backdrop.remove(), closeDelay);
    settle(result);
  };

  setBody(content);
  setFooter(footer);
  document.body.append(backdrop);
  requestAnimationFrame(() => backdrop.classList.add('is-visible'));
  releaseFocus = trapFocus(panel, () => dismissible && close(undefined));

  if (dismissible) {
    backdrop.addEventListener('mousedown', (event) => {
      if (event.target === backdrop) close(undefined);
    });
    backdrop.querySelector('[data-overlay-close]').addEventListener('click', () => close(undefined));
  }
  backdrop.addEventListener('click', (event) => {
    const target = event.target instanceof Element ? event.target.closest('[data-dismiss]') : null;
    if (target) close(target.dataset.dismiss === 'confirm' ? true : undefined);
  });

  const controller = { element: backdrop, panel, body, footer: footerElement, close, closed, setBody, setFooter };
  setTimeout(() => focusFirst(body.querySelector('[data-autofocus]') ? body : panel), 30);
  if (onMount) onMount(controller);
  return controller;
};

export const openModal = (options) => openOverlay({ kind: 'modal', ...options });

export const openDrawer = (options) => openOverlay({ kind: 'drawer', size: 'lg', ...options });

export const confirmDialog = ({
  title,
  message,
  confirmText = 'Xác nhận',
  cancelText = 'Hủy',
  tone = 'primary',
  body = '',
}) =>
  new Promise((resolve) => {
    const iconName = tone === 'danger' ? 'alertTriangle' : 'info';
    const modal = openModal({
      title,
      size: 'sm',
      content: html`
        <div class="confirm-icon ${tone === 'danger' ? 'confirm-icon--danger' : ''}">${icon(iconName, 'icon--lg')}</div>
        <p>${message}</p>
        ${body}
      `,
      footer: html`
        <button type="button" class="btn btn--secondary" data-dismiss="cancel">${cancelText}</button>
        <button type="button" class="btn ${tone === 'danger' ? 'btn--danger' : 'btn--primary'}" data-dismiss="confirm" data-autofocus>${confirmText}</button>
      `,
    });
    modal.closed.then((result) => resolve(result === true));
  });
