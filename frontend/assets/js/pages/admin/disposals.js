import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { mountDisposalTable } from '../shared/disposal-views.js';

startPage({
  role: 'ADMIN',
  nav: 'disposals',
  title: 'Duyệt xuất hủy',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Duyệt xuất hủy',
      subtitle: 'Xem xét các đề xuất hủy thuốc hết hạn, hư hỏng hoặc bị thu hồi do nhân viên kho lập.',
      mount: (container) => mountDisposalTable({ container, role: 'ADMIN' }),
    }),
});
