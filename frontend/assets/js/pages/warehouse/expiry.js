import { html } from '../../core/html.js';
import { getQuery } from '../../core/dom.js';
import { formatNumber } from '../../core/format.js';
import { startPage } from '../../components/page.js';
import { pageHeading } from '../../components/blocks.js';
import { inventoryService } from '../../services/inventory-service.js';
import { mountBatchTable } from '../shared/batch-views.js';

startPage({
  role: 'WAREHOUSE',
  nav: 'expiry',
  title: 'Hạn sử dụng',
  render: async ({ content }) => {
    const summary = await inventoryService.expirySummary();
    const options = [
      ...summary.tiers.map((tier) => ({
        key: String(tier.days),
        label: `Trong ${tier.days} ngày`,
        count: tier.count,
        params: { expiryWithin: String(tier.days), status: '', hasStock: '' },
      })),
      {
        key: 'expired',
        label: 'Đã hết hạn',
        count: summary.expired,
        params: { expiryWithin: '', status: 'EXPIRED', hasStock: 'true' },
      },
    ];
    const requested = getQuery().tier;
    let active = options.find((option) => option.key === requested) || options[options.length - 2] || options[0];

    content.innerHTML = html`
      ${pageHeading({ title: 'Hạn sử dụng', subtitle: 'Ưu tiên xuất bán lô gần hết hạn. Lô đã quá hạn không thể bán và cần hủy theo quy trình.' })}
      <div class="chip-row" role="group" aria-label="Ngưỡng hạn sử dụng">
        ${options.map((option) => html`<button type="button" class="chip" data-tier="${option.key}" aria-pressed="${option === active}">${option.label} · ${formatNumber(option.count)} lô</button>`)}
      </div>
      <div data-table></div>
    `.value;

    const table = mountBatchTable({
      container: content.querySelector('[data-table]'),
      canWriteOff: true,
      showStatusFilter: false,
      baseParams: {},
    });
    table.setParams(active.params);

    content.addEventListener('click', (event) => {
      const button = event.target instanceof Element ? event.target.closest('[data-tier]') : null;
      if (!button) return;
      active = options.find((option) => option.key === button.dataset.tier);
      content
        .querySelectorAll('[data-tier]')
        .forEach((chip) => chip.setAttribute('aria-pressed', String(chip === button)));
      table.setParams(active.params);
    });
  },
});
