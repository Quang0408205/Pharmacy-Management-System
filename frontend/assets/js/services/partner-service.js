import { api } from '../api/client.js';

export const supplierService = {
  list: (params) => api.get('/suppliers', params),
  get: (id) => api.get(`/suppliers/${id}`),
  create: (payload) => api.post('/suppliers', payload),
  update: (id, payload) => api.put(`/suppliers/${id}`, payload),
  setStatus: (id, status) => api.patch(`/suppliers/${id}/status`, { status }),
};

export const customerService = {
  list: (params) => api.get('/customers', params),
  get: (id) => api.get(`/customers/${id}`),
  byPhone: (phone) => api.get('/customers/by-phone', { phone }),
  loyalty: (id) => api.get(`/customers/${id}/loyalty`),
  create: (payload) => api.post('/customers', payload),
  update: (id, payload) => api.put(`/customers/${id}`, payload),
};
