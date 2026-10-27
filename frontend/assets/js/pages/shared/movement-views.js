import { html } from '../../core/html.js';
import { formatDateTime, formatNumber } from '../../core/format.js';
import { getQuery } from '../../core/dom.js';
import { createDataTable } from '../../components/data-table.js';
import { statusBadge, statusOptions } from '../../components/badge.js';
import { inventoryService } from '../../services/inventory-service.js';

export const mountMovementTable = ({ container }) => {
  const query = getQuery();
  const table = createDataTable({
    container,
    columns: [
      {
        key: 'createdAt',
        label: 'Thời gian',
        sortable: true,
        className: 'nowrap',
        render: (row) => formatDateTime(row.createdAt),
      },
      { key: 'medicineName', label: 'Thuốc', sortable: true },
      { key: 'batchCode', label: 'Lô', sortable: true },
      { key: 'type', label: 'Loại', sortable: true, render: (row) => statusBadge('movement', row.type) },
      {
        key: 'quantity',
        label: 'Số lượng',
        sortable: true,
        align: 'right',
        render: (row) =>
          html`<span class="${row.quantity < 0 ? 'diff-negative' : 'diff-positive'}">${row.quantity > 0 ? '+' : ''}${formatNumber(row.quantity)}</span>`,
      },
      { key: 'referenceCode', label: 'Chứng từ', sortable: true },
      { key: 'performedByName', label: 'Thực hiện bởi', sortable: true },
    ],
    fetch: (params) => inventoryService.movements(params),
    filters: [
      { type: 'search', name: 'q', placeholder: 'Tìm thuốc, lô, chứng từ, người thực hiện...' },
      { type: 'select', name: 'type', label: 'Loại biến động', options: statusOptions('movement') },
      { type: 'date', name: 'from', label: 'Từ ngày' },
      { type: 'date', name: 'to', label: 'Đến ngày' },
    ],
    initialParams: { q: query.q || '', type: query.type || '', sort: 'createdAt,desc' },
    defaultSort: 'createdAt,desc',
    caption: 'Lịch sử biến động kho',
    emptyTitle: 'Chưa có biến động kho',
    emptyIcon: 'history',
  });
  table.reload();
  return table;
};
