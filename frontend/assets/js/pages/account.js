import { html } from '../core/html.js';
import { qs } from '../core/dom.js';
import { icon } from '../core/icons.js';
import { errorMessage } from '../core/errors.js';
import { startPage } from '../components/page.js';
import { card, detailList, pageHeading } from '../components/blocks.js';
import { statusLabel } from '../components/badge.js';
import { toast } from '../components/toast.js';
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
} from '../components/form.js';
import { authService } from '../services/auth-service.js';

const fields = [
  {
    name: 'currentPassword',
    label: 'Mật khẩu hiện tại',
    type: 'password',
    required: true,
    autocomplete: 'current-password',
    full: true,
  },
  {
    name: 'newPassword',
    label: 'Mật khẩu mới',
    type: 'password',
    required: true,
    pattern: passwordRule,
    patternMessage: passwordRuleMessage,
    validate: (value, values) => (value === values.currentPassword ? 'Mật khẩu mới phải khác mật khẩu hiện tại.' : ''),
    full: true,
  },
  {
    name: 'confirmPassword',
    label: 'Xác nhận mật khẩu mới',
    type: 'password',
    required: true,
    validate: (value, values) => (value === values.newPassword ? '' : 'Mật khẩu xác nhận không khớp.'),
    full: true,
  },
];

startPage({
  nav: 'account',
  title: 'Tài khoản của tôi',
  render: async ({ user, content }) => {
    content.innerHTML = html`
      ${pageHeading({ title: 'Tài khoản của tôi', subtitle: 'Xem thông tin đăng nhập và đổi mật khẩu của bạn.' })}
      <div class="grid grid--2">
        ${card({
          title: 'Thông tin tài khoản',
          body: detailList([
            ['Họ và tên', user.name],
            ['Tên đăng nhập', user.username],
            ['Email', user.email],
            ['Vai trò', statusLabel('role', user.role)],
          ]),
        })}
        ${card({
          title: 'Đổi mật khẩu',
          body: html`<form novalidate class="stack" data-form>
            ${renderFields(fields, {}, 'pw')}
            <div class="password-meter" data-level="0" data-meter aria-live="polite">
              <div class="password-meter__bars"><span class="password-meter__bar"></span><span class="password-meter__bar"></span><span class="password-meter__bar"></span><span class="password-meter__bar"></span></div>
              <span class="form-help" data-meter-label>Độ mạnh mật khẩu: chưa nhập</span>
            </div>
            <p class="form-help">Sau khi đổi, các thiết bị khác đang đăng nhập bằng tài khoản này sẽ bị đăng xuất.</p>
            <div><button type="submit" class="btn btn--primary" data-submit>${icon('save', 'icon--sm')} Đổi mật khẩu</button></div>
          </form>`,
        })}
      </div>
    `.value;

    const form = qs('[data-form]', content);
    const submit = qs('[data-submit]', content);
    const meter = qs('[data-meter]', content);
    const meterLabel = qs('[data-meter-label]', content);
    bindPasswordToggles(content);

    form.elements.newPassword.addEventListener('input', () => {
      const { score, label } = passwordStrength(form.elements.newPassword.value);
      meter.dataset.level = String(score);
      meterLabel.textContent = `Độ mạnh mật khẩu: ${label || 'chưa nhập'}`;
    });

    form.addEventListener('submit', async (event) => {
      event.preventDefault();
      clearErrors(form);
      const values = readValues(form, fields);
      const errors = validateFields(fields, values);
      if (Object.keys(errors).length) return showErrors(form, errors, 'pw');
      setLoading(submit, true);
      try {
        await authService.changePassword({ currentPassword: values.currentPassword, newPassword: values.newPassword });
        form.reset();
        meter.dataset.level = '0';
        meterLabel.textContent = 'Độ mạnh mật khẩu: chưa nhập';
        toast.success('Đã đổi mật khẩu', 'Lần đăng nhập sau hãy dùng mật khẩu mới.');
      } catch (error) {
        if (error.fieldErrors) showErrors(form, error.fieldErrors, 'pw');
        toast.error('Không thể đổi mật khẩu', errorMessage(error));
      } finally {
        setLoading(submit, false);
      }
      return undefined;
    });
  },
});
