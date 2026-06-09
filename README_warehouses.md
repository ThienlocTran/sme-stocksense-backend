# Tổng Hợp Tính Năng API Warehouse (Task T40, T41 & T42)

Tài liệu này tổng hợp ngắn gọn các thông tin về chức năng liên quan đến quản lý kho hàng, bao gồm:
1. **T40 - Tạo API danh sách kho** (Lấy danh sách, lọc, tìm kiếm).
2. **T41 - Tạo API thêm kho** (Thêm mới kho hàng, validate mã kho không trùng).
3. **T42 - Tạo API cập nhật kho** (Sửa thông tin tên, địa chỉ, trạng thái và cấm sửa mã kho).

Chi tiết giải thích từng dòng code đã được viết trực tiếp dưới dạng chú thích (inline comments) trong từng file mã nguồn.

---

## 1. Các File Được Tạo / Sửa
- **Database Migration**:
  - `src/main/resources/db/migration/V3__create_warehouses_table.sql`: Thay đổi cấu trúc bảng `kho` để thêm enum `trang_thai_kho` và đổi cột `dang_hoat_dong` sang `trang_thai`.
- **Thực thể (Entity & Enum)**:
  - `src/main/java/com/smartflow/smestocksensebackend/entity/WarehouseStatus.java`: Định nghĩa trạng thái `HOAT_DONG` / `NGUNG_HOAT_DONG`.
  - `src/main/java/com/smartflow/smestocksensebackend/entity/Warehouse.java`: Thực thể JPA ánh xạ với cơ sở dữ liệu (bảng `kho`).
- **Truy vấn (Repository)**:
  - `src/main/java/com/smartflow/smestocksensebackend/repository/WarehouseRepository.java`: Khai báo truy vấn động và thêm phương thức kiểm tra tồn tại mã kho `existsByCodeIgnoreCase`.
- **DTOs**:
  - `src/main/java/com/smartflow/smestocksensebackend/dto/request/CreateWarehouseRequest.java`: DTO nhận yêu cầu tạo mới kho hàng với các ràng buộc xác thực dữ liệu đầu vào: `maKho`, `tenKho`, `diaChi`, `trangThai`.
  - `src/main/java/com/smartflow/smestocksensebackend/dto/request/UpdateWarehouseRequest.java`: DTO nhận yêu cầu cập nhật kho hàng với các ràng buộc xác thực: `tenKho`, `diaChi`, `trangThai`.
  - `src/main/java/com/smartflow/smestocksensebackend/dto/response/WarehouseResponse.java`: Đóng gói dữ liệu trả về cho Frontend với các trường: `id`, `maKho`, `tenKho`, `diaChi`, `trangThai`.
- **Nghiệp vụ (Service)**:
  - `src/main/java/com/smartflow/smestocksensebackend/service/WarehouseService.java`: Định nghĩa interface dịch vụ bao gồm `getWarehouses`, `createWarehouse` và `updateWarehouse`.
  - `src/main/java/com/smartflow/smestocksensebackend/service/impl/WarehouseServiceImpl.java`: Logic tìm kiếm động, xử lý tạo mới, cập nhật kho hàng, validate không đổi mã kho, và kiểm tra tính hợp lệ của trạng thái.
- **Điều phối (Controller)**:
  - `src/main/java/com/smartflow/smestocksensebackend/controller/WarehouseController.java`: API Endpoint đón nhận request `GET /api/warehouses`, `POST /api/warehouses` và `PUT /api/warehouses/{id}`.
- **Cấu hình Bảo mật (Security)**:
  - `src/main/java/com/smartflow/smestocksensebackend/config/SecurityConfig.java`: Phân quyền cho endpoint:
    - `GET /api/warehouses`: ADMIN, MANAGER, EMPLOYEE
    - `POST /api/warehouses`: ADMIN, MANAGER
    - `PUT /api/warehouses/*`: ADMIN, MANAGER
- **Kiểm thử (Unit Test)**:
  - `src/test/java/com/smartflow/smestocksensebackend/service/impl/WarehouseServiceImplTest.java`: Bổ sung kiểm thử đơn vị cho cả luồng lấy danh sách, tạo mới và cập nhật kho hàng.

---

## 2. Hướng Dẫn Chạy & Cấu Hình
1. Cấu hình biến môi trường kết nối database trong file `.env` ở thư mục gốc của project (Sử dụng thông tin kết nối PostgreSQL của bạn):
   ```properties
   DATABASE_URL=jdbc:postgresql://<YOUR_DB_HOST>/<YOUR_DB_NAME>?sslmode=require
   DATABASE_USERNAME=<YOUR_DB_USERNAME>
   DATABASE_PASSWORD=<YOUR_DB_PASSWORD>
   ```
2. Khởi chạy ứng dụng:
   ```bash
   .\mvnw spring-boot:run
   ```

---

## 3. Cách Kiểm Thử (Testing)

- **Chạy Unit Test tự động**:
  ```bash
  .\mvnw test
  ```
- **Gọi thử nghiệm qua các API Endpoints (Postman / curl)**:
  - **Lấy danh sách toàn bộ kho**:
    - URL: `GET http://localhost:8080/api/warehouses`
  - **Tìm kiếm theo từ khóa (mã, tên hoặc địa chỉ)**:
    - URL: `GET http://localhost:8080/api/warehouses?keyword=KHO`
  - **Lọc theo trạng thái hoạt động**:
    - URL: `GET http://localhost:8080/api/warehouses?status=HOAT_DONG`
  - **Thêm mới kho hàng**:
    - URL: `POST http://localhost:8080/api/warehouses`
    - Body (JSON):
      ```json
      {
        "maKho": "KHO-01",
        "tenKho": "Kho trung tâm",
        "diaChi": "TP.HCM",
        "trangThai": "HOAT_DONG"
      }
      ```
    - Response trả về mã 201 Created cùng thông tin kho hàng vừa được tạo.
  - **Validate mã kho bắt buộc**:
    - Gửi request thiếu trường `maKho` hoặc `maKho` để trống, nhận lại lỗi 400 Bad Request kèm thông báo chi tiết lỗi xác thực của trường `maKho`.
  - **Validate mã kho không được trùng**:
    - Gửi request với `maKho` đã tồn tại trong CSDL, hệ thống trả về lỗi 400 Bad Request kèm thông báo lỗi của trường `code` (ví dụ: `{"code": "Mã kho đã tồn tại."}`).
  - **Validate tên kho bắt buộc**:
    - Gửi request thiếu trường `tenKho` hoặc `tenKho` để trống, nhận lại lỗi 400 Bad Request kèm thông báo chi tiết lỗi xác thực của trường `tenKho`.
  - **Mặc định trạng thái HOAT_DONG**:
    - Gửi request không kèm trường `trangThai` hoặc `trangThai` để trống, kho hàng mới được tạo sẽ tự động có trạng thái là `HOAT_DONG`.
  - **Cập nhật kho hàng**:
    - URL: `PUT http://localhost:8080/api/warehouses/{id}`
    - Body (JSON):
      ```json
      {
        "tenKho": "Kho trung tâm cập nhật",
        "diaChi": "TP.HCM",
        "trangThai": "HOAT_DONG"
      }
      ```
    - Response trả về mã 200 OK cùng thông tin kho hàng vừa được cập nhật.
    - Lưu ý: Mã kho (`maKho` / `code`) KHÔNG cho phép sửa để tránh ảnh hưởng dữ liệu nhập/xuất/tồn sau này.
  - **Validate tên kho bắt buộc khi cập nhật**:
    - Gửi request thiếu trường `tenKho` hoặc `tenKho` để trống, nhận lại lỗi 400 Bad Request kèm thông báo chi tiết lỗi xác thực của trường `tenKho`.
  - **Validate trạng thái chỉ nhận HOAT_DONG hoặc NGUNG_HOAT_DONG khi cập nhật**:
    - Gửi request với `trangThai` không hợp lệ (ví dụ: `TAM_DUNG`), hệ thống trả về lỗi 400 Bad Request.
  - **Cập nhật kho với ID không tồn tại**:
    - Gửi request đến ID không tồn tại trong hệ thống, hệ thống trả về lỗi 404 Not Found kèm thông báo `Kho hàng không tồn tại.`.
