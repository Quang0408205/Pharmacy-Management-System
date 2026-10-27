import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { mountCustomerTable } from '../shared/partner-views.js';

startPage({
  role: 'ADMIN',
  nav: 'customers',
  title: 'Khách hàng',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Khách hàng',
      subtitle: 'Hồ sơ khách hàng và lịch sử mua thuốc.',
      mount: (container) => mountCustomerTable({ container, role: 'ADMIN' }),
    }),
});
