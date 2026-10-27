import { roleHome } from '../config/app-config.js';
import { pageUrl } from '../core/dom.js';
import { persistent, session } from '../core/storage.js';
import { authService } from './auth-service.js';
import { setUnauthorizedHandler } from '../api/client.js';

const flashKey = 'flash';
const prefsKey = 'prefs';

export const flash = {
  set: (message) => session.set(flashKey, message),
  take: () => {
    const message = session.get(flashKey);
    session.remove(flashKey);
    return message;
  },
};

export const preferences = {
  get: () => persistent.get(prefsKey, { theme: 'light', density: 'comfortable', sidebarCollapsed: null }),
  update(patch) {
    const next = { ...this.get(), ...patch };
    persistent.set(prefsKey, next);
    this.apply();
    return next;
  },
  apply() {
    const { theme, density } = this.get();
    document.documentElement.dataset.theme = theme === 'dark' ? 'dark' : 'light';
    document.documentElement.dataset.density = density === 'compact' ? 'compact' : 'comfortable';
  },
};

export const goToLogin = (reason) => {
  if (reason) flash.set(reason);
  const target = new URL(pageUrl('pages/auth/login.html'));
  window.location.replace(target.href);
};

export const goHome = (role) => {
  window.location.replace(pageUrl(roleHome[role]));
};

export const requireSession = async (allowedRole) => {
  setUnauthorizedHandler(() =>
    goToLogin({ type: 'warning', title: 'Phiên đăng nhập đã hết hạn', message: 'Vui lòng đăng nhập lại.' }),
  );
  let user;
  try {
    user = await authService.currentUser();
  } catch {
    goToLogin();
    return new Promise(() => {});
  }
  if (allowedRole && user.role !== allowedRole) {
    flash.set({
      type: 'error',
      title: 'Không có quyền truy cập',
      message: 'Bạn không được phép mở trang của vai trò khác.',
    });
    goHome(user.role);
    return new Promise(() => {});
  }
  return user;
};

export const logout = async () => {
  try {
    await authService.logout();
  } finally {
    goToLogin({ type: 'success', title: 'Đã đăng xuất', message: 'Hẹn gặp lại bạn.' });
  }
};
