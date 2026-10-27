import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { mountSupplierTable } from '../shared/partner-views.js';

startPage({
  role: 'ADMIN',
  nav: 'suppliers',
  title: 'Nhà cung cấp',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Nhà cung cấp',
      subtitle: 'Quản lý hồ sơ, thông tin liên hệ và lịch sử nhập hàng.',
      mount: (container) => mountSupplierTable({ container, role: 'ADMIN' }),
    }),
});
