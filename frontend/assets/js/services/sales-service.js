import { api } from '../api/client.js';

export const invoiceService = {
  list: (params) => api.get('/invoices', params),
  get: (id) => api.get(`/invoices/${id}`),
  nextNumber: () => api.get('/invoices/next-number'),
  checkout: (payload) => api.post('/invoices', payload),
  cancel: (id, reason) => api.post(`/invoices/${id}/cancel`, { reason }),
  drafts: () => api.get('/invoice-drafts'),
  saveDraft: (payload) => api.post('/invoice-drafts', payload),
  removeDraft: (id) => api.delete(`/invoice-drafts/${id}`),
  salesSummary: () => api.get('/dashboard/sales'),
};
