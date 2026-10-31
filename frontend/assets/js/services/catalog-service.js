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
  manufacturers: (q) => api.get('/medicines/manufacturers', { q, limit: 200 }),
  uploadImage: (id, file) => {
    const form = new FormData();
    form.append('image', file);
    return api.post(`/medicines/${id}/image`, form);
  },
  removeImage: (id) => api.delete(`/medicines/${id}/image`),
};
