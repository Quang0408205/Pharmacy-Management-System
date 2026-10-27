import { html, raw } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { debounce, delegate, qs } from '../../core/dom.js';
import { formatCurrency, formatDateTime, formatNumber, formatRelative } from '../../core/format.js';
import { errorMessage } from '../../core/errors.js';
import { medicineImage } from '../../core/medicine-image.js';
import { startPage } from '../../components/page.js';
import { statusBadge, paymentLabels } from '../../components/badge.js';
import { confirmDialog, openModal } from '../../components/overlay.js';
import { toast } from '../../components/toast.js';
import { emptyState, errorState, skeletonCards } from '../../components/states.js';
import { setLoading } from '../../components/form.js';
import { categoryService, medicineService } from '../../services/catalog-service.js';
import { customerService } from '../../services/partner-service.js';
import { invoiceService } from '../../services/sales-service.js';
import { openCustomerForm } from '../shared/partner-views.js';
import { openInvoiceDrawer, openPrintPreview } from '../shared/invoice-views.js';

const pageSize = 24;

const state = {
  cart: [],
  customer: null,
  discountType: 'AMOUNT',
  discountValue: 0,
  paymentMethod: 'CASH',
  received: 0,
  draftId: null,
  categoryId: '',
  q: '',
  page: 1,
  totalPages: 1,
  medicines: [],
  invoiceNumber: '',
};

const subtotal = () => state.cart.reduce((sum, line) => sum + line.quantity * line.medicine.price, 0);

const discountAmount = () => {
  const base = subtotal();
  const value = Number(state.discountValue) || 0;
  const amount = state.discountType === 'PERCENT' ? Math.round((base * Math.min(value, 100)) / 100) : value;
  return Math.max(0, Math.min(amount, base));
};

const total = () => subtotal() - discountAmount();

const quickAmounts = () => {
  const due = total();
  if (!due) return [];
  const steps = [50000, 100000, 200000, 500000];
  const options = new Set([due]);
  steps.forEach((step) => options.add(Math.ceil(due / step) * step));
  return [...options].sort((a, b) => a - b).slice(0, 4);
};

const medicineCard = (medicine) => {
  const inCart = state.cart.find((line) => line.medicine.id === medicine.id);
  const remaining = medicine.availableQty - (inCart ? inCart.quantity : 0);
  const disabled = medicine.availableQty <= 0 || remaining <= 0;
  return html`<button type="button" class="med-card ${inCart ? 'is-selected' : ''}" data-add="${medicine.id}" ${disabled ? raw('disabled') : ''} aria-label="Thêm ${medicine.name} vào giỏ">
    ${medicine.prescriptionType !== 'OTC' ? html`<span class="med-card__flag">${statusBadge('prescription', medicine.prescriptionType)}</span>` : ''}
    <img class="med-card__image" src="${medicineImage(medicine)}" alt="" loading="lazy" />
    <span class="med-card__name">${medicine.name}</span>
    <span class="med-card__meta">${medicine.manufacturer} · ${medicine.packaging}</span>
    <span class="med-card__meta">${medicine.availableQty > 0 ? `Còn ${formatNumber(medicine.availableQty)} ${medicine.unit}` : 'Hết hàng'}${medicine.availableQty > 0 && medicine.stockStatus === 'LOW_STOCK' ? ' · Sắp hết' : ''}</span>
    <span class="med-card__footer"><span class="med-card__price">${formatCurrency(medicine.price)}</span><span class="med-card__add">${icon(disabled ? 'ban' : 'plus', 'icon--sm')}</span></span>
  </button>`;
};

const cartLine = (line) => html`<div class="cart-line" data-line="${line.medicine.id}">
  <div>
    <div class="cart-line__name">${line.medicine.name}</div>
    <div class="text-secondary text-small">${formatCurrency(line.medicine.price)} / ${line.medicine.unit} · tối đa ${formatNumber(line.medicine.availableQty)}</div>
  </div>
  <div class="cart-line__total">${formatCurrency(line.quantity * line.medicine.price)}</div>
  <div class="cart-line__fields">
    <div class="qty" role="group" aria-label="Số lượng ${line.medicine.name}">
      <button type="button" class="qty__button" data-dec="${line.medicine.id}" ${line.quantity <= 1 ? raw('disabled') : ''} aria-label="Giảm số lượng">−</button>
      <input class="qty__input" type="number" inputmode="numeric" min="1" max="${line.medicine.availableQty}" step="1" value="${line.quantity}" data-qty="${line.medicine.id}" aria-label="Số lượng" />
      <button type="button" class="qty__button" data-inc="${line.medicine.id}" ${line.quantity >= line.medicine.availableQty ? raw('disabled') : ''} aria-label="Tăng số lượng">+</button>
    </div>
    <button type="button" class="icon-button icon-button--sm" data-remove="${line.medicine.id}" aria-label="Xóa ${line.medicine.name} khỏi giỏ">${icon('trash', 'icon--sm')}</button>
  </div>
  <div class="cart-line__error" data-error="${line.medicine.id}" role="alert"></div>
</div>`;

const shell = (user) => html`<div class="workspace" data-workspace>
  <section class="workspace__left" aria-label="Chọn thuốc">
    <div class="card">
      <div class="workspace__left-head">
        <div class="chip-row" data-categories role="group" aria-label="Danh mục thuốc"></div>
        <div class="toolbar__search">
          ${icon('search')}
          <input class="form-control" type="search" data-search placeholder="Tìm theo tên, mã thuốc hoặc quét mã vạch..." aria-label="Tìm thuốc" autocomplete="off" />
        </div>
      </div>
      <div class="workspace__scroll" data-grid-region aria-live="polite"></div>
      <button type="button" class="btn btn--primary mobile-cart-toggle" data-show-cart>${icon('cart', 'icon--sm')} Xem giỏ hàng <span class="cart-badge" data-cart-count-mobile>0</span></button>
    </div>
  </section>
  <section class="workspace__right" aria-label="Hóa đơn">
    <div class="card">
      <div class="invoice-head">
        <div class="invoice-head__meta"><span>Số hóa đơn</span><strong data-invoice-number>...</strong></div>
        <div class="invoice-head__meta"><span>Ngày lập</span><strong>${formatDateTime(new Date())}</strong></div>
        <div class="invoice-head__meta"><span>Nhân viên bán</span><strong class="truncate">${user.name}</strong></div>
        <div class="invoice-head__full row" data-customer-region></div>
        <button type="button" class="btn btn--ghost btn--sm mobile-only" data-show-catalog>${icon('chevronLeft', 'icon--sm')} Quay lại chọn thuốc</button>
      </div>
      <div class="workspace__scroll workspace__scroll--cart" data-cart-region aria-live="polite"></div>
      <div class="workspace__footer">
        <div class="summary-lines" data-summary></div>
        <div class="pos-options">
          <div class="form-field">
            <label class="form-label" for="discount-value">Giảm giá</label>
            <div class="row row--tight">
              <select class="form-control" data-discount-type aria-label="Loại giảm giá"><option value="AMOUNT">VNĐ</option><option value="PERCENT">%</option></select>
              <input class="form-control grow" id="discount-value" type="number" min="0" step="500" value="0" data-discount-value />
            </div>
            <span class="form-error" data-discount-error role="alert"></span>
          </div>
          <div class="form-field" data-received-region>
            <label class="form-label" for="received">Tiền khách đưa</label>
            <input class="form-control" id="received" type="number" min="0" step="1000" value="" data-received inputmode="numeric" />
            <span class="form-error" data-received-error role="alert"></span>
          </div>
        </div>
        <div class="payment-methods" role="group" aria-label="Phương thức thanh toán">
          ${Object.entries({ CASH: 'banknote', TRANSFER: 'smartphone', CARD: 'creditCard' }).map(([key, name]) => html`<button type="button" class="payment-methods__option" data-payment="${key}" aria-pressed="${key === 'CASH'}">${icon(name, 'icon--sm')}${paymentLabels[key]}</button>`)}
        </div>
        <div class="pos-change" data-change-region>
          <div class="chip-row" data-quick-amounts></div>
          <div class="pos-change__value"><span>Tiền thừa</span><strong data-change>0 ₫</strong></div>
        </div>
        <div class="workspace-actions">
          <button type="button" class="btn btn--primary btn--lg btn--block-row" data-checkout>${icon('checkCircle')} Thanh toán</button>
          <button type="button" class="btn btn--secondary btn--sm" data-save-draft>${icon('save', 'icon--sm')} Lưu nháp</button>
          <button type="button" class="btn btn--secondary btn--sm" data-open-drafts>${icon('clipboard', 'icon--sm')} Nháp <span class="cart-badge" data-draft-count>0</span></button>
          <button type="button" class="btn btn--ghost btn--sm" data-reset>${icon('reset', 'icon--sm')} Xóa giỏ</button>
        </div>
      </div>
    </div>
  </section>
</div>`;

startPage({
  role: 'SALES',
  nav: 'pos',
  title: 'Bán thuốc tại quầy',
  render: async ({ user, content }) => {
    content.innerHTML = shell(user).value;
    const refs = {
      workspace: qs('[data-workspace]', content),
      categories: qs('[data-categories]', content),
      search: qs('[data-search]', content),
      gridRegion: qs('[data-grid-region]', content),
      cartRegion: qs('[data-cart-region]', content),
      customerRegion: qs('[data-customer-region]', content),
      summary: qs('[data-summary]', content),
      discountType: qs('[data-discount-type]', content),
      discountValue: qs('[data-discount-value]', content),
      discountError: qs('[data-discount-error]', content),
      received: qs('[data-received]', content),
      receivedRegion: qs('[data-received-region]', content),
      receivedError: qs('[data-received-error]', content),
      quick: qs('[data-quick-amounts]', content),
      change: qs('[data-change]', content),
      checkout: qs('[data-checkout]', content),
      invoiceNumber: qs('[data-invoice-number]', content),
      draftCount: qs('[data-draft-count]', content),
    };
    const cartCounts = content.querySelectorAll('[data-cart-count-mobile]');

    const renderCustomer = () => {
      refs.customerRegion.innerHTML = html`
        <span class="text-secondary">Khách hàng:</span>
        <strong class="grow truncate">${state.customer ? `${state.customer.name} · ${state.customer.phone}` : 'Khách lẻ'}</strong>
        <button type="button" class="btn btn--secondary btn--sm" data-pick-customer>${icon('users', 'icon--sm')} ${state.customer ? 'Đổi' : 'Chọn'}</button>
        ${state.customer ? html`<button type="button" class="icon-button icon-button--sm" data-clear-customer aria-label="Bỏ chọn khách hàng">${icon('x', 'icon--sm')}</button>` : ''}
      `.value;
    };

    const renderSummary = () => {
      const due = total();
      refs.summary.innerHTML = html`
        <div class="summary-lines__row"><span>Tạm tính</span><span>${formatCurrency(subtotal())}</span></div>
        <div class="summary-lines__row"><span>Giảm giá</span><span>-${formatCurrency(discountAmount())}</span></div>
        <div class="summary-lines__row summary-lines__row--total"><span>Tổng thanh toán</span><span>${formatCurrency(due)}</span></div>`.value;
      const isCash = state.paymentMethod === 'CASH';
      refs.receivedRegion.classList.toggle('is-hidden', !isCash);
      content.querySelector('[data-change-region]').classList.toggle('hidden', !isCash);
      const change = Math.max(0, (Number(state.received) || 0) - due);
      refs.change.textContent = formatCurrency(change);
      refs.quick.innerHTML = quickAmounts()
        .map(
          (amount, index) =>
            html`<button type="button" class="chip" data-quick="${amount}">${index === 0 && amount === due ? 'Vừa đủ' : formatNumber(amount)}</button>`
              .value,
        )
        .join('');
      refs.checkout.disabled = !state.cart.length;
      cartCounts.forEach((element) => {
        element.textContent = String(state.cart.length);
      });
    };

    const renderCart = (highlightId = null) => {
      if (!state.cart.length) {
        refs.cartRegion.innerHTML = emptyState({
          title: 'Giỏ hàng trống',
          message: 'Chọn thuốc ở bên trái để thêm vào hóa đơn.',
          iconName: 'cart',
        }).value;
      } else {
        refs.cartRegion.innerHTML = state.cart.map((line) => cartLine(line).value).join('');
        if (highlightId) {
          const element = refs.cartRegion.querySelector(`[data-line="${CSS.escape(highlightId)}"]`);
          if (element) {
            element.classList.add('is-added');
            element.scrollIntoView({ block: 'nearest' });
          }
        }
      }
      renderSummary();
    };

    const renderGrid = () => {
      if (!state.medicines.length) {
        refs.gridRegion.innerHTML = emptyState({
          title: 'Không tìm thấy thuốc',
          message: 'Thử từ khóa hoặc danh mục khác.',
          iconName: 'search',
        }).value;
        return;
      }
      refs.gridRegion.innerHTML = html`<div class="med-grid">${state.medicines.map(medicineCard)}</div>
        ${state.page < state.totalPages ? html`<div class="row" ><button type="button" class="btn btn--secondary btn--block" data-more>Xem thêm thuốc</button></div>` : ''}`.value;
    };

    const loadMedicines = async ({ append = false } = {}) => {
      if (!append) refs.gridRegion.innerHTML = skeletonCards(8).value;
      try {
        const result = await medicineService.list({
          sellable: 'true',
          q: state.q,
          categoryId: state.categoryId,
          size: pageSize,
          page: state.page,
          sort: 'name,asc',
        });
        state.medicines = append ? [...state.medicines, ...result.items] : result.items;
        state.totalPages = result.totalPages;
        renderGrid();
        return result;
      } catch (error) {
        refs.gridRegion.innerHTML = errorState(error).value;
        return null;
      }
    };

    const refreshInvoiceNumber = async () => {
      try {
        refs.invoiceNumber.textContent = (await invoiceService.nextNumber()).code;
      } catch {
        refs.invoiceNumber.textContent = 'Chưa xác định';
      }
    };

    const refreshDraftCount = async () => {
      try {
        refs.draftCount.textContent = String((await invoiceService.drafts()).length);
      } catch {
        refs.draftCount.textContent = '0';
      }
    };

    const addToCart = (medicine) => {
      const existing = state.cart.find((line) => line.medicine.id === medicine.id);
      if (existing) {
        if (existing.quantity >= medicine.availableQty) {
          toast.warning(
            'Đã đạt tồn kho tối đa',
            `${medicine.name} chỉ còn ${formatNumber(medicine.availableQty)} ${medicine.unit}.`,
          );
          return;
        }
        existing.quantity += 1;
      } else {
        if (medicine.availableQty <= 0) {
          toast.error('Thuốc đã hết hàng', medicine.name);
          return;
        }
        state.cart.push({ medicine, quantity: 1 });
      }
      renderCart(medicine.id);
      renderGrid();
      cartCounts.forEach((element) => {
        element.classList.remove('is-popped');
        void element.offsetWidth;
        element.classList.add('is-popped');
      });
    };

    const setQuantity = (id, value, { source } = {}) => {
      const line = state.cart.find((item) => item.medicine.id === id);
      if (!line) return;
      const number = Number(value);
      const errorBox = refs.cartRegion.querySelector(`[data-error="${CSS.escape(id)}"]`);
      let message = '';
      if (!Number.isInteger(number) || number < 1) message = 'Số lượng phải là số nguyên lớn hơn 0.';
      else if (number > line.medicine.availableQty)
        message = `Vượt tồn kho: chỉ còn ${formatNumber(line.medicine.availableQty)} ${line.medicine.unit}.`;
      if (message) {
        if (errorBox) errorBox.textContent = message;
        if (source) source.value = String(line.quantity);
        return;
      }
      line.quantity = number;
      renderCart();
      renderGrid();
    };

    const resetCart = () => {
      state.cart = [];
      state.customer = null;
      state.discountType = 'AMOUNT';
      state.discountValue = 0;
      state.paymentMethod = 'CASH';
      state.received = 0;
      state.draftId = null;
      refs.discountType.value = 'AMOUNT';
      refs.discountValue.value = '0';
      refs.received.value = '';
      refs.discountError.textContent = '';
      refs.receivedError.textContent = '';
      content
        .querySelectorAll('[data-payment]')
        .forEach((button) => button.setAttribute('aria-pressed', String(button.dataset.payment === 'CASH')));
      renderCustomer();
      renderCart();
      renderGrid();
    };

    const validateDiscount = () => {
      const value = Number(state.discountValue) || 0;
      let message = '';
      if (value < 0) message = 'Giảm giá không được âm.';
      else if (state.discountType === 'PERCENT' && value > 100) message = 'Giảm giá tối đa 100%.';
      else if (state.discountType === 'AMOUNT' && value > subtotal()) message = 'Giảm giá không được lớn hơn tạm tính.';
      refs.discountError.textContent = message;
      return !message;
    };

    const pickCustomer = async () => {
      const modal = openModal({
        title: 'Chọn khách hàng',
        content: html`<div class="stack">
          <div class="toolbar__search">${icon('search')}<input class="form-control" type="search" data-customer-search data-autofocus placeholder="Tìm theo tên hoặc số điện thoại..." aria-label="Tìm khách hàng" /></div>
          <div data-customer-results class="list"></div>
        </div>`,
        footer: html`<button type="button" class="btn btn--secondary" data-pick-walkin>Khách lẻ</button><button type="button" class="btn btn--primary" data-new-customer>${icon('plus', 'icon--sm')} Thêm khách hàng mới</button>`,
      });
      const results = modal.body.querySelector('[data-customer-results]');
      let customers = [];
      const search = async () => {
        try {
          const query = modal.body.querySelector('[data-customer-search]').value.trim();
          customers = (await customerService.list({ q: query, size: 8, sort: 'name,asc' })).items;
          results.innerHTML = customers.length
            ? customers
                .map(
                  (customer) =>
                    html`<button type="button" class="list__item list__item--button" data-customer="${customer.id}"><span class="avatar">${customer.name
                      .split(' ')
                      .slice(-2)
                      .map((part) => part[0])
                      .join(
                        '',
                      )}</span><span class="list__main"><strong>${customer.name}</strong><small>${customer.phone} · ${customer.code}</small></span></button>`
                      .value,
                )
                .join('')
            : emptyState({ title: 'Không tìm thấy khách hàng', iconName: 'users' }).value;
        } catch (error) {
          results.innerHTML = errorState(error, { retry: false }).value;
        }
      };
      modal.body.querySelector('[data-customer-search]').addEventListener('input', debounce(search, 250));
      search();
      delegate(modal.body, 'click', '[data-customer]', (event, button) => {
        state.customer = customers.find((customer) => customer.id === button.dataset.customer) || null;
        renderCustomer();
        modal.close();
      });
      modal.footer.querySelector('[data-pick-walkin]').addEventListener('click', () => {
        state.customer = null;
        renderCustomer();
        modal.close();
      });
      modal.footer.querySelector('[data-new-customer]').addEventListener('click', async () => {
        const created = await openCustomerForm();
        if (created && created !== true) {
          state.customer = created;
          renderCustomer();
          modal.close();
        }
      });
    };

    const askPrescription = (needsCode) =>
      new Promise((resolve) => {
        const modal = openModal({
          title: 'Kiểm tra đơn thuốc',
          size: 'sm',
          content: html`<form data-prescription novalidate class="stack">
            <div class="alert alert--warning">${icon('alertTriangle')}<span>Giỏ hàng có thuốc ${needsCode ? 'kiểm soát đặc biệt' : 'kê đơn'}. Cần xác nhận đã kiểm tra đơn thuốc theo quy tắc demo.</span></div>
            ${needsCode ? html`<div class="form-field"><label class="form-label" for="rx-code">Mã đơn thuốc <span class="required">*</span></label><input class="form-control" id="rx-code" name="code" data-autofocus autocomplete="off" /><span class="form-error" data-rx-error role="alert"></span></div>` : ''}
            <label class="check"><input type="checkbox" name="confirmed" /> Tôi đã kiểm tra đơn thuốc hợp lệ</label>
            <span class="form-error" data-rx-confirm-error role="alert"></span>
          </form>`,
          footer: html`<button type="button" class="btn btn--secondary" data-dismiss="cancel">Hủy</button><button type="submit" form="rx-form" class="btn btn--primary" data-rx-submit>Xác nhận và thanh toán</button>`,
        });
        const form = modal.body.querySelector('form');
        form.id = 'rx-form';
        form.addEventListener('submit', (event) => {
          event.preventDefault();
          const code = needsCode ? form.elements.code.value.trim() : '';
          const confirmed = form.elements.confirmed.checked;
          modal.body.querySelector('[data-rx-confirm-error]').textContent = confirmed
            ? ''
            : 'Vui lòng xác nhận đã kiểm tra đơn thuốc.';
          if (needsCode)
            modal.body.querySelector('[data-rx-error]').textContent = code ? '' : 'Vui lòng nhập mã đơn thuốc.';
          if (!confirmed || (needsCode && !code)) return;
          resolve({ prescriptionConfirmed: true, prescriptionCode: code });
          modal.close();
        });
        modal.closed.then(() => resolve(null));
      });

    const showSuccess = (invoice) => {
      const modal = openModal({
        title: 'Thanh toán thành công',
        size: 'sm',
        content: html`<div class="auth-state"><span class="auth-state__icon">${icon('checkCircle', 'icon--lg')}</span>
          <h3>${invoice.code}</h3>
          <p>Tổng thanh toán: <strong>${formatCurrency(invoice.total)}</strong><br />Phương thức: ${paymentLabels[invoice.paymentMethod]}<br />Tiền thừa: <strong>${formatCurrency(invoice.changeAmount)}</strong></p>
          <p class="text-secondary text-small">Đã trừ kho theo FEFO và ghi nhận ${formatNumber(invoice.items.reduce((sum, item) => sum + item.allocations.length, 0))} lô xuất.</p></div>`,
        footer: html`<button type="button" class="btn btn--secondary" data-act="detail">Xem chi tiết</button><button type="button" class="btn btn--secondary" data-act="print">${icon('printer', 'icon--sm')} In hóa đơn mẫu</button><button type="button" class="btn btn--primary" data-dismiss="confirm" data-autofocus>Đơn mới</button>`,
      });
      modal.footer.addEventListener('click', (event) => {
        const button = event.target instanceof Element ? event.target.closest('[data-act]') : null;
        if (!button) return;
        if (button.dataset.act === 'print') openPrintPreview(invoice);
        if (button.dataset.act === 'detail') openInvoiceDrawer(invoice.id, { role: 'SALES' });
      });
    };

    const checkout = async () => {
      if (!state.cart.length) {
        toast.warning('Giỏ hàng đang trống', 'Hãy chọn ít nhất một loại thuốc.');
        return;
      }
      if (!validateDiscount()) return;
      const due = total();
      if (state.paymentMethod === 'CASH' && (Number(state.received) || 0) < due) {
        refs.receivedError.textContent = 'Tiền khách đưa chưa đủ để thanh toán.';
        refs.received.focus();
        return;
      }
      refs.receivedError.textContent = '';
      const restricted = state.cart.filter((line) => line.medicine.prescriptionType !== 'OTC');
      const controlled = restricted.some((line) => line.medicine.prescriptionType === 'CONTROLLED');
      let prescription = { prescriptionConfirmed: false, prescriptionCode: '' };
      if (restricted.length) {
        if (controlled && !state.customer) {
          toast.error('Cần thông tin khách hàng', 'Thuốc kiểm soát đặc biệt bắt buộc gắn khách hàng.');
          pickCustomer();
          return;
        }
        prescription = await askPrescription(controlled);
        if (!prescription) return;
      }
      setLoading(refs.checkout, true);
      try {
        const invoice = await invoiceService.checkout({
          customerId: state.customer ? state.customer.id : null,
          items: state.cart.map((line) => ({ medicineId: line.medicine.id, quantity: line.quantity })),
          discountType: state.discountType,
          discountValue: Number(state.discountValue) || 0,
          paymentMethod: state.paymentMethod,
          receivedAmount: state.paymentMethod === 'CASH' ? Number(state.received) || 0 : due,
          draftId: state.draftId,
          ...prescription,
        });
        resetCart();
        refs.workspace.classList.remove('is-cart-view');
        showSuccess(invoice);
        refreshInvoiceNumber();
        refreshDraftCount();
        state.page = 1;
        loadMedicines();
      } catch (error) {
        toast.error('Thanh toán không thành công', errorMessage(error));
        if (error.fieldErrors && error.fieldErrors.customerId) pickCustomer();
        state.page = 1;
        loadMedicines();
      } finally {
        setLoading(refs.checkout, false);
        refs.checkout.disabled = !state.cart.length;
      }
    };

    const saveDraft = async () => {
      if (!state.cart.length) {
        toast.warning('Giỏ hàng đang trống', 'Không có gì để lưu nháp.');
        return;
      }
      try {
        await invoiceService.saveDraft({
          id: state.draftId,
          customerId: state.customer ? state.customer.id : null,
          items: state.cart.map((line) => ({ medicineId: line.medicine.id, quantity: line.quantity })),
          discountType: state.discountType,
          discountValue: Number(state.discountValue) || 0,
          paymentMethod: state.paymentMethod,
        });
        toast.success('Đã lưu đơn nháp', 'Bạn có thể mở lại ở mục Đơn nháp.');
        resetCart();
        refreshDraftCount();
      } catch (error) {
        toast.error('Không thể lưu nháp', errorMessage(error));
      }
    };

    const openDrafts = async () => {
      const modal = openModal({
        title: 'Đơn nháp đã lưu',
        size: 'lg',
        content: html`<div class="skeleton-table"><span class="skeleton skeleton--line"></span><span class="skeleton skeleton--line"></span></div>`,
      });
      let drafts = [];
      const draw = async () => {
        try {
          drafts = await invoiceService.drafts();
          modal.setBody(
            drafts.length
              ? html`<div class="table-wrap"><table class="table"><thead><tr><th>Lưu lúc</th><th>Khách hàng</th><th>Thuốc</th><th class="actions">Thao tác</th></tr></thead>
                <tbody>${drafts.map(
                  (
                    draft,
                  ) => html`<tr><td class="nowrap">${formatRelative(draft.savedAt)}</td><td>${draft.customerName}</td>
                  <td>${draft.items.map((item) => `${item.medicineName} × ${item.quantity}`).join(', ')}</td>
                  <td class="actions"><button type="button" class="btn btn--primary btn--sm" data-open="${draft.id}">Mở</button> <button type="button" class="btn btn--secondary btn--sm" data-delete="${draft.id}">Xóa</button></td></tr>`,
                )}</tbody></table></div>`
              : emptyState({ title: 'Chưa có đơn nháp', iconName: 'clipboard' }),
          );
        } catch (error) {
          modal.setBody(errorState(error, { retry: false }));
        }
      };
      delegate(modal.body, 'click', '[data-delete]', async (event, button) => {
        try {
          await invoiceService.removeDraft(button.dataset.delete);
          toast.success('Đã xóa đơn nháp');
          refreshDraftCount();
          draw();
        } catch (error) {
          toast.error('Không thể xóa', errorMessage(error));
        }
      });
      delegate(modal.body, 'click', '[data-open]', async (event, button) => {
        const draft = drafts.find((item) => item.id === button.dataset.open);
        if (!draft) return;
        if (
          state.cart.length &&
          !(await confirmDialog({
            title: 'Thay thế giỏ hàng hiện tại?',
            message: 'Giỏ hàng đang có thuốc sẽ bị thay bằng đơn nháp.',
            confirmText: 'Thay thế',
          }))
        )
          return;
        try {
          const loaded = await Promise.all(
            draft.items.map((item) =>
              medicineService.get(item.medicineId).then((medicine) => ({ medicine, quantity: item.quantity })),
            ),
          );
          const usable = loaded
            .filter((line) => line.medicine.status === 'ACTIVE' && line.medicine.availableQty > 0)
            .map((line) => ({ ...line, quantity: Math.min(line.quantity, line.medicine.availableQty) }));
          state.cart = usable;
          state.customer = draft.customerId ? await customerService.get(draft.customerId) : null;
          state.discountType = draft.discountType;
          state.discountValue = draft.discountValue;
          state.paymentMethod = draft.paymentMethod;
          state.draftId = draft.id;
          refs.discountType.value = draft.discountType;
          refs.discountValue.value = String(draft.discountValue);
          content
            .querySelectorAll('[data-payment]')
            .forEach((control) =>
              control.setAttribute('aria-pressed', String(control.dataset.payment === state.paymentMethod)),
            );
          renderCustomer();
          renderCart();
          renderGrid();
          if (usable.length < loaded.length)
            toast.warning('Một số thuốc đã hết hàng hoặc ngừng bán', 'Các dòng đó đã được bỏ khỏi giỏ.');
          modal.close();
        } catch (error) {
          toast.error('Không thể mở đơn nháp', errorMessage(error));
        }
      });
      draw();
    };

    renderCustomer();
    renderCart();
    refreshInvoiceNumber();
    refreshDraftCount();

    try {
      const categories = await categoryService.list();
      refs.categories.innerHTML = [{ id: '', name: 'Tất cả' }, ...categories]
        .map(
          (category) =>
            html`<button type="button" class="chip" data-category="${category.id}" aria-pressed="${category.id === ''}">${category.name}</button>`
              .value,
        )
        .join('');
    } catch {
      refs.categories.innerHTML = '';
    }

    delegate(content, 'click', '[data-category]', (event, button) => {
      state.categoryId = button.dataset.category;
      state.page = 1;
      refs.categories
        .querySelectorAll('[data-category]')
        .forEach((chip) => chip.setAttribute('aria-pressed', String(chip === button)));
      loadMedicines();
    });
    refs.search.addEventListener(
      'input',
      debounce(() => {
        state.q = refs.search.value.trim();
        state.page = 1;
        loadMedicines();
      }, 280),
    );
    refs.search.addEventListener('keydown', (event) => {
      if (event.key !== 'Enter') return;
      event.preventDefault();
      const exact = state.medicines.filter(
        (medicine) =>
          medicine.barcode === refs.search.value.trim() ||
          medicine.code.toLowerCase() === refs.search.value.trim().toLowerCase(),
      );
      const target = exact.length === 1 ? exact[0] : state.medicines.length === 1 ? state.medicines[0] : null;
      if (target) {
        addToCart(target);
        refs.search.select();
      } else toast.info('Chưa xác định được thuốc', 'Nhập đúng mã vạch/mã thuốc hoặc chọn từ danh sách.');
    });

    delegate(content, 'click', '[data-add]', (event, button) => {
      const medicine = state.medicines.find((item) => item.id === button.dataset.add);
      if (medicine) addToCart(medicine);
    });
    delegate(content, 'click', '[data-more]', () => {
      state.page += 1;
      loadMedicines({ append: true });
    });
    delegate(content, 'click', '[data-inc]', (event, button) => {
      const line = state.cart.find((item) => item.medicine.id === button.dataset.inc);
      if (line) setQuantity(line.medicine.id, line.quantity + 1);
    });
    delegate(content, 'click', '[data-dec]', (event, button) => {
      const line = state.cart.find((item) => item.medicine.id === button.dataset.dec);
      if (line) setQuantity(line.medicine.id, line.quantity - 1);
    });
    delegate(content, 'change', '[data-qty]', (event, input) =>
      setQuantity(input.dataset.qty, input.value, { source: input }),
    );
    delegate(content, 'click', '[data-remove]', (event, button) => {
      state.cart = state.cart.filter((line) => line.medicine.id !== button.dataset.remove);
      renderCart();
      renderGrid();
    });
    delegate(content, 'click', '[data-pick-customer]', pickCustomer);
    delegate(content, 'click', '[data-clear-customer]', () => {
      state.customer = null;
      renderCustomer();
    });
    delegate(content, 'click', '[data-payment]', (event, button) => {
      state.paymentMethod = button.dataset.payment;
      content
        .querySelectorAll('[data-payment]')
        .forEach((control) => control.setAttribute('aria-pressed', String(control === button)));
      refs.receivedError.textContent = '';
      renderSummary();
    });
    delegate(content, 'click', '[data-quick]', (event, button) => {
      state.received = Number(button.dataset.quick);
      refs.received.value = String(state.received);
      refs.receivedError.textContent = '';
      renderSummary();
    });
    refs.received.addEventListener('input', () => {
      state.received = Number(refs.received.value) || 0;
      refs.receivedError.textContent = '';
      renderSummary();
    });
    refs.discountType.addEventListener('change', () => {
      state.discountType = refs.discountType.value;
      validateDiscount();
      renderSummary();
    });
    refs.discountValue.addEventListener('input', () => {
      state.discountValue = refs.discountValue.value;
      validateDiscount();
      renderSummary();
    });
    delegate(content, 'click', '[data-checkout]', checkout);
    delegate(content, 'click', '[data-save-draft]', saveDraft);
    delegate(content, 'click', '[data-open-drafts]', openDrafts);
    delegate(content, 'click', '[data-reset]', async () => {
      if (!state.cart.length) {
        toast.info('Giỏ hàng đang trống');
        return;
      }
      const confirmed = await confirmDialog({
        title: 'Xóa giỏ hàng?',
        message: 'Tất cả thuốc trong giỏ và thông tin thanh toán sẽ bị xóa.',
        confirmText: 'Xóa giỏ',
        tone: 'danger',
      });
      if (confirmed) resetCart();
    });
    delegate(content, 'click', '[data-show-cart]', () => refs.workspace.classList.add('is-cart-view'));
    delegate(content, 'click', '[data-show-catalog]', () => refs.workspace.classList.remove('is-cart-view'));

    await loadMedicines();
  },
});
