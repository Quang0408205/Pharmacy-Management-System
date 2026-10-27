import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { getQuery, pageUrl } from '../../core/dom.js';
import { formatCurrency, formatDate, formatDateTime, formatNumber } from '../../core/format.js';
import { createDataTable } from '../../components/data-table.js';
import { confirmDialog, openDrawer } from '../../components/overlay.js';
import { detailList, rowButton } from '../../components/blocks.js';
import { statusBadge, statusOptions } from '../../components/badge.js';
import { skeletonTable } from '../../components/states.js';
import { toast } from '../../components/toast.js';
import { errorMessage } from '../../core/errors.js';
import { receiptService } from '../../services/inventory-service.js';

export const openReceiptDrawer = async (id, { role, onChanged = () => {} }) => {
  const drawer = openDrawer({ title: 'Chi tiết phiếu nhập', content: skeletonTable(6) });
  try {
    const receipt = await receiptService.get(id);
    drawer.panel.querySelector('h2').textContent = receipt.code;
    drawer.setBody(html`<div class="stack">
      <div class="row">${statusBadge('receipt', receipt.status)}</div>
      ${detailList([
        ['Nhà cung cấp', receipt.supplierName],
        ['Ngày nhập', formatDate(receipt.receivingDate)],
        ['Nhân viên lập phiếu', receipt.createdByName],
        ['Xác nhận lúc', receipt.confirmedAt ? formatDateTime(receipt.confirmedAt) : 'Chưa xác nhận'],
        ['Tổng số lượng', formatNumber(receipt.totalQuantity)],
        ['Tổng giá trị', formatCurrency(receipt.totalAmount)],
        ['Ghi chú', receipt.note],
      ])}
      <h3>Danh sách thuốc nhập</h3>
      <div class="table-wrap"><table class="table"><thead><tr><th>Thuốc</th><th>Số lô</th><th>Hạn dùng</th><th class="num">SL</th><th class="num">Giá nhập</th><th class="num">Thành tiền</th></tr></thead>
        <tbody>${receipt.items.map(
          (
            item,
          ) => html`<tr><td><strong>${item.medicineName}</strong><br /><small class="text-secondary">${item.medicineCode}</small></td><td>${item.batchCode}</td><td>${formatDate(item.expiryDate)}</td>
          <td class="num">${formatNumber(item.quantity)} ${item.unit}</td><td class="num">${formatCurrency(item.purchasePrice)}</td><td class="num">${formatCurrency(item.subtotal)}</td></tr>`,
        )}</tbody></table></div>
    </div>`);
    const canEditDraft = role === 'WAREHOUSE' && receipt.status === 'DRAFT';
    drawer.setFooter(
      canEditDraft
        ? html`<button type="button" class="btn btn--danger" data-act="delete">${icon('trash', 'icon--sm')} Xóa nháp</button>
            <a class="btn btn--primary" href="${pageUrl(`pages/warehouse/receiving.html?draft=${receipt.id}`)}">${icon('edit', 'icon--sm')} Tiếp tục chỉnh sửa</a>`
        : html`<button type="button" class="btn btn--secondary" data-dismiss="cancel">Đóng</button>`,
    );
    drawer.footer.addEventListener('click', async (event) => {
      if (!(event.target instanceof Element) || !event.target.closest('[data-act="delete"]')) return;
      const confirmed = await confirmDialog({
        title: 'Xóa phiếu nháp?',
        message: `Phiếu ${receipt.code} sẽ bị xóa vĩnh viễn.`,
        confirmText: 'Xóa phiếu',
        tone: 'danger',
      });
      if (!confirmed) return;
      try {
        await receiptService.remove(receipt.id);
        toast.success('Đã xóa phiếu nháp', receipt.code);
        drawer.close();
        onChanged();
      } catch (error) {
        toast.error('Không thể xóa', errorMessage(error));
      }
    });
  } catch (error) {
    drawer.setBody(html`<div class="alert alert--danger" role="alert">${errorMessage(error)}</div>`);
  }
};

export const mountReceiptTable = ({ container, role, onLoaded }) => {
  const query = getQuery();
  const table = createDataTable({
    container,
    columns: [
      { key: 'code', label: 'Mã phiếu', sortable: true, render: (row) => html`<strong>${row.code}</strong>` },
      { key: 'receivingDate', label: 'Ngày nhập', sortable: true, render: (row) => formatDate(row.receivingDate) },
      { key: 'supplierName', label: 'Nhà cung cấp', sortable: true },
      { key: 'itemCount', label: 'Số dòng', align: 'right' },
      {
        key: 'totalQuantity',
        label: 'Tổng SL',
        sortable: true,
        align: 'right',
        render: (row) => formatNumber(row.totalQuantity),
      },
      {
        key: 'totalAmount',
        label: 'Giá trị',
        sortable: true,
        align: 'right',
        render: (row) => formatCurrency(row.totalAmount),
      },
      { key: 'createdByName', label: 'Nhân viên', sortable: true },
      { key: 'status', label: 'Trạng thái', sortable: true, render: (row) => statusBadge('receipt', row.status) },
    ],
    fetch: (params) => receiptService.list(params),
    filters: [
      { type: 'search', name: 'q', placeholder: 'Tìm mã phiếu, nhà cung cấp, nhân viên...' },
      { type: 'select', name: 'status', label: 'Trạng thái', options: statusOptions('receipt') },
      { type: 'date', name: 'from', label: 'Từ ngày' },
      { type: 'date', name: 'to', label: 'Đến ngày' },
    ],
    initialParams: { q: query.q || '', status: query.status || '', sort: 'receivingDate,desc' },
    defaultSort: 'receivingDate,desc',
    caption: 'Danh sách phiếu nhập',
    emptyTitle: 'Chưa có phiếu nhập',
    emptyIcon: 'clipboard',
    onLoaded,
    toolbarExtra:
      role === 'WAREHOUSE'
        ? html`<a class="btn btn--primary" href="${pageUrl('pages/warehouse/receiving.html')}">${icon('plus', 'icon--sm')} Tạo phiếu nhập</a>`
        : '',
    rowActions: (row) => rowButton('view', `Xem phiếu ${row.code}`, 'eye'),
    onRowClick: (row) => openReceiptDrawer(row.id, { role, onChanged: () => table.reload() }),
    onAction: (action, row) => {
      if (action === 'view') openReceiptDrawer(row.id, { role, onChanged: () => table.reload() });
    },
  });
  table.reload();
  return table;
};
