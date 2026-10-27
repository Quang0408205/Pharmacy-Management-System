import { api } from '../api/client.js';

export const authService = {
  login: ({ identifier, password, remember }) => api.post('/auth/login', { identifier, password, remember }),
  logout: () => api.post('/auth/logout', {}),
  currentUser: async () => (await api.get('/auth/me')).user,
  register: (payload) => api.post('/auth/register', payload),
  checkInvitation: (code) => api.get(`/auth/invitations/${encodeURIComponent(code)}`),
  forgotPassword: (email) => api.post('/auth/forgot-password', { email }),
  checkResetToken: (token) => api.get(`/auth/reset-tokens/${encodeURIComponent(token)}`),
  resetPassword: ({ token, password }) => api.post('/auth/reset-password', { token, password }),
};
