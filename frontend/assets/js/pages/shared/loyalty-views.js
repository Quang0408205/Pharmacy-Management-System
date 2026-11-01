import { html } from '../../core/html.js';
import { formatCurrency, formatNumber } from '../../core/format.js';
import { pageHeading } from '../../components/blocks.js';
import { statusBadge } from '../../components/badge.js';
import { skeletonTable } from '../../components/states.js';
import { customerService } from '../../services/partner-service.js';
import { mountCustomerTable } from './partner-views.js';

const tierHint = (tier, nextMin) =>
  tier.tier === 'STANDARD' ? `Dưới ${formatCurrency(nextMin)}` : `Từ ${formatCurrency(tier.minSpent)} trở lên`;

const tierCard = (
  tier,
  nextMin,
) => html`<article class="card card--hover tier-card tier-card--${tier.tier.toLowerCase()}">
  <div class="tier-card__head">${statusBadge('tier', tier.tier)}<span class="tier-card__rate">Tích điểm ×${tier.earnRatePercent / 100}</span></div>
  <div class="tier-card__count">${formatNumber(tier.customerCount)}<small> khách</small></div>
  <div class="tier-card__hint">${tierHint(tier, nextMin)}</div>
</article>`;

export const mountLoyaltyPage = async ({ content, role }) => {
  const heading = pageHeading({
    title: 'Khách hàng thân thiết',
    subtitle: 'Hạng thành viên tính theo tổng chi tiêu tích lũy. Hạng càng cao tích điểm càng nhiều.',
  });
  content.innerHTML = html`${heading}${skeletonTable(4)}`.value;
  const summary = await customerService.loyaltySummary();
  const tiers = summary.tiers;
  content.innerHTML = html`
    ${heading}
    ${summary.enabled ? '' : html`<div class="alert alert--warning">Chương trình tích điểm đang tắt, khách chưa được tích hoặc đổi điểm.</div>`}
    <div class="grid grid--kpi-4">${tiers.map((tier) => tierCard(tier, tiers[1].minSpent))}</div>
    <div data-table></div>
  `.value;
  mountCustomerTable({
    container: content.querySelector('[data-table]'),
    role,
    defaultSort: 'totalSpent,desc',
  });
};
