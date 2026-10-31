import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { mountBatchTable } from '../shared/batch-views.js';

startPage({
  role: 'WAREHOUSE',
  nav: 'batches',
  title: 'Lô thuốc',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Lô thuốc',
      subtitle: 'Theo dõi từng lô: nguồn nhập, hạn sử dụng, số lượng ban đầu và còn lại.',
      mount: (container) => mountBatchTable({ container, canDispose: true }),
    }),
});
