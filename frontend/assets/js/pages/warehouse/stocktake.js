import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { formatNumber } from '../../core/format.js';
import { errorMessage } from '../../core/errors.js';
import { startPage } from '../../components/page.js';
import { mountTablePage, rowButton } from '../../components/blocks.js';
import { confirmDialog, openDrawer } from '../../components/overlay.js';
import { openFormDialog } from '../../components/form-modal.js';
import { skeletonTable } from '../../components/states.js';
import { toast } from '../../components/toast.js';
import { setLoading } from '../../components/form.js';
import { categoryService } from '../../services/catalog-service.js';
import { stocktakeService } from '../../services/inventory-service.js';
import { differenceCell, mountStocktakeTable, openStocktakeDrawer } from '../shared/stocktake-views.js';

const lineStatus = (line) => {
  if (line.actualQty === null || line.actualQty === '') return html`<span class="badge">Chưa nhập</span>`;
  return Number(line.actualQty) === line.systemQty
    ? html`<span class="badge badge--success">Khớp</span>`
    : html`<span class="badge badge--warning">Lệch</span>`;
};

const openEditor = async (id, onChanged) => {
  const drawer = openDrawer({ title: 'Kiểm kê kho', size: 'lg', content: skeletonTable(8) });
  try {
    const stocktake = await stocktakeService.get(id);
    drawer.panel.querySelector('h2').textContent = `${stocktake.code} · ${stocktake.scopeLabel}`;
    const lines = stocktake.lines.map((line) => ({ ...line }));

    drawer.setBody(html`<div class="stack">
      <p class="text-secondary">Nhập số lượng thực tế đếm được cho từng lô. Dòng chênh lệch bắt buộc ghi lý do trước khi gửi duyệt. Quản trị viên sẽ duyệt và điều chỉnh tồn kho.</p>
      <div class="alert alert--danger hidden" data-alert role="alert"></div>
      <div class="table-wrap"><table class="table">
        <thead><tr><th>Thuốc</th><th>Lô</th><th class="num">Hệ thống</th><th class="num">Thực tế</th><th class="num">Chênh lệch</th><th>Lý do</th><th>Trạng thái</th></tr></thead>
        <tbody>${lines.map(
          (
            line,
            index,
          ) => html`<tr data-row="${index}"><td>${line.medicineName}</td><td>${line.batchCode}</td><td class="num">${formatNumber(line.systemQty)}</td>
          <td class="num"><input class="form-control stocktake-input" type="number" min="0" step="1" data-actual="${index}" value="${line.actualQty ?? ''}" aria-label="Số lượng thực tế lô ${line.batchCode}" /></td>
          <td class="num" data-diff="${index}">${differenceCell(line.difference)}</td>
          <td><input class="form-control" type="text" data-reason="${index}" value="${line.reason}" aria-label="Lý do chênh lệch lô ${line.batchCode}" placeholder="Nếu có chênh lệch" /></td>
          <td data-status="${index}">${lineStatus(line)}</td></tr>`,
        )}</tbody></table></div>
    </div>`);
    drawer.setFooter(html`<button type="button" class="btn btn--danger" data-act="delete">${icon('trash', 'icon--sm')} Xóa phiếu</button>
      <button type="button" class="btn btn--secondary" data-act="save">${icon('save', 'icon--sm')} Lưu nháp</button>
      <button type="button" class="btn btn--primary" data-act="submit">${icon('send', 'icon--sm')} Gửi duyệt</button>`);

    const alertBox = drawer.body.querySelector('[data-alert]');
    drawer.body.addEventListener('input', (event) => {
      const target = event.target;
      if (!(target instanceof HTMLInputElement)) return;
      const index = Number(target.dataset.actual ?? target.dataset.reason);
      const line = lines[index];
      if (target.dataset.actual !== undefined) {
        line.actualQty = target.value === '' ? null : Number(target.value);
        line.difference = line.actualQty === null ? 0 : line.actualQty - line.systemQty;
        drawer.body.querySelector(`[data-diff="${index}"]`).innerHTML = differenceCell(line.difference).value;
        drawer.body.querySelector(`[data-status="${index}"]`).innerHTML = lineStatus(line).value;
      } else line.reason = target.value;
      target.removeAttribute('aria-invalid');
    });

    const collect = () => ({
      lines: lines.map((line) => ({ batchId: line.batchId, actualQty: line.actualQty, reason: line.reason })),
    });

    drawer.footer.addEventListener('click', async (event) => {
      const button = event.target instanceof Element ? event.target.closest('[data-act]') : null;
      if (!button) return;
      const action = button.dataset.act;
      alertBox.classList.add('hidden');
      if (action === 'delete') {
        if (
          !(await confirmDialog({
            title: 'Xóa phiếu kiểm kê nháp?',
            message: `Phiếu ${stocktake.code} sẽ bị xóa.`,
            confirmText: 'Xóa phiếu',
            tone: 'danger',
          }))
        )
          return;
        try {
          await stocktakeService.remove(stocktake.id);
          toast.success('Đã xóa phiếu kiểm kê', stocktake.code);
          drawer.close();
          onChanged();
        } catch (error) {
          toast.error('Không thể xóa', errorMessage(error));
        }
        return;
      }
      if (action === 'submit') {
        const missing = lines.filter((line) => line.actualQty === null);
        const noReason = lines.filter(
          (line) => line.actualQty !== null && line.difference !== 0 && !line.reason.trim(),
        );
        if (missing.length || noReason.length) {
          alertBox.textContent = missing.length
            ? `Còn ${missing.length} dòng chưa nhập số lượng thực tế.`
            : `Còn ${noReason.length} dòng chênh lệch chưa ghi lý do.`;
          alertBox.classList.remove('hidden');
          const targetRow = missing.length ? lines.indexOf(missing[0]) : lines.indexOf(noReason[0]);
          const input = drawer.body.querySelector(
            missing.length ? `[data-actual="${targetRow}"]` : `[data-reason="${targetRow}"]`,
          );
          input.setAttribute('aria-invalid', 'true');
          input.focus();
          return;
        }
        if (
          !(await confirmDialog({
            title: 'Gửi duyệt phiếu kiểm kê?',
            message: 'Sau khi gửi, bạn không thể chỉnh sửa. Quản trị viên sẽ duyệt hoặc từ chối.',
            confirmText: 'Gửi duyệt',
          }))
        )
          return;
      }
      setLoading(button, true);
      try {
        if (action === 'save') {
          await stocktakeService.save(stocktake.id, collect());
          toast.success('Đã lưu nháp kiểm kê', stocktake.code);
        } else {
          await stocktakeService.submit(stocktake.id, collect());
          toast.success('Đã gửi duyệt', `${stocktake.code} đang chờ quản trị viên duyệt.`);
          drawer.close();
        }
        onChanged();
      } catch (error) {
        alertBox.textContent = errorMessage(error);
        alertBox.classList.remove('hidden');
        toast.error('Không thể thực hiện', errorMessage(error));
      } finally {
        setLoading(button, false);
      }
    });
  } catch (error) {
    drawer.setBody(html`<div class="alert alert--danger" role="alert">${errorMessage(error)}</div>`);
  }
};

const openCreate = async () => {
  const categories = await categoryService.list();
  return openFormDialog({
    title: 'Tạo phiên kiểm kê',
    intro: html`<p class="text-secondary text-small">Hệ thống chụp lại tồn của các lô còn hàng trong phạm vi đã chọn (tối đa 60 lô) để bạn đối chiếu với số đếm thực tế.</p>`,
    fields: [
      {
        name: 'categoryId',
        label: 'Phạm vi kiểm kê',
        type: 'select',
        required: true,
        full: true,
        options: [
          { value: 'ALL', label: 'Toàn bộ kho' },
          ...categories.map((category) => ({ value: category.id, label: category.name })),
        ],
      },
      { name: 'note', label: 'Ghi chú', type: 'textarea', full: true },
    ],
    values: { categoryId: 'ALL' },
    submitText: 'Tạo phiên kiểm kê',
    onSubmit: (values) => stocktakeService.create(values),
  }).closed;
};

startPage({
  role: 'WAREHOUSE',
  nav: 'stocktake',
  title: 'Kiểm kê',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Kiểm kê kho',
      subtitle: 'Tạo phiên kiểm kê, nhập số lượng thực tế và gửi quản trị viên duyệt điều chỉnh.',
      mount: (container) => {
        const table = mountStocktakeTable({
          container,
          role: 'WAREHOUSE',
          toolbarExtra: html`<button type="button" class="btn btn--primary" data-action="create">${icon('plus', 'icon--sm')} Tạo phiên kiểm kê</button>`,
          rowActions: (row) =>
            rowButton(
              row.status === 'DRAFT' ? 'edit' : 'view',
              row.status === 'DRAFT' ? `Nhập số liệu ${row.code}` : `Xem phiếu ${row.code}`,
              row.status === 'DRAFT' ? 'edit' : 'eye',
            ),
          onAction: async (action, row) => {
            if (action === 'create') {
              const created = await openCreate();
              if (created && created !== true) {
                table.reload();
                openEditor(created.id, () => table.reload());
              }
              return;
            }
            if (row.status === 'DRAFT') openEditor(row.id, () => table.reload());
            else openStocktakeDrawer(row.id, { role: 'WAREHOUSE', onChanged: () => table.reload() });
          },
        });
        return table;
      },
    }),
});
