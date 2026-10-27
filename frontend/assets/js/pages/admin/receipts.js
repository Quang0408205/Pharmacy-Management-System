import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { mountReceiptTable } from '../shared/receipt-views.js';

startPage({
  role: 'ADMIN',
  nav: 'receipts',
  title: 'Phiếu nhập',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Phiếu nhập',
      subtitle: 'Theo dõi các phiếu nhập kho do nhân viên kho lập.',
      mount: (container) => mountReceiptTable({ container, role: 'ADMIN' }),
    }),
});
