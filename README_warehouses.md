# Tổng Hợp Tính Năng API Warehouse (Task T40)

Tài liệu này tổng hợp ngắn gọn các thông tin cần thiết về chức năng lấy danh sách kho hàng (**T40 - Tạo API danh sách kho**). Chi tiết giải thích từng dòng code đã được viết trực tiếp dưới dạng chú thích (inline comments) trong từng file mã nguồn.

---

## 1. Các File Được Tạo / Sửa
- **Database Migration**:
  - `src/main/resources/db/migration/V3__create_warehouses_table.sql`: Tạo bảng `warehouses` và kiểu dữ liệu enum `warehouse_status`.
- **Thực thể (Entity & Enum)**:
  - `src/main/java/com/smartflow/smestocksensebackend/entity/WarehouseStatus.java`: Định nghĩa trạng thái `ACTIVE` / `INACTIVE`.
  - `src/main/java/com/smartflow/smestocksensebackend/entity/Warehouse.java`: Thực thể JPA ánh xạ với cơ sở dữ liệu.
- **Truy vấn (Repository)**:
  - `src/main/java/com/smartflow/smestocksensebackend/repository/WarehouseRepository.java`: Thực hiện truy vấn và lọc dữ liệu.
- **DTO Response**:
  - `src/main/java/com/smartflow/smestocksensebackend/dto/response/WarehouseResponse.java`: Đóng gói dữ liệu trả về cho Frontend.
- **Nghiệp vụ (Service)**:
  - `src/main/java/com/smartflow/smestocksensebackend/service/WarehouseService.java`: Định nghĩa interface dịch vụ.
  - `src/main/java/com/smartflow/smestocksensebackend/service/impl/WarehouseServiceImpl.java`: Logic tìm kiếm động (Specification) và validate tham số đầu vào.
- **Điều phối (Controller)**:
  - `src/main/java/com/smartflow/smestocksensebackend/controller/WarehouseController.java`: API Endpoint đón nhận request `GET /api/warehouses`.
- **Kiểm thử (Unit Test)**:
  - `src/test/java/com/smartflow/smestocksensebackend/service/impl/WarehouseServiceImplTest.java`: Kiểm thử đơn vị cho tầng Service.

---

## 2. Hướng Dẫn Chạy & Cấu Hình
1. Cấu hình biến môi trường kết nối database trong file `.env` ở thư mục gốc của project:
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
- **Gọi thử nghiệm qua các API Endpoints (Postman / Trình duyệt)**:
  - Lấy danh sách toàn bộ kho: `GET http://localhost:8080/api/warehouses`
  - Lọc theo từ khóa (mã, tên hoặc địa chỉ): `GET http://localhost:8080/api/warehouses?keyword=WH01`
  - Lọc theo trạng thái hoạt động: `GET http://localhost:8080/api/warehouses?status=ACTIVE`
  - Test ném lỗi trạng thái không hợp lệ: `GET http://localhost:8080/api/warehouses?status=INVALID_STATUS`
