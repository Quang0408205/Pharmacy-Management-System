import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { mountMovementTable } from '../shared/movement-views.js';

startPage({
  role: 'WAREHOUSE',
  nav: 'movements',
  title: 'Lịch sử kho',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Lịch sử kho',
      subtitle: 'Nhập, xuất, điều chỉnh và hủy hàng, kèm chứng từ và người thực hiện.',
      mount: (container) => mountMovementTable({ container }),
    }),
});
