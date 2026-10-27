import { html, raw } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { debounce, delegate, getQuery, pageUrl, qs } from '../../core/dom.js';
import { formatCurrency, formatDate, formatNumber, todayKey } from '../../core/format.js';
import { errorMessage } from '../../core/errors.js';
import { medicineImage } from '../../core/medicine-image.js';
import { startPage } from '../../components/page.js';
import { confirmDialog, openModal } from '../../components/overlay.js';
import { toast } from '../../components/toast.js';
import { emptyState, errorState, skeletonTable } from '../../components/states.js';
import { setLoading } from '../../components/form.js';
import { categoryService, medicineService } from '../../services/catalog-service.js';
import { supplierService } from '../../services/partner-service.js';
import { receiptService } from '../../services/inventory-service.js';
import { openReceiptDrawer } from '../shared/receipt-views.js';

const state = {
  code: '',
  supplierId: '',
  receivingDate: todayKey(),
  note: '',
  lines: [],
  draftId: null,
  categoryId: '',
  q: '',
  medicines: [],
  page: 1,
  totalPages: 1,
};

const lineSubtotal = (line) => (Number(line.quantity) || 0) * (Number(line.purchasePrice) || 0);
const totalQuantity = () => state.lines.reduce((sum, line) => sum + (Number(line.quantity) || 0), 0);
const totalAmount = () => state.lines.reduce((sum, line) => sum + lineSubtotal(line), 0);

const lineFields = [
  { key: 'batchCode', label: 'Số lô', type: 'text', placeholder: 'VD: L2610-0001' },
  { key: 'manufactureDate', label: 'Ngày sản xuất', type: 'date' },
  { key: 'expiryDate', label: 'Hạn sử dụng', type: 'date', required: true },
  { key: 'quantity', label: 'Số lượng', type: 'number', min: 1, step: 1 },
  { key: 'purchasePrice', label: 'Giá nhập (VNĐ)', type: 'number', min: 0, step: 500 },
];

const renderLine = (line, index) => html`<div class="receipt-line" data-line="${index}">
  <div class="receipt-line__head">
    <div><strong>${line.medicine.name}</strong><div class="text-secondary text-small">${line.medicine.code} · ${line.medicine.manufacturer} · Đơn vị: ${line.medicine.unit}</div></div>
    <button type="button" class="icon-button icon-button--sm" data-remove="${index}" aria-label="Xóa ${line.medicine.name} khỏi phiếu">${icon('trash', 'icon--sm')}</button>
  </div>
  <div class="receipt-line__grid">
    ${lineFields.map(
      (field) => html`<div class="form-field">
        <label class="form-label" for="line-${index}-${field.key}">${field.label}${field.required ? html` <span class="required" aria-hidden="true">*</span>` : ''}</label>
        <input class="form-control" id="line-${index}-${field.key}" type="${field.type}" data-field="${field.key}" data-index="${index}" value="${line[field.key] ?? ''}"
          ${field.min !== undefined ? raw(`min="${field.min}"`) : ''} ${field.step !== undefined ? raw(`step="${field.step}"`) : ''} ${field.placeholder ? raw(`placeholder="${field.placeholder}"`) : ''} />
        <span class="form-error" data-line-error="${index}-${field.key}" role="alert"></span>
      </div>`,
    )}
    <div class="receipt-line__subtotal"><span>Thành tiền</span><span data-subtotal="${index}">${formatCurrency(lineSubtotal(line))}</span></div>
  </div>
</div>`;

const shell = (user) => html`<div class="workspace">
  <section class="workspace__left" aria-label="Chọn thuốc nhập">
    <div class="card">
      <div class="workspace__left-head">
        <div class="chip-row" data-categories role="group" aria-label="Danh mục thuốc"></div>
        <div class="toolbar__search">${icon('search')}<input class="form-control" type="search" data-search placeholder="Tìm thuốc theo tên, mã, nhà sản xuất..." aria-label="Tìm thuốc" autocomplete="off" /></div>
      </div>
      <div class="workspace__scroll" data-list-region aria-live="polite"></div>
      <button type="button" class="btn btn--primary mobile-cart-toggle" data-show-receipt>${icon('clipboard', 'icon--sm')} Xem phiếu nhập <span class="cart-badge" data-line-count>0</span></button>
    </div>
  </section>
  <section class="workspace__right" aria-label="Phiếu nhập">
    <div class="card">
      <div class="invoice-head invoice-head--two">
        <div class="invoice-head__meta"><span>Mã phiếu</span><strong data-code>...</strong></div>
        <div class="invoice-head__meta"><span>Nhân viên</span><strong class="truncate">${user.name}</strong></div>
        <div class="form-field">
          <label class="form-label" for="receiving-date">Ngày nhập</label>
          <input class="form-control" id="receiving-date" type="date" data-date max="${todayKey()}" value="${todayKey()}" />
        </div>
        <div class="form-field">
          <label class="form-label" for="supplier">Nhà cung cấp <span class="required" aria-hidden="true">*</span></label>
          <select class="form-control" id="supplier" data-supplier><option value="">Chọn nhà cung cấp</option></select>
          <span class="form-error" data-supplier-error role="alert"></span>
        </div>
        <button type="button" class="btn btn--ghost btn--sm mobile-only" data-show-catalog>${icon('chevronLeft', 'icon--sm')} Quay lại chọn thuốc</button>
      </div>
      <div class="workspace__scroll workspace__scroll--cart" data-lines-region aria-live="polite"></div>
      <div class="workspace__footer">
        <div class="summary-lines">
          <div class="summary-lines__row"><span>Tổng số lượng</span><strong data-total-qty>0</strong></div>
          <div class="summary-lines__row summary-lines__row--total"><span>Tổng tiền nhập</span><span data-total-amount>0 ₫</span></div>
        </div>
        <div class="workspace-actions">
          <button type="button" class="btn btn--primary btn--lg btn--block-row" data-confirm>${icon('checkCircle')} Xác nhận nhập kho</button>
          <button type="button" class="btn btn--secondary btn--sm" data-save-draft>${icon('save', 'icon--sm')} Lưu nháp</button>
          <a class="btn btn--ghost btn--sm" href="${pageUrl('pages/warehouse/receipts.html')}">Danh sách phiếu</a>
          <button type="button" class="btn btn--ghost btn--sm" data-reset>${icon('reset', 'icon--sm')} Làm mới</button>
        </div>
      </div>
    </div>
  </section>
</div>`;

startPage({
  role: 'WAREHOUSE',
  nav: 'receiving',
  title: 'Nhập thuốc',
  render: async ({ user, content }) => {
    content.innerHTML = shell(user).value;
    const workspace = qs('.workspace', content);
    const refs = {
      categories: qs('[data-categories]', content),
      search: qs('[data-search]', content),
      list: qs('[data-list-region]', content),
      lines: qs('[data-lines-region]', content),
      code: qs('[data-code]', content),
      supplier: qs('[data-supplier]', content),
      supplierError: qs('[data-supplier-error]', content),
      date: qs('[data-date]', content),
      totalQty: qs('[data-total-qty]', content),
      totalAmount: qs('[data-total-amount]', content),
      lineCount: qs('[data-line-count]', content),
      confirm: qs('[data-confirm]', content),
    };

    state.supplierId = '';
    state.receivingDate = todayKey();
    state.note = '';
    state.lines = [];
    state.draftId = null;
    state.q = '';
    state.categoryId = '';
    state.page = 1;

    const renderTotals = () => {
      refs.totalQty.textContent = formatNumber(totalQuantity());
      refs.totalAmount.textContent = formatCurrency(totalAmount());
      refs.lineCount.textContent = String(state.lines.length);
      state.lines.forEach((line, index) => {
        const subtotal = content.querySelector(`[data-subtotal="${index}"]`);
        if (subtotal) subtotal.textContent = formatCurrency(lineSubtotal(line));
      });
    };

    const renderLines = () => {
      refs.lines.innerHTML = state.lines.length
        ? state.lines.map((line, index) => renderLine(line, index).value).join('')
        : emptyState({
            title: 'Phiếu nhập chưa có thuốc',
            message: 'Chọn thuốc ở bên trái để thêm dòng nhập hàng.',
            iconName: 'inbox',
          }).value;
      renderTotals();
      renderList();
    };

    function renderList() {
      if (!state.medicines.length) {
        refs.list.innerHTML = emptyState({
          title: 'Không tìm thấy thuốc',
          message: 'Thử từ khóa hoặc danh mục khác.',
          iconName: 'search',
        }).value;
        return;
      }
      const selected = new Set(state.lines.map((line) => line.medicine.id));
      refs.list.innerHTML = html`${state.medicines.map(
        (
          medicine,
        ) => html`<button type="button" class="med-list-row ${selected.has(medicine.id) ? 'is-selected' : ''}" data-select="${medicine.id}" aria-label="Thêm ${medicine.name} vào phiếu nhập">
          <img class="med-thumb" src="${medicineImage(medicine)}" alt="" width="44" height="44" loading="lazy" />
          <span class="grow"><strong>${medicine.name}</strong><br /><span class="text-secondary text-small">${medicine.code} · ${medicine.manufacturer} · ${medicine.packaging}</span></span>
          <span class="text-small text-secondary">Tồn ${formatNumber(medicine.availableQty)}</span>
          <span class="med-card__add">${icon(selected.has(medicine.id) ? 'check' : 'plus', 'icon--sm')}</span>
        </button>`,
      )}${state.page < state.totalPages ? html`<button type="button" class="btn btn--secondary btn--block" data-more>Xem thêm thuốc</button>` : ''}`.value;
    }

    const loadMedicines = async ({ append = false } = {}) => {
      if (!append) refs.list.innerHTML = skeletonTable(8).value;
      try {
        const result = await medicineService.list({
          q: state.q,
          categoryId: state.categoryId,
          status: 'ACTIVE',
          size: 20,
          page: state.page,
          sort: 'name,asc',
        });
        state.medicines = append ? [...state.medicines, ...result.items] : result.items;
        state.totalPages = result.totalPages;
        renderList();
      } catch (error) {
        refs.list.innerHTML = errorState(error).value;
      }
    };

    const addLine = (medicine) => {
      state.lines.push({
        medicine,
        batchCode: '',
        manufactureDate: '',
        expiryDate: '',
        quantity: 10,
        purchasePrice: medicine.costPrice || 0,
      });
      renderLines();
      const element = content.querySelector(`[data-line="${state.lines.length - 1}"]`);
      if (element) {
        element.classList.add('is-added');
        element.scrollIntoView({ block: 'nearest' });
        element.querySelector('[data-field="batchCode"]').focus();
      }
    };

    const clearLineErrors = () => {
      content.querySelectorAll('[data-line-error]').forEach((element) => {
        element.textContent = '';
      });
      content.querySelectorAll('[aria-invalid]').forEach((element) => element.removeAttribute('aria-invalid'));
      refs.supplierError.textContent = '';
    };

    const showServerErrors = (fieldErrors = {}) => {
      let first = null;
      Object.entries(fieldErrors).forEach(([key, message]) => {
        const [, index, field] = key.split('.');
        if (key === 'supplierId') {
          refs.supplierError.textContent = message;
          refs.supplier.setAttribute('aria-invalid', 'true');
          first = first || refs.supplier;
          return;
        }
        const output = content.querySelector(`[data-line-error="${index}-${field}"]`);
        const input = content.querySelector(`#line-${index}-${field}`);
        if (!output || !input) return;
        output.textContent = message;
        input.setAttribute('aria-invalid', 'true');
        first = first || input;
      });
      if (first) first.focus();
    };

    const payload = (confirm) => ({
      supplierId: state.supplierId,
      receivingDate: state.receivingDate,
      note: state.note,
      confirm,
      items: state.lines.map((line) => ({
        medicineId: line.medicine.id,
        batchCode: line.batchCode,
        manufactureDate: line.manufactureDate,
        expiryDate: line.expiryDate,
        quantity: Number(line.quantity),
        purchasePrice: Number(line.purchasePrice),
      })),
    });

    const resetReceipt = async () => {
      state.lines = [];
      state.supplierId = '';
      state.receivingDate = todayKey();
      state.draftId = null;
      refs.supplier.value = '';
      refs.date.value = todayKey();
      clearLineErrors();
      renderLines();
      try {
        refs.code.textContent = (await receiptService.nextCode()).code;
      } catch {
        refs.code.textContent = 'Chưa xác định';
      }
    };

    const showResult = (receipt) => {
      const modal = openModal({
        title: 'Nhập kho thành công',
        size: 'lg',
        content: html`<div class="stack">
          <div class="alert alert--success">${icon('checkCircle')}<span>Phiếu <strong>${receipt.code}</strong> đã xác nhận. Tồn kho đã được cộng ${formatNumber(receipt.totalQuantity)} đơn vị từ ${receipt.supplierName}.</span></div>
          <div class="table-wrap"><table class="table"><thead><tr><th>Thuốc</th><th>Lô mới tạo</th><th>Hạn dùng</th><th class="num">Số lượng</th><th class="num">Thành tiền</th></tr></thead>
            <tbody>${receipt.items.map((item) => html`<tr><td>${item.medicineName}</td><td><strong>${item.batchCode}</strong></td><td>${formatDate(item.expiryDate)}</td><td class="num">+${formatNumber(item.quantity)} ${item.unit}</td><td class="num">${formatCurrency(item.subtotal)}</td></tr>`)}</tbody></table></div>
          <div class="summary-lines"><div class="summary-lines__row summary-lines__row--total"><span>Tổng giá trị nhập</span><span>${formatCurrency(receipt.totalAmount)}</span></div></div>
        </div>`,
        footer: html`<button type="button" class="btn btn--secondary" data-act="view">Xem phiếu nhập</button><a class="btn btn--secondary" href="${pageUrl('pages/warehouse/inventory.html')}">Xem tồn kho</a><button type="button" class="btn btn--primary" data-dismiss="confirm" data-autofocus>Tạo phiếu mới</button>`,
      });
      modal.footer
        .querySelector('[data-act="view"]')
        .addEventListener('click', () => openReceiptDrawer(receipt.id, { role: 'WAREHOUSE' }));
    };

    const submit = async (confirm) => {
      clearLineErrors();
      if (!state.supplierId) {
        refs.supplierError.textContent = 'Vui lòng chọn nhà cung cấp.';
        refs.supplier.setAttribute('aria-invalid', 'true');
        refs.supplier.focus();
        return;
      }
      if (!state.lines.length) {
        toast.warning('Phiếu nhập chưa có thuốc', 'Hãy chọn ít nhất một loại thuốc.');
        return;
      }
      if (confirm) {
        const ok = await confirmDialog({
          title: 'Xác nhận nhập kho?',
          message: `Sau khi xác nhận, ${formatNumber(totalQuantity())} đơn vị sẽ được cộng vào tồn kho và phiếu không thể chỉnh sửa.`,
          confirmText: 'Xác nhận nhập kho',
        });
        if (!ok) return;
      }
      const button = confirm ? refs.confirm : content.querySelector('[data-save-draft]');
      setLoading(button, true);
      try {
        const body = payload(confirm);
        const receipt = state.draftId
          ? await receiptService.update(state.draftId, body)
          : await receiptService.create(body);
        if (confirm) {
          showResult(receipt);
          await resetReceipt();
          state.page = 1;
          loadMedicines();
        } else {
          state.draftId = receipt.id;
          refs.code.textContent = receipt.code;
          toast.success('Đã lưu nháp phiếu nhập', receipt.code);
        }
      } catch (error) {
        showServerErrors(error.fieldErrors);
        toast.error(confirm ? 'Không thể xác nhận nhập kho' : 'Không thể lưu nháp', errorMessage(error));
      } finally {
        setLoading(button, false);
      }
    };

    const [categories, suppliers] = await Promise.all([
      categoryService.list(),
      supplierService.list({ status: 'ACTIVE', size: 100, sort: 'name,asc' }),
    ]);
    refs.categories.innerHTML = [{ id: '', name: 'Tất cả' }, ...categories]
      .map(
        (category) =>
          html`<button type="button" class="chip" data-category="${category.id}" aria-pressed="${category.id === ''}">${category.name}</button>`
            .value,
      )
      .join('');
    suppliers.items.forEach((supplier) => {
      const option = document.createElement('option');
      option.value = supplier.id;
      option.textContent = supplier.name;
      refs.supplier.append(option);
    });

    const { draft } = getQuery();
    if (draft) {
      try {
        const receipt = await receiptService.get(draft);
        if (receipt.status !== 'DRAFT') throw new Error('Phiếu đã được xác nhận, không thể chỉnh sửa.');
        const medicines = await Promise.all(receipt.items.map((item) => medicineService.get(item.medicineId)));
        state.draftId = receipt.id;
        state.supplierId = receipt.supplierId;
        state.receivingDate = receipt.receivingDate;
        state.lines = receipt.items.map((item, index) => ({ ...item, medicine: medicines[index] }));
        refs.supplier.value = receipt.supplierId;
        refs.date.value = receipt.receivingDate;
        refs.code.textContent = receipt.code;
        toast.info('Đang chỉnh sửa phiếu nháp', receipt.code);
      } catch (error) {
        toast.error('Không thể mở phiếu nháp', error.message);
      }
    }
    if (!state.draftId) refs.code.textContent = (await receiptService.nextCode()).code;
    renderLines();

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
    delegate(content, 'click', '[data-select]', (event, button) => {
      const medicine = state.medicines.find((item) => item.id === button.dataset.select);
      if (medicine) addLine(medicine);
    });
    delegate(content, 'click', '[data-more]', () => {
      state.page += 1;
      loadMedicines({ append: true });
    });
    delegate(content, 'click', '[data-remove]', (event, button) => {
      state.lines.splice(Number(button.dataset.remove), 1);
      renderLines();
    });
    delegate(content, 'input', '[data-field]', (event, input) => {
      const line = state.lines[Number(input.dataset.index)];
      if (!line) return;
      line[input.dataset.field] = input.value;
      input.removeAttribute('aria-invalid');
      const output = content.querySelector(`[data-line-error="${input.dataset.index}-${input.dataset.field}"]`);
      if (output) output.textContent = '';
      renderTotals();
    });
    refs.supplier.addEventListener('change', () => {
      state.supplierId = refs.supplier.value;
      refs.supplierError.textContent = '';
    });
    refs.date.addEventListener('change', () => {
      state.receivingDate = refs.date.value || todayKey();
    });
    delegate(content, 'click', '[data-confirm]', () => submit(true));
    delegate(content, 'click', '[data-save-draft]', () => submit(false));
    delegate(content, 'click', '[data-reset]', async () => {
      if (
        state.lines.length &&
        !(await confirmDialog({
          title: 'Làm mới phiếu nhập?',
          message: 'Các dòng thuốc đang nhập sẽ bị xóa khỏi màn hình.',
          confirmText: 'Làm mới',
          tone: 'danger',
        }))
      )
        return;
      await resetReceipt();
    });
    delegate(content, 'click', '[data-show-receipt]', () => workspace.classList.add('is-cart-view'));
    delegate(content, 'click', '[data-show-catalog]', () => workspace.classList.remove('is-cart-view'));

    await loadMedicines();
  },
});
