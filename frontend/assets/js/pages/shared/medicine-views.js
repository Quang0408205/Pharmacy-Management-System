import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { getQuery } from '../../core/dom.js';
import { medicineUnits } from '../../config/app-config.js';
import { formatCurrency, formatDate, formatDateTime, formatNumber } from '../../core/format.js';
import { medicineImage } from '../../core/medicine-image.js';
import { createDataTable } from '../../components/data-table.js';
import { openFormDialog } from '../../components/form-modal.js';
import { openDrawer, confirmDialog } from '../../components/overlay.js';
import { bindTabs, detailList, medicineCell, rowButton, tabs } from '../../components/blocks.js';
import { statusBadge, statusOptions } from '../../components/badge.js';
import { toast } from '../../components/toast.js';
import { categoryService, medicineService } from '../../services/catalog-service.js';
import { errorMessage } from '../../core/errors.js';
import { skeletonTable } from '../../components/states.js';

const prescriptionOptions = [
  { value: 'OTC', label: 'Không kê đơn (OTC)' },
  { value: 'RX', label: 'Thuốc kê đơn' },
  { value: 'CONTROLLED', label: 'Kiểm soát đặc biệt' },
];

const stockOptions = [
  { value: 'IN_STOCK', label: 'Còn hàng' },
  { value: 'LOW_STOCK', label: 'Sắp hết' },
  { value: 'OUT_OF_STOCK', label: 'Hết hàng' },
];

export const loadCategoryOptions = async () =>
  (await categoryService.list()).map((category) => ({ value: category.id, label: category.name }));

const formFields = (categories) => [
  { name: 'code', label: 'Mã thuốc', required: true, maxlength: 20 },
  { name: 'barcode', label: 'Mã vạch', inputmode: 'numeric', maxlength: 14 },
  { name: 'name', label: 'Tên thuốc', required: true, full: true },
  { name: 'genericName', label: 'Hoạt chất' },
  { name: 'strength', label: 'Hàm lượng' },
  { name: 'dosageForm', label: 'Dạng bào chế' },
  {
    name: 'unit',
    label: 'Đơn vị tính',
    type: 'select',
    required: true,
    options: medicineUnits.map((unit) => ({ value: unit, label: unit })),
  },
  { name: 'packaging', label: 'Quy cách đóng gói' },
  {
    name: 'manufacturer',
    label: 'Nhà sản xuất',
    type: 'combobox',
    required: true,
    maxlength: 200,
    placeholder: 'Chọn hoặc nhập nhà sản xuất',
    loadOptions: () => medicineService.manufacturers(),
  },
  { name: 'country', label: 'Nước sản xuất' },
  {
    name: 'categoryId',
    label: 'Danh mục',
    type: 'select',
    required: true,
    options: categories,
    placeholder: 'Chọn danh mục',
  },
  { name: 'prescriptionType', label: 'Phân loại kê đơn', type: 'select', required: true, options: prescriptionOptions },
  { name: 'price', label: 'Giá bán (VNĐ)', type: 'number', min: 1, step: 500, required: true },
  { name: 'costPrice', label: 'Giá nhập (VNĐ)', type: 'number', min: 0, step: 500 },
  {
    name: 'minStock',
    label: 'Ngưỡng tồn tối thiểu',
    type: 'number',
    min: 0,
    required: true,
    help: 'Cảnh báo sắp hết khi tồn khả dụng thấp hơn hoặc bằng ngưỡng.',
  },
  { name: 'description', label: 'Mô tả', type: 'textarea', full: true },
];

const imageField = (medicine) => html`<div class="image-field" data-image-field>
  <img class="image-field__preview" data-image-preview src="${medicineImage(medicine || {})}" alt="Ảnh thuốc" />
  <div class="image-field__actions">
    <strong>Ảnh thuốc</strong>
    <span class="text-secondary text-small">JPG, PNG hoặc WebP, tối đa 2MB. Nếu không có ảnh, hệ thống dùng ảnh minh họa theo dạng bào chế.</span>
    <div class="row">
      <button type="button" class="btn btn--secondary btn--sm" data-image-pick>${icon('upload', 'icon--sm')} Chọn ảnh</button>
      <button type="button" class="btn btn--ghost btn--sm ${medicine?.imageUrl ? '' : 'hidden'}" data-image-remove>${icon('trash', 'icon--sm')} Xóa ảnh</button>
    </div>
    <input type="file" class="hidden" accept="image/jpeg,image/png,image/webp" data-image-input />
    <span class="form-error" data-image-error role="alert"></span>
  </div>
</div>`;

const bindImageField = (root, medicine) => {
  const preview = root.querySelector('[data-image-preview]');
  const input = root.querySelector('[data-image-input]');
  const removeButton = root.querySelector('[data-image-remove]');
  const error = root.querySelector('[data-image-error]');
  const state = { file: null, remove: false };
  const fallback = medicineImage({ ...medicine, imageUrl: null });
  let objectUrl = null;
  const show = (source, removable) => {
    preview.src = source;
    removeButton.classList.toggle('hidden', !removable);
  };
  root.querySelector('[data-image-pick]').addEventListener('click', () => input.click());
  input.addEventListener('change', () => {
    const [file] = input.files;
    error.textContent = '';
    if (!file) return;
    if (!['image/jpeg', 'image/png', 'image/webp'].includes(file.type) || file.size > 2 * 1024 * 1024) {
      error.textContent = 'Ảnh phải là JPG, PNG hoặc WebP và không quá 2MB.';
      input.value = '';
      return;
    }
    if (objectUrl) URL.revokeObjectURL(objectUrl);
    objectUrl = URL.createObjectURL(file);
    state.file = file;
    state.remove = false;
    show(objectUrl, true);
  });
  removeButton.addEventListener('click', () => {
    input.value = '';
    state.file = null;
    state.remove = Boolean(medicine?.imageUrl);
    show(fallback, false);
  });
  return state;
};

const syncImage = async (id, state) => {
  if (state.file) return medicineService.uploadImage(id, state.file);
  if (state.remove) return medicineService.removeImage(id);
  return null;
};

export const openMedicineForm = async ({ medicine = null, nextCode = '' }) => {
  const categories = await loadCategoryOptions();
  let imageState = { file: null, remove: false };
  const dialog = openFormDialog({
    title: medicine ? `Sửa thuốc: ${medicine.name}` : 'Thêm thuốc mới',
    asDrawer: true,
    submitText: medicine ? 'Lưu thay đổi' : 'Thêm thuốc',
    intro: imageField(medicine),
    fields: formFields(categories),
    values: medicine || { code: nextCode, prescriptionType: 'OTC', minStock: 20, unit: 'Hộp' },
    onSubmit: async (values) => {
      let saved = medicine ? await medicineService.update(medicine.id, values) : await medicineService.create(values);
      toast.success(medicine ? 'Đã cập nhật thuốc' : 'Đã thêm thuốc', saved.name);
      try {
        saved = (await syncImage(saved.id, imageState)) || saved;
      } catch (error) {
        toast.error('Đã lưu thuốc nhưng chưa cập nhật được ảnh', errorMessage(error));
      }
      return saved;
    },
  });
  imageState = bindImageField(dialog.body, medicine);
  return dialog.closed;
};

const batchRows = (batches, role) =>
  batches.length
    ? html`<div class="table-wrap"><table class="table">
        <thead><tr><th>Số lô</th><th>Ngày nhập</th><th>Hạn dùng</th>${role === 'SALES' ? '' : html`<th class="num">Nhập</th>`}<th class="num">Còn lại</th><th>Trạng thái</th></tr></thead>
        <tbody>${batches.map(
          (batch) => html`<tr>
          <td>${batch.code}</td><td>${formatDate(batch.receivedDate)}</td><td>${formatDate(batch.expiryDate)}</td>
          ${role === 'SALES' ? '' : html`<td class="num">${formatNumber(batch.initialQty)}</td>`}
          <td class="num">${formatNumber(batch.remainingQty)}</td><td>${statusBadge('batch', batch.status)}</td></tr>`,
        )}</tbody>
      </table></div>`
    : html`<p class="text-secondary">Chưa có lô nào còn hàng.</p>`;

const movementRows = (movements) =>
  movements.length
    ? html`<div class="table-wrap"><table class="table">
        <thead><tr><th>Thời gian</th><th>Loại</th><th>Lô</th><th class="num">Số lượng</th><th>Chứng từ</th><th>Thực hiện</th></tr></thead>
        <tbody>${movements.map(
          (movement) => html`<tr>
          <td class="nowrap">${formatDateTime(movement.createdAt)}</td><td>${statusBadge('movement', movement.type)}</td><td>${movement.batchCode}</td>
          <td class="num ${movement.quantity < 0 ? 'diff-negative' : 'diff-positive'}">${movement.quantity > 0 ? '+' : ''}${formatNumber(movement.quantity)}</td>
          <td>${movement.referenceCode}</td><td>${movement.performedByName}</td></tr>`,
        )}</tbody>
      </table></div>`
    : html`<p class="text-secondary">Chưa có biến động kho.</p>`;

const overview = (medicine, role) =>
  html`<div class="stack">
    <div class="row">
      <img class="med-thumb med-thumb--lg" src="${medicineImage(medicine)}" alt="Hình minh họa ${medicine.name}" />
      <div class="grow stack--sm stack">
        <h3>${medicine.name}</h3>
        <div class="row">${statusBadge('medicineStatus', medicine.status)} ${statusBadge('stock', medicine.stockStatus)} ${statusBadge('prescription', medicine.prescriptionType)}</div>
        <span class="text-secondary">${medicine.manufacturer}${medicine.country ? ` · ${medicine.country}` : ''}</span>
      </div>
    </div>
    ${detailList([
      ['Mã thuốc', medicine.code],
      ['Mã vạch', medicine.barcode],
      ['Hoạt chất', medicine.genericName],
      ['Hàm lượng', medicine.strength],
      ['Dạng bào chế', medicine.dosageForm],
      ['Quy cách', medicine.packaging],
      ['Danh mục', medicine.categoryName],
      ['Đơn vị tính', medicine.unit],
      ['Giá bán', formatCurrency(medicine.price)],
      ...(role === 'SALES' ? [] : [['Giá nhập gần nhất', formatCurrency(medicine.costPrice)]]),
      ['Tồn khả dụng', `${formatNumber(medicine.availableQty)} ${medicine.unit}`],
      ['Tồn quá hạn (không bán)', `${formatNumber(medicine.expiredQty)} ${medicine.unit}`],
      ['Hạn dùng gần nhất', formatDate(medicine.nearestExpiry)],
      ['Ngưỡng tồn tối thiểu', formatNumber(medicine.minStock)],
    ])}
    ${medicine.description ? html`<p class="text-secondary">${medicine.description}</p>` : ''}
  </div>`;

export const openMedicineDrawer = async (id, { role, onChanged = () => {} }) => {
  const drawer = openDrawer({ title: 'Chi tiết thuốc', content: skeletonTable(6) });
  const canEdit = role === 'ADMIN';
  try {
    let medicine = await medicineService.get(id);
    const items = [
      { key: 'overview', label: 'Tổng quan' },
      { key: 'batches', label: `Danh sách lô (${medicine.batches.length})` },
      ...(role === 'SALES' ? [] : [{ key: 'movements', label: 'Biến động kho' }]),
    ];
    const draw = (active) => {
      const panel = {
        overview: overview(medicine, role),
        batches: batchRows(medicine.batches, role),
        movements: movementRows(medicine.movements),
      }[active];
      drawer.setBody(
        html`${tabs(items, active, 'med')}<div class="tab-panel" id="med-panel" role="tabpanel">${panel}</div>`,
      );
      bindTabs(drawer.body, draw);
    };
    const drawFooter = () => {
      drawer.setFooter(
        canEdit
          ? html`<button type="button" class="btn btn--secondary" data-act="toggle">${medicine.status === 'ACTIVE' ? 'Ngừng kinh doanh' : 'Kích hoạt lại'}</button>
              <button type="button" class="btn btn--primary" data-act="edit">${icon('edit', 'icon--sm')} Sửa thuốc</button>`
          : html`<button type="button" class="btn btn--secondary" data-dismiss="cancel">Đóng</button>`,
      );
    };
    drawer.panel.querySelector('h2').textContent = medicine.name;
    draw('overview');
    drawFooter();
    drawer.footer.addEventListener('click', async (event) => {
      const button = event.target instanceof Element ? event.target.closest('[data-act]') : null;
      if (!button) return;
      if (button.dataset.act === 'edit') {
        const saved = await openMedicineForm({ medicine });
        if (saved && saved !== true) {
          medicine = { ...medicine, ...saved };
          draw('overview');
          drawFooter();
          onChanged();
        }
      }
      if (button.dataset.act === 'toggle') {
        const next = medicine.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
        const confirmed = await confirmDialog({
          title: next === 'INACTIVE' ? 'Ngừng kinh doanh thuốc?' : 'Kích hoạt lại thuốc?',
          message:
            next === 'INACTIVE'
              ? `${medicine.name} sẽ không xuất hiện ở quầy bán hàng.`
              : `${medicine.name} sẽ được bán trở lại.`,
          confirmText: next === 'INACTIVE' ? 'Ngừng kinh doanh' : 'Kích hoạt',
          tone: next === 'INACTIVE' ? 'danger' : 'primary',
        });
        if (!confirmed) return;
        try {
          medicine = { ...medicine, ...(await medicineService.setStatus(medicine.id, next)) };
          toast.success('Đã cập nhật trạng thái', medicine.name);
          draw('overview');
          drawFooter();
          onChanged();
        } catch (error) {
          toast.error('Không thể cập nhật', errorMessage(error));
        }
      }
    });
  } catch (error) {
    drawer.setBody(html`<div class="alert alert--danger" role="alert">${errorMessage(error)}</div>`);
  }
  return drawer;
};

const columnsByMode = {
  admin: [
    { key: 'name', label: 'Thuốc', sortable: true, render: (row) => medicineCell(row) },
    { key: 'categoryName', label: 'Danh mục', sortable: true },
    { key: 'price', label: 'Giá bán', sortable: true, align: 'right', render: (row) => formatCurrency(row.price) },
    {
      key: 'availableQty',
      label: 'Tồn khả dụng',
      sortable: true,
      align: 'right',
      render: (row) => `${formatNumber(row.availableQty)} ${row.unit}`,
    },
    {
      key: 'stockStatus',
      label: 'Tình trạng kho',
      sortable: true,
      render: (row) => statusBadge('stock', row.stockStatus),
    },
    { key: 'status', label: 'Trạng thái', sortable: true, render: (row) => statusBadge('medicineStatus', row.status) },
  ],
  sales: [
    { key: 'name', label: 'Thuốc', sortable: true, render: (row) => medicineCell(row) },
    { key: 'prescriptionType', label: 'Phân loại', render: (row) => statusBadge('prescription', row.prescriptionType) },
    { key: 'price', label: 'Giá bán', sortable: true, align: 'right', render: (row) => formatCurrency(row.price) },
    { key: 'unit', label: 'Đơn vị' },
    {
      key: 'availableQty',
      label: 'Tồn có thể bán',
      sortable: true,
      align: 'right',
      render: (row) => formatNumber(row.availableQty),
    },
    { key: 'nearestExpiry', label: 'Hạn gần nhất', sortable: true, render: (row) => formatDate(row.nearestExpiry) },
    { key: 'stockStatus', label: 'Tình trạng', sortable: true, render: (row) => statusBadge('stock', row.stockStatus) },
  ],
  warehouse: [
    { key: 'code', label: 'Mã thuốc', sortable: true },
    { key: 'name', label: 'Tên thuốc', sortable: true, render: (row) => medicineCell(row, row.manufacturer) },
    { key: 'categoryName', label: 'Danh mục', sortable: true },
    { key: 'totalQty', label: 'Tổng tồn', sortable: true, align: 'right', render: (row) => formatNumber(row.totalQty) },
    {
      key: 'availableQty',
      label: 'Tồn khả dụng',
      sortable: true,
      align: 'right',
      render: (row) => formatNumber(row.availableQty),
    },
    { key: 'unit', label: 'Đơn vị' },
    {
      key: 'stockStatus',
      label: 'Trạng thái tồn',
      sortable: true,
      render: (row) => statusBadge('stock', row.stockStatus),
    },
  ],
};

export const mountMedicineTable = async ({ container, role, mode, presets = {} }) => {
  const categories = await loadCategoryOptions();
  const query = getQuery();
  const filters = [
    { type: 'search', name: 'q', placeholder: 'Tìm theo tên, mã, mã vạch, hoạt chất...' },
    { type: 'select', name: 'categoryId', label: 'Danh mục', options: categories },
    ...(mode === 'admin'
      ? [{ type: 'select', name: 'status', label: 'Trạng thái', options: statusOptions('medicineStatus') }]
      : []),
    { type: 'select', name: 'stockStatus', label: 'Tình trạng kho', options: stockOptions },
    ...(mode === 'sales'
      ? [{ type: 'select', name: 'prescriptionType', label: 'Phân loại', options: prescriptionOptions }]
      : []),
  ];
  const table = createDataTable({
    container,
    columns: columnsByMode[mode],
    fetch: (params) => medicineService.list(params),
    filters,
    initialParams: { q: query.q || '', stockStatus: query.stockStatus || '', size: 10, ...presets },
    defaultSort: mode === 'warehouse' ? 'code,asc' : 'name,asc',
    caption: 'Danh sách thuốc',
    emptyTitle: 'Chưa có thuốc nào',
    emptyIcon: 'pill',
    toolbarExtra:
      mode === 'admin'
        ? html`<button type="button" class="btn btn--primary" data-action="create">${icon('plus', 'icon--sm')} Thêm thuốc</button>`
        : '',
    rowActions:
      mode === 'admin'
        ? (row) => html`${rowButton('view', `Xem ${row.name}`, 'eye')}${rowButton('edit', `Sửa ${row.name}`, 'edit')}`
        : null,
    onRowClick: (row) => openMedicineDrawer(row.id, { role, onChanged: () => table.reload() }),
    onAction: async (action, row) => {
      if (action === 'create') {
        const { code } = await medicineService.nextCode();
        const saved = await openMedicineForm({ nextCode: code });
        if (saved) table.reload();
      }
      if (action === 'view') openMedicineDrawer(row.id, { role, onChanged: () => table.reload() });
      if (action === 'edit') {
        const detail = await medicineService.get(row.id);
        const saved = await openMedicineForm({ medicine: detail });
        if (saved) table.reload();
      }
    },
  });
  await table.reload();
  return table;
};
