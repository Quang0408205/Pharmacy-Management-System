# PharmaCare - Pharmacy Management System (Frontend)

Frontend cho hệ thống quản lý mua bán thuốc nội bộ, ba vai trò **ADMIN**, **SALES**, **WAREHOUSE**.
Công nghệ: HTML5, CSS3, Vanilla JavaScript (ES Modules), Chart.js. Không dùng framework SPA, jQuery hay Tailwind.

Giai đoạn hiện tại chạy hoàn toàn trên **Mock API** (dữ liệu sinh tất định, lưu trong `localStorage` của trình duyệt). Kiến trúc được giữ để thay bằng Java Spring Boot REST API mà không sửa mã giao diện.

## 1. Chạy local

Yêu cầu Node.js 18+.

```bash
cd frontend
npm install
npm start
```

Mở http://localhost:5173/ (có thể đổi cổng bằng biến `PORT`). Có thể dùng bất kỳ static server nào khác, miễn là thư mục gốc phục vụ là `frontend/`.

Các lệnh khác:

| Lệnh                     | Công dụng                                                                    |
| ------------------------ | ---------------------------------------------------------------------------- |
| `npm run format`         | Prettier format toàn bộ HTML/CSS/JS/JSON/MD                                  |
| `npm run format:check`   | Kiểm tra format                                                              |
| `npm run lint`           | ESLint (cấm `alert`, `console`, biến thừa...)                                |
| `npm run check:comments` | Bảo đảm không có comment trong HTML/CSS/JS và không có inline style/handler  |
| `npm run test:e2e`       | Kiểm thử đầu-cuối bằng Chrome hoặc Edge headless (đặt `CHROME_PATH` nếu cần) |

Thư viện Chart.js được vendor sẵn tại `assets/vendor/chart.umd.js` để chạy offline. Thư mục `assets/vendor/` là mã bên thứ ba nên được loại khỏi kiểm tra comment và format. Font Be Vietnam Pro tải từ Google Fonts, có fallback sang font hệ thống khi offline.

### Tài khoản demo

Mật khẩu chung: `Demo@12345` (chỉ dùng cho môi trường demo, không dùng cho production).

| Vai trò   | Tài khoản                 | Sau đăng nhập                    |
| --------- | ------------------------- | -------------------------------- |
| ADMIN     | `admin@pharmacy.demo`     | `pages/admin/dashboard.html`     |
| SALES     | `sales@pharmacy.demo`     | `pages/sales/pos.html`           |
| WAREHOUSE | `warehouse@pharmacy.demo` | `pages/warehouse/dashboard.html` |

Mã mời demo cho trang kích hoạt tài khoản: `NV-SALES-2026` (email `nhanvien.moi@pharmacy.demo`), `NV-KHO-2026` (email `thukho.moi@pharmacy.demo`), `HET-HAN-0001` (đã hết hạn). Token đặt lại mật khẩu demo: `demo-reset-valid` (hợp lệ), mọi giá trị khác là hết hạn.

Dữ liệu mẫu được tự đặt lại sau 7 ngày. Admin có thể đặt lại thủ công ở **Cài đặt > Dữ liệu demo**.

## 2. Cấu trúc thư mục

```
frontend/
  index.html                      điều hướng theo phiên đăng nhập
  pages/
    auth/      login, register, forgot-password, reset-password
    admin/     12 trang quản trị
    sales/     pos, invoices, customers, lookup, history
    warehouse/ dashboard, receiving, receipts, inventory, batches, expiry, stocktake, suppliers, movements
  assets/
    css/       tokens, base, layout, components, workspace (POS/nhập kho), auth, animations
    js/
      config/      app-config.js (apiMode, apiBaseUrl), navigation.js (menu theo role), demo-data.js
      core/        html (escape an toàn), format, storage, dom, icons, errors, medicine-image
      api/         client.js, adapters/mock-adapter.js, adapters/http-adapter.js
      services/    auth, catalog, partner, sales, inventory, admin, session
      mock/        seed, database, router, domain (FEFO, trạng thái lô/tồn), routes/*
      components/  shell (sidebar + header), page, overlay (modal, drawer, confirm), toast, data-table,
                   form, form-modal, badge, states (skeleton, empty, error), charts, blocks, auth-layout
      pages/       mã riêng của từng trang; pages/shared dùng chung giữa các role
    vendor/    chart.umd.js
  tools/       serve.mjs, generate-pages.mjs, check-comments.mjs, e2e.mjs
```

Mỗi trang HTML chỉ chứa `<div id="app">` và một thẻ `<script type="module">`. Sidebar, header, breadcrumb, dropdown người dùng, thông báo và tìm kiếm toàn cục đều do `components/shell.js` dựng từ cấu hình `config/navigation.js`, nên không có bản sao sidebar nào và không có race condition khi nạp partial.

## 3. Kiến trúc Mock API

```
Trang (pages/*) -> services/* -> api/client.js -> adapter -> (mock/routes/* | Spring Boot)
```

- `config/app-config.js` chứa công tắc:

  ```js
  export const appConfig = { apiMode: 'mock', apiBaseUrl: '/api/v1' };
  ```

- Trang và component **không** đọc JSON hay `localStorage` dữ liệu nghiệp vụ trực tiếp, chỉ gọi service. Service gọi `api.get/post/put/patch/delete(path, ...)`.
- `mock-adapter.js` mô phỏng độ trễ, nhân bản dữ liệu trả về, định tuyến tới `mock/routes/*`. Mỗi route khai báo vai trò được phép, nên API mock cũng trả `401/403` giống backend thật.
- `http-adapter.js` là adapter thật: `fetch` tới `apiBaseUrl`, `credentials: 'include'`, chuyển lỗi HTTP sang `ApiError`.
- Toàn bộ trạng thái (thuốc, lô, hóa đơn, phiếu nhập, biến động kho, kiểm kê...) nằm trong một nguồn duy nhất `mock/database.js` (lưu theo từng collection trong `localStorage`), nên bán hàng, nhập kho và kiểm kê luôn đồng bộ khi chuyển trang.
- Dữ liệu seed sinh tất định (`mock/seed.js`): 500 thuốc, 15 danh mục, 15 nhà cung cấp, 100 phiếu nhập (96 đã xác nhận + 4 nháp), 1.000 lô, 500 hóa đơn, 100 khách hàng, 9 tài khoản. Sổ biến động kho khớp với tồn từng lô (tồn = nhập - xuất + điều chỉnh).
- Quy tắc nghiệp vụ nằm ở mock (và sẽ nằm ở backend): bán theo **FEFO**, không bán lô hết hạn, thuốc kê đơn/kiểm soát cần xác nhận đơn thuốc, không bán vượt tồn, phiếu nhập đã xác nhận bất biến, kiểm kê cần Admin duyệt mới điều chỉnh kho.

### Bảo mật phía client

- Mật khẩu không bao giờ lưu dạng rõ. Mock lưu băm SHA-256 có salt (`db.credentials`), chỉ phục vụ demo.
- Phiên mock chỉ lưu `userId` trong `sessionStorage` (hoặc `localStorage` 8 giờ nếu chọn "Ghi nhớ"). Không có access token hay dữ liệu nhạy cảm ở client.
- Role guard ở frontend (`requireSession`) chỉ để điều hướng và trải nghiệm. Quyền thật phải được backend kiểm tra.
- Mọi dữ liệu chèn vào HTML đều qua `html` tagged template, tự escape.

## 4. Hợp đồng API (REST) cần backend cung cấp

Danh sách phân trang trả về `{ items, page, size, total, totalPages }` và nhận query `q`, `page`, `size`, `sort=field,asc|desc` cùng các bộ lọc.
Lỗi trả JSON `{ "code": "...", "message": "...", "fieldErrors": { "field": "thông báo" } }` với mã HTTP phù hợp (400, 401, 403, 404, 409, 422).
Trường `fieldErrors` của phiếu nhập dùng khóa dạng `items.<chỉ số>.<trường>` (ví dụ `items.0.expiryDate`).

| Nhóm         | Endpoint                                                                                                                                                                                   | Vai trò                                   |
| ------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------ | ----------------------------------------- |
| Xác thực     | `POST /auth/login`, `POST /auth/logout`, `GET /auth/me`, `POST /auth/register`, `GET /auth/invitations/{code}`                                                                             | công khai                                 |
|              | `POST /auth/forgot-password`, `GET /auth/reset-tokens/{token}`, `POST /auth/reset-password`                                                                                                | công khai                                 |
| Danh mục     | `GET/POST /categories`, `PUT/DELETE /categories/{id}`                                                                                                                                      | đọc: tất cả, ghi: ADMIN                   |
| Thuốc        | `GET /medicines`, `GET /medicines/{id}`, `GET /medicines/next-code`, `POST /medicines`, `PUT /medicines/{id}`, `PATCH /medicines/{id}/status`                                              | đọc: tất cả, ghi: ADMIN                   |
| Nhà cung cấp | `GET/POST /suppliers`, `GET/PUT /suppliers/{id}`, `PATCH /suppliers/{id}/status`                                                                                                           | ADMIN, WAREHOUSE                          |
| Khách hàng   | `GET/POST /customers`, `GET/PUT /customers/{id}`                                                                                                                                           | ADMIN, SALES                              |
| Hóa đơn      | `GET /invoices`, `GET /invoices/{id}`, `GET /invoices/next-number`, `POST /invoices` (checkout), `POST /invoices/{id}/cancel`                                                              | SALES tạo, ADMIN hủy                      |
|              | `GET/POST /invoice-drafts`, `DELETE /invoice-drafts/{id}`, `GET /dashboard/sales`                                                                                                          | SALES                                     |
| Phiếu nhập   | `GET/POST /receipts`, `GET/PUT/DELETE /receipts/{id}`, `GET /receipts/next-code` (`confirm: true` để xác nhận nhập kho)                                                                    | WAREHOUSE ghi, ADMIN xem                  |
| Lô & kho     | `GET /batches`, `GET /batches/{id}`, `POST /batches/{id}/write-off`, `GET /movements`, `GET /inventory/summary`, `GET /expiry/summary`                                                     | ADMIN, WAREHOUSE                          |
| Kiểm kê      | `GET/POST /stocktakes`, `GET/PUT/DELETE /stocktakes/{id}`, `POST /stocktakes/{id}/submit`, `/approve`, `/reject`                                                                           | WAREHOUSE tạo, ADMIN duyệt                |
| Quản trị     | `GET /employees`, `GET/PATCH /employees/{id}`, `PATCH /employees/{id}/status`, `GET/POST /invitations`, `POST /invitations/{id}/revoke`                                                    | ADMIN                                     |
|              | `GET/PUT /settings`, `GET /activity-logs`, `GET /notifications`, `POST /system/reset` (chỉ mock, bỏ khi dùng backend thật)                                                                 | ADMIN (settings đọc: tất cả)              |
| Báo cáo      | `GET /dashboard/admin`, `GET /dashboard/warehouse`, `GET /reports/summary`, `/reports/revenue`, `/reports/best-sellers`, `/reports/inventory-distribution`, `/reports/revenue-by-employee` | ADMIN (warehouse dashboard: cả WAREHOUSE) |

Hình dạng dữ liệu chính (xem `mock/seed.js` để biết đầy đủ): `Medicine` có `price`, `costPrice`, `prescriptionType` (`OTC|RX|CONTROLLED`), `minStock`, và khi đọc kèm `totalQty`, `availableQty`, `stockStatus`. `Batch` có `medicineId`, `receiptId`, `expiryDate`, `initialQty`, `remainingQty` và trạng thái suy ra (`AVAILABLE|LOW_STOCK|EXPIRING_SOON|EXPIRED|OUT_OF_STOCK`). `Invoice.items[].allocations` ghi lô xuất theo FEFO. Ngày dùng `YYYY-MM-DD`, thời điểm dùng ISO-8601.

Mapping MongoDB gợi ý: mỗi collection tương ứng một mảng trong `mock/database.js` (`medicines`, `batches`, `receipts`, `invoices`, `movements`, `stocktakes`, `customers`, `suppliers`, `employees`, `invitations`, `activityLogs`, `settings`). Các trường `*Name` (ví dụ `supplierName`, `employeeName`) là snapshot để hiển thị, phù hợp mô hình embed của MongoDB.

## 5. Chuyển sang Spring Boot REST API

1. Triển khai các endpoint ở bảng trên dưới tiền tố `/api/v1`, dùng Spring Security với **session cookie `HttpOnly`** (hoặc JWT trong cookie `HttpOnly`) và phân quyền bằng `@PreAuthorize("hasRole('ADMIN')")` theo cột Vai trò. Không trả hay lưu token ở `localStorage`.
2. Cùng origin: đặt thư mục `frontend/` vào `src/main/resources/static` (hoặc reverse proxy), `apiBaseUrl` giữ `/api/v1`. Khác origin: cấu hình CORS cho phép `credentials`, và đổi `apiBaseUrl` thành URL tuyệt đối.
3. Đổi cấu hình trong `assets/js/config/app-config.js`:

   ```js
   export const appConfig = { apiMode: 'real', apiBaseUrl: '/api/v1' };
   ```

   Khi `apiMode` khác `mock`, `mock-adapter`, seed và dữ liệu demo **không được nạp**; trang đăng nhập cũng tự ẩn khối tài khoản demo.

4. Trả lỗi đúng định dạng ở mục 4 để giao diện hiển thị thông báo theo từng trường.
5. Logic mà mock đang làm phải chuyển vào service backend: tính tổng tiền từ giá hiện tại, phân bổ lô FEFO và trừ kho trong một transaction, kiểm tra thuốc kê đơn/kiểm soát, bất biến của phiếu nhập đã xác nhận, điều chỉnh kho khi Admin duyệt kiểm kê, ghi sổ biến động và nhật ký hoạt động.
6. Có thể bỏ thư mục `assets/js/mock/`, `assets/js/config/demo-data.js` và `adapters/mock-adapter.js` khi không còn cần demo.

## 6. Giao diện và khả năng truy cập

- Design tokens tập trung ở `assets/css/tokens.css` (màu, typography, spacing, bo góc, đổ bóng, thời lượng animation, z-index, breakpoint). Hỗ trợ giao diện sáng/tối và mật độ bảng gọn (Cài đặt > Giao diện).
- Thời lượng animation: 150ms (nhanh), 220ms (thường), 240ms (sidebar, drawer), 200ms (modal). `prefers-reduced-motion` tắt gần như mọi chuyển động.
- Breakpoint kiểm thử: 1440, 1366, 1024, 768, 375. Desktop sidebar cố định; tablet sidebar thu gọn mặc định; mobile sidebar là drawer; bảng cuộn ngang; POS và nhập kho chuyển sang chế độ xem giỏ riêng.
- Có skip link, focus ring rõ, label cho mọi trường, thông báo lỗi `role="alert"`, modal bẫy focus và đóng bằng `Esc`, trạng thái không chỉ dựa vào màu (mọi badge có chữ).

## 7. Hạn chế hiện tại

- Dữ liệu nằm trong `localStorage` từng trình duyệt (khoảng 2,2 triệu ký tự), không chia sẻ giữa người dùng hoặc thiết bị.
- Mock chưa mô phỏng khóa đồng thời: hai tab cùng bán một lô có thể ghi đè nhau (backend cần transaction hoặc optimistic locking).
- Hóa đơn "in" chỉ dùng `window.print()` trên mẫu đơn giản, không phải hóa đơn điện tử hợp lệ.
- Thanh toán chuyển khoản/thẻ chỉ ghi nhận phương thức, không tích hợp cổng thanh toán.
- Hình ảnh thuốc là hình minh họa sinh bằng SVG theo dạng bào chế; backend có thể trả `imageUrl` để thay thế.
- Font chữ tải từ Google Fonts nên cần mạng lần đầu để hiển thị đúng font.
