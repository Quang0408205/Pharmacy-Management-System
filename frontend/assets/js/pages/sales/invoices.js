import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { mountInvoiceTable } from '../shared/invoice-views.js';

startPage({
  role: 'SALES',
  nav: 'invoices',
  title: 'Hóa đơn',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Hóa đơn',
      subtitle: 'Tra cứu hóa đơn theo số hóa đơn hoặc ngày, xem chi tiết và in bản mẫu.',
      mount: (container) => mountInvoiceTable({ container, role: 'SALES' }),
    }),
});
