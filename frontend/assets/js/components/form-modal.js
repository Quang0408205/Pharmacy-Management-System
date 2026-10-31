import { html } from '../core/html.js';
import { errorMessage } from '../core/errors.js';
import {
  clearErrors,
  readValues,
  renderFields,
  setLoading,
  showErrors,
  validateFields,
  bindPasswordToggles,
  bindComboboxes,
} from './form.js';
import { openDrawer, openModal } from './overlay.js';
import { toast } from './toast.js';
import { uid } from '../core/dom.js';

export const openFormDialog = ({
  title,
  subtitle,
  fields,
  values = {},
  submitText = 'Lưu',
  size = '',
  asDrawer = false,
  intro = '',
  onSubmit,
}) => {
  const prefix = uid('form');
  const formId = `${prefix}-form`;
  const open = asDrawer ? openDrawer : openModal;
  const dialog = open({
    title,
    subtitle,
    size: asDrawer ? 'md' : size,
    content: html`<form id="${formId}" novalidate>
      <div data-form-alert class="alert alert--danger hidden" role="alert"></div>
      ${intro}
      ${renderFields(fields, values, prefix)}
    </form>`,
    footer: html`
      <button type="button" class="btn btn--secondary" data-dismiss="cancel">Hủy</button>
      <button type="submit" form="${formId}" class="btn btn--primary" data-submit>${submitText}</button>
    `,
  });

  const form = dialog.body.querySelector('form');
  const submit = dialog.footer.querySelector('[data-submit]');
  const alert = form.querySelector('[data-form-alert]');
  bindPasswordToggles(form);
  bindComboboxes(form, fields);

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    alert.classList.add('hidden');
    clearErrors(form);
    const formValues = readValues(form, fields);
    const errors = validateFields(fields, formValues);
    if (Object.keys(errors).length) {
      showErrors(form, errors, prefix);
      return;
    }
    setLoading(submit, true);
    try {
      const result = await onSubmit(formValues);
      dialog.close(result === undefined ? true : result);
    } catch (error) {
      setLoading(submit, false);
      if (error.fieldErrors && Object.keys(error.fieldErrors).length) showErrors(form, error.fieldErrors, prefix);
      alert.textContent = errorMessage(error);
      alert.classList.remove('hidden');
      toast.error('Không thể lưu', errorMessage(error));
    }
  });

  return dialog;
};
