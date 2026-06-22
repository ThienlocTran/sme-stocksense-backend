# T102: SERVICE TĂNG TỒN KHO KHI HOÀN TẤT PHIẾU NHẬP

Tài liệu này tổng hợp thiết kế, cấu trúc mã nguồn, quy tắc nghiệp vụ và hướng dẫn kiểm thử cho **Core Service tăng số lượng tồn kho** nội bộ.

---

## 1. Giới thiệu chung (Overview)
Tính năng T102 cung cấp một Service nội bộ (`InventoryService`) có nhiệm vụ cộng dồn số lượng thực tế nhận được của sản phẩm vào bảng tồn kho (`ton_kho`) tại một kho cụ thể sau khi phiếu nhập kho hoàn tất kiểm đếm và được duyệt hoàn thành.

## 2. Cấu trúc Cơ sở dữ liệu liên quan (Database Schema)
Thao tác trực tiếp trên bảng `ton_kho`:
- `id` (BIGINT, PK): Khóa chính tự tăng.
- `san_pham_id` (BIGINT, FK): Tham chiếu tới sản phẩm (`san_pham`).
- `kho_id` (BIGINT, FK): Tham chiếu tới kho hàng (`kho`).
- `so_luong` (INT): Số lượng tồn kho hiện tại (mặc định: 0).
- `ngay_cap_nhat` (TIMESTAMP): Thời gian cập nhật tồn kho lần cuối.

> [!NOTE]
> Bảng `ton_kho` có Unique Index trên cặp `(san_pham_id, kho_id)` để đảm bảo mỗi sản phẩm chỉ có duy nhất một dòng tồn kho tại mỗi kho hàng.

---

## 3. Quy tắc Nghiệp vụ (Business Rules)
1. **Kiểm tra hợp lệ**:
   - Sản phẩm (`san_pham_id`) phải tồn tại trong hệ thống. Nếu không -> Ném `NotFoundException("Sản phẩm không tồn tại.")` để rollback transaction.
   - Kho hàng (`kho_id`) phải tồn tại trong hệ thống. Nếu không -> Ném `NotFoundException("Kho hàng không tồn tại.")` để rollback transaction.
2. **Logic cộng dồn**:
   - **Trường hợp chưa có dòng tồn kho** (chưa từng nhập sản phẩm này vào kho này): Hệ thống thực hiện **Insert** dòng mới với số lượng bằng số lượng thực nhận (`quantity`).
   - **Trường hợp đã có dòng tồn kho**: Hệ thống thực hiện **Update** cộng dồn số lượng thực nhận vào số lượng hiện tại (`existingQuantity + quantity`).

---

## 4. Cấu trúc các file mã nguồn triển khai (Code Structure)
- **Entity**:
  - [InventoryLevel.java](src/main/java/com/smartflow/smestocksensebackend/entity/InventoryLevel.java): Thực thể JPA ánh xạ bảng `ton_kho`.
- **Repository**:
  - [InventoryLevelRepository.java](src/main/java/com/smartflow/smestocksensebackend/repository/InventoryLevelRepository.java): Khai báo các phương thức truy vấn Spring Data JPA, bao gồm tìm kiếm theo sản phẩm và kho hàng.
- **Service**:
  - [InventoryService.java](src/main/java/com/smartflow/smestocksensebackend/service/InventoryService.java): Định nghĩa interface cho Core Service tăng tồn kho.
  - [InventoryServiceImpl.java](src/main/java/com/smartflow/smestocksensebackend/service/impl/InventoryServiceImpl.java): Triển khai chi tiết logic nghiệp vụ, tích hợp `@Transactional` để đảm bảo tính toàn vẹn dữ liệu.
- **Unit Test**:
  - [InventoryServiceImplTest.java](src/test/java/com/smartflow/smestocksensebackend/service/impl/InventoryServiceImplTest.java): Kiểm thử các trường hợp thành công (tạo mới/cộng dồn) và ngoại lệ (sản phẩm/kho không tồn tại).

---

## 5. Hướng dẫn chạy kiểm thử (Testing)
Chạy riêng các test case cho Service Tồn kho:
```bash
./mvnw test -Dtest=InventoryServiceImplTest
```
Chạy toàn bộ test suite:
```bash
./mvnw test
```

> [!NOTE]
> **Chỉ dùng khi kết nối DB Neon thật bị lệch checksum Flyway**: Nếu Flyway báo lỗi `validate-on-migrate`, thêm cờ sau làm giải pháp tạm thời:
> ```bash
> ./mvnw test "-Dspring.flyway.validate-on-migrate=false"
> ```
> Không nên dùng cờ này trong môi trường CI/CD chính thức.
