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

const openInviteForm = () =>
  openFormDialog({
    title: 'Tạo lời mời nhân viên',
    intro: html`<p class="text-secondary text-small">Nhân viên nhận mã mời sẽ tự tạo mật khẩu và kích hoạt tài khoản. Lời mời có hiệu lực 7 ngày.</p>`,
    fields: [
      { name: 'email', label: 'Email nhân viên', type: 'email', required: true, full: true },
      {
        name: 'role',
        label: 'Vai trò',
        type: 'select',
        required: true,
        options: roleOptions,
        placeholder: 'Chọn vai trò',
        full: true,
      },
    ],
    submitText: 'Tạo lời mời',
    onSubmit: (values) => employeeService.invite(values),
  }).closed;

const showInvitationCode = (invitation) =>
  openModal({
    title: 'Đã tạo lời mời',
    size: 'sm',
    content: html`<p>Gửi mã sau cho <strong>${invitation.email}</strong> để kích hoạt tài khoản (vai trò ${statusBadge('role', invitation.role)}):</p>
      <p class="text-strong" data-code>${invitation.code}</p>
      <p class="text-secondary text-small">Trang kích hoạt: <a href="${pageUrl(`pages/auth/register.html?code=${invitation.code}`)}">Mở trang kích hoạt</a>. Đây là môi trường demo, hệ thống không gửi email thật.</p>`,
    footer: html`<button type="button" class="btn btn--primary" data-dismiss="confirm">Đã hiểu</button>`,
  });

const openInvitationsModal = async () => {
  const modal = openModal({ title: 'Danh sách lời mời', size: 'lg', content: skeletonTable(4) });
  const draw = async () => {
    try {
      const invitations = await employeeService.invitations();
      modal.setBody(
        invitations.length
          ? html`<div class="table-wrap"><table class="table"><thead><tr><th>Mã mời</th><th>Email</th><th>Vai trò</th><th>Hết hạn</th><th>Trạng thái</th><th class="actions">Thao tác</th></tr></thead>
              <tbody>${invitations.map(
                (
                  invitation,
                ) => html`<tr><td><strong>${invitation.code}</strong></td><td>${invitation.email}</td><td>${statusBadge('role', invitation.role)}</td><td>${formatDateTime(invitation.expiresAt)}</td><td>${statusBadge('invitation', invitation.status)}</td>
                <td class="actions">${invitation.status === 'PENDING' ? html`<button type="button" class="btn btn--secondary btn--sm" data-revoke="${invitation.id}">Thu hồi</button>` : ''}</td></tr>`,
              )}</tbody></table></div>`
          : html`<p class="text-secondary">Chưa có lời mời nào.</p>`,
      );
    } catch (error) {
      modal.setBody(html`<div class="alert alert--danger" role="alert">${errorMessage(error)}</div>`);
    }
  };
  modal.body.addEventListener('click', async (event) => {
    const button = event.target instanceof Element ? event.target.closest('[data-revoke]') : null;
    if (!button) return;
    try {
      await employeeService.revokeInvitation(button.dataset.revoke);
      toast.success('Đã thu hồi lời mời');
      draw();
    } catch (error) {
      toast.error('Không thể thu hồi', errorMessage(error));
    }
  });
  draw();
};

const openEmployeeDrawer = async (id, onChanged) => {
  const drawer = openDrawer({ title: 'Hồ sơ nhân viên', content: skeletonTable(5) });
  try {
    let employee = await employeeService.get(id);
    const items = [
      { key: 'profile', label: 'Thông tin' },
      { key: 'activity', label: 'Hoạt động gần đây' },
    ];
    const draw = (active) => {
      drawer.panel.querySelector('h2').textContent = employee.name;
      const panel =
        active === 'profile'
          ? html`<div class="stack"><div class="row">${statusBadge('role', employee.role)} ${statusBadge('employee', employee.status)}</div>
              ${detailList([
                ['Email', employee.email],
                ['Tên đăng nhập', employee.username],
                ['Số điện thoại', employee.phone],
                ['Đăng nhập gần nhất', employee.lastLoginAt ? formatRelative(employee.lastLoginAt) : 'Chưa đăng nhập'],
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
    draw('profile');
    drawer.setFooter(html`<button type="button" class="btn btn--secondary" data-act="role">Đổi vai trò</button>
      <button type="button" class="btn ${employee.status === 'ACTIVE' ? 'btn--danger' : 'btn--primary'}" data-act="status">${employee.status === 'ACTIVE' ? 'Vô hiệu hóa' : 'Kích hoạt'}</button>`);
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
        if (updated && updated !== true) {
          employee = { ...employee, ...updated };
          draw('profile');
          onChanged();
        }
      }
      if (button.dataset.act === 'status') {
        const next = employee.status === 'ACTIVE' ? 'INACTIVE' : 'ACTIVE';
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
          employee = { ...employee, ...(await employeeService.setStatus(employee.id, next)) };
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
      subtitle: 'Quản lý tài khoản, vai trò và lời mời kích hoạt cho nhân viên.',
      actions: html`<button type="button" class="btn btn--secondary" data-page-action="invitations">${icon('mail', 'icon--sm')} Danh sách lời mời</button>
        <button type="button" class="btn btn--primary" data-page-action="invite">${icon('plus', 'icon--sm')} Tạo lời mời</button>`,
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
              render: (row) => statusBadge('employee', row.status),
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
          if (!button) return;
          if (button.dataset.pageAction === 'invitations') openInvitationsModal();
          if (button.dataset.pageAction === 'invite') {
            const invitation = await openInviteForm();
            if (invitation && invitation !== true) showInvitationCode(invitation);
          }
        });
        table.reload();
        return table;
      },
    }),
});
