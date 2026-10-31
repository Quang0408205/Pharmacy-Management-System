import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { delegate, getQuery, qs } from '../../core/dom.js';
import { formatDate, formatNumber } from '../../core/format.js';
import { errorMessage } from '../../core/errors.js';
import { startPage } from '../../components/page.js';
import { mountTablePage } from '../../components/blocks.js';
import { openModal } from '../../components/overlay.js';
import { setLoading } from '../../components/form.js';
import { toast } from '../../components/toast.js';
import { batchService, disposalService } from '../../services/inventory-service.js';
import { mountDisposalTable, openDisposalDrawer } from '../shared/disposal-views.js';

const reasonOptions = [
  ['EXPIRED', 'Hết hạn'],
  ['DAMAGED', 'Hư hỏng'],
  ['RECALLED', 'Thu hồi'],
];

const batchLabel = (batch) =>
  `${batch.medicineName} · Lô ${batch.code} · HSD ${formatDate(batch.expiryDate)} · còn ${formatNumber(batch.remainingQty)} ${batch.unit}`;

const lineRow = (batches, index, line) => html`<div class="form-grid" data-line="${index}">
  <div class="form-field form-field--full">
    <label class="form-label" for="line-batch-${index}">Lô thuốc</label>
    <select class="form-control" id="line-batch-${index}" data-field="batchId">
      <option value="">Chọn lô cần hủy</option>
      ${batches.map(
        (batch) =>
          html`<option value="${batch.id}" ${batch.id === line.batchId ? 'selected' : ''}>${batchLabel(batch)}</option>`,
      )}
    </select>
  </div>
  <div class="form-field">
    <label class="form-label" for="line-qty-${index}">Số lượng hủy</label>
    <input class="form-control" id="line-qty-${index}" type="number" min="1" data-field="quantity" value="${line.quantity}" />
  </div>
  <div class="form-field">
    <label class="form-label" for="line-type-${index}">Loại lý do</label>
    <select class="form-control" id="line-type-${index}" data-field="reasonType">
      ${reasonOptions.map(
        ([value, label]) =>
          html`<option value="${value}" ${value === line.reasonType ? 'selected' : ''}>${label}</option>`,
      )}
    </select>
  </div>
  <div class="form-field form-field--full">
    <label class="form-label" for="line-detail-${index}">Lý do chi tiết</label>
    <input class="form-control" id="line-detail-${index}" data-field="reasonDetail" maxlength="300" value="${line.reasonDetail}" />
  </div>
  <div class="form-field form-field--full"><button type="button" class="btn btn--ghost btn--sm" data-remove-line="${index}">${icon('trash', 'icon--sm')} Bỏ lô này</button></div>
</div>`;

const openCreate = async (presetBatchId) => {
  const batches = (await batchService.list({ hasStock: 'true', size: 200, sort: 'expiryDate,asc' })).items.filter(
    (batch) => batch.status !== 'QUARANTINED',
  );
  const defaultLine = (batchId = '') => {
    const batch = batches.find((item) => item.id === batchId);
    return {
      batchId,
      quantity: batch ? batch.remainingQty : '',
      reasonType: batch && batch.status === 'EXPIRED' ? 'EXPIRED' : 'DAMAGED',
      reasonDetail: '',
    };
  };
  const lines = [defaultLine(presetBatchId)];
  const modal = openModal({
    title: 'Lập phiếu xuất hủy',
    size: 'lg',
    content: html`<form novalidate data-disposal-form>
      <div class="alert alert--danger hidden" role="alert" data-form-alert></div>
      <p class="text-secondary text-small">Các lô trong phiếu sẽ bị cách ly khi bạn gửi duyệt. Tồn kho chỉ bị trừ sau khi quản trị viên duyệt và bạn xác nhận thực hiện hủy.</p>
      <div class="form-grid">
        <div class="form-field form-field--full">
          <label class="form-label" for="disposal-reason">Lý do chung <span class="required" aria-hidden="true">*</span></label>
          <input class="form-control" id="disposal-reason" data-reason maxlength="300" placeholder="Ví dụ: Tiêu hủy thuốc hết hạn tháng 10" />
        </div>
        <div class="form-field form-field--full">
          <label class="form-label" for="disposal-note">Ghi chú</label>
          <textarea class="form-control" id="disposal-note" rows="2" data-note maxlength="500"></textarea>
        </div>
      </div>
      <h3>Các lô cần hủy</h3>
      <div data-lines></div>
      <button type="button" class="btn btn--secondary btn--sm" data-add-line>${icon('plus', 'icon--sm')} Thêm lô</button>
    </form>`,
    footer: html`<button type="button" class="btn btn--secondary" data-dismiss="cancel">Hủy</button><button type="button" class="btn btn--primary" data-save>Lưu nháp</button>`,
  });
  const form = qs('[data-disposal-form]', modal.body);
  const alertBox = qs('[data-form-alert]', form);
  const container = qs('[data-lines]', form);

  const draw = () => {
    container.innerHTML = lines.map((line, index) => lineRow(batches, index, line).value).join('');
  };
  const sync = () => {
    container.querySelectorAll('[data-line]').forEach((element) => {
      const line = lines[Number(element.dataset.line)];
      element.querySelectorAll('[data-field]').forEach((input) => {
        line[input.dataset.field] = input.value;
      });
    });
  };
  draw();

  delegate(form, 'change', '[data-field="batchId"]', (event, select) => {
    sync();
    const index = Number(select.closest('[data-line]').dataset.line);
    lines[index] = { ...defaultLine(select.value), reasonDetail: lines[index].reasonDetail };
    draw();
  });
  delegate(form, 'click', '[data-add-line]', () => {
    sync();
    lines.push(defaultLine());
    draw();
  });
  delegate(form, 'click', '[data-remove-line]', (event, button) => {
    sync();
    if (lines.length === 1) {
      lines[0] = defaultLine();
    } else {
      lines.splice(Number(button.dataset.removeLine), 1);
    }
    draw();
  });

  const save = qs('[data-save]', modal.footer);
  save.addEventListener('click', async () => {
    sync();
    alertBox.classList.add('hidden');
    const reason = qs('[data-reason]', form).value.trim();
    const problem = !reason
      ? 'Vui lòng nhập lý do chung.'
      : lines.some((line) => !line.batchId || !(Number(line.quantity) >= 1))
        ? 'Mỗi lô cần chọn lô thuốc và số lượng lớn hơn 0.'
        : '';
    if (problem) {
      alertBox.textContent = problem;
      alertBox.classList.remove('hidden');
      return;
    }
    setLoading(save, true);
    try {
      const disposal = await disposalService.create({
        reason,
        note: qs('[data-note]', form).value.trim(),
        lines: lines.map((line) => ({
          batchId: line.batchId,
          quantity: Number(line.quantity),
          reasonType: line.reasonType,
          reasonDetail: line.reasonDetail.trim(),
        })),
      });
      toast.success('Đã lưu nháp phiếu xuất hủy', disposal.code);
      modal.close(disposal);
    } catch (error) {
      setLoading(save, false);
      alertBox.textContent = errorMessage(error);
      alertBox.classList.remove('hidden');
    }
  });
  return modal.closed;
};

startPage({
  role: 'WAREHOUSE',
  nav: 'disposals',
  title: 'Xuất hủy',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Xuất hủy thuốc',
      subtitle: 'Lập phiếu hủy thuốc hết hạn, hư hỏng hoặc bị thu hồi, gửi quản trị viên duyệt rồi thực hiện hủy.',
      mount: (container) => {
        const create = async (presetBatchId) => {
          try {
            const created = await openCreate(presetBatchId);
            if (created && created !== true) {
              table.reload();
              openDisposalDrawer(created.id, { role: 'WAREHOUSE', onChanged: () => table.reload() });
            }
          } catch (error) {
            toast.error('Không thể mở biểu mẫu', errorMessage(error));
          }
        };
        const table = mountDisposalTable({
          container,
          role: 'WAREHOUSE',
          toolbarExtra: html`<button type="button" class="btn btn--primary" data-action="create">${icon('plus', 'icon--sm')} Lập phiếu xuất hủy</button>`,
          onAction: (action) => {
            if (action === 'create') create();
          },
        });
        const preset = getQuery().batchId;
        if (preset) create(preset);
        return table;
      },
    }),
});
