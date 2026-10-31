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
import { authService } from '../../services/auth-service.js';

const token = getQuery().token || '';

const fields = [
  {
    name: 'password',
    label: 'Mật khẩu mới',
    type: 'password',
    required: true,
    pattern: passwordRule,
    patternMessage: passwordRuleMessage,
    full: true,
  },
  {
    name: 'confirmPassword',
    label: 'Xác nhận mật khẩu mới',
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
  title: 'Đặt lại mật khẩu',
  content: html`<div class="stack"><span class="skeleton skeleton--line"></span><span class="skeleton skeleton--line"></span><span class="skeleton skeleton--block"></span></div>`,
});

const showForm = () => {
  card.innerHTML = html`
    <h1>Đặt lại mật khẩu</h1>
    <p class="auth-card__lead">Tạo mật khẩu mới cho tài khoản của bạn.</p>
    <div class="alert alert--danger hidden" data-error role="alert"></div>
    <form class="auth-form" novalidate data-form>
      ${renderFields(fields, {}, 'rst')}
      <div class="password-meter" data-level="0" data-meter aria-live="polite">
        <div class="password-meter__bars"><span class="password-meter__bar"></span><span class="password-meter__bar"></span><span class="password-meter__bar"></span><span class="password-meter__bar"></span></div>
        <span class="form-help" data-meter-label>Độ mạnh mật khẩu: chưa nhập</span>
      </div>
      <button type="submit" class="btn btn--primary btn--lg btn--block" data-submit>Đặt lại mật khẩu</button>
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
      showErrors(form, errors, 'rst');
      return;
    }
    setLoading(submit, true);
    try {
      await authService.resetPassword({ token, password: values.password });
      card.innerHTML = stateView({
        title: 'Đặt lại mật khẩu thành công',
        message: 'Bạn có thể đăng nhập bằng mật khẩu mới.',
        tone: 'success',
        iconName: 'checkCircle',
        actions: html`<a class="btn btn--primary btn--lg btn--block" href="${pageUrl('pages/auth/login.html')}">Đến trang đăng nhập</a>`,
      }).value;
    } catch (error) {
      setLoading(submit, false);
      if (error.fieldErrors) showErrors(form, error.fieldErrors, 'rst');
      errorBox.textContent = errorMessage(error);
      errorBox.classList.remove('hidden');
    }
  });
};

const showInvalid = (message) => {
  card.innerHTML = stateView({
    title: 'Liên kết không hợp lệ hoặc đã hết hạn',
    message,
    actions: html`<a class="btn btn--primary btn--block" href="${pageUrl('pages/auth/forgot-password.html')}">Cần liên kết mới?</a>
      <a class="btn btn--ghost btn--block" href="${pageUrl('pages/auth/login.html')}">Quay lại đăng nhập</a>`,
  }).value;
};

if (!token) {
  showInvalid('Thiếu mã xác thực. Vui lòng mở lại liên kết do quản trị viên gửi.');
} else {
  authService
    .checkResetToken(token)
    .then(showForm)
    .catch((error) => showInvalid(errorMessage(error)));
}
