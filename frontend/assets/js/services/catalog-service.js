import { api } from '../api/client.js';

export const categoryService = {
  list: () => api.get('/categories'),
  create: (payload) => api.post('/categories', payload),
  update: (id, payload) => api.put(`/categories/${id}`, payload),
  remove: (id) => api.delete(`/categories/${id}`),
};

export const medicineService = {
  list: (params) => api.get('/medicines', params),
  get: (id) => api.get(`/medicines/${id}`),
  nextCode: () => api.get('/medicines/next-code'),
  create: (payload) => api.post('/medicines', payload),
  update: (id, payload) => api.put(`/medicines/${id}`, payload),
  setStatus: (id, status) => api.patch(`/medicines/${id}/status`, { status }),
};
