import { html } from '../../core/html.js';
import { getQuery } from '../../core/dom.js';
import { formatCompact, formatNumber } from '../../core/format.js';
import { startPage } from '../../components/page.js';
import { bindTabs, kpiCard, pageHeading, tabs } from '../../components/blocks.js';
import { inventoryService } from '../../services/inventory-service.js';
import { mountMedicineTable } from '../shared/medicine-views.js';
import { mountBatchTable } from '../shared/batch-views.js';
import { mountMovementTable } from '../shared/movement-views.js';
import { mountStocktakeTable } from '../shared/stocktake-views.js';

const items = [
  { key: 'stock', label: 'Tồn theo thuốc' },
  { key: 'batches', label: 'Danh sách lô' },
  { key: 'low', label: 'Thuốc tồn thấp' },
  { key: 'expiring', label: 'Lô sắp hết hạn' },
  { key: 'movements', label: 'Lịch sử biến động' },
  { key: 'stocktakes', label: 'Phiếu kiểm kê' },
];

const mounts = {
  stock: (container) =>
    mountMedicineTable({ container, role: 'ADMIN', mode: 'warehouse', presets: { sort: 'availableQty,asc' } }),
  batches: (container) => mountBatchTable({ container }),
  low: (container) =>
    mountMedicineTable({
      container,
      role: 'ADMIN',
      mode: 'warehouse',
      presets: { stockStatus: 'LOW_STOCK', sort: 'availableQty,asc' },
    }),
  expiring: (container) => mountBatchTable({ container, baseParams: { expiryWithin: 90 }, showStatusFilter: false }),
  movements: (container) => mountMovementTable({ container }),
  stocktakes: (container) => mountStocktakeTable({ container, role: 'ADMIN' }),
};

startPage({
  role: 'ADMIN',
  nav: 'inventory',
  title: 'Quản lý kho',
  render: async ({ content }) => {
    const summary = await inventoryService.summary();
    const requested = getQuery().tab;
    const active = items.some((item) => item.key === requested) ? requested : 'stock';
    content.innerHTML = html`
      ${pageHeading({ title: 'Quản lý kho', subtitle: 'Tổng quan tồn kho, lô thuốc, biến động và phê duyệt kiểm kê.' })}
      <div class="grid grid--kpi-4">
        ${kpiCard({ label: 'Tổng tồn kho', value: formatNumber(summary.totalUnits), hint: `${formatNumber(summary.totalBatches)} lô còn hàng`, iconName: 'package' })}
        ${kpiCard({ label: 'Giá trị tồn (giá nhập)', value: formatCompact(summary.stockValue), iconName: 'banknote', tone: 'info' })}
        ${kpiCard({ label: 'Thuốc tồn thấp / hết', value: `${formatNumber(summary.lowStock)} / ${formatNumber(summary.outOfStock)}`, iconName: 'alertTriangle', tone: 'warning' })}
        ${kpiCard({ label: 'Lô sắp hết hạn / hết hạn', value: `${formatNumber(summary.expiringBatches)} / ${formatNumber(summary.expiredBatches)}`, iconName: 'hourglass', tone: 'danger' })}
      </div>
      <div>${tabs(items, active, 'inv')}<div class="tab-panel" id="inv-panel" role="tabpanel" data-panel></div></div>
    `.value;
    const panel = content.querySelector('[data-panel]');
    const show = (key) => {
      panel.innerHTML = '';
      mounts[key](panel);
    };
    bindTabs(content, show);
    show(active);
  },
});
