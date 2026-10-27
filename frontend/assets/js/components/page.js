import { preferences, flash, requireSession } from '../services/session-service.js';
import { buildShell } from './shell.js';
import { toast, showToast } from './toast.js';
import { errorState } from './states.js';
import { errorMessage } from '../core/errors.js';
import { qs } from '../core/dom.js';

export const startPage = async ({ role, nav, title, crumbs = [], render }) => {
  preferences.apply();
  const user = await requireSession(role);
  const root = qs('#app');
  document.title = `${title} · PharmaCare`;
  const content = buildShell({ root, user, navKey: nav, title, crumbs });

  const message = flash.take();
  if (message) showToast(message);

  const context = { user, content, root };
  const run = async () => {
    try {
      await render(context);
    } catch (error) {
      content.innerHTML = errorState(error).value;
      toast.error('Không thể mở trang', errorMessage(error));
      const retry = content.querySelector('[data-retry]');
      if (retry) retry.addEventListener('click', run);
    }
  };
  await run();
  return context;
};
