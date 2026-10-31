import { api } from '../api/client.js';

export const receiptService = {
  list: (params) => api.get('/receipts', params),
  get: (id) => api.get(`/receipts/${id}`),
  nextCode: () => api.get('/receipts/next-code'),
  create: (payload) => api.post('/receipts', payload),
  update: (id, payload) => api.put(`/receipts/${id}`, payload),
  remove: (id) => api.delete(`/receipts/${id}`),
};

export const batchService = {
  list: (params) => api.get('/batches', params),
  get: (id) => api.get(`/batches/${id}`),
};

export const inventoryService = {
  summary: () => api.get('/inventory/summary'),
  expirySummary: () => api.get('/expiry/summary'),
  movements: (params) => api.get('/movements', params),
};

export const stocktakeService = {
  list: (params) => api.get('/stocktakes', params),
  get: (id) => api.get(`/stocktakes/${id}`),
  create: (payload) => api.post('/stocktakes', payload),
  save: (id, payload) => api.put(`/stocktakes/${id}`, payload),
  remove: (id) => api.delete(`/stocktakes/${id}`),
  submit: (id, payload) => api.post(`/stocktakes/${id}/submit`, payload),
  approve: (id, note) => api.post(`/stocktakes/${id}/approve`, { note }),
  reject: (id, note) => api.post(`/stocktakes/${id}/reject`, { note }),
};

export const disposalService = {
  list: (params) => api.get('/disposals', params),
  get: (id) => api.get(`/disposals/${id}`),
  create: (payload) => api.post('/disposals', payload),
  update: (id, payload) => api.put(`/disposals/${id}`, payload),
  remove: (id) => api.delete(`/disposals/${id}`),
  submit: (id) => api.post(`/disposals/${id}/submit`),
  execute: (id) => api.post(`/disposals/${id}/execute`),
  approve: (id, note) => api.post(`/disposals/${id}/approve`, { note }),
  reject: (id, note) => api.post(`/disposals/${id}/reject`, { note }),
};
