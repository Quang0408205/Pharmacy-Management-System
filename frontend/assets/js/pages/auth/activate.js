import { html } from '../../core/html.js';
import { getQuery, pageUrl, qs } from '../../core/dom.js';
import { errorMessage } from '../../core/errors.js';
import { icon } from '../../core/icons.js';
import { mountAuthPage } from '../../components/auth-layout.js';
import {
  bindPasswordToggles,
  clearErrors,
  passwordRule,
  passwordRuleMessage,
  passwordStrength,
  readValues,
  renderFields,
  setLoading,
  showErrors,
  validateFields,
} from '../../components/form.js';
import { detailList } from '../../components/blocks.js';
import { statusLabel } from '../../components/badge.js';
import { authService } from '../../services/auth-service.js';

const token = getQuery().token || '';

const fields = [
  {
    name: 'password',
    label: 'Mật khẩu',
    type: 'password',
    required: true,
    pattern: passwordRule,
    patternMessage: passwordRuleMessage,
    full: true,
  },
  {
    name: 'confirmPassword',
    label: 'Xác nhận mật khẩu',
    type: 'password',
    required: true,
    full: true,
    validate: (value, values) => (value === values.password ? '' : 'Mật khẩu xác nhận không khớp.'),
  },
];

const stateView = ({
  title,
  message,
  tone = 'danger',
  iconName = 'alertCircle',
  actions,
}) => html`<div class="auth-state">
  <span class="auth-state__icon ${tone === 'danger' ? 'auth-state__icon--danger' : ''}">${icon(iconName, 'icon--lg')}</span>
  <h1>${title}</h1>
  <p class="text-secondary">${message}</p>
  ${actions}
</div>`;

const card = mountAuthPage({
  title: 'Kích hoạt tài khoản',
  content: html`<div class="stack"><span class="skeleton skeleton--line"></span><span class="skeleton skeleton--line"></span><span class="skeleton skeleton--block"></span></div>`,
});

const showInvalid = (message) => {
  card.innerHTML = stateView({
    title: 'Liên kết không hợp lệ hoặc đã hết hạn',
    message,
    actions: html`<a class="btn btn--primary btn--block" href="${pageUrl('pages/auth/forgot-password.html')}">Cần liên kết mới?</a>
      <a class="btn btn--ghost btn--block" href="${pageUrl('pages/auth/login.html')}">Quay lại đăng nhập</a>`,
  }).value;
};

const showForm = (account) => {
  card.innerHTML = html`
    <h1>Kích hoạt tài khoản</h1>
    <p class="auth-card__lead">Xin chào <strong>${account.name}</strong>. Hãy tạo mật khẩu để bắt đầu sử dụng hệ thống.</p>
    ${detailList([
      ['Tên đăng nhập', account.username],
      ['Email', account.email],
      ['Vai trò', statusLabel('role', account.role)],
    ])}
    <div class="alert alert--danger hidden" data-error role="alert"></div>
    <form class="auth-form" novalidate data-form>
      ${renderFields(fields, {}, 'act')}
      <div class="password-meter" data-level="0" data-meter aria-live="polite">
        <div class="password-meter__bars"><span class="password-meter__bar"></span><span class="password-meter__bar"></span><span class="password-meter__bar"></span><span class="password-meter__bar"></span></div>
        <span class="form-help" data-meter-label>Độ mạnh mật khẩu: chưa nhập</span>
      </div>
      <button type="submit" class="btn btn--primary btn--lg btn--block" data-submit>Kích hoạt tài khoản</button>
    </form>
  `.value;

  const form = qs('[data-form]', card);
  const errorBox = qs('[data-error]', card);
  const submit = qs('[data-submit]', card);
  const meter = qs('[data-meter]', card);
  const meterLabel = qs('[data-meter-label]', card);
  bindPasswordToggles(card);

  form.elements.password.addEventListener('input', () => {
    const { score, label } = passwordStrength(form.elements.password.value);
    meter.dataset.level = String(score);
    meterLabel.textContent = `Độ mạnh mật khẩu: ${label || 'chưa nhập'}`;
  });

  form.addEventListener('submit', async (event) => {
    event.preventDefault();
    errorBox.classList.add('hidden');
    clearErrors(form);
    const values = readValues(form, fields);
    const errors = validateFields(fields, values);
    if (Object.keys(errors).length) {
      showErrors(form, errors, 'act');
      return;
    }
    setLoading(submit, true);
    try {
      await authService.activate({ token, password: values.password });
      card.innerHTML = stateView({
        title: 'Kích hoạt thành công',
        message: 'Tài khoản của bạn đã sẵn sàng. Hãy đăng nhập để bắt đầu làm việc.',
        tone: 'success',
        iconName: 'checkCircle',
        actions: html`<a class="btn btn--primary btn--lg btn--block" href="${pageUrl('pages/auth/login.html')}">Đến trang đăng nhập</a>`,
      }).value;
    } catch (error) {
      setLoading(submit, false);
      if (error.fieldErrors) showErrors(form, error.fieldErrors, 'act');
      errorBox.textContent = errorMessage(error);
      errorBox.classList.remove('hidden');
    }
  });
};

if (!token) {
  showInvalid('Thiếu mã xác thực. Vui lòng mở lại liên kết kích hoạt do quản trị viên gửi.');
} else {
  authService
    .activationInfo(token)
    .then(showForm)
    .catch((error) => showInvalid(errorMessage(error)));
}
