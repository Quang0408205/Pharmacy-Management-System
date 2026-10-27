import { html } from '../../core/html.js';
import { pageUrl, qs } from '../../core/dom.js';
import { errorMessage } from '../../core/errors.js';
import { icon } from '../../core/icons.js';
import { mountAuthPage } from '../../components/auth-layout.js';
import { setLoading } from '../../components/form.js';
import { authService } from '../../services/auth-service.js';
import { appConfig } from '../../config/app-config.js';
import { demoResetTokens } from '../../config/demo-data.js';

const card = mountAuthPage({
  title: 'Quên mật khẩu',
  content: html`
    <h1>Quên mật khẩu</h1>
    <p class="auth-card__lead">Nhập email tài khoản. Nếu email hợp lệ, hướng dẫn đặt lại mật khẩu sẽ được gửi tới hộp thư của bạn.</p>
    <div class="alert alert--danger hidden" data-error role="alert"></div>
    <form class="auth-form" novalidate data-form>
      <div class="form-field">
        <label class="form-label" for="email">Email</label>
        <input class="form-control" id="email" name="email" type="email" autocomplete="email" required data-autofocus aria-describedby="email-error" />
        <span class="form-error" id="email-error" role="alert"></span>
      </div>
      <button type="submit" class="btn btn--primary btn--lg btn--block" data-submit>Gửi yêu cầu</button>
    </form>
    <p class="auth-footer"><a href="${pageUrl('pages/auth/login.html')}">${icon('chevronLeft', 'icon--sm')} Quay lại đăng nhập</a></p>
  `,
});

const form = qs('[data-form]', card);
const errorBox = qs('[data-error]', card);
const submit = qs('[data-submit]', card);
const emailError = qs('#email-error', card);

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  errorBox.classList.add('hidden');
  const email = form.elements.email.value.trim();
  const valid = /^\S+@\S+\.\S+$/.test(email);
  emailError.textContent = valid ? '' : 'Vui lòng nhập một địa chỉ email hợp lệ.';
  form.elements.email.toggleAttribute('aria-invalid', !valid);
  if (!valid) {
    form.elements.email.focus();
    return;
  }
  setLoading(submit, true);
  try {
    await authService.forgotPassword(email);
    card.innerHTML = html`<div class="auth-state">
      <span class="auth-state__icon">${icon('mail', 'icon--lg')}</span>
      <h1>Đã ghi nhận yêu cầu</h1>
      <p class="text-secondary">Nếu email này thuộc về một tài khoản, bạn sẽ nhận được hướng dẫn đặt lại mật khẩu. Hãy kiểm tra cả thư rác.</p>
      ${
        appConfig.apiMode === 'mock'
          ? html`<div class="alert alert--info"><span>Môi trường demo: không có email nào được gửi. Để thử màn hình đặt lại mật khẩu, dùng liên kết demo bên dưới.</span></div>
            <a class="btn btn--secondary btn--block" href="${pageUrl(`pages/auth/reset-password.html?token=${demoResetTokens.valid}`)}">Mở trang đặt lại (token hợp lệ)</a>
            <a class="btn btn--ghost btn--block" href="${pageUrl(`pages/auth/reset-password.html?token=${demoResetTokens.expired}`)}">Mở trang đặt lại (token hết hạn)</a>`
          : ''
      }
      <a class="btn btn--primary btn--block" href="${pageUrl('pages/auth/login.html')}">Về trang đăng nhập</a>
    </div>`.value;
  } catch (error) {
    setLoading(submit, false);
    errorBox.textContent = errorMessage(error);
    errorBox.classList.remove('hidden');
  }
});
