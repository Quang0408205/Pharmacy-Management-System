import { api } from '../api/client.js';
import { session } from '../core/storage.js';

const userKey = 'user';

export const authService = {
  login: async ({ identifier, password, remember }) => {
    const result = await api.post('/auth/login', { identifier, password, remember });
    session.set(userKey, result.user);
    return result;
  },
  logout: async () => {
    session.remove(userKey);
    return api.post('/auth/logout', {});
  },
  currentUser: async () => {
    try {
      const { user } = await api.get('/auth/me');
      session.set(userKey, user);
      return user;
    } catch (error) {
      session.remove(userKey);
      throw error;
    }
  },
  cachedUser: () => session.get(userKey),
  forgetUser: () => session.remove(userKey),
  activationInfo: (token) => api.get(`/auth/activation/${encodeURIComponent(token)}`),
  activate: ({ token, password }) => api.post('/auth/activate', { token, password }),
  checkResetToken: (token) => api.get(`/auth/reset-tokens/${encodeURIComponent(token)}`),
  resetPassword: ({ token, password }) => api.post('/auth/reset-password', { token, password }),
};
