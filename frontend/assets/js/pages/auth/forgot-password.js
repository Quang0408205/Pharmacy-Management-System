import { html } from '../../core/html.js';
import { pageUrl } from '../../core/dom.js';
import { icon } from '../../core/icons.js';
import { mountAuthPage } from '../../components/auth-layout.js';

mountAuthPage({
  title: 'Quên mật khẩu',
  content: html`
    <div class="auth-state">
      <span class="auth-state__icon">${icon('idCard', 'icon--lg')}</span>
      <h1>Quên mật khẩu</h1>
      <p class="text-secondary">Hệ thống nội bộ không gửi email đặt lại mật khẩu. Hãy liên hệ quản trị viên của nhà thuốc để được cấp liên kết đặt lại mật khẩu.</p>
      <p class="text-secondary text-small">Liên kết đặt lại có hiệu lực trong 30 phút và chỉ dùng được một lần.</p>
      <a class="btn btn--primary btn--lg btn--block" href="${pageUrl('pages/auth/login.html')}">${icon('chevronLeft', 'icon--sm')} Quay lại đăng nhập</a>
    </div>
  `,
});
