import { html } from '../core/html.js';
import { icon } from '../core/icons.js';
import { qs } from '../core/dom.js';
import { preferences } from '../services/session-service.js';

const pattern = html`<svg class="auth-aside__pattern" viewBox="0 0 200 200" fill="none" stroke-width="2" aria-hidden="true">
  <circle cx="100" cy="100" r="96" />
  <circle cx="100" cy="100" r="70" />
  <circle cx="100" cy="100" r="44" />
  <path d="M100 70v60M70 100h60" />
</svg>
<svg class="auth-aside__pattern-top" viewBox="0 0 120 120" fill="none" stroke-width="3" aria-hidden="true">
  <rect x="10" y="40" width="100" height="40" rx="20" transform="rotate(-35 60 60)" />
  <path d="M60 28 60 92" transform="rotate(-35 60 60)" />
</svg>`;

export const mountAuthPage = ({ title, content }) => {
  preferences.apply();
  document.title = `${title} · PharmaCare`;
  const root = qs('#app');
  root.innerHTML = html`<div class="auth-layout">
    <aside class="auth-aside" aria-label="Giới thiệu hệ thống">
      <div class="auth-aside__brand">
        <span class="brand-mark">${icon('cross', 'icon--lg')}</span>
        <div><strong>PharmaCare</strong><span>Pharmacy Management System</span></div>
      </div>
      <div class="auth-aside__copy">
        <h2>Quản lý nhà thuốc chính xác, an toàn và liền mạch</h2>
        <p>Một nền tảng nội bộ cho bán hàng, kho và quản trị: theo dõi tồn theo lô, cảnh báo hạn dùng và đối soát hóa đơn theo thời gian thực.</p>
        <ul class="auth-aside__features">
          <li>${icon('shield')} Phân quyền theo vai trò Admin, Bán hàng, Kho</li>
          <li>${icon('hourglass')} Cảnh báo lô sắp hết hạn và bán theo FEFO</li>
          <li>${icon('chart')} Báo cáo doanh thu và tồn kho trực quan</li>
        </ul>
      </div>
      ${pattern}
    </aside>
    <main class="auth-main" id="main-content">
      <div class="auth-card" data-auth-card>${content}</div>
    </main>
  </div>`.value;
  return qs('[data-auth-card]', root);
};
