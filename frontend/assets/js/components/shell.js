import { html, raw } from '../core/html.js';
import { icon } from '../core/icons.js';
import { debounce, delegate, pageUrl, qs } from '../core/dom.js';
import { initials } from '../core/format.js';
import { roleLabels } from '../config/app-config.js';
import { navigation, roleTitles, searchTargets } from '../config/navigation.js';
import { logout, preferences } from '../services/session-service.js';
import { medicineService } from '../services/catalog-service.js';
import { notificationService } from '../services/admin-service.js';

const compactQuery = window.matchMedia('(max-width: 1024px)');
const mobileQuery = window.matchMedia('(max-width: 768px)');

const renderSidebar = (user, activeKey) => html`<aside class="sidebar" id="sidebar" aria-label="Thanh bên">
  <div class="sidebar__brand">
    <span class="brand-mark">${icon('cross', 'icon--lg')}</span>
    <span class="brand-text"><strong>PharmaCare</strong><span>${roleTitles[user.role]}</span></span>
  </div>
  <nav class="sidebar__nav" aria-label="Điều hướng chính">
    <ul>
      ${navigation[user.role].map(
        (item) => html`<li>
          <a class="nav-item ${item.key === activeKey ? 'is-active' : ''}" href="${pageUrl(item.href)}" title="${item.label}" ${item.key === activeKey ? raw('aria-current="page"') : ''}>
            ${icon(item.icon)}<span class="nav-item__label">${item.label}</span>
          </a>
        </li>`,
      )}
    </ul>
  </nav>
  <div class="sidebar__footer">
    <button type="button" class="sidebar-toggle" data-action="toggle-sidebar" aria-expanded="true" aria-controls="sidebar">
      ${icon('chevronsLeft')}<span class="sidebar-toggle__label">Thu gọn menu</span>
    </button>
  </div>
</aside>`;

const renderHeader = (user, crumbs, title) => html`<header class="app-header">
  <button type="button" class="icon-button menu-button" data-action="open-mobile-menu" aria-label="Mở menu điều hướng">${icon('menu')}</button>
  <nav class="breadcrumb" aria-label="Đường dẫn">
    <span>${roleTitles[user.role]}</span>
    ${crumbs.map((crumb) => html`${icon('chevronRight', 'icon--sm')}<span>${crumb}</span>`)}
    ${icon('chevronRight', 'icon--sm')}<span aria-current="page">${title}</span>
  </nav>
  <span class="app-header__spacer"></span>
  <div class="header-search" data-search>
    ${icon('search')}
    <input type="search" placeholder="Tìm thuốc theo tên, mã, mã vạch..." aria-label="Tìm kiếm thuốc toàn hệ thống" autocomplete="off" data-search-input />
    <div class="search-results hidden" data-search-results role="listbox" aria-label="Kết quả tìm kiếm"></div>
  </div>
  <div class="dropdown" data-dropdown>
    <button type="button" class="icon-button" data-dropdown-trigger aria-label="Thông báo" aria-haspopup="true" aria-expanded="false">
      ${icon('bell')}<span class="icon-button__dot hidden" data-notification-count>0</span>
    </button>
    <div class="dropdown__panel dropdown__panel--wide" data-notification-panel>
      <div class="dropdown__heading">Thông báo</div>
      <p class="text-secondary text-small dropdown__heading">Đang tải...</p>
    </div>
  </div>
  <div class="dropdown" data-dropdown>
    <button type="button" class="user-trigger" data-dropdown-trigger aria-haspopup="true" aria-expanded="false">
      <span class="avatar">${initials(user.name)}</span>
      <span class="user-trigger__info"><strong>${user.name}</strong><span>${roleLabels[user.role]}</span></span>
      ${icon('chevronDown', 'icon--sm')}
    </button>
    <div class="dropdown__panel">
      <div class="dropdown__heading">${user.name}<br /><span class="text-secondary text-small">${user.email}</span></div>
      <div class="dropdown__divider"></div>
      <button type="button" class="dropdown__item" data-action="toggle-theme">${icon('moon')}<span data-theme-label>Chế độ tối</span></button>
      <div class="dropdown__divider"></div>
      <button type="button" class="dropdown__item dropdown__item--danger" data-action="logout">${icon('logout')} Đăng xuất</button>
    </div>
  </div>
</header>`;

const renderNotifications = (panel, countBadge, items) => {
  countBadge.textContent = String(items.length);
  countBadge.classList.toggle('hidden', !items.length);
  panel.innerHTML = html`<div class="dropdown__heading">Thông báo</div>
    ${
      items.length
        ? items.map(
            (item) => html`<a class="dropdown__item notification-item" href="${pageUrl(item.link)}">
            ${icon(item.tone === 'danger' ? 'alertCircle' : item.tone === 'info' ? 'info' : 'alertTriangle')}
            <span class="notification-item__text"><strong>${item.title}</strong><span class="text-secondary text-small">${item.message}</span></span>
          </a>`,
          )
        : html`<p class="text-secondary text-small dropdown__heading">Không có thông báo mới.</p>`
    }`.value;
};

const bindDropdowns = (root) => {
  const closeAll = (except) => {
    root.querySelectorAll('[data-dropdown].is-open').forEach((dropdown) => {
      if (dropdown === except) return;
      dropdown.classList.remove('is-open');
      dropdown.querySelector('[data-dropdown-trigger]').setAttribute('aria-expanded', 'false');
    });
  };
  delegate(root, 'click', '[data-dropdown-trigger]', (event, trigger) => {
    const dropdown = trigger.closest('[data-dropdown]');
    const open = !dropdown.classList.contains('is-open');
    closeAll(dropdown);
    dropdown.classList.toggle('is-open', open);
    trigger.setAttribute('aria-expanded', String(open));
  });
  document.addEventListener('click', (event) => {
    if (!(event.target instanceof Element) || !event.target.closest('[data-dropdown]')) closeAll();
  });
  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') closeAll();
  });
};

const bindSearch = (root, role) => {
  const input = qs('[data-search-input]', root);
  const results = qs('[data-search-results]', root);
  const target = pageUrl(searchTargets[role]);
  const go = (query) => {
    window.location.href = `${target}?q=${encodeURIComponent(query)}`;
  };
  const search = debounce(async () => {
    const query = input.value.trim();
    if (query.length < 2) {
      results.classList.add('hidden');
      return;
    }
    try {
      const { items } = await medicineService.list({
        q: query,
        size: 6,
        sellable: role === 'SALES' ? 'true' : undefined,
      });
      results.innerHTML = html`${
        items.length
          ? items.map(
              (item) =>
                html`<button type="button" class="search-results__item" data-result="${item.name}" role="option"><strong>${item.name}</strong><br /><span class="text-secondary text-small">${item.code} · ${item.manufacturer}</span></button>`,
            )
          : html`<p class="text-secondary text-small search-results__item">Không tìm thấy thuốc phù hợp.</p>`
      }`.value;
      results.classList.remove('hidden');
    } catch {
      results.classList.add('hidden');
    }
  }, 280);
  input.addEventListener('input', search);
  input.addEventListener('keydown', (event) => {
    if (event.key === 'Enter' && input.value.trim()) go(input.value.trim());
    if (event.key === 'Escape') results.classList.add('hidden');
  });
  delegate(results, 'click', '[data-result]', (event, button) => go(button.dataset.result));
  document.addEventListener('click', (event) => {
    if (!(event.target instanceof Element) || !event.target.closest('[data-search]')) results.classList.add('hidden');
  });
};

export const buildShell = ({ root, user, navKey, title, crumbs = [] }) => {
  root.innerHTML = html`
    <a class="skip-link" href="#main-content">Bỏ qua điều hướng</a>
    <div class="app-shell" id="app-shell">
      ${renderSidebar(user, navKey)}
      <div class="sidebar-backdrop" data-action="close-mobile-menu"></div>
      <div class="app-shell__body">
        ${renderHeader(user, crumbs, title)}
        <main class="app-main" id="main-content" tabindex="-1"><div class="page-content" data-page-content></div></main>
      </div>
    </div>
  `.value;

  const shell = qs('#app-shell', root);
  const toggle = qs('[data-action="toggle-sidebar"]', root);
  const themeLabel = qs('[data-theme-label]', root);

  const applyCollapsed = (collapsed) => {
    shell.classList.toggle('is-collapsed', collapsed);
    toggle.setAttribute('aria-expanded', String(!collapsed));
    toggle.innerHTML =
      html`${icon(collapsed ? 'chevronsRight' : 'chevronsLeft')}<span class="sidebar-toggle__label">${collapsed ? 'Mở rộng menu' : 'Thu gọn menu'}</span>`.value;
  };
  const resolveCollapsed = () => {
    const { sidebarCollapsed } = preferences.get();
    return compactQuery.matches ? sidebarCollapsed !== false : sidebarCollapsed === true;
  };
  applyCollapsed(resolveCollapsed());
  compactQuery.addEventListener('change', () => applyCollapsed(resolveCollapsed()));

  const refreshThemeLabel = () => {
    themeLabel.textContent = preferences.get().theme === 'dark' ? 'Chế độ sáng' : 'Chế độ tối';
  };
  refreshThemeLabel();

  delegate(root, 'click', '[data-action]', (event, button) => {
    const { action } = button.dataset;
    if (action === 'toggle-sidebar') {
      if (mobileQuery.matches) return;
      const collapsed = !shell.classList.contains('is-collapsed');
      preferences.update({ sidebarCollapsed: collapsed });
      applyCollapsed(collapsed);
    }
    if (action === 'open-mobile-menu') {
      if (compactQuery.matches && !mobileQuery.matches) {
        preferences.update({ sidebarCollapsed: false });
        applyCollapsed(false);
      } else shell.classList.add('is-mobile-open');
    }
    if (action === 'close-mobile-menu') {
      shell.classList.remove('is-mobile-open');
      if (compactQuery.matches && !mobileQuery.matches) {
        preferences.update({ sidebarCollapsed: true });
        applyCollapsed(true);
      }
    }
    if (action === 'toggle-theme') {
      preferences.update({ theme: preferences.get().theme === 'dark' ? 'light' : 'dark' });
      refreshThemeLabel();
      window.dispatchEvent(new CustomEvent('pms:theme-changed'));
    }
    if (action === 'logout') logout();
  });

  bindDropdowns(root);
  bindSearch(root, user.role);

  const panel = qs('[data-notification-panel]', root);
  const badge = qs('[data-notification-count]', root);
  notificationService
    .list()
    .then((items) => renderNotifications(panel, badge, items))
    .catch(() => {
      panel.innerHTML =
        html`<div class="dropdown__heading">Thông báo</div><p class="text-secondary text-small dropdown__heading">Không tải được thông báo.</p>`.value;
    });

  document.addEventListener('keydown', (event) => {
    if (event.key === 'Escape') shell.classList.remove('is-mobile-open');
  });

  return qs('[data-page-content]', root);
};
