import { database } from './database.js';
import { getSessionUserId } from './session.js';
import { forbidden, notFound, unauthorized } from '../core/errors.js';

const routes = [];

const compile = (path) => {
  const keys = [];
  const pattern = path.replace(/:([a-zA-Z]+)/g, (_, key) => {
    keys.push(key);
    return '([^/]+)';
  });
  return { regex: new RegExp(`^${pattern}$`), keys };
};

export const route = (method, path, access, handler) => {
  routes.push({ method, ...compile(path), access, handler });
};

export const dispatch = async ({ method, path, params = {}, body = {} }) => {
  await database.ready();
  const normalizedPath = path.replace(/\/+$/, '') || '/';
  for (const entry of routes) {
    if (entry.method !== method) continue;
    const match = entry.regex.exec(normalizedPath);
    if (!match) continue;
    const routeParams = Object.fromEntries(entry.keys.map((key, index) => [key, decodeURIComponent(match[index + 1])]));
    let user = null;
    if (entry.access !== 'public') {
      const userId = getSessionUserId();
      user = userId ? database.data.employees.find((employee) => employee.id === userId) : null;
      if (!user || user.status !== 'ACTIVE') throw unauthorized();
      if (Array.isArray(entry.access) && !entry.access.includes(user.role)) throw forbidden();
    }
    return entry.handler({ params: routeParams, query: params, body, user, data: database.data });
  }
  throw notFound(`Không tìm thấy API ${method} ${path}`);
};
