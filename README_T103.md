# T103 - Ghi Giao Dịch Nhập Kho Sau Khi Hoàn Tất

**Mục đích**: Ghi log lịch sử biến động kho (Audit log) khi thực hiện nhập kho sau khi hoàn tất.

---

## 1. Cơ Chế Nghiệp Vụ (Business Logic)
* Kế thừa từ Task T73 (thiết kế schema giao dịch kho).
* Khi tiến hành tăng tồn kho (`increaseInventory`), hệ thống sẽ lưu vết biến động bằng cách tạo một bản ghi giao dịch kho loại `NHAP_KHO`.
* Giao dịch liên kết trực tiếp tới ID của phiếu nhập (`phieu_nhap_id`).
* Lưu trữ các thông tin chính:
  * Số lượng thay đổi (`so_luong`)
  * Số lượng tồn kho trước khi thay đổi (`so_luong_truoc`)
  * Số lượng tồn kho sau khi thay đổi (`so_luong_sau`)
  * Người thực hiện (`nguoi_tao_id` lấy từ Security Context)
  * Ghi chú đối soát: *"Kế thừa T73, track biến động kho phục vụ đối soát"*

## 2. Thiết Kế Hệ Thống & Cấu Trúc File

### 2.1 Các File Mới Tạo & Cập Nhật

* **Entity**:
  * [InventoryTransaction.java](src/main/java/com/smartflow/smestocksensebackend/entity/InventoryTransaction.java): Ánh xạ đến bảng `giao_dich_kho` trong Database.
  * [InventoryTransactionType.java](src/main/java/com/smartflow/smestocksensebackend/entity/InventoryTransactionType.java): Enum định nghĩa các loại biến động kho (`NHAP_KHO`, `XUAT_KHO`, `NHAP_DAU_KY`, `DIEU_CHINH_TANG`, `DIEU_CHINH_GIAM`).

* **Repository**:
  * [InventoryTransactionRepository.java](src/main/java/com/smartflow/smestocksensebackend/repository/InventoryTransactionRepository.java): Giao tiếp DB cho bảng `giao_dich_kho`.

* **Service**:
  * [InventoryTransactionService.java](src/main/java/com/smartflow/smestocksensebackend/service/InventoryTransactionService.java): Định nghĩa API ghi log biến động nội bộ.
  * [InventoryTransactionServiceImpl.java](src/main/java/com/smartflow/smestocksensebackend/service/impl/InventoryTransactionServiceImpl.java): Xử lý logic nghiệp vụ ghi log, tự động gán nhân viên đang đăng nhập và lưu vết.
  * [InventoryService.java](src/main/java/com/smartflow/smestocksensebackend/service/InventoryService.java): Cập nhật signature nạp chồng nhận thêm `ImportReceipt`.
  * [InventoryServiceImpl.java](src/main/java/com/smartflow/smestocksensebackend/service/impl/InventoryServiceImpl.java): Thực hiện đo lường `so_luong_truoc` và `so_luong_sau` trong Pessimistic Lock, sau đó kích hoạt ghi log giao dịch.

### 2.2 Đảm Bảo An Toàn Đồng Thời (Concurrency Safety)
* Việc đo lường số lượng trước/sau được tiến hành ngay bên trong khối đồng bộ của `increaseInventory` sử dụng **Pessimistic Write Lock** (`SELECT ... FOR UPDATE`).
* Đảm bảo các giá trị `so_luong_truoc` và `so_luong_sau` hoàn toàn chính xác ngay cả khi nhiều yêu cầu cập nhật đồng thời xảy ra trên cùng một sản phẩm ở một kho.

---

## 3. Kiểm Thử Hệ Thống (Unit Tests)
* **[InventoryTransactionServiceImplTest.java](src/test/java/com/smartflow/smestocksensebackend/service/impl/InventoryTransactionServiceImplTest.java)**: Kiểm thử độc lập khả năng ghi log giao dịch kho, kiểm tra đầy đủ các trường thông tin lưu vết, xử lý trường hợp không tìm thấy kho/sản phẩm, và xử lý khi không có nhân viên đăng nhập.
* **[InventoryServiceImplTest.java](src/test/java/com/smartflow/smestocksensebackend/service/impl/InventoryServiceImplTest.java)**: Bổ sung ca kiểm thử `increaseInventory_withImportReceipt_shouldLogTransaction` để xác nhận luồng tích hợp ghi nhận giao dịch hoạt động chuẩn xác.
