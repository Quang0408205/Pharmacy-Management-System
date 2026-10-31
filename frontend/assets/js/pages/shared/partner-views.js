import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { getQuery } from '../../core/dom.js';
import { formatCurrency, formatDate, formatDateTime, formatNumber } from '../../core/format.js';
import { createDataTable } from '../../components/data-table.js';
import { openFormDialog } from '../../components/form-modal.js';
import { confirmDialog, openDrawer } from '../../components/overlay.js';
import { bindTabs, detailList, rowButton, tabs } from '../../components/blocks.js';
import { statusBadge, statusOptions } from '../../components/badge.js';
import { skeletonTable } from '../../components/states.js';
import { toast } from '../../components/toast.js';
import { errorMessage } from '../../core/errors.js';
import { customerService, supplierService } from '../../services/partner-service.js';
import { openInvoiceDrawer } from './invoice-views.js';
import { openReceiptDrawer } from './receipt-views.js';

const supplierFields = [
  { name: 'name', label: 'Tên nhà cung cấp', required: true, full: true },
  { name: 'contactName', label: 'Người liên hệ' },
  { name: 'phone', label: 'Số điện thoại', type: 'tel', required: true },
  { name: 'email', label: 'Email', type: 'email' },
  { name: 'taxCode', label: 'Mã số thuế' },
  {
    name: 'paymentTerms',
    label: 'Điều khoản thanh toán',
    type: 'select',
    options: ['Thanh toán ngay', 'Công nợ 15 ngày', 'Công nợ 30 ngày'].map((value) => ({ value, label: value })),
  },
  { name: 'address', label: 'Địa chỉ', full: true },
  { name: 'note', label: 'Ghi chú', type: 'textarea', full: true },
];

const customerFields = [
  { name: 'name', label: 'Họ và tên', required: true, full: true },
  { name: 'phone', label: 'Số điện thoại', type: 'tel', required: true },
  { name: 'email', label: 'Email', type: 'email' },
  {
    name: 'gender',
    label: 'Giới tính',
    type: 'select',
    options: [
      { value: 'Nam', label: 'Nam' },
      { value: 'Nữ', label: 'Nữ' },
    ],
  },
  { name: 'birthYear', label: 'Năm sinh', type: 'number', min: 1900, max: new Date().getFullYear() },
  { name: 'address', label: 'Địa chỉ', full: true },
  { name: 'note', label: 'Ghi chú', type: 'textarea', full: true },
];

export const openSupplierForm = (supplier = null) =>
  openFormDialog({
    title: supplier ? `Sửa nhà cung cấp` : 'Thêm nhà cung cấp',
    subtitle: supplier ? supplier.name : '',
    fields: supplierFields,
    values: supplier || { paymentTerms: 'Thanh toán ngay' },
    size: 'lg',
    submitText: supplier ? 'Lưu thay đổi' : 'Thêm nhà cung cấp',
    onSubmit: async (values) => {
      const saved = supplier ? await supplierService.update(supplier.id, values) : await supplierService.create(values);
      toast.success(supplier ? 'Đã cập nhật nhà cung cấp' : 'Đã thêm nhà cung cấp', saved.name);
      return saved;
    },
  }).closed;

export const openCustomerForm = (customer = null) =>
  openFormDialog({
    title: customer ? 'Sửa khách hàng' : 'Thêm khách hàng',
    subtitle: customer ? customer.name : '',
    fields: customerFields,
    values: customer || { gender: 'Nam' },
    size: 'lg',
    submitText: customer ? 'Lưu thay đổi' : 'Thêm khách hàng',
    onSubmit: async (values) => {
      const saved = customer ? await customerService.update(customer.id, values) : await customerService.create(values);
      toast.success(customer ? 'Đã cập nhật khách hàng' : 'Đã thêm khách hàng', saved.name);
      return saved;
    },
  }).closed;

export const openQuickCustomerForm = (phone = '') =>
  openFormDialog({
    title: 'Tạo nhanh khách hàng',
    subtitle: 'Chỉ cần số điện thoại và tên để bắt đầu tích điểm.',
    fields: [
      { name: 'phone', label: 'Số điện thoại', type: 'tel', required: true },
      { name: 'name', label: 'Họ và tên', required: true, full: true },
    ],
    values: { phone },
    size: 'sm',
    submitText: 'Tạo khách hàng',
    onSubmit: async (values) => {
      const saved = await customerService.create({ ...values, gender: 'Nam' });
      toast.success('Đã thêm khách hàng', saved.name);
      return saved;
    },
  }).closed;

const receiptHistory = (receipts) =>
  receipts.length
    ? html`<div class="table-wrap"><table class="table"><thead><tr><th>Phiếu nhập</th><th>Ngày nhập</th><th class="num">Số lượng</th><th class="num">Giá trị</th><th>Trạng thái</th></tr></thead>
        <tbody>${receipts.map(
          (
            receipt,
          ) => html`<tr class="is-clickable" data-receipt="${receipt.id}" tabindex="0"><td><strong>${receipt.code}</strong></td><td>${formatDate(receipt.receivingDate)}</td>
          <td class="num">${formatNumber(receipt.totalQuantity)}</td><td class="num">${formatCurrency(receipt.totalAmount)}</td><td>${statusBadge('receipt', receipt.status)}</td></tr>`,
        )}</tbody></table></div>`
    : html`<p class="text-secondary">Chưa có phiếu nhập nào từ nhà cung cấp này.</p>`;

export const openSupplierDrawer = async (id, { role, onChanged = () => {} }) => {
  const drawer = openDrawer({ title: 'Hồ sơ nhà cung cấp', content: skeletonTable(5) });
  try {
    let supplier = await supplierService.get(id);
    const items = [
      { key: 'profile', label: 'Hồ sơ' },
      { key: 'history', label: `Lịch sử nhập (${supplier.receiptCount})` },
    ];
    const draw = (active) => {
      drawer.panel.querySelector('h2').textContent = supplier.name;
      const panel =
        active === 'profile'
          ? html`<div class="stack"><div class="row">${statusBadge('supplier', supplier.status)}<span class="text-secondary">${supplier.code}</span></div>
              ${detailList([
                ['Người liên hệ', supplier.contactName],
                ['Số điện thoại', supplier.phone],
                ['Email', supplier.email],
                ['Mã số thuế', supplier.taxCode],
                ['Điều khoản thanh toán', supplier.paymentTerms],
                ['Tổng giá trị đã nhập', formatCurrency(supplier.totalAmount)],
                ['Địa chỉ', supplier.address],
                ['Ghi chú', supplier.note],
              ])}</div>`
          : receiptHistory(supplier.receipts);
      drawer.setBody(
        html`${tabs(items, active, 'sup')}<div class="tab-panel" id="sup-panel" role="tabpanel">${panel}</div>`,
      );
      bindTabs(drawer.body, draw);
    };
    draw('profile');
    drawer.setFooter(html`<button type="button" class="btn btn--secondary" data-act="toggle">${supplier.status === 'ACTIVE' ? 'Ngừng hợp tác' : 'Kích hoạt lại'}</button>
      <button type="button" class="btn btn--primary" data-act="edit">${icon('edit', 'icon--sm')} Sửa hồ sơ</button>`);
    drawer.body.addEventListener('click', (event) => {
      const row = event.target instanceof Element ? event.target.closest('[data-receipt]') : null;
      if (row) openReceiptDrawer(row.dataset.receipt, { role });
    });
    drawer.footer.addEventListener('click', async (event) => {
      const button = event.target instanceof Element ? event.target.closest('[data-act]') : null;
      if (!button) return;
      if (button.dataset.act === 'edit') {
        const saved = await openSupplierForm(supplier);
        if (saved && saved !== true) {
          supplier = { ...supplier, ...saved };
          draw('profile');
          onChanged();
        }
      }
      if (button.dataset.act === 'toggle') {
        const next = supplier.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
        const confirmed = await confirmDialog({
          title: next === 'INACTIVE' ? 'Ngừng hợp tác?' : 'Kích hoạt lại nhà cung cấp?',
          message:
            next === 'INACTIVE'
              ? `${supplier.name} sẽ không thể chọn khi tạo phiếu nhập mới.`
              : `${supplier.name} sẽ có thể chọn lại khi nhập hàng.`,
          confirmText: 'Xác nhận',
          tone: next === 'INACTIVE' ? 'danger' : 'primary',
        });
        if (!confirmed) return;
        try {
          supplier = { ...supplier, ...(await supplierService.setStatus(supplier.id, next)) };
          drawer.footer.querySelector('[data-act="toggle"]').textContent =
            supplier.status === 'ACTIVE' ? 'Ngừng hợp tác' : 'Kích hoạt lại';
          draw('profile');
          toast.success('Đã cập nhật trạng thái', supplier.name);
          onChanged();
        } catch (error) {
          toast.error('Không thể cập nhật', errorMessage(error));
        }
      }
    });
  } catch (error) {
    drawer.setBody(html`<div class="alert alert--danger" role="alert">${errorMessage(error)}</div>`);
  }
};

export const mountSupplierTable = ({ container, role }) => {
  const query = getQuery();
  const table = createDataTable({
    container,
    columns: [
      { key: 'code', label: 'Mã', sortable: true },
      {
        key: 'name',
        label: 'Nhà cung cấp',
        sortable: true,
        render: (row) =>
          html`<div class="cell-main"><div><strong>${row.name}</strong><small>${row.contactName}</small></div></div>`,
      },
      {
        key: 'phone',
        label: 'Liên hệ',
        render: (row) => html`<div>${row.phone}<br /><small class="text-secondary">${row.email}</small></div>`,
      },
      { key: 'paymentTerms', label: 'Thanh toán' },
      { key: 'receiptCount', label: 'Số phiếu nhập', sortable: true, align: 'right' },
      { key: 'status', label: 'Trạng thái', sortable: true, render: (row) => statusBadge('supplier', row.status) },
    ],
    fetch: (params) => supplierService.list(params),
    filters: [
      { type: 'search', name: 'q', placeholder: 'Tìm tên, mã, người liên hệ, số điện thoại...' },
      { type: 'select', name: 'status', label: 'Trạng thái', options: statusOptions('supplier') },
    ],
    initialParams: { q: query.q || '' },
    defaultSort: 'code,asc',
    caption: 'Danh sách nhà cung cấp',
    emptyTitle: 'Chưa có nhà cung cấp',
    emptyIcon: 'truck',
    toolbarExtra: html`<button type="button" class="btn btn--primary" data-action="create">${icon('plus', 'icon--sm')} Thêm nhà cung cấp</button>`,
    rowActions: (row) =>
      html`${rowButton('view', `Xem hồ sơ ${row.name}`, 'eye')}${rowButton('edit', `Sửa ${row.name}`, 'edit')}`,
    onRowClick: (row) => openSupplierDrawer(row.id, { role, onChanged: () => table.reload() }),
    onAction: async (action, row) => {
      if (action === 'create' && (await openSupplierForm())) table.reload();
      if (action === 'view') openSupplierDrawer(row.id, { role, onChanged: () => table.reload() });
      if (action === 'edit' && (await openSupplierForm(row))) table.reload();
    },
  });
  table.reload();
  return table;
};

const loyaltyLabels = {
  EARN: 'Tích điểm',
  REDEEM: 'Dùng điểm',
  REDEEM_REFUND: 'Hoàn điểm',
  EARN_REVOKE: 'Thu hồi điểm',
};

const loyaltyPanel = (customer, history) =>
  html`<div class="stack">
    <div class="points-balance"><span class="points-balance__icon">${icon('star', 'icon--lg')}</span><div><strong>${formatNumber(customer.loyaltyPoints)} điểm</strong><span class="text-secondary">Số điểm hiện có của ${customer.name}</span></div></div>
    ${
      history && history.length
        ? html`<div class="table-wrap"><table class="table"><thead><tr><th>Thời gian</th><th>Loại</th><th>Hóa đơn</th><th class="num">Điểm</th><th class="num">Còn lại</th></tr></thead>
            <tbody>${history.map(
              (
                entry,
              ) => html`<tr title="${entry.note || ''}"><td class="nowrap">${formatDateTime(entry.createdAt)}</td><td>${loyaltyLabels[entry.type] || entry.type}</td><td>${entry.invoiceCode || ''}</td>
              <td class="num ${entry.points < 0 ? 'diff-negative' : 'diff-positive'}">${entry.points > 0 ? '+' : ''}${formatNumber(entry.points)}</td><td class="num">${formatNumber(entry.balanceAfter)}</td></tr>`,
            )}</tbody></table></div>`
        : html`<p class="text-secondary">${history === null ? 'Đang tải lịch sử điểm...' : 'Chưa có giao dịch điểm.'}</p>`
    }
  </div>`;

export const openCustomerDrawer = async (id, { role, onChanged = () => {} }) => {
  const drawer = openDrawer({ title: 'Hồ sơ khách hàng', content: skeletonTable(5) });
  try {
    let customer = await customerService.get(id);
    let loyaltyHistory = null;
    let loyaltyRequested = false;
    const items = () => [
      { key: 'profile', label: 'Thông tin' },
      { key: 'history', label: `Lịch sử mua (${customer.invoiceCount})` },
      { key: 'points', label: 'Điểm tích lũy' },
    ];
    const draw = (active) => {
      drawer.panel.querySelector('h2').textContent = customer.name;
      const panel =
        active === 'profile'
          ? html`<div class="stack"><span class="text-secondary">${customer.code}</span>
              ${detailList([
                ['Số điện thoại', customer.phone],
                ['Email', customer.email],
                ['Giới tính', customer.gender],
                ['Năm sinh', customer.birthYear],
                ['Số hóa đơn', formatNumber(customer.invoiceCount)],
                ['Tổng chi tiêu', formatCurrency(customer.totalSpent)],
                ['Điểm tích lũy', `${formatNumber(customer.loyaltyPoints)} điểm`],
                ['Địa chỉ', customer.address],
                ['Ghi chú', customer.note],
              ])}</div>`
          : active === 'points'
            ? loyaltyPanel(customer, loyaltyHistory)
            : customer.invoices.length
              ? html`<div class="table-wrap"><table class="table"><thead><tr><th>Hóa đơn</th><th>Thời gian</th><th class="num">Tổng tiền</th><th>Trạng thái</th></tr></thead>
                <tbody>${customer.invoices.map(
                  (
                    invoice,
                  ) => html`<tr class="is-clickable" data-invoice="${invoice.id}" tabindex="0"><td><strong>${invoice.code}</strong></td><td class="nowrap">${formatDateTime(invoice.createdAt)}</td>
                  <td class="num">${formatCurrency(invoice.total)}</td><td>${statusBadge('invoice', invoice.status)}</td></tr>`,
                )}</tbody></table></div>`
              : html`<p class="text-secondary">Khách hàng chưa có giao dịch.</p>`;
      drawer.setBody(
        html`${tabs(items(), active, 'cus')}<div class="tab-panel" id="cus-panel" role="tabpanel">${panel}</div>`,
      );
      bindTabs(drawer.body, async (key) => {
        if (key === 'points' && !loyaltyRequested) {
          loyaltyRequested = true;
          draw(key);
          try {
            loyaltyHistory = await customerService.loyalty(customer.id);
          } catch (error) {
            toast.error('Không tải được lịch sử điểm', errorMessage(error));
          }
        }
        draw(key);
      });
    };
    draw('profile');
    drawer.setFooter(
      html`<button type="button" class="btn btn--secondary" data-dismiss="cancel">Đóng</button><button type="button" class="btn btn--primary" data-act="edit">${icon('edit', 'icon--sm')} Sửa thông tin</button>`,
    );
    drawer.body.addEventListener('click', (event) => {
      const row = event.target instanceof Element ? event.target.closest('[data-invoice]') : null;
      if (row) openInvoiceDrawer(row.dataset.invoice, { role });
    });
    drawer.footer.addEventListener('click', async (event) => {
      if (!(event.target instanceof Element) || !event.target.closest('[data-act="edit"]')) return;
      const saved = await openCustomerForm(customer);
      if (saved && saved !== true) {
        customer = { ...customer, ...saved };
        draw('profile');
        onChanged();
      }
    });
  } catch (error) {
    drawer.setBody(html`<div class="alert alert--danger" role="alert">${errorMessage(error)}</div>`);
  }
};

export const mountCustomerTable = ({ container, role }) => {
  const query = getQuery();
  const table = createDataTable({
    container,
    columns: [
      { key: 'code', label: 'Mã KH', sortable: true },
      {
        key: 'name',
        label: 'Khách hàng',
        sortable: true,
        render: (row) =>
          html`<div class="cell-main"><div><strong>${row.name}</strong><small>${row.email}</small></div></div>`,
      },
      { key: 'phone', label: 'Số điện thoại', sortable: true },
      { key: 'invoiceCount', label: 'Số hóa đơn', sortable: true, align: 'right' },
      {
        key: 'loyaltyPoints',
        label: 'Điểm',
        sortable: true,
        align: 'right',
        render: (row) => formatNumber(row.loyaltyPoints),
      },
      {
        key: 'totalSpent',
        label: 'Tổng chi tiêu',
        sortable: true,
        align: 'right',
        render: (row) => formatCurrency(row.totalSpent),
      },
      { key: 'lastPurchase', label: 'Mua gần nhất', sortable: true, render: (row) => formatDate(row.lastPurchase) },
    ],
    fetch: (params) => customerService.list(params),
    filters: [{ type: 'search', name: 'q', placeholder: 'Tìm tên, mã khách, số điện thoại...' }],
    initialParams: { q: query.q || '' },
    defaultSort: 'code,desc',
    caption: 'Danh sách khách hàng',
    emptyTitle: 'Chưa có khách hàng',
    emptyIcon: 'users',
    toolbarExtra: html`<button type="button" class="btn btn--primary" data-action="create">${icon('plus', 'icon--sm')} Thêm khách hàng</button>`,
    rowActions: (row) =>
      html`${rowButton('view', `Xem ${row.name}`, 'eye')}${rowButton('edit', `Sửa ${row.name}`, 'edit')}`,
    onRowClick: (row) => openCustomerDrawer(row.id, { role, onChanged: () => table.reload() }),
    onAction: async (action, row) => {
      if (action === 'create' && (await openCustomerForm())) table.reload();
      if (action === 'view') openCustomerDrawer(row.id, { role, onChanged: () => table.reload() });
      if (action === 'edit' && (await openCustomerForm(row))) table.reload();
    },
  });
  table.reload();
  return table;
};
