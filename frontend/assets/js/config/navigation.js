export const navigation = {
  ADMIN: [
    { key: 'dashboard', label: 'Tổng quan', icon: 'dashboard', href: 'pages/admin/dashboard.html' },
    { key: 'medicines', label: 'Quản lý thuốc', icon: 'pill', href: 'pages/admin/medicines.html' },
    { key: 'categories', label: 'Danh mục thuốc', icon: 'layers', href: 'pages/admin/categories.html' },
    { key: 'inventory', label: 'Quản lý kho', icon: 'package', href: 'pages/admin/inventory.html' },
    { key: 'receipts', label: 'Phiếu nhập', icon: 'clipboard', href: 'pages/admin/receipts.html' },
    { key: 'suppliers', label: 'Nhà cung cấp', icon: 'truck', href: 'pages/admin/suppliers.html' },
    { key: 'invoices', label: 'Hóa đơn', icon: 'receipt', href: 'pages/admin/invoices.html' },
    { key: 'customers', label: 'Khách hàng', icon: 'users', href: 'pages/admin/customers.html' },
    { key: 'employees', label: 'Nhân viên', icon: 'idCard', href: 'pages/admin/employees.html' },
    { key: 'reports', label: 'Thống kê & báo cáo', icon: 'chart', href: 'pages/admin/reports.html' },
    { key: 'activity', label: 'Nhật ký hoạt động', icon: 'activity', href: 'pages/admin/activity.html' },
    { key: 'settings', label: 'Cài đặt', icon: 'sliders', href: 'pages/admin/settings.html' },
  ],
  SALES: [
    { key: 'pos', label: 'Bán thuốc tại quầy', icon: 'cart', href: 'pages/sales/pos.html' },
    { key: 'invoices', label: 'Hóa đơn', icon: 'receipt', href: 'pages/sales/invoices.html' },
    { key: 'customers', label: 'Khách hàng', icon: 'users', href: 'pages/sales/customers.html' },
    { key: 'lookup', label: 'Tra cứu thuốc', icon: 'search', href: 'pages/sales/lookup.html' },
    { key: 'history', label: 'Lịch sử bán', icon: 'history', href: 'pages/sales/history.html' },
  ],
  WAREHOUSE: [
    { key: 'dashboard', label: 'Tổng quan kho', icon: 'dashboard', href: 'pages/warehouse/dashboard.html' },
    { key: 'receiving', label: 'Nhập thuốc', icon: 'inbox', href: 'pages/warehouse/receiving.html' },
    { key: 'receipts', label: 'Phiếu nhập', icon: 'clipboard', href: 'pages/warehouse/receipts.html' },
    { key: 'inventory', label: 'Tồn kho', icon: 'package', href: 'pages/warehouse/inventory.html' },
    { key: 'batches', label: 'Lô thuốc', icon: 'archive', href: 'pages/warehouse/batches.html' },
    { key: 'expiry', label: 'Hạn sử dụng', icon: 'hourglass', href: 'pages/warehouse/expiry.html' },
    { key: 'stocktake', label: 'Kiểm kê', icon: 'clipboardCheck', href: 'pages/warehouse/stocktake.html' },
    { key: 'suppliers', label: 'Nhà cung cấp', icon: 'truck', href: 'pages/warehouse/suppliers.html' },
    { key: 'movements', label: 'Lịch sử kho', icon: 'history', href: 'pages/warehouse/movements.html' },
  ],
};

export const roleTitles = {
  ADMIN: 'Quản trị hệ thống',
  SALES: 'Bán hàng tại quầy',
  WAREHOUSE: 'Vận hành kho',
};

export const searchTargets = {
  ADMIN: 'pages/admin/medicines.html',
  SALES: 'pages/sales/lookup.html',
  WAREHOUSE: 'pages/warehouse/inventory.html',
};
