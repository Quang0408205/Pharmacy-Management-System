import { html } from '../../core/html.js';
import { formatCurrency, formatNumber } from '../../core/format.js';
import { startPage } from '../../components/page.js';
import { kpiCard, pageHeading } from '../../components/blocks.js';
import { invoiceService } from '../../services/sales-service.js';
import { mountInvoiceTable } from '../shared/invoice-views.js';

startPage({
  role: 'SALES',
  nav: 'history',
  title: 'Lịch sử bán',
  render: async ({ content }) => {
    const summary = await invoiceService.salesSummary();
    content.innerHTML = html`
      ${pageHeading({ title: 'Lịch sử bán', subtitle: 'Các hóa đơn do bạn lập.' })}
      <div class="grid grid--kpi-3">
        ${kpiCard({ label: 'Hóa đơn hôm nay', value: formatNumber(summary.invoicesToday), iconName: 'receipt' })}
        ${kpiCard({ label: 'Doanh số hôm nay', value: formatCurrency(summary.revenueToday), iconName: 'banknote', tone: 'success' })}
        ${kpiCard({ label: 'Đơn nháp đang lưu', value: formatNumber(summary.draftCount), iconName: 'clipboard', tone: 'warning' })}
      </div>
      <div data-table></div>
    `.value;
    mountInvoiceTable({ container: content.querySelector('[data-table]'), role: 'SALES', mine: true });
  },
});
