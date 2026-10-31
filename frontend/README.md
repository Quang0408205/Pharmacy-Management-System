# PharmaCare - Pharmacy Management System (Frontend)

Frontend cho hệ thống quản lý mua bán thuốc nội bộ, ba vai trò **ADMIN**, **SALES**, **WAREHOUSE**.
Công nghệ: HTML5, CSS3, Vanilla JavaScript (ES Modules), Chart.js. Không dùng framework SPA, jQuery hay Tailwind.

Frontend gọi trực tiếp REST API của backend Spring Boot (`/api/v1`) qua cùng origin (nginx proxy sang backend), xác thực bằng cookie `HttpOnly`. Toàn bộ dữ liệu lấy từ backend; dữ liệu khởi tạo nằm ở `database/seed_data`.

## 1. Chạy local

Chạy toàn bộ hệ thống (MongoDB, backend, frontend) bằng Docker:

```bash
docker compose up -d --build
```

Mở http://localhost:8088/. Frontend được phục vụ bởi nginx và proxy `/api/` sang backend, nên cookie phiên cùng origin.

Chỉ phát triển giao diện (cần backend đang chạy ở `localhost:8080`):

```bash
cd frontend
npm install
npm start
```

Các lệnh khác:

| Lệnh                     | Công dụng                                                                   |
| ------------------------ | --------------------------------------------------------------------------- |
| `npm run format`         | Prettier format toàn bộ HTML/CSS/JS/JSON/MD                                 |
| `npm run format:check`   | Kiểm tra format                                                             |
| `npm run lint`           | ESLint (cấm `alert`, `console`, biến thừa...)                               |
| `npm run check:comments` | Bảo đảm không có comment trong HTML/CSS/JS và không có inline style/handler |

Thư viện Chart.js được vendor sẵn tại `assets/vendor/chart.umd.js` để chạy offline. Thư mục `assets/vendor/` là mã bên thứ ba nên được loại khỏi kiểm tra comment và format. Font Be Vietnam Pro tải từ Google Fonts, có fallback sang font hệ thống khi offline.

### Tài khoản đăng nhập

Tài khoản và dữ liệu ban đầu được backend nạp từ `database/seed_data` khi cơ sở dữ liệu còn trống. Mật khẩu của mọi tài khoản seed là giá trị biến `SEED_DEFAULT_PASSWORD` (mặc định `Pharmacy@123`; chỉ dùng cho môi trường phát triển).

| Vai trò | Tên đăng nhập | Sau đăng nhập                |
| ------- | ------------- | ---------------------------- |
| ADMIN   | `admin`       | `pages/admin/dashboard.html` |

Seed chỉ tạo một tài khoản Admin (Quang). Các tài khoản bán hàng và kho do Admin tạo ở trang **Nhân viên**.

Không có đăng ký công khai. Admin tạo nhân viên ở trang **Nhân viên**, nhận một liên kết kích hoạt hiển thị một lần (hiệu lực 72 giờ) và gửi cho nhân viên. Quên mật khẩu: Admin tạo liên kết đặt lại (hiệu lực 30 phút) cho nhân viên đó.

## 2. Cấu trúc thư mục

```
frontend/
  index.html                      điều hướng theo phiên đăng nhập
  pages/
    auth/      login, activate, forgot-password, reset-password
    admin/     13 trang quản trị
    sales/     pos, invoices, customers, lookup, history
    warehouse/ dashboard, receiving, receipts, inventory, batches, expiry, stocktake, suppliers, movements
  assets/
    css/       tokens, base, layout, components, workspace (POS/nhập kho), auth, animations
    js/
      config/      app-config.js (apiBaseUrl), navigation.js (menu theo role)
      core/        html (escape an toàn), format, storage, dom, icons, errors, medicine-image
      api/         client.js, adapters/http-adapter.js
      services/    auth, catalog, partner, sales, inventory, admin, session
      components/  shell (sidebar + header), page, overlay (modal, drawer, confirm), toast, data-table,
                   form, form-modal, badge, states (skeleton, empty, error), charts, blocks, auth-layout
      pages/       mã riêng của từng trang; pages/shared dùng chung giữa các role
    vendor/    chart.umd.js
  tools/       serve.mjs, generate-pages.mjs, check-comments.mjs
```

Mỗi trang HTML chỉ chứa `<div id="app">` và một thẻ `<script type="module">`. Sidebar, header, breadcrumb, dropdown người dùng, thông báo và tìm kiếm toàn cục đều do `components/shell.js` dựng từ cấu hình `config/navigation.js`, nên không có bản sao sidebar nào và không có race condition khi nạp partial.

## 3. Kiến trúc gọi API

```
Trang (pages/*) -> services/* -> api/client.js -> adapters/http-adapter.js -> Spring Boot
```

- `config/app-config.js` chứa `apiBaseUrl` (mặc định `/api/v1`, đổi được qua build arg `API_BASE_URL` của Dockerfile).
- Trang và component **không** đọc dữ liệu nghiệp vụ trực tiếp, chỉ gọi service. Service gọi `api.get/post/put/patch/delete(path, ...)`.
- `http-adapter.js`: `fetch` tới `apiBaseUrl` với `credentials: 'include'`, chuyển lỗi HTTP sang `ApiError`. Mã 401 ngoài `/auth/*` làm giao diện tự đăng xuất.
- Mọi quy tắc nghiệp vụ nằm ở backend: bán theo **FEFO**, không bán lô hết hạn, thuốc kê đơn/kiểm soát cần xác nhận đơn thuốc, không bán vượt tồn, phiếu nhập đã xác nhận bất biến, kiểm kê và xuất hủy cần Admin duyệt.

### Bảo mật phía client

- Phiên là JWT trong cookie `HttpOnly`, `SameSite=Strict`; JavaScript không đọc được token và không lưu token hay mật khẩu ở client.
- Role guard ở frontend (`requireSession`) chỉ để điều hướng và trải nghiệm. Quyền thật do backend kiểm tra.
- Mọi dữ liệu chèn vào HTML đều qua `html` tagged template, tự escape.

## 4. Hợp đồng API (REST) cần backend cung cấp

Danh sách phân trang trả về `{ items, page, size, total, totalPages }` và nhận query `q`, `page`, `size`, `sort=field,asc|desc` cùng các bộ lọc.
Lỗi trả JSON `{ "code": "...", "message": "...", "fieldErrors": { "field": "thông báo" } }` với mã HTTP phù hợp (400, 401, 403, 404, 409, 422).
Trường `fieldErrors` của phiếu nhập dùng khóa dạng `items.<chỉ số>.<trường>` (ví dụ `items.0.expiryDate`).

| Nhóm         | Endpoint                                                                                                                                                                                   | Vai trò                                   |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------- |
| Xác thực     | `POST /auth/login`, `POST /auth/logout`, `GET /auth/me`, `GET /auth/activation/{token}`, `POST /auth/activate`, `GET /auth/reset-tokens/{token}`, `POST /auth/reset-password`              | công khai                                 |
| Danh mục     | `GET/POST /categories`, `PUT/DELETE /categories/{id}`                                                                                                                                      | đọc: tất cả, ghi: ADMIN                   |
| Thuốc        | `GET /medicines`, `GET /medicines/{id}`, `GET /medicines/next-code`, `POST /medicines`, `PUT /medicines/{id}`, `PATCH /medicines/{id}/status`                                              | đọc: tất cả, ghi: ADMIN                   |
| Nhà cung cấp | `GET/POST /suppliers`, `GET/PUT /suppliers/{id}`, `PATCH /suppliers/{id}/status`                                                                                                           | ADMIN, WAREHOUSE                          |
| Khách hàng   | `GET/POST /customers`, `GET/PUT /customers/{id}`                                                                                                                                           | ADMIN, SALES                              |
| Hóa đơn      | `GET /invoices`, `GET /invoices/{id}`, `GET /invoices/next-number`, `POST /invoices` (checkout), `POST /invoices/{id}/cancel`                                                              | SALES tạo, ADMIN hủy                      |
|              | `GET/POST /invoice-drafts`, `DELETE /invoice-drafts/{id}`, `GET /dashboard/sales`                                                                                                          | SALES                                     |
| Phiếu nhập   | `GET/POST /receipts`, `GET/PUT/DELETE /receipts/{id}`, `GET /receipts/next-code` (`confirm: true` để xác nhận nhập kho)                                                                    | WAREHOUSE ghi, ADMIN xem                  |
| Lô & kho     | `GET /batches`, `GET /batches/{id}`, `POST /batches/{id}/write-off`, `GET /movements`, `GET /inventory/summary`, `GET /expiry/summary`, `GET/POST /disposals`, `/disposals/{id}/approve    | reject`                                   | ADMIN, WAREHOUSE |
| Kiểm kê      | `GET/POST /stocktakes`, `GET/PUT/DELETE /stocktakes/{id}`, `POST /stocktakes/{id}/submit`, `/approve`, `/reject`                                                                           | WAREHOUSE tạo, ADMIN duyệt                |
| Quản trị     | `GET/POST /employees`, `GET/PATCH /employees/{id}`, `PATCH /employees/{id}/status`, `POST /employees/{id}/reset-link`, `POST /employees/{id}/unlock`                                       | ADMIN                                     |
|              | `GET/PUT /settings`, `GET /activity-logs`, `GET /notifications`                                                                                                                            | ADMIN (settings đọc: tất cả)              |
| Báo cáo      | `GET /dashboard/admin`, `GET /dashboard/warehouse`, `GET /reports/summary`, `/reports/revenue`, `/reports/best-sellers`, `/reports/inventory-distribution`, `/reports/revenue-by-employee` | ADMIN (warehouse dashboard: cả WAREHOUSE) |

Hình dạng dữ liệu chính: `Medicine` có `price`, `costPrice`, `prescriptionType` (`OTC|RX|CONTROLLED`), `minStock`, và khi đọc kèm `totalQty`, `availableQty`, `stockStatus`. `Batch` có `medicineId`, `receiptId`, `expiryDate`, `initialQty`, `remainingQty` và trạng thái suy ra (`AVAILABLE|LOW_STOCK|EXPIRING_SOON|EXPIRED|OUT_OF_STOCK|QUARANTINED`). `Invoice.items[].allocations` ghi lô xuất theo FEFO. Ngày dùng `YYYY-MM-DD`, thời điểm dùng ISO-8601. Danh sách route đầy đủ và ma trận quyền nằm trong tài liệu thiết kế của backend.

## 5. Giao diện và khả năng truy cập

- Design tokens tập trung ở `assets/css/tokens.css` (màu, typography, spacing, bo góc, đổ bóng, thời lượng animation, z-index, breakpoint). Hỗ trợ giao diện sáng/tối và mật độ bảng gọn (Cài đặt > Giao diện).
- Thời lượng animation: 150ms (nhanh), 220ms (thường), 240ms (sidebar, drawer), 200ms (modal). `prefers-reduced-motion` tắt gần như mọi chuyển động.
- Breakpoint kiểm thử: 1440, 1366, 1024, 768, 375. Desktop sidebar cố định; tablet sidebar thu gọn mặc định; mobile sidebar là drawer; bảng cuộn ngang; POS và nhập kho chuyển sang chế độ xem giỏ riêng.
- Có skip link, focus ring rõ, label cho mọi trường, thông báo lỗi `role="alert"`, modal bẫy focus và đóng bằng `Esc`, trạng thái không chỉ dựa vào màu (mọi badge có chữ).

## 6. Hạn chế hiện tại

- Hóa đơn "in" chỉ dùng `window.print()` trên mẫu đơn giản, không phải hóa đơn điện tử hợp lệ.
- Thanh toán chuyển khoản/thẻ chỉ ghi nhận phương thức, không tích hợp cổng thanh toán.
- Hình ảnh thuốc là hình minh họa sinh bằng SVG theo dạng bào chế; backend có thể trả `imageUrl` để thay thế.
- Font chữ tải từ Google Fonts nên cần mạng lần đầu để hiển thị đúng font.
