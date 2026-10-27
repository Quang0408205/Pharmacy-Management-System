import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { getQuery } from '../../core/dom.js';
import { formatCurrency, formatDate, formatDateTime, formatNumber } from '../../core/format.js';
import { createDataTable } from '../../components/data-table.js';
import { confirmDialog, openDrawer, openModal } from '../../components/overlay.js';
import { openFormDialog } from '../../components/form-modal.js';
import { detailList, rowButton } from '../../components/blocks.js';
import { paymentLabels, statusBadge, statusOptions } from '../../components/badge.js';
import { skeletonTable } from '../../components/states.js';
import { toast } from '../../components/toast.js';
import { invoiceService } from '../../services/sales-service.js';
import { errorMessage } from '../../core/errors.js';

export const invoiceSheet = (invoice) => html`<article class="print-sheet" aria-label="Hóa đơn ${invoice.code}">
  <h3>${invoice.pharmacy ? invoice.pharmacy.name : 'Nhà thuốc'}</h3>
  ${invoice.pharmacy ? html`<p class="text-secondary text-small text-center">${invoice.pharmacy.address} · ${invoice.pharmacy.phone}</p>` : ''}
  <h3>HÓA ĐƠN BÁN HÀNG (MẪU)</h3>
  <p class="text-small">Số: <strong>${invoice.code}</strong> · ${formatDateTime(invoice.createdAt)}<br />Thu ngân: ${invoice.employeeName}<br />Khách hàng: ${invoice.customerName}</p>
  <table class="table">
    <thead><tr><th>Thuốc</th><th class="num">SL</th><th class="num">Đơn giá</th><th class="num">Thành tiền</th></tr></thead>
    <tbody>${invoice.items.map(
      (
        item,
      ) => html`<tr><td>${item.medicineName}<br /><small class="text-secondary">${item.allocations.map((allocation) => `Lô ${allocation.batchCode} (HSD ${formatDate(allocation.expiryDate)})`).join(', ')}</small></td>
      <td class="num">${item.quantity} ${item.unit}</td><td class="num">${formatCurrency(item.unitPrice)}</td><td class="num">${formatCurrency(item.subtotal)}</td></tr>`,
    )}</tbody>
  </table>
  <div class="summary-lines">
    <div class="summary-lines__row"><span>Tạm tính</span><span>${formatCurrency(invoice.subtotal)}</span></div>
    <div class="summary-lines__row"><span>Giảm giá</span><span>-${formatCurrency(invoice.discountAmount)}</span></div>
    <div class="summary-lines__row summary-lines__row--total"><span>Tổng thanh toán</span><span>${formatCurrency(invoice.total)}</span></div>
    <div class="summary-lines__row"><span>${paymentLabels[invoice.paymentMethod]}</span><span>${formatCurrency(invoice.receivedAmount)}</span></div>
    <div class="summary-lines__row"><span>Tiền thừa</span><span>${formatCurrency(invoice.changeAmount)}</span></div>
  </div>
  <p class="text-small text-secondary">Đây là hóa đơn mẫu cho môi trường demo, không có giá trị pháp lý.</p>
</article>`;

export const openPrintPreview = (invoice) => {
  const modal = openModal({
    title: `Hóa đơn ${invoice.code}`,
    size: 'lg',
    content: invoiceSheet(invoice),
    footer: html`<button type="button" class="btn btn--secondary" data-dismiss="cancel">Đóng</button><button type="button" class="btn btn--primary" data-print>${icon('printer', 'icon--sm')} In hóa đơn mẫu</button>`,
  });
  modal.footer.querySelector('[data-print]').addEventListener('click', () => window.print());
  return modal;
};

export const openInvoiceDrawer = async (id, { role, onChanged = () => {} }) => {
  const drawer = openDrawer({ title: 'Chi tiết hóa đơn', content: skeletonTable(6) });
  try {
    let invoice = await invoiceService.get(id);
    const draw = () => {
      drawer.panel.querySelector('h2').textContent = invoice.code;
      drawer.setBody(html`<div class="stack">
        <div class="row">${statusBadge('invoice', invoice.status)}<span class="text-secondary">${formatDateTime(invoice.createdAt)}</span></div>
        ${invoice.status === 'CANCELLED' ? html`<div class="alert alert--danger">${icon('ban')}<span>Đã hủy: ${invoice.cancelReason}</span></div>` : ''}
        <h3>Nhân viên & khách hàng</h3>
        ${detailList([
          [
            'Nhân viên bán',
            invoice.employee ? `${invoice.employee.name} (${invoice.employee.email})` : invoice.employeeName,
          ],
          ['Khách hàng', invoice.customer ? `${invoice.customer.name} · ${invoice.customer.phone}` : 'Khách lẻ'],
          ['Mã đơn thuốc', invoice.prescriptionCode],
          ['Ghi chú', invoice.note],
        ])}
        <h3>Danh sách thuốc</h3>
        <div class="table-wrap"><table class="table"><thead><tr><th>Thuốc</th><th>Lô xuất (FEFO)</th><th class="num">SL</th><th class="num">Đơn giá</th><th class="num">Thành tiền</th></tr></thead>
          <tbody>${invoice.items.map(
            (
              item,
            ) => html`<tr><td><strong>${item.medicineName}</strong><br /><small class="text-secondary">${item.medicineCode}</small></td>
            <td>${item.allocations.length ? item.allocations.map((allocation) => html`<div class="text-small">${allocation.batchCode} × ${allocation.quantity} · HSD ${formatDate(allocation.expiryDate)}</div>`) : '—'}</td>
            <td class="num">${formatNumber(item.quantity)} ${item.unit}</td><td class="num">${formatCurrency(item.unitPrice)}</td><td class="num">${formatCurrency(item.subtotal)}</td></tr>`,
          )}</tbody></table></div>
        <h3>Thanh toán</h3>
        <div class="summary-lines">
          <div class="summary-lines__row"><span>Tạm tính</span><span>${formatCurrency(invoice.subtotal)}</span></div>
          <div class="summary-lines__row"><span>Giảm giá</span><span>-${formatCurrency(invoice.discountAmount)}</span></div>
          <div class="summary-lines__row summary-lines__row--total"><span>Tổng thanh toán</span><span>${formatCurrency(invoice.total)}</span></div>
          <div class="summary-lines__row"><span>Phương thức</span><span>${paymentLabels[invoice.paymentMethod]}</span></div>
          <div class="summary-lines__row"><span>Khách đưa</span><span>${formatCurrency(invoice.receivedAmount)}</span></div>
          <div class="summary-lines__row"><span>Tiền thừa</span><span>${formatCurrency(invoice.changeAmount)}</span></div>
        </div>
      </div>`);
      drawer.setFooter(html`
        <button type="button" class="btn btn--secondary" data-dismiss="cancel">Đóng</button>
        ${role === 'ADMIN' && invoice.status === 'COMPLETED' ? html`<button type="button" class="btn btn--danger" data-act="cancel">${icon('ban', 'icon--sm')} Hủy hóa đơn</button>` : ''}
        <button type="button" class="btn btn--primary" data-act="print">${icon('printer', 'icon--sm')} Xem / in hóa đơn</button>`);
    };
    draw();
    drawer.footer.addEventListener('click', async (event) => {
      const button = event.target instanceof Element ? event.target.closest('[data-act]') : null;
      if (!button) return;
      if (button.dataset.act === 'print') openPrintPreview(invoice);
      if (button.dataset.act === 'cancel') {
        const confirmed = await confirmDialog({
          title: 'Hủy hóa đơn?',
          message: `Hóa đơn ${invoice.code} sẽ bị hủy và số lượng thuốc được hoàn lại đúng lô đã xuất.`,
          confirmText: 'Tiếp tục',
          tone: 'danger',
        });
        if (!confirmed) return;
        const result = await openFormDialog({
          title: 'Lý do hủy hóa đơn',
          fields: [{ name: 'reason', label: 'Lý do', type: 'textarea', required: true, full: true }],
          submitText: 'Hủy hóa đơn',
          onSubmit: ({ reason }) => invoiceService.cancel(invoice.id, reason),
        }).closed;
        if (result && result !== true) {
          toast.success('Đã hủy hóa đơn', invoice.code);
          invoice = await invoiceService.get(invoice.id);
          draw();
          onChanged();
        }
      }
    });
  } catch (error) {
    drawer.setBody(html`<div class="alert alert--danger" role="alert">${errorMessage(error)}</div>`);
  }
};

export const mountInvoiceTable = ({ container, role, mine = false, onLoaded }) => {
  const query = getQuery();
  const showStaff = !mine && role === 'ADMIN';
  const table = createDataTable({
    container,
    columns: [
      { key: 'code', label: 'Số hóa đơn', sortable: true, render: (row) => html`<strong>${row.code}</strong>` },
      {
        key: 'createdAt',
        label: 'Thời gian',
        sortable: true,
        className: 'nowrap',
        render: (row) => formatDateTime(row.createdAt),
      },
      { key: 'customerName', label: 'Khách hàng', sortable: true },
      ...(showStaff || (!mine && role === 'SALES')
        ? [{ key: 'employeeName', label: 'Nhân viên', sortable: true }]
        : []),
      { key: 'itemCount', label: 'Số mặt hàng', align: 'right' },
      { key: 'total', label: 'Tổng tiền', sortable: true, align: 'right', render: (row) => formatCurrency(row.total) },
      { key: 'paymentMethod', label: 'Thanh toán', render: (row) => paymentLabels[row.paymentMethod] },
      { key: 'status', label: 'Trạng thái', sortable: true, render: (row) => statusBadge('invoice', row.status) },
    ],
    fetch: (params) => invoiceService.list({ ...params, mine: mine ? 'true' : undefined }),
    filters: [
      { type: 'search', name: 'q', placeholder: 'Tìm theo số hóa đơn, khách hàng...' },
      { type: 'select', name: 'status', label: 'Trạng thái', options: statusOptions('invoice') },
      { type: 'date', name: 'from', label: 'Từ ngày' },
      { type: 'date', name: 'to', label: 'Đến ngày' },
    ],
    initialParams: { q: query.q || '', sort: 'createdAt,desc' },
    defaultSort: 'createdAt,desc',
    caption: 'Danh sách hóa đơn',
    emptyTitle: 'Chưa có hóa đơn',
    emptyIcon: 'receipt',
    onLoaded,
    rowActions: (row) =>
      html`${rowButton('view', `Xem hóa đơn ${row.code}`, 'eye')}${rowButton('print', `In hóa đơn ${row.code}`, 'printer')}`,
    onRowClick: (row) => openInvoiceDrawer(row.id, { role, onChanged: () => table.reload() }),
    onAction: async (action, row) => {
      if (action === 'view') openInvoiceDrawer(row.id, { role, onChanged: () => table.reload() });
      if (action === 'print') {
        try {
          openPrintPreview(await invoiceService.get(row.id));
        } catch (error) {
          toast.error('Không thể mở hóa đơn', errorMessage(error));
        }
      }
    },
  });
  table.reload();
  return table;
};
