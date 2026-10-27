export const appConfig = {
  apiMode: 'mock',
  apiBaseUrl: '/api/v1',
  mockLatency: { min: 80, max: 240 },
  mockSeedVersion: 4,
  storagePrefix: 'pms',
  defaultPageSize: 10,
};

export const roles = {
  ADMIN: 'ADMIN',
  SALES: 'SALES',
  WAREHOUSE: 'WAREHOUSE',
};

export const roleLabels = {
  ADMIN: 'Quản trị viên',
  SALES: 'Nhân viên bán thuốc',
  WAREHOUSE: 'Nhân viên kho',
};

export const roleHome = {
  ADMIN: 'pages/admin/dashboard.html',
  SALES: 'pages/sales/pos.html',
  WAREHOUSE: 'pages/warehouse/dashboard.html',
};

export const roleFolder = {
  ADMIN: 'admin',
  SALES: 'sales',
  WAREHOUSE: 'warehouse',
};
