import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { mountInvoiceTable } from '../shared/invoice-views.js';

startPage({
  role: 'ADMIN',
  nav: 'invoices',
  title: 'Hóa đơn',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Hóa đơn',
      subtitle: 'Tra cứu hóa đơn bán hàng, nhân viên phụ trách và chi tiết thanh toán.',
      mount: (container) => mountInvoiceTable({ container, role: 'ADMIN' }),
    }),
});
