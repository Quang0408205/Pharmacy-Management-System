import { appConfig } from '../../config/app-config.js';
import { ApiError } from '../../core/errors.js';

const buildUrl = (path, params) => {
  const url = new URL(`${appConfig.apiBaseUrl}${path}`, window.location.origin);
  Object.entries(params || {}).forEach(([key, value]) => url.searchParams.set(key, value));
  return url;
};

const readBody = async (response) => {
  const text = await response.text();
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return null;
  }
};

export const httpAdapter = {
  async request({ method, path, params, body }) {
    let response;
    try {
      response = await fetch(buildUrl(path, params), {
        method,
        credentials: 'include',
        headers: { Accept: 'application/json', ...(body ? { 'Content-Type': 'application/json' } : {}) },
        body: body ? JSON.stringify(body) : undefined,
      });
    } catch {
      throw new ApiError(0, 'NETWORK_ERROR', 'Không thể kết nối tới máy chủ. Vui lòng kiểm tra mạng và thử lại.');
    }
    const payload = await readBody(response);
    if (!response.ok) {
      throw new ApiError(
        response.status,
        payload?.code || 'HTTP_ERROR',
        payload?.message || 'Yêu cầu không thành công.',
        payload?.fieldErrors || {},
      );
    }
    return payload;
  },
};
