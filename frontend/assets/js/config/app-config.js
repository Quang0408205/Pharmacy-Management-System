export const appConfig = {
  apiBaseUrl: '/api/v1',
  storagePrefix: 'pms',
  defaultPageSize: 10,
};

export const medicineUnits = [
  'Hộp',
  'Vỉ',
  'Viên',
  'Chai',
  'Lọ',
  'Tuýp',
  'Gói',
  'Ống',
  'Cái',
  'Bộ',
  'Cuộn',
  'Hũ',
  'Túi',
];

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
