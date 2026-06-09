# Tổng Hợp Tính Năng API Warehouse (Task T40 & T41)

Tài liệu này tổng hợp ngắn gọn các thông tin cần thiết về chức năng liên quan đến quản lý kho hàng, bao gồm:
1. **T40 - Tạo API danh sách kho** (Lấy danh sách, lọc, tìm kiếm).
2. **T41 - Tạo API thêm kho** (Thêm mới kho hàng, validate mã kho không trùng).

Chi tiết giải thích từng dòng code đã được viết trực tiếp dưới dạng chú thích (inline comments) trong từng file mã nguồn.

---

## 1. Các File Được Tạo / Sửa
- **Database Migration**:
  - `src/main/resources/db/migration/V3__create_warehouses_table.sql`: Tạo bảng `warehouses` và kiểu dữ liệu enum `warehouse_status` (Từ T40).
- **Thực thể (Entity & Enum)**:
  - `src/main/java/com/smartflow/smestocksensebackend/entity/WarehouseStatus.java`: Định nghĩa trạng thái `ACTIVE` / `INACTIVE` (Từ T40).
  - `src/main/java/com/smartflow/smestocksensebackend/entity/Warehouse.java`: Thực thể JPA ánh xạ với cơ sở dữ liệu (Từ T40).
- **Truy vấn (Repository)**:
  - `src/main/java/com/smartflow/smestocksensebackend/repository/WarehouseRepository.java`: Khai báo truy vấn động và thêm phương thức kiểm tra tồn tại mã kho `existsByCodeIgnoreCase` (Sửa ở T41).
- **DTOs**:
  - `src/main/java/com/smartflow/smestocksensebackend/dto/request/CreateWarehouseRequest.java`: DTO nhận yêu cầu tạo mới kho hàng với các ràng buộc xác thực dữ liệu (Tạo mới ở T41).
  - `src/main/java/com/smartflow/smestocksensebackend/dto/response/WarehouseResponse.java`: Đóng gói dữ liệu trả về cho Frontend (Từ T40).
- **Nghiệp vụ (Service)**:
  - `src/main/java/com/smartflow/smestocksensebackend/service/WarehouseService.java`: Định nghĩa interface dịch vụ bao gồm `getWarehouses` và `createWarehouse` (Sửa ở T41).
  - `src/main/java/com/smartflow/smestocksensebackend/service/impl/WarehouseServiceImpl.java`: Logic tìm kiếm động, xử lý tạo mới kho hàng, validate mã kho không trùng và chuẩn hóa trạng thái/địa chỉ (Sửa ở T41).
- **Điều phối (Controller)**:
  - `src/main/java/com/smartflow/smestocksensebackend/controller/WarehouseController.java`: API Endpoint đón nhận request `GET /api/warehouses` và `POST /api/warehouses` (Sửa ở T41).
- **Kiểm thử (Unit Test)**:
  - `src/test/java/com/smartflow/smestocksensebackend/service/impl/WarehouseServiceImplTest.java`: Bổ sung kiểm thử đơn vị cho cả luồng lấy danh sách và tạo mới kho hàng (Sửa ở T41).

---

## 2. Hướng Dẫn Chạy & Cấu Hìnnh
1. Cấu hình biến môi trường kết nối database trong file `.env` ở thư mục gốc của project (chứa thông tin cấu hình thật, không được commit lên Git):
   ```properties
   DATABASE_URL=jdbc:postgresql://ep-young-field-aom1b92m-pooler.c-2.ap-southeast-1.aws.neon.tech/neondb?sslmode=require&channelBinding=require
   DATABASE_USERNAME=neondb_owner
   DATABASE_PASSWORD=npg_1abRrCHGiT5s
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
    - URL: `GET http://localhost:8080/api/warehouses?keyword=WH01`
  - **Lọc theo trạng thái hoạt động**:
    - URL: `GET http://localhost:8080/api/warehouses?status=ACTIVE`
  - **Thêm mới kho hàng**:
    - URL: `POST http://localhost:8080/api/warehouses`
    - Body (JSON):
      ```json
      {
        "code": "KHO-01",
        "name": "Kho trung tâm",
        "address": "Quận Bình Thạnh, TP.HCM",
        "status": "ACTIVE"
      }
      ```
    - Response trả về mã 201 Created cùng thông tin kho hàng vừa được tạo.
  - **Validate mã kho bắt buộc**:
    - Gửi request thiếu trường `code` hoặc `code` để trống, nhận lại lỗi 400 Bad Request kèm thông báo chi tiết lỗi xác thực của trường `code`.
  - **Validate mã kho không được trùng**:
    - Gửi request với `code` đã tồn tại trong CSDL, hệ thống trả về lỗi 400 Bad Request kèm thông báo `{"code": "Mã kho đã tồn tại."}`.
  - **Validate tên kho bắt buộc**:
    - Gửi request thiếu trường `name` hoặc `name` để trống, nhận lại lỗi 400 Bad Request kèm thông báo chi tiết lỗi xác thực của trường `name`.
  - **Mặc định trạng thái ACTIVE**:
    - Gửi request không kèm trường `status` hoặc `status` để trống, kho hàng mới được tạo sẽ tự động có trạng thái là `ACTIVE`.
