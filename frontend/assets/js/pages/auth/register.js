import { html } from '../../core/html.js';
import { pageUrl, qs, getQuery } from '../../core/dom.js';
import { errorMessage } from '../../core/errors.js';
import { icon } from '../../core/icons.js';
import { mountAuthPage } from '../../components/auth-layout.js';
import {
  bindPasswordToggles,
  clearErrors,
  passwordRule,
  passwordRuleMessage,
  readValues,
  renderFields,
  setLoading,
  showErrors,
  validateFields,
} from '../../components/form.js';
import { authService } from '../../services/auth-service.js';
import { statusLabel } from '../../components/badge.js';
import { appConfig } from '../../config/app-config.js';
import { demoInvitations } from '../../config/demo-data.js';

const fields = [
  { name: 'fullName', label: 'Họ và tên', required: true, full: true, autocomplete: 'name' },
  { name: 'email', label: 'Email', type: 'email', required: true, autocomplete: 'email' },
  {
    name: 'username',
    label: 'Tên đăng nhập',
    required: true,
    autocomplete: 'username',
    pattern: /^[a-zA-Z0-9._-]{3,}$/,
    patternMessage: 'Tên đăng nhập tối thiểu 3 ký tự, chỉ gồm chữ, số, dấu chấm, gạch.',
  },
  {
    name: 'password',
    label: 'Mật khẩu',
    type: 'password',
    required: true,
    pattern: passwordRule,
    patternMessage: passwordRuleMessage,
  },
  {
    name: 'confirmPassword',
    label: 'Xác nhận mật khẩu',
    type: 'password',
    required: true,
    validate: (value, values) => (value === values.password ? '' : 'Mật khẩu xác nhận không khớp.'),
  },
  {
    name: 'invitationCode',
    label: 'Mã mời',
    required: true,
    full: true,
    help: 'Mã do quản trị viên cấp. Vai trò của bạn được xác định theo lời mời.',
  },
];

const demoHint =
  appConfig.apiMode === 'mock'
    ? html`<section class="demo-accounts">
        <h3>Mã mời demo</h3>
        <div class="demo-accounts__list">
          ${demoInvitations.map((item) => html`<button type="button" class="demo-account" data-invite="${item.code}" data-email="${item.email}"><span><strong>${item.code}</strong><br /><span class="text-secondary text-small">${item.email}</span></span><span class="text-small">${item.note}</span></button>`)}
        </div>
      </section>`
    : '';

const card = mountAuthPage({
  title: 'Kích hoạt tài khoản',
  content: html`
    <h1>Kích hoạt tài khoản</h1>
    <p class="auth-card__lead">Hệ thống nội bộ không cho phép tự đăng ký. Hãy dùng mã mời do quản trị viên cấp.</p>
    <div class="alert alert--danger hidden" data-error role="alert"></div>
    <form class="auth-form" novalidate data-register-form>
      ${renderFields(fields, { invitationCode: getQuery().code || '' }, 'reg')}
      <p class="text-small text-secondary hidden" data-invite-info role="status"></p>
      <button type="submit" class="btn btn--primary btn--lg btn--block" data-submit>Tạo tài khoản</button>
    </form>
    <p class="auth-footer">Đã có tài khoản? <a href="${pageUrl('pages/auth/login.html')}">Đăng nhập</a></p>
    ${demoHint}
  `,
});

const form = qs('[data-register-form]', card);
const errorBox = qs('[data-error]', card);
const submit = qs('[data-submit]', card);
const inviteInfo = qs('[data-invite-info]', card);
bindPasswordToggles(card);

const checkInvitation = async () => {
  const code = form.elements.invitationCode.value.trim();
  inviteInfo.classList.add('hidden');
  if (!code) return;
  try {
    const invitation = await authService.checkInvitation(code);
    inviteInfo.textContent = `Lời mời hợp lệ cho ${invitation.email} với vai trò ${statusLabel('role', invitation.role)}.`;
    inviteInfo.classList.remove('hidden');
    if (!form.elements.email.value) form.elements.email.value = invitation.email;
    showErrors(form, { invitationCode: '' }, 'reg');
  } catch (error) {
    showErrors(form, { invitationCode: errorMessage(error) }, 'reg');
  }
};

form.elements.invitationCode.addEventListener('blur', checkInvitation);
card.addEventListener('click', (event) => {
  const button = event.target instanceof Element ? event.target.closest('[data-invite]') : null;
  if (!button) return;
  form.elements.invitationCode.value = button.dataset.invite;
  form.elements.email.value = button.dataset.email;
  checkInvitation();
});

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  errorBox.classList.add('hidden');
  clearErrors(form);
  const values = readValues(form, fields);
  const errors = validateFields(fields, values);
  if (Object.keys(errors).length) {
    showErrors(form, errors, 'reg');
    return;
  }
  setLoading(submit, true);
  try {
    await authService.register({
      fullName: values.fullName,
      email: values.email,
      username: values.username,
      password: values.password,
      invitationCode: values.invitationCode,
    });
    card.innerHTML = html`<div class="auth-state">
      <span class="auth-state__icon">${icon('checkCircle', 'icon--lg')}</span>
      <h1>Kích hoạt thành công</h1>
      <p class="text-secondary">Tài khoản của bạn đã sẵn sàng. Hãy đăng nhập để bắt đầu làm việc.</p>
      <a class="btn btn--primary btn--lg btn--block" href="${pageUrl('pages/auth/login.html')}">Đến trang đăng nhập</a>
    </div>`.value;
  } catch (error) {
    setLoading(submit, false);
    if (error.fieldErrors) showErrors(form, error.fieldErrors, 'reg');
    errorBox.textContent = errorMessage(error);
    errorBox.classList.remove('hidden');
  }
});
