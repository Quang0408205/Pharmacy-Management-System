import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { getQuery, pageUrl } from '../../core/dom.js';
import { daysUntil, formatCurrency, formatDate, formatDateTime, formatNumber } from '../../core/format.js';
import { createDataTable } from '../../components/data-table.js';
import { openDrawer } from '../../components/overlay.js';
import { detailList } from '../../components/blocks.js';
import { statusBadge, statusOptions } from '../../components/badge.js';
import { skeletonTable } from '../../components/states.js';
import { batchService } from '../../services/inventory-service.js';
import { errorMessage } from '../../core/errors.js';

export const expiryText = (expiryDate) => {
  const days = daysUntil(expiryDate);
  if (days < 0) return `Quá hạn ${Math.abs(days)} ngày`;
  if (days === 0) return 'Hết hạn hôm nay';
  return `Còn ${days} ngày`;
};

const expiryCell = (batch) => html`<div><strong>${formatDate(batch.expiryDate)}</strong>
  <div class="text-small ${batch.status === 'EXPIRED' ? 'text-danger' : 'text-secondary'}">${expiryText(batch.expiryDate)}</div></div>`;

export const openBatchDrawer = async (id, { canDispose = false }) => {
  const drawer = openDrawer({ title: 'Chi tiết lô thuốc', content: skeletonTable(5) });
  try {
    const batch = await batchService.get(id);
    const draw = () => {
      drawer.panel.querySelector('h2').textContent = `Lô ${batch.code}`;
      drawer.setBody(html`<div class="stack">
        <div class="row">${statusBadge('batch', batch.status)}<span class="text-secondary">${batch.medicineName}</span></div>
        ${detailList([
          ['Thuốc', `${batch.medicineName} (${batch.medicineCode})`],
          ['Nguồn nhập', `${batch.supplierName} · ${batch.receiptCode}`],
          ['Ngày nhập', formatDate(batch.receivedDate)],
          ['Ngày sản xuất', formatDate(batch.manufactureDate)],
          ['Hạn sử dụng', `${formatDate(batch.expiryDate)} (${expiryText(batch.expiryDate)})`],
          ['Giá nhập', formatCurrency(batch.purchasePrice)],
          ['Số lượng ban đầu', `${formatNumber(batch.initialQty)} ${batch.unit}`],
          ['Còn lại', `${formatNumber(batch.remainingQty)} ${batch.unit}`],
        ])}
        <h3>Lịch sử biến động của lô</h3>
        ${
          batch.movements.length
            ? html`<div class="table-wrap"><table class="table"><thead><tr><th>Thời gian</th><th>Loại</th><th class="num">Số lượng</th><th>Chứng từ</th><th>Thực hiện</th></tr></thead>
            <tbody>${batch.movements.map(
              (
                movement,
              ) => html`<tr><td class="nowrap">${formatDateTime(movement.createdAt)}</td><td>${statusBadge('movement', movement.type)}</td>
              <td class="num ${movement.quantity < 0 ? 'diff-negative' : 'diff-positive'}">${movement.quantity > 0 ? '+' : ''}${formatNumber(movement.quantity)}</td><td>${movement.referenceCode}</td><td>${movement.performedByName}</td></tr>`,
            )}</tbody></table></div>`
            : html`<p class="text-secondary">Chưa có biến động.</p>`
        }
      </div>`);
      const disposalAllowed = canDispose && batch.remainingQty > 0 && batch.status !== 'QUARANTINED';
      drawer.setFooter(
        disposalAllowed
          ? html`<button type="button" class="btn btn--secondary" data-dismiss="cancel">Đóng</button><a class="btn btn--danger" href="${pageUrl(`pages/warehouse/disposals.html?batchId=${batch.id}`)}">${icon('trash', 'icon--sm')} Lập phiếu xuất hủy</a>`
          : html`<button type="button" class="btn btn--secondary" data-dismiss="cancel">Đóng</button>`,
      );
    };
    draw();
  } catch (error) {
    drawer.setBody(html`<div class="alert alert--danger" role="alert">${errorMessage(error)}</div>`);
  }
};

export const mountBatchTable = ({
  container,
  canDispose = false,
  baseParams = {},
  showStatusFilter = true,
  onLoaded,
  extraFilters = [],
}) => {
  const query = getQuery();
  const filters = [
    { type: 'search', name: 'q', placeholder: 'Tìm số lô, thuốc, nhà cung cấp, phiếu nhập...' },
    ...(showStatusFilter
      ? [{ type: 'select', name: 'status', label: 'Trạng thái', options: statusOptions('batch') }]
      : []),
    ...extraFilters,
  ];
  const table = createDataTable({
    container,
    columns: [
      { key: 'code', label: 'Số lô', sortable: true, render: (row) => html`<strong>${row.code}</strong>` },
      {
        key: 'medicineName',
        label: 'Thuốc',
        sortable: true,
        render: (row) =>
          html`<div class="cell-main"><div><strong>${row.medicineName}</strong><small>${row.medicineCode}</small></div></div>`,
      },
      {
        key: 'supplierName',
        label: 'Nguồn nhập',
        sortable: true,
        render: (row) =>
          html`<div class="cell-main"><div class="truncate"><strong>${row.supplierName}</strong><small>${row.receiptCode}</small></div></div>`,
      },
      { key: 'receivedDate', label: 'Ngày nhập', sortable: true, render: (row) => formatDate(row.receivedDate) },
      { key: 'expiryDate', label: 'Hạn sử dụng', sortable: true, render: expiryCell },
      {
        key: 'initialQty',
        label: 'SL ban đầu',
        sortable: true,
        align: 'right',
        render: (row) => formatNumber(row.initialQty),
      },
      {
        key: 'remainingQty',
        label: 'Còn lại',
        sortable: true,
        align: 'right',
        render: (row) => formatNumber(row.remainingQty),
      },
      { key: 'status', label: 'Trạng thái', sortable: true, render: (row) => statusBadge('batch', row.status) },
    ],
    fetch: (params) => batchService.list({ ...params, ...baseParams }),
    filters,
    initialParams: { q: query.q || '', status: query.status || '', sort: 'expiryDate,asc', ...baseParams },
    defaultSort: 'expiryDate,asc',
    caption: 'Danh sách lô thuốc',
    emptyTitle: 'Không có lô nào',
    emptyIcon: 'archive',
    onLoaded,
    onRowClick: (row) => openBatchDrawer(row.id, { canDispose }),
  });
  table.reload();
  return table;
};
