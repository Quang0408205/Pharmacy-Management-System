import { html } from '../../core/html.js';
import { getQuery } from '../../core/dom.js';
import { formatCurrency, formatNumber } from '../../core/format.js';
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
    mountMedicineTable({ container, role: 'ADMIN', mode: 'warehouse', presets: { sort: 'availableQty,desc' } }),
  batches: (container) => mountBatchTable({ container }),
  low: (container) =>
    mountMedicineTable({
      container,
      role: 'ADMIN',
      mode: 'warehouse',
      presets: { stockStatus: 'LOW_STOCK', sort: 'availableQty,asc' },
    }),
  expiring: (container, summary) =>
    mountBatchTable({
      container,
      baseParams: { expiryWithin: summary.expiryWarningDays },
      showStatusFilter: false,
    }),
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
      <div class="grid grid--kpi-3">
        ${kpiCard({ label: 'Thuốc đang có hàng', value: `${formatNumber(summary.inStockMedicines)} / ${formatNumber(summary.activeMedicines)}`, hint: `${formatNumber(summary.activeMedicines - summary.inStockMedicines)} thuốc đang kinh doanh chưa có hàng trong kho`, iconName: 'pill' })}
        ${kpiCard({ label: 'Lô còn hàng', value: formatNumber(summary.totalBatches), hint: 'Số lô đang còn tồn, kể cả lô sắp hết hạn', iconName: 'package' })}
        ${kpiCard({ label: 'Giá trị tồn kho (theo giá nhập)', value: formatCurrency(summary.stockValue), hint: 'Tổng số lượng còn lại × giá nhập của từng lô', iconName: 'banknote', tone: 'info' })}
        ${kpiCard({ label: 'Thuốc dưới mức tồn tối thiểu', value: formatNumber(summary.lowStock), hint: 'Đang có hàng nhưng thấp hơn mức tối thiểu (tab Thuốc tồn thấp)', iconName: 'alertTriangle', tone: 'warning' })}
        ${kpiCard({ label: 'Lô sắp hết hạn', value: formatNumber(summary.expiringBatches), hint: `Còn hạn trong ${formatNumber(summary.expiryWarningDays)} ngày tới (tab Lô sắp hết hạn)`, iconName: 'hourglass', tone: 'danger' })}
        ${kpiCard({ label: 'Lô đã hết hạn còn tồn', value: formatNumber(summary.expiredBatches), hint: 'Không được bán, cần lập phiếu xuất hủy', iconName: 'ban', tone: 'danger' })}
      </div>
      <div>${tabs(items, active, 'inv')}<div class="tab-panel" id="inv-panel" role="tabpanel" data-panel></div></div>
    `.value;
    const panel = content.querySelector('[data-panel]');
    const show = (key) => {
      panel.innerHTML = '';
      mounts[key](panel, summary);
    };
    bindTabs(content, show);
    show(active);
  },
});
