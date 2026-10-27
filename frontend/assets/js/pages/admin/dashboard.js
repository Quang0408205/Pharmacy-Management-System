import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { pageUrl } from '../../core/dom.js';
import {
  formatCompact,
  formatCurrency,
  formatDate,
  formatDateTime,
  formatNumber,
  formatRelative,
  greeting,
  shortDate,
} from '../../core/format.js';
import { startPage } from '../../components/page.js';
import { card, kpiCard, medicineCell } from '../../components/blocks.js';
import { chartBox, renderChart } from '../../components/charts.js';
import { statusBadge } from '../../components/badge.js';
import { emptyState, skeletonTable } from '../../components/states.js';
import { dashboardService } from '../../services/admin-service.js';
import { openInvoiceDrawer } from '../shared/invoice-views.js';
import { expiryText } from '../shared/batch-views.js';

const link = (path) => pageUrl(`pages/admin/${path}`);

const hero = (user, kpis) => html`<section class="hero" aria-labelledby="hero-title">
  <div class="hero__content">
    <h1 id="hero-title">${greeting()}, ${user.name}!</h1>
    <p>Hôm nay đã có <strong>${formatNumber(kpis.todayInvoices)}</strong> hóa đơn với doanh thu <strong>${formatCurrency(kpis.todayRevenue)}</strong>. Có ${formatNumber(kpis.lowStockCount)} thuốc cần theo dõi tồn kho và ${formatNumber(kpis.expiringBatches)} lô sắp hết hạn.</p>
  </div>
  <div class="hero__actions quick-actions">
    <a class="btn btn--light" href="${link('medicines.html')}">${icon('pill', 'icon--sm')} Quản lý thuốc</a>
    <a class="btn btn--outline-light" href="${link('invoices.html')}">${icon('receipt', 'icon--sm')} Hóa đơn</a>
    <a class="btn btn--outline-light" href="${link('employees.html')}">${icon('idCard', 'icon--sm')} Mời nhân viên</a>
    <a class="btn btn--outline-light" href="${link('reports.html')}">${icon('chart', 'icon--sm')} Báo cáo</a>
  </div>
</section>`;

const kpiGrid = (kpis) => html`<div class="grid grid--kpi">
  ${kpiCard({ label: 'Doanh thu hôm nay', value: formatCurrency(kpis.todayRevenue), iconName: 'banknote' })}
  ${kpiCard({ label: 'Doanh thu tháng', value: formatCurrency(kpis.monthRevenue), iconName: 'trendingUp', tone: 'info' })}
  ${kpiCard({ label: 'Số hóa đơn hôm nay', value: formatNumber(kpis.todayInvoices), hint: `Tháng này: ${formatNumber(kpis.monthInvoices)} hóa đơn`, iconName: 'receipt', tone: 'success' })}
  ${kpiCard({ label: 'Thuốc đang kinh doanh', value: formatNumber(kpis.activeMedicines), iconName: 'pill' })}
  ${kpiCard({ label: 'Thuốc sắp hết', value: formatNumber(kpis.lowStockCount), hint: 'Gồm cả thuốc đã hết hàng', iconName: 'alertTriangle', tone: 'warning' })}
  ${kpiCard({ label: 'Lô gần hết hạn', value: formatNumber(kpis.expiringBatches), iconName: 'hourglass', tone: 'danger' })}
</div>`;

const recentTable = (invoices) =>
  invoices.length
    ? html`<div class="table-wrap"><table class="table"><caption class="visually-hidden">Giao dịch gần đây</caption>
        <thead><tr><th scope="col">Hóa đơn</th><th scope="col">Khách hàng</th><th scope="col">Nhân viên</th><th scope="col" class="num">Tổng tiền</th><th scope="col">Trạng thái</th></tr></thead>
        <tbody>${invoices.map(
          (
            invoice,
          ) => html`<tr class="is-clickable" data-invoice="${invoice.id}" tabindex="0"><td><strong>${invoice.code}</strong><br /><small class="text-secondary">${formatDateTime(invoice.createdAt)}</small></td>
          <td>${invoice.customerName}</td><td>${invoice.employeeName}</td><td class="num">${formatCurrency(invoice.total)}</td><td>${statusBadge('invoice', invoice.status)}</td></tr>`,
        )}</tbody></table></div>`
    : emptyState({ title: 'Chưa có giao dịch', iconName: 'receipt' });

const lowStockList = (items) =>
  items.length
    ? html`<div class="list">${items.map((medicine) => html`<div class="list__item"><div class="list__main">${medicineCell(medicine, `Tồn ${formatNumber(medicine.availableQty)} / ngưỡng ${formatNumber(medicine.minStock)} ${medicine.unit}`)}</div>${statusBadge('stock', medicine.stockStatus)}</div>`)}</div>`
    : emptyState({
        title: 'Tồn kho ổn định',
        message: 'Không có thuốc nào dưới ngưỡng cảnh báo.',
        iconName: 'checkCircle',
      });

const expiringList = (items) =>
  items.length
    ? html`<div class="list">${items.map(
        (
          batch,
        ) => html`<div class="list__item"><div class="list__main"><strong>${batch.medicineName}</strong><small>Lô ${batch.code} · còn ${formatNumber(batch.remainingQty)} ${batch.unit}</small></div>
        <div class="text-right"><div>${formatDate(batch.expiryDate)}</div><small class="${batch.status === 'EXPIRED' ? 'text-danger' : 'text-secondary'}">${expiryText(batch.expiryDate)}</small></div></div>`,
      )}</div>`
    : emptyState({ title: 'Không có lô cần chú ý', iconName: 'checkCircle' });

const activityList = (items) =>
  items.length
    ? html`<div class="list">${items.map((log) => html`<div class="list__item"><span class="timeline__dot"></span><div class="list__main"><strong>${log.description}</strong><small>${log.actorName} · ${formatRelative(log.createdAt)}</small></div>${statusBadge('role', log.role)}</div>`)}</div>`
    : emptyState({ title: 'Chưa có hoạt động', iconName: 'activity' });

const drawCharts = (data) => {
  renderChart(document.querySelector('#revenue-chart'), (theme) => ({
    ariaLabel: 'Biểu đồ doanh thu 14 ngày gần nhất',
    chart: {
      type: 'bar',
      data: {
        labels: data.revenueSeries.map((point) => shortDate(point.date)),
        datasets: [
          {
            label: 'Doanh thu',
            data: data.revenueSeries.map((point) => point.revenue),
            backgroundColor: theme.primary,
            borderRadius: 6,
            maxBarThickness: 28,
          },
        ],
      },
      options: {
        plugins: {
          legend: { display: false },
          tooltip: { callbacks: { label: (context) => ` ${formatCurrency(context.parsed.y)}` } },
        },
        scales: {
          y: { beginAtZero: true, grid: { color: theme.grid }, ticks: { callback: (value) => formatCompact(value) } },
          x: { grid: { display: false } },
        },
      },
    },
  }));
  const sorted = [...data.categoryDistribution].sort((a, b) => b.medicines - a.medicines);
  const top = sorted.slice(0, 7);
  const others = sorted.slice(7).reduce((sum, entry) => sum + entry.medicines, 0);
  renderChart(document.querySelector('#category-chart'), (theme) => ({
    ariaLabel: 'Biểu đồ phân bố thuốc theo danh mục',
    chart: {
      type: 'doughnut',
      data: {
        labels: [...top.map((entry) => entry.name), ...(others ? ['Danh mục khác'] : [])],
        datasets: [
          {
            data: [...top.map((entry) => entry.medicines), ...(others ? [others] : [])],
            backgroundColor: [...top.map((entry) => entry.color), ...(others ? ['#94a3b8'] : [])],
            borderColor: theme.surface,
            borderWidth: 2,
          },
        ],
      },
      options: {
        cutout: '62%',
        plugins: { legend: { position: 'bottom', labels: { boxWidth: 10, font: { size: 11 } } } },
      },
    },
  }));
};

startPage({
  role: 'ADMIN',
  nav: 'dashboard',
  title: 'Tổng quan',
  render: async ({ user, content }) => {
    content.innerHTML =
      html`<div class="stack">${skeletonTable(2)}<div class="grid grid--kpi">${Array.from({ length: 6 }, () => html`<span class="skeleton skeleton--block"></span>`)}</div></div>`.value;
    const data = await dashboardService.admin();
    content.innerHTML = html`
      ${hero(user, data.kpis)}
      ${kpiGrid(data.kpis)}
      <div class="grid grid--main-side">
        ${card({ title: 'Doanh thu 14 ngày gần nhất', body: chartBox('revenue-chart', 'Doanh thu 14 ngày') })}
        ${card({ title: 'Phân bố thuốc theo danh mục', body: chartBox('category-chart', 'Phân bố danh mục') })}
      </div>
      <div class="grid grid--main-side">
        ${card({ title: 'Giao dịch gần đây', flush: true, actions: html`<a class="btn btn--ghost btn--sm" href="${link('invoices.html')}">Xem tất cả</a>`, body: recentTable(data.recentInvoices) })}
        ${card({ title: 'Cảnh báo tồn thấp', flush: true, actions: html`<a class="btn btn--ghost btn--sm" href="${link('inventory.html')}">Chi tiết</a>`, body: lowStockList(data.lowStock) })}
      </div>
      <div class="grid grid--2">
        ${card({ title: 'Lô sắp hết hạn', flush: true, actions: html`<a class="btn btn--ghost btn--sm" href="${link('inventory.html')}">Chi tiết</a>`, body: expiringList(data.expiring) })}
        ${card({ title: 'Hoạt động nhân viên gần đây', flush: true, actions: html`<a class="btn btn--ghost btn--sm" href="${link('activity.html')}">Nhật ký</a>`, body: activityList(data.activities) })}
      </div>
    `.value;
    drawCharts(data);
    content.addEventListener('click', (event) => {
      const row = event.target instanceof Element ? event.target.closest('[data-invoice]') : null;
      if (row) openInvoiceDrawer(row.dataset.invoice, { role: 'ADMIN' });
    });
  },
});
