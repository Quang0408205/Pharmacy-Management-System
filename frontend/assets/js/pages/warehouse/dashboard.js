import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { pageUrl } from '../../core/dom.js';
import { formatCurrency, formatDate, formatDateTime, formatNumber, greeting, shortDate } from '../../core/format.js';
import { startPage } from '../../components/page.js';
import { card, kpiCard, medicineCell } from '../../components/blocks.js';
import { chartBox, renderChart } from '../../components/charts.js';
import { statusBadge } from '../../components/badge.js';
import { emptyState, skeletonTable } from '../../components/states.js';
import { dashboardService } from '../../services/admin-service.js';
import { expiryText } from '../shared/batch-views.js';
import { openReceiptDrawer } from '../shared/receipt-views.js';

const link = (path) => pageUrl(`pages/warehouse/${path}`);

const hero = (user, data) => html`<section class="hero" aria-labelledby="hero-title">
  <div class="hero__content">
    <h1 id="hero-title">${greeting()}, ${user.name}!</h1>
    <p>Hôm nay có <strong>${formatNumber(data.kpis.movementsToday)}</strong> biến động kho. Đang có ${formatNumber(data.kpis.pendingReceipts)} phiếu nhập chờ xác nhận, ${formatNumber(data.kpis.lowStockCount)} thuốc sắp hết và ${formatNumber(data.kpis.expiringBatches)} lô sắp hết hạn cần xử lý.</p>
  </div>
  <div class="hero__actions">
    <a class="btn btn--light" href="${link('receiving.html')}">${icon('plus', 'icon--sm')} Tạo phiếu nhập</a>
    <a class="btn btn--outline-light" href="${link('stocktake.html')}">${icon('clipboardCheck', 'icon--sm')} Kiểm kê kho</a>
  </div>
</section>`;

const taskList = (tasks) =>
  html`<div class="list">${tasks.map(
    (task) => html`<a class="task-item" href="${link(task.link)}">
  <span class="task-item__count ${task.tone === 'danger' ? 'task-item__count--danger' : task.tone === 'info' ? 'task-item__count--info' : ''}">${formatNumber(task.count)}</span>
  <span class="grow">${task.label}</span>${icon('chevronRight', 'icon--sm')}</a>`,
  )}</div>`;

const lowStockList = (items) =>
  items.length
    ? html`<div class="list">${items.map((medicine) => html`<div class="list__item"><div class="list__main">${medicineCell(medicine, `Tồn ${formatNumber(medicine.availableQty)} / ngưỡng ${formatNumber(medicine.minStock)} ${medicine.unit}`)}</div>${statusBadge('stock', medicine.stockStatus)}</div>`)}</div>`
    : emptyState({ title: 'Tồn kho ổn định', iconName: 'checkCircle' });

const expiringList = (items) =>
  items.length
    ? html`<div class="list">${items.map(
        (
          batch,
        ) => html`<div class="list__item"><div class="list__main"><strong>${batch.medicineName}</strong><small>Lô ${batch.code} · còn ${formatNumber(batch.remainingQty)} ${batch.unit}</small></div>
        <div class="text-right"><div>${formatDate(batch.expiryDate)}</div><small class="${batch.status === 'EXPIRED' ? 'text-danger' : 'text-secondary'}">${expiryText(batch.expiryDate)}</small></div></div>`,
      )}</div>`
    : emptyState({ title: 'Không có lô cần chú ý', iconName: 'checkCircle' });

const receiptsTable = (receipts) =>
  receipts.length
    ? html`<div class="table-wrap"><table class="table"><caption class="visually-hidden">Phiếu nhập gần đây</caption>
        <thead><tr><th scope="col">Phiếu</th><th scope="col">Nhà cung cấp</th><th scope="col" class="num">Tổng SL</th><th scope="col" class="num">Giá trị</th><th scope="col">Trạng thái</th></tr></thead>
        <tbody>${receipts.map(
          (
            receipt,
          ) => html`<tr class="is-clickable" data-receipt="${receipt.id}" tabindex="0"><td><strong>${receipt.code}</strong><br /><small class="text-secondary">${formatDate(receipt.receivingDate)}</small></td>
          <td>${receipt.supplierName}</td><td class="num">${formatNumber(receipt.totalQuantity)}</td><td class="num">${formatCurrency(receipt.totalAmount)}</td><td>${statusBadge('receipt', receipt.status)}</td></tr>`,
        )}</tbody></table></div>`
    : emptyState({ title: 'Chưa có phiếu nhập', iconName: 'clipboard' });

const movementList = (items) =>
  items.length
    ? html`<div class="list">${items.map(
        (
          movement,
        ) => html`<div class="list__item"><div class="list__main"><strong>${movement.medicineName}</strong><small>${movement.referenceCode} · ${formatDateTime(movement.createdAt)}</small></div>
        <div class="text-right">${statusBadge('movement', movement.type)}<div class="${movement.quantity < 0 ? 'diff-negative' : 'diff-positive'}">${movement.quantity > 0 ? '+' : ''}${formatNumber(movement.quantity)}</div></div></div>`,
      )}</div>`
    : emptyState({ title: 'Chưa có biến động', iconName: 'history' });

startPage({
  role: 'WAREHOUSE',
  nav: 'dashboard',
  title: 'Tổng quan kho',
  render: async ({ user, content }) => {
    content.innerHTML =
      html`<div class="stack">${skeletonTable(2)}<div class="grid grid--kpi">${Array.from({ length: 6 }, () => html`<span class="skeleton skeleton--block"></span>`)}</div></div>`.value;
    const data = await dashboardService.warehouse();
    const { kpis } = data;
    content.innerHTML = html`
      ${hero(user, data)}
      <div class="grid grid--kpi">
        ${kpiCard({ label: 'Thuốc đang quản lý', value: formatNumber(kpis.totalMedicines), iconName: 'pill' })}
        ${kpiCard({ label: 'Tổng số lô còn hàng', value: formatNumber(kpis.totalBatches), iconName: 'archive', tone: 'info' })}
        ${kpiCard({ label: 'Thuốc sắp hết', value: formatNumber(kpis.lowStockCount), iconName: 'alertTriangle', tone: 'warning' })}
        ${kpiCard({ label: 'Lô sắp hết hạn', value: formatNumber(kpis.expiringBatches), iconName: 'hourglass', tone: 'danger' })}
        ${kpiCard({ label: 'Phiếu nhập đang chờ', value: formatNumber(kpis.pendingReceipts), iconName: 'clipboard', tone: 'info' })}
        ${kpiCard({ label: 'Biến động kho hôm nay', value: formatNumber(kpis.movementsToday), iconName: 'history', tone: 'success' })}
      </div>
      <div class="grid grid--main-side">
        ${card({ title: 'Công việc cần xử lý', flush: true, body: taskList(data.tasks) })}
        ${card({ title: 'Nhập / xuất 14 ngày', body: chartBox('movement-chart', 'Nhập xuất theo ngày', 'chart-box--sm') })}
      </div>
      <div class="grid grid--2">
        ${card({ title: 'Thuốc sắp hết', flush: true, actions: html`<a class="btn btn--ghost btn--sm" href="${link('inventory.html?stockStatus=LOW_STOCK')}">Xem tất cả</a>`, body: lowStockList(data.lowStock) })}
        ${card({ title: 'Lô sắp hết hạn', flush: true, actions: html`<a class="btn btn--ghost btn--sm" href="${link('expiry.html')}">Xem tất cả</a>`, body: expiringList(data.expiring) })}
      </div>
      <div class="grid grid--main-side">
        ${card({ title: 'Phiếu nhập gần đây', flush: true, actions: html`<a class="btn btn--ghost btn--sm" href="${link('receipts.html')}">Xem tất cả</a>`, body: receiptsTable(data.recentReceipts) })}
        ${card({ title: 'Biến động kho gần đây', flush: true, actions: html`<a class="btn btn--ghost btn--sm" href="${link('movements.html')}">Lịch sử</a>`, body: movementList(data.recentMovements) })}
      </div>
    `.value;
    renderChart(content.querySelector('#movement-chart'), (theme) => ({
      ariaLabel: 'Biểu đồ nhập xuất kho 14 ngày',
      chart: {
        type: 'bar',
        data: {
          labels: data.movementSeries.map((point) => shortDate(point.date)),
          datasets: [
            {
              label: 'Nhập',
              data: data.movementSeries.map((point) => point.inQty),
              backgroundColor: theme.primary,
              borderRadius: 4,
            },
            {
              label: 'Xuất',
              data: data.movementSeries.map((point) => point.outQty),
              backgroundColor: theme.secondary,
              borderRadius: 4,
            },
          ],
        },
        options: {
          plugins: { legend: { position: 'bottom', labels: { boxWidth: 10 } } },
          scales: {
            y: { beginAtZero: true, grid: { color: theme.grid } },
            x: { grid: { display: false }, ticks: { maxTicksLimit: 7 } },
          },
        },
      },
    }));
    content.addEventListener('click', (event) => {
      const row = event.target instanceof Element ? event.target.closest('[data-receipt]') : null;
      if (row) openReceiptDrawer(row.dataset.receipt, { role: 'WAREHOUSE' });
    });
  },
});
