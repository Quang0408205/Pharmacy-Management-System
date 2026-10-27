import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { mountReceiptTable } from '../shared/receipt-views.js';

startPage({
  role: 'WAREHOUSE',
  nav: 'receipts',
  title: 'Phiếu nhập',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Phiếu nhập',
      subtitle: 'Phiếu đã xác nhận không thể chỉnh sửa. Phiếu nháp có thể tiếp tục hoặc xóa.',
      mount: (container) => mountReceiptTable({ container, role: 'WAREHOUSE' }),
    }),
});
