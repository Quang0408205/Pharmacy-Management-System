import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { mountMedicineTable } from '../shared/medicine-views.js';

startPage({
  role: 'ADMIN',
  nav: 'medicines',
  title: 'Quản lý thuốc',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Quản lý thuốc',
      subtitle: 'Danh mục thuốc, giá bán, thông tin lô và trạng thái kinh doanh.',
      mount: (container) => mountMedicineTable({ container, role: 'ADMIN', mode: 'admin' }),
    }),
});
