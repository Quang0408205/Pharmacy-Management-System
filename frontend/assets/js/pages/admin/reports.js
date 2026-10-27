import { html } from '../../core/html.js';
import {
  addDaysKey,
  formatCompact,
  formatCurrency,
  formatDate,
  formatNumber,
  shortDate,
  todayKey,
} from '../../core/format.js';
import { startPage } from '../../components/page.js';
import { card, kpiCard, pageHeading, progressBar } from '../../components/blocks.js';
import { chartBox, renderChart } from '../../components/charts.js';
import { emptyState, skeletonTable } from '../../components/states.js';
import { reportService } from '../../services/admin-service.js';
import { mountBatchTable } from '../shared/batch-views.js';

const ranges = [
  { key: '7', label: '7 ngày' },
  { key: '30', label: '30 ngày' },
  { key: '90', label: '90 ngày' },
];

const bestSellerList = (items) =>
  items.length
    ? html`<div class="list">${items.map((item, index) => html`<div class="list__item"><strong>${index + 1}</strong><div class="list__main"><strong>${item.name}</strong><small>${item.code} · ${formatNumber(item.quantity)} ${item.unit} · ${formatCurrency(item.revenue)}</small>${progressBar(item.quantity / items[0].quantity)}</div></div>`)}</div>`
    : emptyState({ title: 'Chưa có dữ liệu bán hàng', iconName: 'chart' });

const employeeList = (items) =>
  items.length
    ? html`<div class="list">${items.map((item) => html`<div class="list__item"><div class="list__main"><strong>${item.name}</strong><small>${formatNumber(item.invoices)} hóa đơn · ${formatCurrency(item.revenue)}</small>${progressBar(item.revenue / items[0].revenue)}</div></div>`)}</div>`
    : emptyState({ title: 'Chưa có dữ liệu', iconName: 'users' });

startPage({
  role: 'ADMIN',
  nav: 'reports',
  title: 'Thống kê & báo cáo',
  render: async ({ content }) => {
    content.innerHTML = html`
      ${pageHeading({
        title: 'Thống kê & báo cáo',
        subtitle: 'Doanh thu, sản phẩm bán chạy, phân bố tồn kho và hiệu suất nhân viên.',
        actions: html`<label class="row">Khoảng thời gian
          <select class="form-control" data-range aria-label="Khoảng thời gian">${ranges.map((range) => html`<option value="${range.key}" ${range.key === '30' ? 'selected' : ''}>${range.label}</option>`)}</select></label>`,
      })}
      <div data-report-body>${skeletonTable(6)}</div>
    `.value;
    const body = content.querySelector('[data-report-body]');
    const select = content.querySelector('[data-range]');

    const load = async () => {
      body.innerHTML = skeletonTable(6).value;
      const days = Number(select.value);
      const params = { from: addDaysKey(todayKey(), -(days - 1)), to: todayKey() };
      const [summary, revenue, bestSellers, distribution, byEmployee] = await Promise.all([
        reportService.summary(params),
        reportService.revenue(params),
        reportService.bestSellers({ ...params, limit: 8 }),
        reportService.inventoryDistribution(),
        reportService.revenueByEmployee(params),
      ]);
      body.innerHTML = html`<div class="page-content">
        <div class="grid grid--kpi-4">
          ${kpiCard({ label: 'Doanh thu', value: formatCurrency(summary.revenue), hint: `${formatDate(summary.from)} - ${formatDate(summary.to)}`, iconName: 'banknote' })}
          ${kpiCard({ label: 'Số hóa đơn', value: formatNumber(summary.invoices), hint: `${formatNumber(summary.cancelled)} hóa đơn đã hủy`, iconName: 'receipt', tone: 'info' })}
          ${kpiCard({ label: 'Giá trị trung bình', value: formatCurrency(summary.averageOrder), iconName: 'trendingUp', tone: 'success' })}
          ${kpiCard({ label: 'Số lượng bán ra', value: formatNumber(summary.itemsSold), iconName: 'package', tone: 'warning' })}
        </div>
        <div class="grid grid--main-side">
          ${card({ title: 'Doanh thu theo ngày', body: chartBox('report-revenue', 'Doanh thu theo ngày') })}
          ${card({ title: 'Phân bố tồn kho theo danh mục', body: chartBox('report-distribution', 'Phân bố tồn kho') })}
        </div>
        <div class="grid grid--2">
          ${card({ title: 'Thuốc bán chạy', flush: true, body: bestSellerList(bestSellers) })}
          ${card({ title: 'Doanh thu theo nhân viên', flush: true, body: employeeList(byEmployee) })}
        </div>
        <section><h2>Lô sắp hết hạn trong 60 ngày</h2><div data-expiring></div></section>
      </div>`.value;

      renderChart(body.querySelector('#report-revenue'), (theme) => ({
        ariaLabel: 'Biểu đồ doanh thu theo ngày',
        chart: {
          type: 'line',
          data: {
            labels: revenue.map((point) => shortDate(point.date)),
            datasets: [
              {
                label: 'Doanh thu',
                data: revenue.map((point) => point.revenue),
                borderColor: theme.primary,
                backgroundColor: `${theme.primary}22`,
                fill: true,
                tension: 0.3,
                pointRadius: days > 30 ? 0 : 3,
              },
            ],
          },
          options: {
            plugins: {
              legend: { display: false },
              tooltip: { callbacks: { label: (context) => ` ${formatCurrency(context.parsed.y)}` } },
            },
            scales: {
              y: {
                beginAtZero: true,
                grid: { color: theme.grid },
                ticks: { callback: (value) => formatCompact(value) },
              },
              x: { grid: { display: false }, ticks: { maxTicksLimit: 12 } },
            },
          },
        },
      }));
      const sorted = [...distribution].sort((a, b) => b.quantity - a.quantity).slice(0, 10);
      renderChart(body.querySelector('#report-distribution'), (theme) => ({
        ariaLabel: 'Biểu đồ tồn kho theo danh mục',
        chart: {
          type: 'bar',
          data: {
            labels: sorted.map((entry) => entry.name),
            datasets: [
              {
                label: 'Tồn khả dụng',
                data: sorted.map((entry) => entry.quantity),
                backgroundColor: sorted.map((entry) => entry.color),
                borderRadius: 4,
              },
            ],
          },
          options: {
            indexAxis: 'y',
            plugins: { legend: { display: false } },
            scales: {
              x: { grid: { color: theme.grid }, ticks: { callback: (value) => formatCompact(value) } },
              y: { grid: { display: false }, ticks: { font: { size: 11 } } },
            },
          },
        },
      }));
      mountBatchTable({
        container: body.querySelector('[data-expiring]'),
        baseParams: { expiryWithin: 60 },
        showStatusFilter: false,
      });
    };

    select.addEventListener('change', load);
    await load();
  },
});
