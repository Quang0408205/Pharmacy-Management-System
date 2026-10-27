export const qs = (selector, root = document) => root.querySelector(selector);

export const qsa = (selector, root = document) => [...root.querySelectorAll(selector)];

export const delegate = (root, eventName, selector, handler) => {
  const listener = (event) => {
    const target = event.target instanceof Element ? event.target.closest(selector) : null;
    if (target && root.contains(target)) handler(event, target);
  };
  root.addEventListener(eventName, listener);
  return () => root.removeEventListener(eventName, listener);
};

export const debounce = (fn, wait = 250) => {
  let timer = null;
  const wrapped = (...args) => {
    clearTimeout(timer);
    timer = setTimeout(() => fn(...args), wait);
  };
  wrapped.cancel = () => clearTimeout(timer);
  return wrapped;
};

const focusableSelector =
  'a[href], button:not([disabled]), input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

export const trapFocus = (container, onEscape) => {
  const previous = document.activeElement;
  const handler = (event) => {
    if (event.key === 'Escape') {
      event.stopPropagation();
      onEscape();
      return;
    }
    if (event.key !== 'Tab') return;
    const items = qsa(focusableSelector, container).filter((item) => item.offsetParent !== null);
    if (!items.length) return;
    const first = items[0];
    const last = items[items.length - 1];
    if (event.shiftKey && document.activeElement === first) {
      event.preventDefault();
      last.focus();
    } else if (!event.shiftKey && document.activeElement === last) {
      event.preventDefault();
      first.focus();
    }
  };
  container.addEventListener('keydown', handler);
  return () => {
    container.removeEventListener('keydown', handler);
    if (previous instanceof HTMLElement && document.contains(previous)) previous.focus();
  };
};

export const focusFirst = (container) => {
  const target = qs('[data-autofocus]', container) || qsa(focusableSelector, container)[0];
  if (target) target.focus();
};

export const uid = (prefix = 'id') => `${prefix}-${Math.random().toString(36).slice(2, 10)}`;

export const sleep = (ms) => new Promise((resolve) => setTimeout(resolve, ms));

export const pageUrl = (relativeFromRoot) => {
  const root = new URL('../../../', import.meta.url);
  return new URL(relativeFromRoot, root).href;
};

export const getQuery = () => Object.fromEntries(new URLSearchParams(window.location.search));
