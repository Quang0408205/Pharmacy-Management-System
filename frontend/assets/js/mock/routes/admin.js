import { database } from '../database.js';
import {
  buildStockIndex,
  decorateBatch,
  decorateMedicine,
  publicUser,
  requireFields,
  stockStatusOf,
} from '../domain.js';
import { inDateRange, matchesQuery, respondList } from '../query.js';
import { badRequest, conflict, notFound } from '../../core/errors.js';
import { addDays, addDaysKey, toDateKey, todayKey } from '../../core/format.js';
import { route } from '../router.js';

const admin = ['ADMIN'];
const everyone = ['ADMIN', 'SALES', 'WAREHOUSE'];
const adminWarehouse = ['ADMIN', 'WAREHOUSE'];
const roleList = ['ADMIN', 'SALES', 'WAREHOUSE'];

const invitationCode = (role) => {
  const prefix = { ADMIN: 'QT', SALES: 'BH', WAREHOUSE: 'KHO' }[role];
  return `NV-${prefix}-${Math.random().toString(36).slice(2, 8).toUpperCase()}`;
};

route('GET', '/employees', admin, ({ query, data }) => {
  const counts = new Map();
  data.invoices
    .filter((invoice) => invoice.status === 'COMPLETED')
    .forEach((invoice) => counts.set(invoice.employeeId, (counts.get(invoice.employeeId) || 0) + 1));
  let list = data.employees.map((employee) => ({
    ...publicUser(employee),
    createdAt: employee.createdAt,
    lastLoginAt: employee.lastLoginAt,
    invoiceCount: counts.get(employee.id) || 0,
  }));
  if (query.q)
    list = list.filter((employee) =>
      matchesQuery([employee.name, employee.email, employee.username, employee.phone], query.q),
    );
  if (query.role) list = list.filter((employee) => employee.role === query.role);
  if (query.status) list = list.filter((employee) => employee.status === query.status);
  return respondList(list, query, {
    sortable: ['name', 'email', 'role', 'status', 'lastLoginAt', 'invoiceCount'],
    defaultSort: 'name,asc',
  });
});

route('GET', '/employees/:id', admin, ({ params, data }) => {
  const employee = data.employees.find((item) => item.id === params.id);
  if (!employee) throw notFound('Không tìm thấy nhân viên.');
  const invoices = data.invoices.filter(
    (invoice) => invoice.employeeId === employee.id && invoice.status === 'COMPLETED',
  );
  const receipts = data.receipts.filter(
    (receipt) => receipt.createdById === employee.id && receipt.status === 'CONFIRMED',
  );
  const activities = data.activityLogs
    .filter((log) => log.actorId === employee.id)
    .slice(-8)
    .reverse();
  return {
    ...publicUser(employee),
    createdAt: employee.createdAt,
    lastLoginAt: employee.lastLoginAt,
    invoiceCount: invoices.length,
    revenue: invoices.reduce((sum, invoice) => sum + invoice.total, 0),
    receiptCount: receipts.length,
    activities,
  };
});

const activeAdmins = (data) =>
  data.employees.filter((employee) => employee.role === 'ADMIN' && employee.status === 'ACTIVE');

route('PATCH', '/employees/:id', admin, ({ params, body, data, user }) => {
  const employee = data.employees.find((item) => item.id === params.id);
  if (!employee) throw notFound('Không tìm thấy nhân viên.');
  if (!roleList.includes(body.role)) throw badRequest('Vai trò không hợp lệ.');
  if (employee.role === 'ADMIN' && body.role !== 'ADMIN' && activeAdmins(data).length <= 1) {
    throw conflict('Hệ thống phải có ít nhất một quản trị viên đang hoạt động.');
  }
  if (employee.id === user.id && body.role !== 'ADMIN') throw conflict('Bạn không thể tự hạ quyền của chính mình.');
  employee.role = body.role;
  database.save('employees');
  database.log(user, 'ASSIGN_ROLE', 'EMPLOYEE', employee.username, `Gán vai trò ${body.role} cho ${employee.name}`);
  return publicUser(employee);
});

route('PATCH', '/employees/:id/status', admin, ({ params, body, data, user }) => {
  const employee = data.employees.find((item) => item.id === params.id);
  if (!employee) throw notFound('Không tìm thấy nhân viên.');
  if (!['ACTIVE', 'INACTIVE'].includes(body.status)) throw badRequest('Trạng thái không hợp lệ.');
  if (employee.id === user.id) throw conflict('Bạn không thể vô hiệu hóa chính tài khoản của mình.');
  if (employee.role === 'ADMIN' && body.status === 'INACTIVE' && activeAdmins(data).length <= 1) {
    throw conflict('Hệ thống phải có ít nhất một quản trị viên đang hoạt động.');
  }
  employee.status = body.status;
  database.save('employees');
  database.log(
    user,
    'UPDATE_EMPLOYEE_STATUS',
    'EMPLOYEE',
    employee.username,
    `${body.status === 'ACTIVE' ? 'Kích hoạt' : 'Vô hiệu hóa'} tài khoản ${employee.name}`,
  );
  return publicUser(employee);
});

route('GET', '/invitations', admin, ({ data }) =>
  [...data.invitations]
    .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
    .map((invitation) => ({
      ...invitation,
      status:
        invitation.status === 'PENDING' && new Date(invitation.expiresAt) < new Date() ? 'EXPIRED' : invitation.status,
    })),
);

route('POST', '/invitations', admin, ({ body, data, user }) => {
  requireFields(body, { email: 'Email', role: 'Vai trò' });
  if (!/^\S+@\S+\.\S+$/.test(body.email.trim()))
    throw badRequest('Email không hợp lệ.', { email: 'Email không hợp lệ.' });
  if (!roleList.includes(body.role)) throw badRequest('Vai trò không hợp lệ.', { role: 'Vai trò không hợp lệ.' });
  const email = body.email.trim().toLowerCase();
  if (data.employees.some((employee) => employee.email.toLowerCase() === email))
    throw conflict('Email này đã thuộc về một nhân viên.', { email: 'Email đã có tài khoản.' });
  if (
    data.invitations.some(
      (invitation) =>
        invitation.email.toLowerCase() === email &&
        invitation.status === 'PENDING' &&
        new Date(invitation.expiresAt) > new Date(),
    )
  ) {
    throw conflict('Email này đã có lời mời đang chờ.', { email: 'Đã có lời mời đang chờ cho email này.' });
  }
  const invitation = {
    id: database.nextId('inv', 'invitations', 2),
    code: invitationCode(body.role),
    email,
    role: body.role,
    status: 'PENDING',
    expiresAt: addDays(new Date(), 7).toISOString(),
    createdByName: user.name,
    createdAt: new Date().toISOString(),
  };
  data.invitations.push(invitation);
  database.save('invitations');
  database.log(user, 'CREATE_INVITATION', 'INVITATION', invitation.code, `Tạo lời mời ${body.role} cho ${email}`);
  return invitation;
});

route('POST', '/invitations/:id/revoke', admin, ({ params, data, user }) => {
  const invitation = data.invitations.find((item) => item.id === params.id);
  if (!invitation) throw notFound('Không tìm thấy lời mời.');
  if (invitation.status !== 'PENDING') throw conflict('Chỉ có thể thu hồi lời mời đang chờ.');
  invitation.status = 'REVOKED';
  database.save('invitations');
  database.log(user, 'REVOKE_INVITATION', 'INVITATION', invitation.code, `Thu hồi lời mời ${invitation.code}`);
  return invitation;
});

route('GET', '/settings', everyone, ({ data }) => data.settings);

route('PUT', '/settings', admin, ({ body, data, user }) => {
  const { pharmacy = {}, lowStockMultiplier, expiryWarningDays, expiryTiers, appearance, notifications } = body;
  const fieldErrors = {};
  if (pharmacy.name !== undefined && !String(pharmacy.name).trim())
    fieldErrors['pharmacy.name'] = 'Tên nhà thuốc là bắt buộc.';
  const multiplier = Number(lowStockMultiplier);
  if (lowStockMultiplier !== undefined && (!Number.isFinite(multiplier) || multiplier < 0.5 || multiplier > 5)) {
    fieldErrors.lowStockMultiplier = 'Hệ số cảnh báo phải từ 0.5 đến 5.';
  }
  const days = Number(expiryWarningDays);
  if (expiryWarningDays !== undefined && (!Number.isInteger(days) || days < 7 || days > 365)) {
    fieldErrors.expiryWarningDays = 'Số ngày cảnh báo phải từ 7 đến 365.';
  }
  const tiers = Array.isArray(expiryTiers) ? expiryTiers.map(Number) : null;
  if (
    expiryTiers !== undefined &&
    (!tiers ||
      tiers.length !== 3 ||
      tiers.some((value) => !Number.isInteger(value) || value < 1 || value > 365) ||
      tiers[0] >= tiers[1] ||
      tiers[1] >= tiers[2])
  ) {
    fieldErrors.expiryTiers = 'Ba ngưỡng phải là số nguyên tăng dần trong khoảng 1-365 ngày.';
  }
  if (Object.keys(fieldErrors).length) throw badRequest('Cài đặt chưa hợp lệ.', fieldErrors);
  if (Object.keys(pharmacy).length) data.settings.pharmacy = { ...data.settings.pharmacy, ...pharmacy };
  if (lowStockMultiplier !== undefined) data.settings.lowStockMultiplier = multiplier;
  if (expiryWarningDays !== undefined) data.settings.expiryWarningDays = days;
  if (tiers) data.settings.expiryTiers = tiers;
  if (appearance) data.settings.appearance = { ...data.settings.appearance, ...appearance };
  if (notifications) data.settings.notifications = { ...data.settings.notifications, ...notifications };
  database.save('settings');
  database.log(user, 'UPDATE_SETTINGS', 'SETTINGS', 'SETTINGS', 'Cập nhật cài đặt hệ thống');
  return data.settings;
});

route('POST', '/system/reset', admin, async () => {
  await database.reset();
  return { success: true };
});

route('GET', '/activity-logs', admin, ({ query, data }) => {
  let list = data.activityLogs;
  if (query.role) list = list.filter((log) => log.role === query.role);
  if (query.from || query.to) list = list.filter((log) => inDateRange(log.createdAt, query.from, query.to));
  if (query.q)
    list = list.filter((log) => matchesQuery([log.actorName, log.description, log.entityCode, log.action], query.q));
  return respondList(list, query, {
    sortable: ['createdAt', 'actorName', 'role', 'action'],
    defaultSort: 'createdAt,desc',
  });
});

route('GET', '/notifications', everyone, ({ data, user }) => {
  const index = buildStockIndex();
  const today = todayKey();
  const warning = addDaysKey(today, data.settings.expiryWarningDays);
  const prefix = { ADMIN: 'admin', SALES: 'sales', WAREHOUSE: 'warehouse' }[user.role];
  const lowStock = data.medicines.filter(
    (medicine) =>
      medicine.status === 'ACTIVE' && stockStatusOf(medicine, index.get(medicine.id), data.settings) !== 'IN_STOCK',
  ).length;
  const expiring = data.batches.filter(
    (batch) => batch.remainingQty > 0 && batch.expiryDate >= today && batch.expiryDate <= warning,
  ).length;
  const expired = data.batches.filter((batch) => batch.remainingQty > 0 && batch.expiryDate < today).length;
  const items = [];
  const lowStockLink = { ADMIN: 'inventory.html', SALES: 'lookup.html', WAREHOUSE: 'inventory.html' }[user.role];
  const expiryLink = { ADMIN: 'inventory.html', SALES: 'lookup.html', WAREHOUSE: 'expiry.html' }[user.role];
  if (lowStock)
    items.push({
      id: 'low-stock',
      tone: 'warning',
      title: `${lowStock} thuốc sắp hết hoặc đã hết`,
      message: 'Cần kiểm tra và lên kế hoạch nhập hàng.',
      link: `pages/${prefix}/${lowStockLink}`,
    });
  if (expiring)
    items.push({
      id: 'expiring',
      tone: 'warning',
      title: `${expiring} lô sắp hết hạn`,
      message: `Trong vòng ${data.settings.expiryWarningDays} ngày tới.`,
      link: `pages/${prefix}/${expiryLink}`,
    });
  if (expired)
    items.push({
      id: 'expired',
      tone: 'danger',
      title: `${expired} lô đã hết hạn còn tồn`,
      message: 'Không được bán, cần xử lý hủy theo quy trình.',
      link: `pages/${prefix}/${expiryLink}`,
    });
  if (user.role !== 'SALES') {
    const pendingReceipts = data.receipts.filter((receipt) => receipt.status === 'DRAFT').length;
    if (pendingReceipts)
      items.push({
        id: 'receipts',
        tone: 'info',
        title: `${pendingReceipts} phiếu nhập đang chờ`,
        message: 'Phiếu nháp chưa được xác nhận nhập kho.',
        link: `pages/${prefix}/receipts.html`,
      });
  }
  if (user.role === 'ADMIN') {
    const pendingStocktakes = data.stocktakes.filter((item) => item.status === 'PENDING_APPROVAL').length;
    if (pendingStocktakes)
      items.push({
        id: 'stocktakes',
        tone: 'info',
        title: `${pendingStocktakes} phiếu kiểm kê chờ duyệt`,
        message: 'Cần quản trị viên duyệt điều chỉnh kho.',
        link: 'pages/admin/inventory.html?tab=stocktakes',
      });
  }
  return items;
});

const completedInvoices = (data) => data.invoices.filter((invoice) => invoice.status === 'COMPLETED');

const revenueSeries = (invoices, fromKey, toKey) => {
  const buckets = new Map();
  for (let key = fromKey; key <= toKey; key = addDaysKey(key, 1))
    buckets.set(key, { date: key, revenue: 0, invoices: 0 });
  invoices.forEach((invoice) => {
    const bucket = buckets.get(toDateKey(invoice.createdAt));
    if (!bucket) return;
    bucket.revenue += invoice.total;
    bucket.invoices += 1;
  });
  return [...buckets.values()];
};

const defaultRange = (query, days = 29) => ({
  from: query.from || addDaysKey(todayKey(), -days),
  to: query.to || todayKey(),
});

const categoryDistribution = (data) => {
  const index = buildStockIndex();
  const totals = new Map(
    data.categories.map((category) => [
      category.id,
      { categoryId: category.id, name: category.name, color: category.color, medicines: 0, quantity: 0, value: 0 },
    ]),
  );
  data.medicines.forEach((medicine) => {
    const entry = totals.get(medicine.categoryId);
    entry.medicines += 1;
    entry.quantity += index.get(medicine.id).availableQty;
    entry.value += index.get(medicine.id).availableQty * medicine.costPrice;
  });
  return [...totals.values()];
};

const lowStockMedicines = (data, limit) => {
  const index = buildStockIndex();
  return data.medicines
    .filter((medicine) => medicine.status === 'ACTIVE')
    .map((medicine) => decorateMedicine(medicine, index))
    .filter((medicine) => medicine.stockStatus !== 'IN_STOCK')
    .sort((a, b) => a.availableQty / (a.minStock || 1) - b.availableQty / (b.minStock || 1))
    .slice(0, limit);
};

const expiringBatches = (data, limit) => {
  const today = todayKey();
  return data.batches
    .filter((batch) => batch.remainingQty > 0 && batch.expiryDate <= addDaysKey(today, data.settings.expiryWarningDays))
    .sort((a, b) => a.expiryDate.localeCompare(b.expiryDate))
    .slice(0, limit)
    .map(decorateBatch);
};

route('GET', '/dashboard/admin', admin, ({ data }) => {
  const today = todayKey();
  const monthPrefix = today.slice(0, 7);
  const completed = completedInvoices(data);
  const todays = completed.filter((invoice) => toDateKey(invoice.createdAt) === today);
  const month = completed.filter((invoice) => toDateKey(invoice.createdAt).startsWith(monthPrefix));
  const index = buildStockIndex();
  const warning = addDaysKey(today, data.settings.expiryWarningDays);
  const lowCount = data.medicines.filter(
    (medicine) =>
      medicine.status === 'ACTIVE' && stockStatusOf(medicine, index.get(medicine.id), data.settings) !== 'IN_STOCK',
  ).length;
  return {
    kpis: {
      todayRevenue: todays.reduce((sum, invoice) => sum + invoice.total, 0),
      monthRevenue: month.reduce((sum, invoice) => sum + invoice.total, 0),
      todayInvoices: todays.length,
      monthInvoices: month.length,
      activeMedicines: data.medicines.filter((medicine) => medicine.status === 'ACTIVE').length,
      lowStockCount: lowCount,
      expiringBatches: data.batches.filter(
        (batch) => batch.remainingQty > 0 && batch.expiryDate >= today && batch.expiryDate <= warning,
      ).length,
    },
    revenueSeries: revenueSeries(completed, addDaysKey(today, -13), today),
    categoryDistribution: categoryDistribution(data).filter((entry) => entry.medicines > 0),
    recentInvoices: [...data.invoices]
      .sort((a, b) => b.createdAt.localeCompare(a.createdAt))
      .slice(0, 6)
      .map(({ items, ...invoice }) => ({ ...invoice, itemCount: items.length })),
    lowStock: lowStockMedicines(data, 6),
    expiring: expiringBatches(data, 6),
    activities: [...data.activityLogs].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 6),
  };
});

route('GET', '/dashboard/warehouse', adminWarehouse, ({ data }) => {
  const today = todayKey();
  const index = buildStockIndex();
  const warning = addDaysKey(today, data.settings.expiryWarningDays);
  const withStock = data.batches.filter((batch) => batch.remainingQty > 0);
  const lowCount = data.medicines.filter(
    (medicine) =>
      medicine.status === 'ACTIVE' && stockStatusOf(medicine, index.get(medicine.id), data.settings) !== 'IN_STOCK',
  ).length;
  const expiringCount = withStock.filter((batch) => batch.expiryDate >= today && batch.expiryDate <= warning).length;
  const expiredCount = withStock.filter((batch) => batch.expiryDate < today).length;
  const pendingReceipts = data.receipts.filter((receipt) => receipt.status === 'DRAFT');
  const pendingStocktakes = data.stocktakes.filter(
    (item) => item.status === 'PENDING_APPROVAL' || item.status === 'DRAFT',
  );
  const series = new Map();
  for (let key = addDaysKey(today, -13); key <= today; key = addDaysKey(key, 1))
    series.set(key, { date: key, inQty: 0, outQty: 0 });
  data.movements.forEach((movement) => {
    const bucket = series.get(toDateKey(movement.createdAt));
    if (!bucket) return;
    if (movement.type === 'IN') bucket.inQty += movement.quantity;
    if (movement.type === 'OUT') bucket.outQty += -movement.quantity;
  });
  return {
    kpis: {
      totalMedicines: data.medicines.filter((medicine) => medicine.status === 'ACTIVE').length,
      totalBatches: withStock.length,
      lowStockCount: lowCount,
      expiringBatches: expiringCount,
      pendingReceipts: pendingReceipts.length,
      movementsToday: data.movements.filter((movement) => toDateKey(movement.createdAt) === today).length,
    },
    tasks: [
      {
        id: 'low',
        label: 'Thuốc sắp hết cần lên kế hoạch nhập',
        count: lowCount,
        tone: 'warning',
        link: 'inventory.html?stockStatus=LOW_STOCK',
      },
      {
        id: 'expiring',
        label: 'Lô sắp hết hạn cần ưu tiên xuất bán',
        count: expiringCount,
        tone: 'warning',
        link: 'expiry.html',
      },
      {
        id: 'expired',
        label: 'Lô đã hết hạn cần xử lý hủy',
        count: expiredCount,
        tone: 'danger',
        link: 'expiry.html?tier=expired',
      },
      {
        id: 'receipts',
        label: 'Phiếu nhập nháp chờ xác nhận',
        count: pendingReceipts.length,
        tone: 'info',
        link: 'receipts.html?status=DRAFT',
      },
      {
        id: 'stocktakes',
        label: 'Phiếu kiểm kê đang xử lý',
        count: pendingStocktakes.length,
        tone: 'info',
        link: 'stocktake.html',
      },
    ],
    movementSeries: [...series.values()],
    lowStock: lowStockMedicines(data, 6),
    expiring: expiringBatches(data, 6),
    recentReceipts: [...data.receipts]
      .sort((a, b) => b.receivingDate.localeCompare(a.receivingDate) || b.id.localeCompare(a.id))
      .slice(0, 5)
      .map(({ items, ...receipt }) => ({ ...receipt, itemCount: items.length })),
    recentMovements: [...data.movements].sort((a, b) => b.createdAt.localeCompare(a.createdAt)).slice(0, 6),
  };
});

route('GET', '/reports/summary', admin, ({ query, data }) => {
  const { from, to } = defaultRange(query);
  const list = data.invoices.filter((invoice) => inDateRange(invoice.createdAt, from, to));
  const completed = list.filter((invoice) => invoice.status === 'COMPLETED');
  const revenue = completed.reduce((sum, invoice) => sum + invoice.total, 0);
  return {
    from,
    to,
    revenue,
    invoices: completed.length,
    cancelled: list.length - completed.length,
    averageOrder: completed.length ? Math.round(revenue / completed.length) : 0,
    itemsSold: completed.reduce(
      (sum, invoice) => sum + invoice.items.reduce((total, item) => total + item.quantity, 0),
      0,
    ),
  };
});

route('GET', '/reports/revenue', admin, ({ query, data }) => {
  const { from, to } = defaultRange(query);
  return revenueSeries(completedInvoices(data), from, to);
});

route('GET', '/reports/best-sellers', admin, ({ query, data }) => {
  const { from, to } = defaultRange(query);
  const totals = new Map();
  completedInvoices(data)
    .filter((invoice) => inDateRange(invoice.createdAt, from, to))
    .forEach((invoice) => {
      invoice.items.forEach((item) => {
        const entry = totals.get(item.medicineId) || {
          medicineId: item.medicineId,
          name: item.medicineName,
          code: item.medicineCode,
          unit: item.unit,
          quantity: 0,
          revenue: 0,
        };
        entry.quantity += item.quantity;
        entry.revenue += item.subtotal;
        totals.set(item.medicineId, entry);
      });
    });
  return [...totals.values()]
    .sort((a, b) => b.quantity - a.quantity || b.revenue - a.revenue)
    .slice(0, Number(query.limit) || 10);
});

route('GET', '/reports/inventory-distribution', admin, ({ data }) =>
  categoryDistribution(data).filter((entry) => entry.medicines > 0),
);

route('GET', '/reports/revenue-by-employee', admin, ({ query, data }) => {
  const { from, to } = defaultRange(query);
  const totals = new Map();
  completedInvoices(data)
    .filter((invoice) => inDateRange(invoice.createdAt, from, to))
    .forEach((invoice) => {
      const entry = totals.get(invoice.employeeId) || {
        employeeId: invoice.employeeId,
        name: invoice.employeeName,
        invoices: 0,
        revenue: 0,
      };
      entry.invoices += 1;
      entry.revenue += invoice.total;
      totals.set(invoice.employeeId, entry);
    });
  return [...totals.values()].sort((a, b) => b.revenue - a.revenue);
});
