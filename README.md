# Pharmacy Management System

Hệ thống quản lý mua bán thuốc cho một nhà thuốc, có ba vai trò:

| Vai trò | Mã | Việc chính |
| --- | --- | --- |
| Quản trị viên | `ADMIN` | Quản lý thuốc, danh mục, nhân viên, duyệt kiểm kê và xuất hủy, hủy hóa đơn, báo cáo, cài đặt |
| Nhân viên bán thuốc | `SALES` | Bán thuốc tại quầy, lưu đơn nháp, quản lý khách hàng, xem hóa đơn của mình |
| Nhân viên kho | `WAREHOUSE` | Nhập thuốc, kiểm kê, lập và thực hiện phiếu xuất hủy, theo dõi lô và hạn dùng |

## Công nghệ

- Backend: Spring Boot 4, Java 21, Spring Security (JWT trong cookie HttpOnly), Spring Data MongoDB, MapStruct, springdoc OpenAPI.
- Cơ sở dữ liệu: MongoDB chạy replica set có xác thực (cần cho transaction).
- Frontend: HTML, CSS, JavaScript thuần (ES modules), phục vụ bằng nginx và proxy `/api/` sang backend.
- Đóng gói: Docker Compose.

## Cấu trúc thư mục

```
backend/demo/        Mã nguồn backend (package com.pharmacy.demo)
frontend/            Giao diện web, xem frontend/README.md
database/mongo/      Dockerfile và entrypoint tạo replica set rs0
database/seed_data/  Dữ liệu ban đầu: danh mục, thuốc, nhà cung cấp, khách hàng, tài khoản
database/scripts/    Script Node cào và chuẩn hóa dữ liệu thuốc, kiểm tra giao diện
docker-compose.yml   MongoDB, backend, frontend
.env.example         Mẫu biến môi trường
```

Backend chia theo tầng: `controller` (kiểm tra dữ liệu và phân quyền) gọi `service` (nghiệp vụ, transaction), rồi `repository` và `entity`. Dữ liệu trả về qua `mapper` sang các `dto`.

## Chạy hệ thống

Yêu cầu: Docker Desktop.

```bash
cp .env.example .env
docker compose up -d --build
```

Đổi các giá trị `change_me` trong `.env` trước khi dùng thật (`MONGO_ROOT_PASSWORD`, `MONGO_REPLICA_KEY`, `JWT_SECRET`).

| Thành phần | Địa chỉ |
| --- | --- |
| Giao diện | http://localhost:8088 |
| API | http://localhost:8080/api/v1 |
| Swagger UI | http://localhost:8080/swagger-ui.html |

Lần chạy đầu, khi cơ sở dữ liệu còn trống, backend tự nạp dữ liệu từ `database/seed_data`: 15 danh mục, 2.024 thuốc, 15 nhà cung cấp, 100 khách hàng và 5 tài khoản. Kho bắt đầu bằng 0; hãy nhập kho bằng tài khoản kho trước khi bán.

Nạp lại từ đầu (xóa toàn bộ dữ liệu):

```bash
docker compose down -v
docker compose up -d --build
```

## Quy trình nghiệp vụ chính

- **Nhập kho**: Kho lưu nháp phiếu nhập, xác nhận thì hệ thống tạo lô, ghi biến động và cập nhật giá nhập tham khảo. Phiếu đã xác nhận không sửa được.
- **Bán thuốc**: hệ thống chọn lô có hạn dùng gần nhất trước (FEFO), tự tính tiền và giảm giá, không bán lô hết hạn hoặc đang cách ly. Thuốc kê đơn cần xác nhận đơn thuốc; thuốc kiểm soát đặc biệt cần thêm mã đơn và khách hàng. Gửi lại cùng `requestId` không tạo hóa đơn mới.
- **Hủy hóa đơn**: chỉ Admin, bắt buộc lý do, hoàn kho về đúng lô đã bán.
- **Kiểm kê**: Kho chụp tồn tối đa 60 lô, nhập số thực tế, gửi duyệt; Admin duyệt thì tồn được điều chỉnh (từ chối nếu lô đã thay đổi sau khi lập phiếu).
- **Xuất hủy**: Kho lập phiếu, gửi duyệt (lô chuyển sang cách ly), Admin duyệt, Kho thực hiện thì mới trừ tồn.
- **Đối soát**: `GET /api/v1/inventory/reconciliation` (Admin) so tồn từng lô với tổng biến động; kết quả đúng là không có lô lệch.

Chỉ `StockLedgerService` được thay đổi số lượng tồn của lô, và mỗi thay đổi đều ghi một biến động trong cùng transaction.

## Phát triển và kiểm tra

```bash
cd backend/demo
./mvnw -B spotless:apply
./mvnw -B verify
```

`verify` chạy cả test tích hợp bằng Testcontainers, nên cần Docker đang chạy. Kết quả nằm ở `target/surefire-reports` (unit) và `target/failsafe-reports` (tích hợp).

Quy ước mã nguồn: định dạng bằng Spotless (Java) và Prettier (frontend), không dùng comment trong mã. Chi tiết lệnh kiểm tra frontend xem `frontend/README.md`.

Chạy backend ngoài Docker với profile `dev` (bật Swagger, seed và `POST /api/v1/system/reset`):

```bash
cd backend/demo
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Cần một MongoDB replica set đang chạy; địa chỉ mặc định là `mongodb://localhost:27017/pharmacy?replicaSet=rs0&directConnection=true`, đổi bằng `SPRING_MONGODB_URI`.
