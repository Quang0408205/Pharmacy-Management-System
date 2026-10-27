import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { mountMedicineTable } from '../shared/medicine-views.js';

startPage({
  role: 'SALES',
  nav: 'lookup',
  title: 'Tra cứu thuốc',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Tra cứu thuốc',
      subtitle: 'Kiểm tra giá, tồn kho, phân loại kê đơn và hạn dùng của từng lô. Chỉ xem, không chỉnh sửa.',
      mount: (container) => mountMedicineTable({ container, role: 'SALES', mode: 'sales' }),
    }),
});
