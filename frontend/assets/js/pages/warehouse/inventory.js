import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { mountMedicineTable } from '../shared/medicine-views.js';

startPage({
  role: 'WAREHOUSE',
  nav: 'inventory',
  title: 'Tồn kho',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Tồn kho',
      subtitle: 'Nhấn vào một thuốc để xem tổng quan, danh sách lô và biến động kho.',
      mount: (container) => mountMedicineTable({ container, role: 'WAREHOUSE', mode: 'warehouse' }),
    }),
});
