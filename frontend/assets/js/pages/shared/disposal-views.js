import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { formatDate, formatDateTime, formatNumber } from '../../core/format.js';
import { errorMessage } from '../../core/errors.js';
import { detailList, rowButton } from '../../components/blocks.js';
import { createDataTable } from '../../components/data-table.js';
import { openFormDialog } from '../../components/form-modal.js';
import { confirmDialog, openDrawer } from '../../components/overlay.js';
import { statusBadge, statusOptions } from '../../components/badge.js';
import { skeletonTable } from '../../components/states.js';
import { toast } from '../../components/toast.js';
import { disposalService } from '../../services/inventory-service.js';

const linesTable = (lines) => html`<div class="table-wrap"><table class="table">
  <thead><tr><th>Thuốc</th><th>Lô</th><th>Hạn dùng</th><th>Lý do</th><th class="num">Số lượng hủy</th></tr></thead>
  <tbody>${lines.map(
    (line) => html`<tr><td>${line.medicineName}</td><td>${line.batchCode}</td><td>${formatDate(line.expiryDate)}</td>
    <td>${statusBadge('disposalReason', line.reasonType)}${line.reasonDetail ? html`<br /><small class="text-secondary">${line.reasonDetail}</small>` : ''}</td>
    <td class="num">${formatNumber(line.quantity)}</td></tr>`,
  )}</tbody>
</table></div>`;

const footerFor = (role, disposal) => {
  if (role === 'ADMIN' && disposal.status === 'PENDING_APPROVAL') {
    return html`<button type="button" class="btn btn--secondary" data-act="reject">Từ chối</button><button type="button" class="btn btn--primary" data-act="approve">${icon('check', 'icon--sm')} Duyệt phiếu</button>`;
  }
  if (role === 'WAREHOUSE' && disposal.status === 'DRAFT') {
    return html`<button type="button" class="btn btn--secondary" data-act="delete">Xóa nháp</button><button type="button" class="btn btn--primary" data-act="submit">${icon('send', 'icon--sm')} Gửi duyệt</button>`;
  }
  if (role === 'WAREHOUSE' && disposal.status === 'APPROVED') {
    return html`<button type="button" class="btn btn--secondary" data-dismiss="cancel">Đóng</button><button type="button" class="btn btn--danger" data-act="execute">${icon('trash', 'icon--sm')} Thực hiện hủy</button>`;
  }
  return html`<button type="button" class="btn btn--secondary" data-dismiss="cancel">Đóng</button>`;
};

export const openDisposalDrawer = async (id, { role, onChanged = () => {} }) => {
  const drawer = openDrawer({ title: 'Phiếu xuất hủy', content: skeletonTable(5) });
  try {
    let disposal = await disposalService.get(id);
    const draw = () => {
      const totalQuantity = disposal.lines.reduce((sum, line) => sum + line.quantity, 0);
      drawer.panel.querySelector('h2').textContent = disposal.code;
      drawer.setBody(html`<div class="stack">
        <div class="row">${statusBadge('disposal', disposal.status)}</div>
        ${detailList([
          ['Lý do chung', disposal.reason],
          ['Người đề xuất', disposal.createdByName],
          ['Ngày tạo', formatDateTime(disposal.createdAt)],
          ['Gửi duyệt', disposal.submittedAt ? formatDateTime(disposal.submittedAt) : 'Chưa gửi'],
          ['Người duyệt', disposal.reviewedByName],
          ['Ý kiến duyệt', disposal.reviewNote],
          ['Người thực hiện', disposal.executedByName],
          ['Ngày thực hiện', disposal.executedAt ? formatDateTime(disposal.executedAt) : ''],
          ['Số lô', formatNumber(disposal.lines.length)],
          ['Tổng số lượng hủy', formatNumber(totalQuantity)],
          ['Ghi chú', disposal.note],
        ])}
        ${linesTable(disposal.lines)}
      </div>`);
      drawer.setFooter(footerFor(role, disposal));
    };
    draw();

    const run = async (action, successTitle) => {
      try {
        disposal = await action();
        toast.success(successTitle, disposal.code);
        draw();
        onChanged();
      } catch (error) {
        toast.error('Không thể thực hiện', errorMessage(error));
      }
    };

    drawer.footer.addEventListener('click', async (event) => {
      const button = event.target instanceof Element ? event.target.closest('[data-act]') : null;
      if (!button) return;
      const act = button.dataset.act;
      if (act === 'approve') {
        const confirmed = await confirmDialog({
          title: 'Duyệt phiếu xuất hủy?',
          message:
            'Duyệt chỉ cho phép nhân viên kho thực hiện. Tồn kho chưa bị trừ cho đến khi kho xác nhận thực hiện hủy.',
          confirmText: 'Duyệt phiếu',
        });
        if (confirmed) await run(() => disposalService.approve(disposal.id, 'Đã duyệt'), 'Đã duyệt phiếu xuất hủy');
      }
      if (act === 'reject') {
        const result = await openFormDialog({
          title: 'Từ chối phiếu xuất hủy',
          intro: html`<p class="text-secondary text-small">Các lô trong phiếu sẽ được trả về trạng thái có thể bán.</p>`,
          fields: [{ name: 'note', label: 'Lý do từ chối', type: 'textarea', required: true, full: true }],
          submitText: 'Từ chối phiếu',
          onSubmit: ({ note }) => disposalService.reject(disposal.id, note),
        }).closed;
        if (result && result !== true) {
          disposal = result;
          toast.success('Đã từ chối phiếu xuất hủy', disposal.code);
          draw();
          onChanged();
        }
      }
      if (act === 'submit') {
        const confirmed = await confirmDialog({
          title: 'Gửi duyệt phiếu xuất hủy?',
          message:
            'Các lô trong phiếu sẽ bị cách ly, không thể bán cho đến khi quản trị viên xử lý. Bạn không thể chỉnh sửa phiếu sau khi gửi.',
          confirmText: 'Gửi duyệt',
        });
        if (confirmed) await run(() => disposalService.submit(disposal.id), 'Đã gửi duyệt phiếu xuất hủy');
      }
      if (act === 'execute') {
        const confirmed = await confirmDialog({
          title: 'Thực hiện xuất hủy?',
          message: 'Số lượng trong phiếu sẽ bị trừ khỏi kho và không thể hoàn tác.',
          confirmText: 'Thực hiện hủy',
          tone: 'danger',
        });
        if (confirmed) await run(() => disposalService.execute(disposal.id), 'Đã thực hiện xuất hủy');
      }
      if (act === 'delete') {
        const confirmed = await confirmDialog({
          title: 'Xóa phiếu xuất hủy nháp?',
          message: `Phiếu ${disposal.code} sẽ bị xóa.`,
          confirmText: 'Xóa phiếu',
          tone: 'danger',
        });
        if (!confirmed) return;
        try {
          await disposalService.remove(disposal.id);
          toast.success('Đã xóa phiếu xuất hủy', disposal.code);
          drawer.close();
          onChanged();
        } catch (error) {
          toast.error('Không thể xóa', errorMessage(error));
        }
      }
    });
  } catch (error) {
    drawer.setBody(html`<div class="alert alert--danger" role="alert">${errorMessage(error)}</div>`);
  }
};

export const mountDisposalTable = ({ container, role, toolbarExtra = '', onAction = () => {} }) => {
  const table = createDataTable({
    container,
    columns: [
      { key: 'code', label: 'Mã phiếu', sortable: true, render: (row) => html`<strong>${row.code}</strong>` },
      { key: 'reason', label: 'Lý do' },
      { key: 'createdAt', label: 'Ngày tạo', sortable: true, render: (row) => formatDateTime(row.createdAt) },
      { key: 'createdByName', label: 'Người đề xuất', sortable: true },
      { key: 'lineCount', label: 'Số lô', align: 'right' },
      { key: 'totalQuantity', label: 'Tổng số lượng', sortable: true, align: 'right' },
      { key: 'status', label: 'Trạng thái', sortable: true, render: (row) => statusBadge('disposal', row.status) },
    ],
    fetch: (params) => disposalService.list(params),
    filters: [
      { type: 'search', name: 'q', placeholder: 'Tìm mã phiếu, người đề xuất, lý do...' },
      { type: 'select', name: 'status', label: 'Trạng thái', options: statusOptions('disposal') },
    ],
    toolbarExtra,
    defaultSort: 'createdAt,desc',
    initialParams: { sort: 'createdAt,desc' },
    caption: 'Danh sách phiếu xuất hủy',
    emptyTitle: 'Chưa có phiếu xuất hủy',
    emptyIcon: 'trash',
    rowActions: (row) => rowButton('view', `Xem phiếu ${row.code}`, 'eye'),
    onRowClick: (row) => openDisposalDrawer(row.id, { role, onChanged: () => table.reload() }),
    onAction: (action, row) => {
      if (action === 'view') openDisposalDrawer(row.id, { role, onChanged: () => table.reload() });
      else onAction(action, row, table);
    },
  });
  table.reload();
  return table;
};
