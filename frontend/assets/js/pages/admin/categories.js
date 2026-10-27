import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { formatNumber } from '../../core/format.js';
import { errorMessage } from '../../core/errors.js';
import { startPage } from '../../components/page.js';
import { mountTablePage, rowButton } from '../../components/blocks.js';
import { createDataTable } from '../../components/data-table.js';
import { openFormDialog } from '../../components/form-modal.js';
import { confirmDialog } from '../../components/overlay.js';
import { toast } from '../../components/toast.js';
import { categoryService } from '../../services/catalog-service.js';

const fields = [
  {
    name: 'code',
    label: 'Mã danh mục',
    required: true,
    maxlength: 10,
    pattern: /^[A-Za-z0-9]{2,10}$/,
    patternMessage: 'Mã gồm 2-10 ký tự chữ hoặc số, không dấu.',
  },
  { name: 'name', label: 'Tên danh mục', required: true },
  { name: 'description', label: 'Mô tả', type: 'textarea', full: true },
];

const openForm = (category = null) =>
  openFormDialog({
    title: category ? 'Sửa danh mục' : 'Thêm danh mục',
    fields,
    values: category || {},
    submitText: category ? 'Lưu thay đổi' : 'Thêm danh mục',
    onSubmit: async (values) => {
      const saved = category ? await categoryService.update(category.id, values) : await categoryService.create(values);
      toast.success(category ? 'Đã cập nhật danh mục' : 'Đã thêm danh mục', saved.name);
      return saved;
    },
  }).closed;

const normalize = (value) =>
  String(value ?? '')
    .normalize('NFD')
    .replace(/[̀-ͯ]/g, '')
    .toLowerCase();

const fetchPage = async ({ q, sort, page, size }) => {
  const needle = normalize(q);
  const all = await categoryService.list();
  const list = all.filter(
    (category) => !needle || normalize(`${category.name} ${category.code} ${category.description}`).includes(needle),
  );
  const [field, direction] = String(sort || 'code,asc').split(',');
  list.sort(
    (a, b) =>
      (typeof a[field] === 'number' ? a[field] - b[field] : String(a[field]).localeCompare(String(b[field]), 'vi')) *
      (direction === 'desc' ? -1 : 1),
  );
  const totalPages = Math.max(1, Math.ceil(list.length / size));
  const current = Math.min(page, totalPages);
  return {
    items: list.slice((current - 1) * size, current * size),
    page: current,
    size,
    total: list.length,
    totalPages,
  };
};

startPage({
  role: 'ADMIN',
  nav: 'categories',
  title: 'Danh mục thuốc',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Danh mục thuốc',
      subtitle: 'Nhóm thuốc dùng để phân loại, lọc và báo cáo.',
      mount: (container) => {
        const table = createDataTable({
          container,
          columns: [
            { key: 'code', label: 'Mã', sortable: true },
            { key: 'name', label: 'Tên danh mục', sortable: true, render: (row) => html`<strong>${row.name}</strong>` },
            { key: 'description', label: 'Mô tả' },
            {
              key: 'medicineCount',
              label: 'Số thuốc',
              sortable: true,
              align: 'right',
              render: (row) => formatNumber(row.medicineCount),
            },
          ],
          fetch: fetchPage,
          filters: [{ type: 'search', name: 'q', placeholder: 'Tìm danh mục...' }],
          defaultSort: 'code,asc',
          pageSize: 20,
          pageSizes: [20, 50],
          caption: 'Danh mục thuốc',
          emptyTitle: 'Chưa có danh mục',
          emptyIcon: 'layers',
          toolbarExtra: html`<button type="button" class="btn btn--primary" data-action="create">${icon('plus', 'icon--sm')} Thêm danh mục</button>`,
          rowActions: (row) =>
            html`${rowButton('edit', `Sửa ${row.name}`, 'edit')}${rowButton('delete', `Xóa ${row.name}`, 'trash')}`,
          onAction: async (action, row) => {
            if (action === 'create' && (await openForm())) table.reload();
            if (action === 'edit' && (await openForm(row))) table.reload();
            if (action === 'delete') {
              const confirmed = await confirmDialog({
                title: 'Xóa danh mục?',
                message: `Danh mục "${row.name}" sẽ bị xóa. Chỉ xóa được danh mục chưa có thuốc.`,
                confirmText: 'Xóa',
                tone: 'danger',
              });
              if (!confirmed) return;
              try {
                await categoryService.remove(row.id);
                toast.success('Đã xóa danh mục', row.name);
                table.reload();
              } catch (error) {
                toast.error('Không thể xóa danh mục', errorMessage(error));
              }
            }
          },
        });
        table.reload();
        return table;
      },
    }),
});
