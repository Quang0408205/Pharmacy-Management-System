import { appConfig } from '../config/app-config.js';

let adapterPromise = null;
let unauthorizedHandler = null;

const resolveAdapter = () => {
  if (!adapterPromise) {
    adapterPromise =
      appConfig.apiMode === 'mock'
        ? import('./adapters/mock-adapter.js').then((module) => module.mockAdapter)
        : import('./adapters/http-adapter.js').then((module) => module.httpAdapter);
  }
  return adapterPromise;
};

const cleanParams = (params) =>
  Object.fromEntries(
    Object.entries(params || {}).filter(([, value]) => value !== undefined && value !== null && value !== ''),
  );

const send = async (method, path, { params, body } = {}) => {
  const adapter = await resolveAdapter();
  try {
    return await adapter.request({ method, path, params: cleanParams(params), body });
  } catch (error) {
    if (error.status === 401 && unauthorizedHandler && !path.startsWith('/auth/')) unauthorizedHandler(error);
    throw error;
  }
};

export const setUnauthorizedHandler = (handler) => {
  unauthorizedHandler = handler;
};

export const api = {
  get: (path, params) => send('GET', path, { params }),
  post: (path, body) => send('POST', path, { body }),
  put: (path, body) => send('PUT', path, { body }),
  patch: (path, body) => send('PATCH', path, { body }),
  delete: (path) => send('DELETE', path),
};
