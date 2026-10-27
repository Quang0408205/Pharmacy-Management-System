export class ApiError extends Error {
  constructor(status, code, message, fieldErrors = {}) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }
}

export const badRequest = (message, fieldErrors) => new ApiError(400, 'BAD_REQUEST', message, fieldErrors);
export const unauthorized = (message = 'Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.') =>
  new ApiError(401, 'UNAUTHORIZED', message);
export const forbidden = (message = 'Bạn không có quyền thực hiện thao tác này.') =>
  new ApiError(403, 'FORBIDDEN', message);
export const notFound = (message = 'Không tìm thấy dữ liệu yêu cầu.') => new ApiError(404, 'NOT_FOUND', message);
export const conflict = (message, fieldErrors) => new ApiError(409, 'CONFLICT', message, fieldErrors);

export const errorMessage = (error) => {
  if (error instanceof ApiError) return error.message;
  return 'Đã xảy ra lỗi không mong muốn. Vui lòng thử lại.';
};
