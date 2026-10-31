import { api } from '../api/client.js';

export const employeeService = {
  list: (params) => api.get('/employees', params),
  get: (id) => api.get(`/employees/${id}`),
  assignRole: (id, role) => api.patch(`/employees/${id}`, { role }),
  setStatus: (id, status) => api.patch(`/employees/${id}/status`, { status }),
  create: (payload) => api.post('/employees', payload),
  resetLink: (id) => api.post(`/employees/${id}/reset-link`, {}),
  unlock: (id) => api.post(`/employees/${id}/unlock`, {}),
};

export const settingsService = {
  get: () => api.get('/settings'),
  update: (payload) => api.put('/settings', payload),
};

export const activityService = {
  list: (params) => api.get('/activity-logs', params),
};

export const notificationService = {
  list: () => api.get('/notifications'),
};

export const dashboardService = {
  admin: () => api.get('/dashboard/admin'),
  warehouse: () => api.get('/dashboard/warehouse'),
};

export const reportService = {
  summary: (params) => api.get('/reports/summary', params),
  revenue: (params) => api.get('/reports/revenue', params),
  bestSellers: (params) => api.get('/reports/best-sellers', params),
  inventoryDistribution: () => api.get('/reports/inventory-distribution'),
  revenueByEmployee: (params) => api.get('/reports/revenue-by-employee', params),
};
