import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { mountCustomerTable } from '../shared/partner-views.js';

startPage({
  role: 'SALES',
  nav: 'customers',
  title: 'Khách hàng',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Khách hàng',
      subtitle: 'Tìm khách hàng, thêm mới, chỉnh sửa và xem lịch sử mua thuốc.',
      mount: (container) => mountCustomerTable({ container, role: 'SALES' }),
    }),
});
