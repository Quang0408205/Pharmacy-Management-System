import { html } from '../core/html.js';

const catalog = {
  medicineStatus: {
    ACTIVE: ['success', 'Đang kinh doanh'],
    INACTIVE: ['neutral', 'Ngừng kinh doanh'],
  },
  stock: {
    IN_STOCK: ['success', 'Còn hàng'],
    LOW_STOCK: ['warning', 'Sắp hết'],
    OUT_OF_STOCK: ['danger', 'Hết hàng'],
  },
  batch: {
    AVAILABLE: ['success', 'Còn hàng'],
    LOW_STOCK: ['warning', 'Tồn thấp'],
    EXPIRING_SOON: ['warning', 'Sắp hết hạn'],
    EXPIRED: ['danger', 'Hết hạn'],
    OUT_OF_STOCK: ['neutral', 'Hết hàng'],
  },
  invoice: {
    COMPLETED: ['success', 'Hoàn tất'],
    CANCELLED: ['danger', 'Đã hủy'],
  },
  receipt: {
    DRAFT: ['info', 'Nháp'],
    CONFIRMED: ['success', 'Đã nhập kho'],
    CANCELLED: ['danger', 'Đã hủy'],
  },
  stocktake: {
    DRAFT: ['neutral', 'Nháp'],
    PENDING_APPROVAL: ['warning', 'Chờ duyệt'],
    APPROVED: ['success', 'Đã duyệt'],
    REJECTED: ['danger', 'Từ chối'],
  },
  movement: {
    IN: ['success', 'Nhập kho'],
    OUT: ['info', 'Xuất kho'],
    ADJUSTMENT: ['warning', 'Điều chỉnh'],
    WRITE_OFF: ['danger', 'Hủy hàng'],
  },
  prescription: {
    OTC: ['neutral', 'Không kê đơn'],
    RX: ['warning', 'Kê đơn'],
    CONTROLLED: ['danger', 'Kiểm soát đặc biệt'],
  },
  employee: {
    ACTIVE: ['success', 'Hoạt động'],
    INACTIVE: ['neutral', 'Vô hiệu hóa'],
  },
  supplier: {
    ACTIVE: ['success', 'Đang hợp tác'],
    INACTIVE: ['neutral', 'Ngừng hợp tác'],
  },
  role: {
    ADMIN: ['primary', 'Quản trị viên'],
    SALES: ['info', 'Nhân viên bán hàng'],
    WAREHOUSE: ['success', 'Nhân viên kho'],
  },
  invitation: {
    PENDING: ['info', 'Đang chờ'],
    USED: ['success', 'Đã sử dụng'],
    EXPIRED: ['neutral', 'Hết hạn'],
    REVOKED: ['danger', 'Đã thu hồi'],
  },
};

export const paymentLabels = { CASH: 'Tiền mặt', TRANSFER: 'Chuyển khoản', CARD: 'Thẻ' };

export const movementLabels = { IN: 'Nhập kho', OUT: 'Xuất kho', ADJUSTMENT: 'Điều chỉnh', WRITE_OFF: 'Hủy hàng' };

export const statusLabel = (kind, value) => (catalog[kind] && catalog[kind][value] ? catalog[kind][value][1] : value);

export const statusBadge = (kind, value) => {
  const entry = catalog[kind] && catalog[kind][value];
  const [tone, label] = entry || ['neutral', value];
  return html`<span class="badge badge--${tone}">${label}</span>`;
};

export const statusOptions = (kind) => Object.entries(catalog[kind]).map(([value, [, label]]) => ({ value, label }));
