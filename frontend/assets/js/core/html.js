class SafeHtml {
  constructor(value) {
    this.value = value;
  }

  toString() {
    return this.value;
  }
}

const entities = {
  '&': '&amp;',
  '<': '&lt;',
  '>': '&gt;',
  '"': '&quot;',
  "'": '&#39;',
};

export const esc = (value) => String(value ?? '').replace(/[&<>"']/g, (char) => entities[char]);

export const raw = (value) => new SafeHtml(String(value ?? ''));

const render = (value) => {
  if (value instanceof SafeHtml) return value.value;
  if (Array.isArray(value)) return value.map(render).join('');
  if (value === null || value === undefined || value === false) return '';
  return esc(value);
};

export const html = (strings, ...values) =>
  new SafeHtml(
    strings.reduce((out, chunk, index) => out + chunk + (index < values.length ? render(values[index]) : ''), ''),
  );

export const mount = (element, content) => {
  element.innerHTML = content instanceof SafeHtml ? content.value : esc(content);
  return element;
};

export const toNode = (content) => {
  const template = document.createElement('template');
  template.innerHTML = content instanceof SafeHtml ? content.value : esc(content);
  return template.content.firstElementChild;
};

export const isSafeHtml = (value) => value instanceof SafeHtml;
