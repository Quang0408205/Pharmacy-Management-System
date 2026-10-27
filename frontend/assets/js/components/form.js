import { html, raw } from '../core/html.js';
import { qs, qsa } from '../core/dom.js';
import { icon } from '../core/icons.js';

const fieldId = (prefix, name) => `${prefix}-${name.replaceAll('.', '-')}`;

const renderControl = (field, value, id, describedBy) => {
  const common = html`id="${id}" name="${field.name}" aria-describedby="${describedBy}" ${field.required ? raw('aria-required="true"') : ''} ${field.disabled ? raw('disabled') : ''}`;
  if (field.type === 'select') {
    return html`<select class="form-control" ${common}>
      ${field.placeholder ? html`<option value="">${field.placeholder}</option>` : ''}
      ${(field.options || []).map((option) => html`<option value="${option.value}" ${String(option.value) === String(value ?? '') ? raw('selected') : ''}>${option.label}</option>`)}
    </select>`;
  }
  if (field.type === 'textarea') {
    return html`<textarea class="form-control" rows="${field.rows || 3}" ${common} ${field.maxlength ? raw(`maxlength="${Number(field.maxlength)}"`) : ''} placeholder="${field.placeholder || ''}">${value ?? ''}</textarea>`;
  }
  if (field.type === 'password') {
    return html`<div class="input-group">
      <input class="form-control" type="password" ${common} value="${value ?? ''}" autocomplete="${field.autocomplete || 'new-password'}" placeholder="${field.placeholder || ''}" />
      <button type="button" class="input-group__button" data-toggle-password="${id}" aria-label="Hiện hoặc ẩn mật khẩu">${icon('eye')}</button>
    </div>`;
  }
  const attributes = [
    field.min !== undefined ? `min="${field.min}"` : '',
    field.max !== undefined ? `max="${field.max}"` : '',
    field.step !== undefined ? `step="${field.step}"` : '',
    field.maxlength ? `maxlength="${Number(field.maxlength)}"` : '',
    field.inputmode ? `inputmode="${field.inputmode}"` : '',
  ].join(' ');
  return html`<input class="form-control" type="${field.type || 'text'}" ${common} value="${value ?? ''}" ${raw(attributes)} placeholder="${field.placeholder || ''}" autocomplete="${field.autocomplete || 'off'}" />`;
};

export const renderFields = (fields, values = {}, prefix = 'f') =>
  html`<div class="form-grid">
    ${fields.map((field) => {
      const id = fieldId(prefix, field.name);
      const describedBy = `${id}-help ${id}-error`;
      return html`<div class="form-field ${field.full ? 'form-field--full' : ''}">
        <label class="form-label" for="${id}">${field.label}${field.required ? html` <span class="required" aria-hidden="true">*</span>` : ''}</label>
        ${renderControl(field, values[field.name], id, describedBy)}
        <span class="form-help" id="${id}-help">${field.help || ''}</span>
        <span class="form-error" id="${id}-error" role="alert"></span>
      </div>`;
    })}
  </div>`;

export const readValues = (form, fields) => {
  const values = {};
  fields.forEach((field) => {
    const element = form.elements[field.name];
    if (!element) return;
    values[field.name] = element.type === 'checkbox' ? element.checked : element.value.trim();
  });
  return values;
};

export const validateFields = (fields, values) => {
  const errors = {};
  fields.forEach((field) => {
    const value = values[field.name];
    const empty = value === '' || value === undefined || value === null;
    if (field.required && empty) {
      errors[field.name] = `${field.label} là bắt buộc.`;
      return;
    }
    if (empty) return;
    if (field.type === 'number') {
      const number = Number(value);
      if (!Number.isFinite(number)) errors[field.name] = `${field.label} phải là số.`;
      else if (field.min !== undefined && number < field.min)
        errors[field.name] = `${field.label} không được nhỏ hơn ${field.min}.`;
      else if (field.max !== undefined && number > field.max)
        errors[field.name] = `${field.label} không được lớn hơn ${field.max}.`;
    }
    if (field.type === 'email' && !/^\S+@\S+\.\S+$/.test(value)) errors[field.name] = 'Email không hợp lệ.';
    if (field.type === 'tel' && !/^(0|\+84)[\d\s]{8,12}$/.test(value))
      errors[field.name] = 'Số điện thoại không hợp lệ (ví dụ 0901 234 567).';
    if (field.pattern && !field.pattern.test(value))
      errors[field.name] = field.patternMessage || `${field.label} không hợp lệ.`;
    if (field.validate) {
      const message = field.validate(value, values);
      if (message) errors[field.name] = message;
    }
  });
  return errors;
};

export const clearErrors = (form) => {
  qsa('.form-error', form).forEach((element) => {
    element.textContent = '';
  });
  qsa('[aria-invalid]', form).forEach((element) => element.removeAttribute('aria-invalid'));
};

export const showErrors = (form, errors, prefix = 'f') => {
  clearErrors(form);
  let first = null;
  Object.entries(errors).forEach(([name, message]) => {
    if (!message) return;
    const id = fieldId(prefix, name);
    const control = qs(`#${CSS.escape(id)}`, form);
    const output = qs(`#${CSS.escape(`${id}-error`)}`, form);
    if (!control || !output) return;
    output.textContent = message;
    control.setAttribute('aria-invalid', 'true');
    if (!first) first = control;
  });
  if (first) first.focus();
  return Boolean(first);
};

export const bindPasswordToggles = (root) => {
  root.addEventListener('click', (event) => {
    const button = event.target instanceof Element ? event.target.closest('[data-toggle-password]') : null;
    if (!button) return;
    const input = qs(`#${CSS.escape(button.dataset.togglePassword)}`, root);
    if (!input) return;
    const show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    button.innerHTML = icon(show ? 'eyeOff' : 'eye').value;
  });
};

export const setLoading = (button, loading, label) => {
  button.classList.toggle('is-loading', loading);
  button.disabled = loading;
  if (label && !loading) button.textContent = label;
};

export const passwordStrength = (password) => {
  let score = 0;
  if (password.length >= 8) score += 1;
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score += 1;
  if (/\d/.test(password)) score += 1;
  if (/[^A-Za-z0-9]/.test(password) || password.length >= 12) score += 1;
  const labels = ['Rất yếu', 'Yếu', 'Trung bình', 'Khá', 'Mạnh'];
  return { score, label: password ? labels[score] : '' };
};

export const passwordRule = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
export const passwordRuleMessage = 'Mật khẩu tối thiểu 8 ký tự, gồm chữ hoa, chữ thường và số.';
