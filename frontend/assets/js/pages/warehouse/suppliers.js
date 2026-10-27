import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { mountSupplierTable } from '../shared/partner-views.js';

startPage({
  role: 'WAREHOUSE',
  nav: 'suppliers',
  title: 'Nhà cung cấp',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Nhà cung cấp',
      subtitle: 'Hồ sơ, thông tin liên hệ và lịch sử nhập hàng của nhà cung cấp.',
      mount: (container) => mountSupplierTable({ container, role: 'WAREHOUSE' }),
    }),
});
