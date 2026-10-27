import { formatDateTime } from '../../core/format.js';
import { html } from '../../core/html.js';
import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { createDataTable } from '../../components/data-table.js';
import { statusBadge, statusOptions } from '../../components/badge.js';
import { activityService } from '../../services/admin-service.js';

startPage({
  role: 'ADMIN',
  nav: 'activity',
  title: 'Nhật ký hoạt động',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Nhật ký hoạt động',
      subtitle: 'Mọi thao tác quan trọng của nhân viên được ghi lại để đối soát.',
      mount: (container) => {
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
            { key: 'actorName', label: 'Người thực hiện', sortable: true },
            {
              key: 'role',
              label: 'Vai trò',
              sortable: true,
              render: (row) => (row.role === 'SYSTEM' ? 'Hệ thống' : statusBadge('role', row.role)),
            },
            { key: 'description', label: 'Nội dung' },
            {
              key: 'entityCode',
              label: 'Đối tượng',
              render: (row) => html`<span class="text-secondary">${row.entityCode}</span>`,
            },
          ],
          fetch: (params) => activityService.list(params),
          filters: [
            { type: 'search', name: 'q', placeholder: 'Tìm người thực hiện, nội dung, mã đối tượng...' },
            { type: 'select', name: 'role', label: 'Vai trò', options: statusOptions('role') },
            { type: 'date', name: 'from', label: 'Từ ngày' },
            { type: 'date', name: 'to', label: 'Đến ngày' },
          ],
          defaultSort: 'createdAt,desc',
          initialParams: { sort: 'createdAt,desc' },
          caption: 'Nhật ký hoạt động',
          emptyTitle: 'Chưa có hoạt động nào',
          emptyIcon: 'activity',
        });
        table.reload();
        return table;
      },
    }),
});
