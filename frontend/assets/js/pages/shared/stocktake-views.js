import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { formatDateTime, formatNumber } from '../../core/format.js';
import { createDataTable } from '../../components/data-table.js';
import { confirmDialog, openDrawer } from '../../components/overlay.js';
import { openFormDialog } from '../../components/form-modal.js';
import { detailList, rowButton } from '../../components/blocks.js';
import { statusBadge, statusOptions } from '../../components/badge.js';
import { skeletonTable } from '../../components/states.js';
import { toast } from '../../components/toast.js';
import { errorMessage } from '../../core/errors.js';
import { stocktakeService } from '../../services/inventory-service.js';

export const differenceCell = (difference) => {
  if (difference === 0) return html`<span class="text-secondary">0</span>`;
  return html`<span class="${difference > 0 ? 'diff-positive' : 'diff-negative'}">${difference > 0 ? '+' : ''}${formatNumber(difference)}</span>`;
};

export const stocktakeLinesTable = (lines) => html`<div class="table-wrap"><table class="table">
  <thead><tr><th>Thuốc</th><th>Lô</th><th class="num">Hệ thống</th><th class="num">Thực tế</th><th class="num">Chênh lệch</th><th>Lý do</th></tr></thead>
  <tbody>${lines.map(
    (
      line,
    ) => html`<tr><td>${line.medicineName}</td><td>${line.batchCode}</td><td class="num">${formatNumber(line.systemQty)}</td>
    <td class="num">${line.actualQty === null ? '—' : formatNumber(line.actualQty)}</td><td class="num">${differenceCell(line.difference)}</td><td>${line.reason || '—'}</td></tr>`,
  )}</tbody>
</table></div>`;

export const openStocktakeDrawer = async (id, { role, onChanged = () => {} }) => {
  const drawer = openDrawer({ title: 'Phiếu kiểm kê', content: skeletonTable(6) });
  try {
    let stocktake = await stocktakeService.get(id);
    const draw = () => {
      const differences = stocktake.lines.filter((line) => line.difference !== 0);
      drawer.panel.querySelector('h2').textContent = stocktake.code;
      drawer.setBody(html`<div class="stack">
        <div class="row">${statusBadge('stocktake', stocktake.status)}</div>
        ${detailList([
          ['Phạm vi', stocktake.scopeLabel],
          ['Người lập', stocktake.createdByName],
          ['Ngày tạo', formatDateTime(stocktake.createdAt)],
          ['Gửi duyệt', stocktake.submittedAt ? formatDateTime(stocktake.submittedAt) : 'Chưa gửi'],
          ['Người duyệt', stocktake.reviewedByName],
          ['Ý kiến duyệt', stocktake.reviewNote],
          ['Số dòng', formatNumber(stocktake.lines.length)],
          ['Dòng chênh lệch', formatNumber(differences.length)],
          ['Ghi chú', stocktake.note],
        ])}
        ${stocktakeLinesTable(stocktake.lines)}
      </div>`);
      const canReview = role === 'ADMIN' && stocktake.status === 'PENDING_APPROVAL';
      drawer.setFooter(
        canReview
          ? html`<button type="button" class="btn btn--secondary" data-act="reject">Từ chối</button><button type="button" class="btn btn--primary" data-act="approve">${icon('check', 'icon--sm')} Duyệt & điều chỉnh kho</button>`
          : html`<button type="button" class="btn btn--secondary" data-dismiss="cancel">Đóng</button>`,
      );
    };
    draw();
    drawer.footer.addEventListener('click', async (event) => {
      const button = event.target instanceof Element ? event.target.closest('[data-act]') : null;
      if (!button) return;
      if (button.dataset.act === 'approve') {
        const confirmed = await confirmDialog({
          title: 'Duyệt phiếu kiểm kê?',
          message:
            'Số lượng tồn của các lô chênh lệch sẽ được điều chỉnh theo kết quả kiểm kê và ghi nhận vào lịch sử kho.',
          confirmText: 'Duyệt phiếu',
        });
        if (!confirmed) return;
        try {
          stocktake = await stocktakeService.approve(stocktake.id, 'Đã duyệt');
          toast.success('Đã duyệt phiếu kiểm kê', stocktake.code);
          draw();
          onChanged();
        } catch (error) {
          toast.error('Không thể duyệt', errorMessage(error));
        }
      }
      if (button.dataset.act === 'reject') {
        const result = await openFormDialog({
          title: 'Từ chối phiếu kiểm kê',
          fields: [{ name: 'note', label: 'Lý do từ chối', type: 'textarea', required: true, full: true }],
          submitText: 'Từ chối phiếu',
          onSubmit: ({ note }) => stocktakeService.reject(stocktake.id, note),
        }).closed;
        if (result && result !== true) {
          stocktake = result;
          toast.success('Đã từ chối phiếu kiểm kê', stocktake.code);
          draw();
          onChanged();
        }
      }
    });
  } catch (error) {
    drawer.setBody(html`<div class="alert alert--danger" role="alert">${errorMessage(error)}</div>`);
  }
};

export const mountStocktakeTable = ({
  container,
  role,
  toolbarExtra = '',
  onAction = null,
  rowActions = null,
  onLoaded,
}) => {
  const table = createDataTable({
    container,
    columns: [
      { key: 'code', label: 'Mã phiếu', sortable: true, render: (row) => html`<strong>${row.code}</strong>` },
      { key: 'scopeLabel', label: 'Phạm vi', sortable: true },
      { key: 'createdAt', label: 'Ngày tạo', sortable: true, render: (row) => formatDateTime(row.createdAt) },
      { key: 'createdByName', label: 'Người lập', sortable: true },
      { key: 'lineCount', label: 'Số dòng', align: 'right' },
      { key: 'differenceCount', label: 'Chênh lệch', align: 'right' },
      { key: 'status', label: 'Trạng thái', sortable: true, render: (row) => statusBadge('stocktake', row.status) },
    ],
    fetch: (params) => stocktakeService.list(params),
    filters: [
      { type: 'search', name: 'q', placeholder: 'Tìm mã phiếu, phạm vi, người lập...' },
      { type: 'select', name: 'status', label: 'Trạng thái', options: statusOptions('stocktake') },
    ],
    defaultSort: 'createdAt,desc',
    initialParams: { sort: 'createdAt,desc' },
    caption: 'Danh sách phiếu kiểm kê',
    emptyTitle: 'Chưa có phiếu kiểm kê',
    emptyIcon: 'clipboardCheck',
    toolbarExtra,
    onLoaded,
    rowActions: rowActions || ((row) => rowButton('view', `Xem phiếu ${row.code}`, 'eye')),
    onRowClick: (row) =>
      onAction ? onAction('open', row) : openStocktakeDrawer(row.id, { role, onChanged: () => table.reload() }),
    onAction: (action, row) => {
      if (onAction) onAction(action, row);
      else if (action === 'view') openStocktakeDrawer(row.id, { role, onChanged: () => table.reload() });
    },
  });
  table.reload();
  return table;
};
