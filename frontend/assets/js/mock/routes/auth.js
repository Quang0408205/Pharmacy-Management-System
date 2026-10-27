import { createSalt, database, hashPassword } from '../database.js';
import { endSession, getSessionUserId, startSession } from '../session.js';
import { publicUser, requireFields } from '../domain.js';
import { ApiError, badRequest, conflict, forbidden, notFound } from '../../core/errors.js';
import { route } from '../router.js';

const passwordRule = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).{8,}$/;
const validResetToken = 'demo-reset-valid';

const findInvitation = (code) =>
  database.data.invitations.find(
    (item) =>
      item.code.toLowerCase() ===
      String(code || '')
        .trim()
        .toLowerCase(),
  );

const invitationState = (invitation) => {
  if (!invitation) return 'NOT_FOUND';
  if (invitation.status === 'PENDING' && new Date(invitation.expiresAt) < new Date()) return 'EXPIRED';
  return invitation.status;
};

route('POST', '/auth/login', 'public', async ({ body, data }) => {
  requireFields(body, { identifier: 'Email hoặc tên đăng nhập', password: 'Mật khẩu' });
  const identifier = body.identifier.trim().toLowerCase();
  const employee = data.employees.find(
    (item) => item.email.toLowerCase() === identifier || item.username.toLowerCase() === identifier,
  );
  const credential = employee ? database.credentials()[employee.id] : null;
  const matches = credential ? (await hashPassword(body.password, credential.salt)) === credential.hash : false;
  if (!employee || !matches)
    throw new ApiError(401, 'INVALID_CREDENTIALS', 'Email/tên đăng nhập hoặc mật khẩu không đúng.');
  if (employee.status !== 'ACTIVE') throw forbidden('Tài khoản đã bị vô hiệu hóa. Vui lòng liên hệ quản trị viên.');
  startSession(employee.id, Boolean(body.remember));
  employee.lastLoginAt = new Date().toISOString();
  database.save('employees');
  database.log(employee, 'LOGIN', 'AUTH', employee.username, `${employee.name} đăng nhập hệ thống`);
  return { user: publicUser(employee) };
});

route('POST', '/auth/logout', 'public', () => {
  endSession();
  return { success: true };
});

route('GET', '/auth/me', 'public', ({ data }) => {
  const userId = getSessionUserId();
  const employee = userId ? data.employees.find((item) => item.id === userId && item.status === 'ACTIVE') : null;
  if (!employee) throw new ApiError(401, 'UNAUTHORIZED', 'Chưa đăng nhập.');
  return { user: publicUser(employee) };
});

route('GET', '/auth/invitations/:code', 'public', ({ params }) => {
  const invitation = findInvitation(params.code);
  const state = invitationState(invitation);
  if (state === 'NOT_FOUND') throw notFound('Mã mời không tồn tại.');
  if (state === 'EXPIRED') throw conflict('Mã mời đã hết hạn. Vui lòng liên hệ quản trị viên để nhận mã mới.');
  if (state !== 'PENDING') throw conflict('Mã mời không còn hiệu lực.');
  return { email: invitation.email, role: invitation.role };
});

route('POST', '/auth/register', 'public', async ({ body, data }) => {
  requireFields(body, {
    fullName: 'Họ tên',
    email: 'Email',
    username: 'Tên đăng nhập',
    password: 'Mật khẩu',
    invitationCode: 'Mã mời',
  });
  const invitation = findInvitation(body.invitationCode);
  const state = invitationState(invitation);
  if (state !== 'PENDING') {
    throw badRequest('Mã mời không hợp lệ hoặc đã hết hạn.', {
      invitationCode: 'Mã mời không hợp lệ hoặc đã hết hạn.',
    });
  }
  if (invitation.email.toLowerCase() !== body.email.trim().toLowerCase()) {
    throw badRequest('Email không khớp với lời mời.', { email: 'Email phải trùng với email được mời.' });
  }
  if (!passwordRule.test(body.password)) {
    throw badRequest('Mật khẩu chưa đủ mạnh.', {
      password: 'Mật khẩu tối thiểu 8 ký tự, gồm chữ hoa, chữ thường và số.',
    });
  }
  if (data.employees.some((item) => item.username.toLowerCase() === body.username.trim().toLowerCase())) {
    throw conflict('Tên đăng nhập đã tồn tại.', { username: 'Tên đăng nhập đã được sử dụng.' });
  }
  const employee = {
    id: database.nextId('emp', 'employees', 2),
    name: body.fullName.trim(),
    email: invitation.email,
    username: body.username.trim(),
    role: invitation.role,
    phone: '',
    status: 'ACTIVE',
    createdAt: new Date().toISOString(),
    lastLoginAt: null,
  };
  const salt = createSalt();
  database.saveCredential(employee.id, { salt, hash: await hashPassword(body.password, salt) });
  data.employees.push(employee);
  invitation.status = 'USED';
  database.save('employees', 'invitations');
  database.log(
    employee,
    'REGISTER',
    'EMPLOYEE',
    employee.username,
    `${employee.name} kích hoạt tài khoản bằng lời mời`,
  );
  return { user: publicUser(employee) };
});

route('POST', '/auth/forgot-password', 'public', ({ body }) => {
  requireFields(body, { email: 'Email' });
  return { accepted: true };
});

route('GET', '/auth/reset-tokens/:token', 'public', ({ params }) => {
  if (params.token === validResetToken) return { valid: true };
  throw conflict('Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.');
});

route('POST', '/auth/reset-password', 'public', ({ body }) => {
  requireFields(body, { token: 'Mã xác thực', password: 'Mật khẩu mới' });
  if (body.token !== validResetToken) throw conflict('Liên kết đặt lại mật khẩu không hợp lệ hoặc đã hết hạn.');
  if (!passwordRule.test(body.password)) {
    throw badRequest('Mật khẩu chưa đủ mạnh.', {
      password: 'Mật khẩu tối thiểu 8 ký tự, gồm chữ hoa, chữ thường và số.',
    });
  }
  return { success: true };
});
