import { html } from '../../core/html.js';
import { icon } from '../../core/icons.js';
import { formatCurrency, formatDateTime, formatNumber, formatRelative } from '../../core/format.js';
import { errorMessage } from '../../core/errors.js';
import { startPage } from '../../components/page.js';
import { bindTabs, detailList, mountTablePage, rowButton, tabs } from '../../components/blocks.js';
import { createDataTable } from '../../components/data-table.js';
import { openFormDialog } from '../../components/form-modal.js';
import { confirmDialog, openDrawer, openModal } from '../../components/overlay.js';
import { statusBadge, statusOptions } from '../../components/badge.js';
import { skeletonTable } from '../../components/states.js';
import { toast } from '../../components/toast.js';
import { employeeService } from '../../services/admin-service.js';
import { pageUrl } from '../../core/dom.js';

const roleOptions = statusOptions('role');

const openCreateForm = () =>
  openFormDialog({
    title: 'Thêm nhân viên',
    intro: html`<p class="text-secondary text-small">Tài khoản được tạo ở trạng thái chờ kích hoạt. Sau khi tạo, bạn nhận một liên kết kích hoạt hiển thị một lần để gửi cho nhân viên.</p>`,
    fields: [
      { name: 'name', label: 'Họ và tên', required: true, full: true, autocomplete: 'off' },
      { name: 'email', label: 'Email', type: 'email', required: true, autocomplete: 'off' },
      {
        name: 'username',
        label: 'Tên đăng nhập',
        required: true,
        autocomplete: 'off',
        pattern: /^[a-zA-Z0-9._-]{3,30}$/,
        patternMessage: 'Tên đăng nhập gồm 3-30 ký tự chữ, số, dấu chấm, gạch dưới hoặc gạch ngang.',
      },
      { name: 'phone', label: 'Số điện thoại', autocomplete: 'off' },
      {
        name: 'role',
        label: 'Vai trò',
        type: 'select',
        required: true,
        options: roleOptions,
        placeholder: 'Chọn vai trò',
      },
    ],
    submitText: 'Tạo tài khoản',
    onSubmit: (values) => employeeService.create(values),
  }).closed;

const showAccountLink = ({ title, intro, link, expiresAt }) => {
  const absoluteLink = pageUrl(link.replace(/^\//, ''));
  const modal = openModal({
    title,
    size: 'md',
    content: html`<div class="stack">
      <p>${intro}</p>
      <div class="form-field">
        <label class="form-label" for="account-link">Liên kết</label>
        <input class="form-control" id="account-link" type="text" readonly value="${absoluteLink}" data-link />
        <span class="form-help">Liên kết có hiệu lực đến ${formatDateTime(expiresAt)} và chỉ dùng được một lần.</span>
      </div>
      <div class="alert alert--warning"><span>Liên kết chỉ hiển thị một lần. Hãy sao chép và gửi ngay cho nhân viên; đóng hộp thoại này bạn sẽ không xem lại được.</span></div>
    </div>`,
    footer: html`<button type="button" class="btn btn--secondary" data-copy>${icon('clipboard', 'icon--sm')} Sao chép liên kết</button>
      <button type="button" class="btn btn--primary" data-dismiss="confirm">Đã gửi</button>`,
  });
  modal.footer.querySelector('[data-copy]').addEventListener('click', async () => {
    const input = modal.body.querySelector('[data-link]');
    try {
      await navigator.clipboard.writeText(input.value);
      toast.success('Đã sao chép liên kết');
    } catch {
      input.select();
      toast.info('Hãy nhấn Ctrl+C để sao chép liên kết');
    }
  });
  return modal;
};

const showActivationLink = (result) =>
  showAccountLink({
    title: 'Đã tạo tài khoản',
    intro: html`Gửi liên kết sau cho <strong>${result.employee.name}</strong> (${statusBadge('role', result.employee.role)}) để đặt mật khẩu và kích hoạt tài khoản.`,
    link: result.activationLink,
    expiresAt: result.expiresAt,
  });

const lockedText = (employee) =>
  employee.locked ? `Đang bị khóa đến ${formatDateTime(employee.lockedUntil)}` : 'Bình thường';

const openEmployeeDrawer = async (id, onChanged) => {
  const drawer = openDrawer({ title: 'Hồ sơ nhân viên', content: skeletonTable(5) });
  try {
    let employee = await employeeService.get(id);
    let activeTab = 'profile';
    const items = [
      { key: 'profile', label: 'Thông tin' },
      { key: 'activity', label: 'Hoạt động gần đây' },
    ];
    const draw = (active) => {
      activeTab = active;
      drawer.panel.querySelector('h2').textContent = employee.name;
      const panel =
        active === 'profile'
          ? html`<div class="stack"><div class="row">${statusBadge('role', employee.role)} ${statusBadge('employee', employee.status)}</div>
              ${detailList([
                ['Email', employee.email],
                ['Tên đăng nhập', employee.username],
                ['Số điện thoại', employee.phone],
                ['Đăng nhập gần nhất', employee.lastLoginAt ? formatRelative(employee.lastLoginAt) : 'Chưa đăng nhập'],
                ['Tình trạng đăng nhập', lockedText(employee)],
                ['Hóa đơn đã bán', formatNumber(employee.invoiceCount)],
                ['Doanh thu', formatCurrency(employee.revenue)],
                ['Phiếu nhập đã xác nhận', formatNumber(employee.receiptCount)],
                ['Ngày tạo tài khoản', formatDateTime(employee.createdAt)],
              ])}</div>`
          : employee.activities.length
            ? html`<div class="list">${employee.activities.map((log) => html`<div class="list__item"><span class="timeline__dot"></span><div class="list__main"><strong>${log.description}</strong><small>${formatDateTime(log.createdAt)}</small></div></div>`)}</div>`
            : html`<p class="text-secondary">Chưa có hoạt động.</p>`;
      drawer.setBody(
        html`${tabs(items, active, 'emp')}<div class="tab-panel" id="emp-panel" role="tabpanel">${panel}</div>`,
      );
      bindTabs(drawer.body, draw);
    };
    const pending = () => employee.status === 'PENDING';
    const nextStatus = () => (employee.status === 'ACTIVE' || pending() ? 'INACTIVE' : 'ACTIVE');
    const drawFooter = () => {
      drawer.setFooter(html`<button type="button" class="btn btn--secondary" data-act="role">Đổi vai trò</button>
        <button type="button" class="btn btn--secondary" data-act="link">${pending() ? 'Tạo lại link kích hoạt' : 'Tạo link đặt lại'}</button>
        ${employee.locked ? html`<button type="button" class="btn btn--secondary" data-act="unlock">Mở khóa</button>` : ''}
        <button type="button" class="btn ${nextStatus() === 'INACTIVE' ? 'btn--danger' : 'btn--primary'}" data-act="status">${nextStatus() === 'INACTIVE' ? 'Vô hiệu hóa' : 'Kích hoạt'}</button>`);
    };
    draw('profile');
    drawFooter();
    const refresh = async () => {
      employee = await employeeService.get(employee.id);
      draw(activeTab);
      drawFooter();
      onChanged();
    };
    drawer.footer.addEventListener('click', async (event) => {
      const button = event.target instanceof Element ? event.target.closest('[data-act]') : null;
      if (!button) return;
      if (button.dataset.act === 'role') {
        const updated = await openFormDialog({
          title: 'Gán vai trò',
          subtitle: employee.name,
          fields: [
            { name: 'role', label: 'Vai trò', type: 'select', required: true, options: roleOptions, full: true },
          ],
          values: { role: employee.role },
          submitText: 'Lưu vai trò',
          onSubmit: async ({ role }) => {
            const result = await employeeService.assignRole(employee.id, role);
            toast.success('Đã cập nhật vai trò', employee.name);
            return result;
          },
        }).closed;
        if (updated && updated !== true) await refresh();
      }
      if (button.dataset.act === 'link') {
        try {
          const result = await employeeService.resetLink(employee.id);
          showAccountLink({
            title: pending() ? 'Liên kết kích hoạt mới' : 'Liên kết đặt lại mật khẩu',
            intro: html`Gửi liên kết sau cho <strong>${employee.name}</strong> ${pending() ? 'để đặt mật khẩu và kích hoạt tài khoản.' : 'để đặt mật khẩu mới. Mật khẩu hiện tại vẫn dùng được cho đến khi nhân viên đặt lại.'}`,
            link: result.resetLink,
            expiresAt: result.expiresAt,
          });
        } catch (error) {
          toast.error('Không thể tạo liên kết', errorMessage(error));
        }
      }
      if (button.dataset.act === 'unlock') {
        try {
          await employeeService.unlock(employee.id);
          toast.success('Đã mở khóa tài khoản', employee.name);
          await refresh();
        } catch (error) {
          toast.error('Không thể mở khóa', errorMessage(error));
        }
      }
      if (button.dataset.act === 'status') {
        const next = nextStatus();
        const confirmed = await confirmDialog({
          title: next === 'INACTIVE' ? 'Vô hiệu hóa tài khoản?' : 'Kích hoạt tài khoản?',
          message:
            next === 'INACTIVE'
              ? `${employee.name} sẽ không thể đăng nhập cho đến khi được kích hoạt lại.`
              : `${employee.name} sẽ có thể đăng nhập trở lại.`,
          confirmText: next === 'INACTIVE' ? 'Vô hiệu hóa' : 'Kích hoạt',
          tone: next === 'INACTIVE' ? 'danger' : 'primary',
        });
        if (!confirmed) return;
        try {
          await employeeService.setStatus(employee.id, next);
          toast.success('Đã cập nhật trạng thái', employee.name);
          drawer.close();
          onChanged();
        } catch (error) {
          toast.error('Không thể cập nhật', errorMessage(error));
        }
      }
    });
  } catch (error) {
    drawer.setBody(html`<div class="alert alert--danger" role="alert">${errorMessage(error)}</div>`);
  }
};

startPage({
  role: 'ADMIN',
  nav: 'employees',
  title: 'Nhân viên',
  render: ({ content }) =>
    mountTablePage({
      content,
      title: 'Nhân viên',
      subtitle: 'Quản lý tài khoản, vai trò và liên kết kích hoạt hoặc đặt lại mật khẩu cho nhân viên.',
      actions: html`<button type="button" class="btn btn--primary" data-page-action="create">${icon('plus', 'icon--sm')} Thêm nhân viên</button>`,
      mount: (container) => {
        const table = createDataTable({
          container,
          columns: [
            {
              key: 'name',
              label: 'Nhân viên',
              sortable: true,
              render: (row) =>
                html`<div class="cell-main"><span class="avatar">${row.name
                  .split(' ')
                  .slice(-2)
                  .map((part) => part[0])
                  .join('')}</span><div><strong>${row.name}</strong><small>${row.email}</small></div></div>`,
            },
            { key: 'username', label: 'Tên đăng nhập' },
            { key: 'role', label: 'Vai trò', sortable: true, render: (row) => statusBadge('role', row.role) },
            { key: 'invoiceCount', label: 'Hóa đơn', sortable: true, align: 'right' },
            {
              key: 'lastLoginAt',
              label: 'Đăng nhập gần nhất',
              sortable: true,
              render: (row) => (row.lastLoginAt ? formatRelative(row.lastLoginAt) : 'Chưa đăng nhập'),
            },
            {
              key: 'status',
              label: 'Trạng thái',
              sortable: true,
              render: (row) =>
                html`${statusBadge('employee', row.status)}${row.locked ? html` <span class="badge badge--warning">Bị khóa</span>` : ''}`,
            },
          ],
          fetch: (params) => employeeService.list(params),
          filters: [
            { type: 'search', name: 'q', placeholder: 'Tìm tên, email, tên đăng nhập...' },
            { type: 'select', name: 'role', label: 'Vai trò', options: roleOptions },
            { type: 'select', name: 'status', label: 'Trạng thái', options: statusOptions('employee') },
          ],
          defaultSort: 'name,asc',
          caption: 'Danh sách nhân viên',
          emptyTitle: 'Chưa có nhân viên',
          emptyIcon: 'idCard',
          rowActions: (row) => rowButton('view', `Xem hồ sơ ${row.name}`, 'eye'),
          onRowClick: (row) => openEmployeeDrawer(row.id, () => table.reload()),
          onAction: (action, row) => {
            if (action === 'view') openEmployeeDrawer(row.id, () => table.reload());
          },
        });
        content.addEventListener('click', async (event) => {
          const button = event.target instanceof Element ? event.target.closest('[data-page-action]') : null;
          if (!button || button.dataset.pageAction !== 'create') return;
          const result = await openCreateForm();
          if (result && result !== true) {
            table.reload();
            showActivationLink(result);
          }
        });
        table.reload();
        return table;
      },
    }),
});
