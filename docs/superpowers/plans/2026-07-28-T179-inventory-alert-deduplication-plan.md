# Kế Hoạch Triển Khai (Implementation Plan) - Task T179: Chống Tạo Trùng Phiếu Cảnh Báo Mở (Deduplication Logic)

**Mã Task:** T179 (Sprint 4 - Low Stock Alert System)  
**Tác giả:** Antigravity AI & User (Phiên bản Balanced Architect 10/10)  
**Ngày tạo:** 2026-07-28  
**Trạng thái:** Hoàn thành (Completed)

---

## 1. Mục tiêu (Goal)
Hoàn thiện cơ chế **Phòng thủ 2 lớp (Dual-Layer Defense)** chống tạo trùng lặp phiếu cảnh báo tồn kho ở trạng thái `OPEN` hoặc `ACKNOWLEDGED` khi xử lý luồng Batch Scan và Spot Check. Đảm bảo Separation of Concerns theo đúng quy định 10/10: chỉ làm Deduplication & Quantity Update khi tụt kho sâu hơn, không lấn sân sang logic leo thang Severity của T180.

---

## 2. Danh sách Công việc Chi tiết (Work Breakdown Structure)

### Task 1: Tạo Flyway Migration V30 cho Partial Unique Index
Thiết lập chỉ mục duy nhất một phần tại DB PostgreSQL để bảo vệ chống Race Condition ở mức cao nhất.

**Files:**
- Create: `src/main/resources/db/migration/V30__add_unique_index_active_inventory_alert.sql`

**Interfaces:**
- Consumes: Bảng `canh_bao_ton_kho`.

- [x] Tạo file `V30__add_unique_index_active_inventory_alert.sql` chứa lệnh `CREATE UNIQUE INDEX idx_unique_active_alert ON canh_bao_ton_kho(san_pham_id, kho_id) WHERE trang_thai IN ('OPEN', 'ACKNOWLEDGED');`.

---

### Task 2: Tái cấu trúc DTO `AlertDetectionResultResponse`
Mở rộng DTO để phản ánh minh bạch 5 nhóm kết quả quét trên Dashboard thay vì gộp chung vào biến `skipped`.

**Files:**
- Modify: `src/main/java/com/smartflow/smestocksensebackend/dto/inventory/AlertDetectionResultResponse.java`

**Interfaces:**
- Consumes: `java.time.LocalDateTime`.
- Produces: Record `AlertDetectionResultResponse(totalScanned, newAlertsCreated, existingAlertsUpdated, existingAlertsUnchanged, raceConditionIgnored, timestamp)`.

- [x] Sửa đổi record thêm các thuộc tính `existingAlertsUpdated`, `existingAlertsUnchanged`, `raceConditionIgnored`.
- [x] Cập nhật các helper methods `empty()` và `of(...)` để khởi tạo DTO mới.

---

### Task 3: Nâng cấp Service Implementation (`InventoryAlertDetectionServiceImpl`)
Cập nhật luồng kiểm tra deduplication bằng `findFirstByProductIdAndWarehouseIdAndStatusIn`, bọc catch `DataIntegrityViolationException` và chú thích tiếng Việt chi tiết.

**Files:**
- Modify: `src/main/java/com/smartflow/smestocksensebackend/service/impl/InventoryAlertDetectionServiceImpl.java`

**Interfaces:**
- Consumes: `InventoryAlertRepository.findFirstByProductIdAndWarehouseIdAndStatusIn(...)`.
- Produces: Luồng scan và spot check với kiểm tra số lượng và xử lý ngoại lệ DB.

- [x] Thay thế lệnh `existsBy...` bằng `findFirstByProductIdAndWarehouseIdAndStatusIn(...)`.
- [x] Thêm logic kiểm tra: nếu đã có phiếu và `newQty < oldQty` -> `existingAlert.setCurrentQuantity(newQty)` và đếm `updated++`. Nếu `newQty >= oldQty` -> đếm `unchanged++`.
- [x] Khi tạo phiếu mới, bọc `createAndSaveAlert(...)` trong `try-catch(DataIntegrityViolationException | ObjectOptimisticLockingFailureException e)`, nếu xảy ra ngoại lệ -> ghi log và đếm `raceConditionIgnored++`.
- [x] Thêm các chú thích (Note) bằng tiếng Việt rõ ràng giải thích từng bước logic.

---

### Task 4: Cập nhật & Viết mới Kiểm thử Mockito (`InventoryAlertDetectionServiceTest`)
Xác minh toàn bộ các kịch bản: tạo mới, không đổi số lượng (`unchanged`), tụt sâu hơn (`updated`), và Race Condition (`raceConditionIgnored`).

**Files:**
- Modify: `src/test/java/com/smartflow/smestocksensebackend/service/impl/InventoryAlertDetectionServiceTest.java`

**Interfaces:**
- Consumes: `InventoryAlertDetectionServiceImpl`, Mock của `InventoryAlertRepository`.

- [x] Sửa lại test cũ phù hợp với constructor DTO mới.
- [x] Viết test `testDeduplication_WhenAlertExists_NoChangeOrIncrease_ShouldRecordUnchanged`.
- [x] Viết test `testDeduplication_WhenStockDropsFurther_ShouldUpdateQuantity`.
- [x] Viết test `testDeduplication_RaceCondition_ShouldCatchExceptionAndIgnore`.
- [x] Chạy lệnh `mvnw test -Dtest=InventoryAlertDetectionServiceTest` xác nhận **BUILD SUCCESS**.

---

### Task 5: Tài Liệu Tổng Hợp & Cập Nhật Tiến Độ
Tạo tài liệu tổng kết Task T179 và cập nhật trạng thái hệ thống.

**Files:**
- Create: `docs/README_T179.md`
- Modify: `feature_list.json`
- Modify: `progress.md`

- [x] Viết tài liệu `docs/README_T179.md` tóm tắt Service/API, cơ chế phòng thủ 2 lớp và cấu trúc DTO bằng tiếng Việt.
- [x] Cập nhật trạng thái `DONE` cho T179 trong `feature_list.json` và `progress.md`.
- [x] Chạy lại test toàn bộ hệ thống để verify lần cuối trước khi hoàn thành.
