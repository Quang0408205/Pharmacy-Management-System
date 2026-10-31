import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { pageUrl, qs } from '../../core/dom.js';
import { errorMessage } from '../../core/errors.js';
import { mountAuthPage } from '../../components/auth-layout.js';
import { bindPasswordToggles, setLoading } from '../../components/form.js';
import { showToast } from '../../components/toast.js';
import { authService } from '../../services/auth-service.js';
import { flash, goHome } from '../../services/session-service.js';

const card = mountAuthPage({
  title: 'Đăng nhập',
  content: html`
    <h1>Đăng nhập</h1>
    <p class="auth-card__lead">Chào mừng trở lại. Vui lòng đăng nhập để tiếp tục.</p>
    <div class="alert alert--danger hidden" data-error role="alert"></div>
    <form class="auth-form" novalidate data-login-form>
      <div class="form-field">
        <label class="form-label" for="identifier">Email hoặc tên đăng nhập</label>
        <input class="form-control" id="identifier" name="identifier" type="text" autocomplete="username" required data-autofocus aria-describedby="identifier-error" />
        <span class="form-error" id="identifier-error" role="alert"></span>
      </div>
      <div class="form-field">
        <label class="form-label" for="password">Mật khẩu</label>
        <div class="input-group">
          <input class="form-control" id="password" name="password" type="password" autocomplete="current-password" required aria-describedby="password-error" />
          <button type="button" class="input-group__button" data-toggle-password="password" aria-label="Hiện hoặc ẩn mật khẩu">${icon('eye')}</button>
        </div>
        <span class="form-error" id="password-error" role="alert"></span>
      </div>
      <div class="auth-links">
        <label class="check"><input type="checkbox" name="remember" /> Ghi nhớ đăng nhập (7 ngày)</label>
        <a href="${pageUrl('pages/auth/forgot-password.html')}">Quên mật khẩu?</a>
      </div>
      <button type="submit" class="btn btn--primary btn--lg btn--block" data-submit>Đăng nhập</button>
    </form>
    <p class="auth-footer">Chưa có tài khoản? Liên hệ quản trị viên để được cấp liên kết kích hoạt.</p>
  `,
});

const form = qs('[data-login-form]', card);
const errorBox = qs('[data-error]', card);
const submit = qs('[data-submit]', card);
bindPasswordToggles(card);

const setFieldError = (name, message) => {
  qs(`#${name}-error`, card).textContent = message;
  const control = qs(`#${name}`, card);
  if (message) control.setAttribute('aria-invalid', 'true');
  else control.removeAttribute('aria-invalid');
};

const message = flash.take();
if (message) showToast(message);

authService
  .currentUser()
  .then((user) => goHome(user.role))
  .catch(() => {});

form.addEventListener('submit', async (event) => {
  event.preventDefault();
  errorBox.classList.add('hidden');
  const identifier = form.elements.identifier.value.trim();
  const password = form.elements.password.value;
  setFieldError('identifier', identifier ? '' : 'Vui lòng nhập email hoặc tên đăng nhập.');
  setFieldError('password', password ? '' : 'Vui lòng nhập mật khẩu.');
  if (!identifier || !password) {
    (identifier ? form.elements.password : form.elements.identifier).focus();
    return;
  }
  setLoading(submit, true);
  try {
    const { user } = await authService.login({ identifier, password, remember: form.elements.remember.checked });
    flash.set({ type: 'success', title: `Xin chào, ${user.name}`, message: 'Đăng nhập thành công.' });
    goHome(user.role);
  } catch (error) {
    setLoading(submit, false);
    errorBox.textContent = errorMessage(error);
    errorBox.classList.remove('hidden');
  }
});
