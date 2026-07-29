# Implementation Plan - Task T180: Tính Mức Độ Cảnh Báo Tồn Kho Đơn Giản & Chính Sách Leo Thang (Alert Severity Calculator)

**Mã Task:** T180 (Sprint 4 - Low Stock Alert System)  
**Tác giả:** Antigravity AI & User  
**Ngày tạo:** 2026-07-28  
**Phương pháp:** Test-Driven Development (TDD) + Ponytail Principle

---

## 1. Mục tiêu (Goal)
Hoàn thiện logic chuyên sâu cho `AlertSeverityCalculator` và tích hợp chính sách leo thang mức độ nghiêm trọng (Escalation Policy) vào `InventoryAlertDetectionServiceImpl`:
- Tự động gán `WARNING` hoặc `CRITICAL` dựa theo số lượng tồn kho thực tế và trạng thái kho.
- Tự động leo thang từ `WARNING` lên `CRITICAL` cho các phiếu đang hoạt động (`OPEN` hoặc `ACKNOWLEDGED`) nếu mặt hàng tiếp tục tụt xuống mức cạn kiệt (`currentQuantity <= 0` hoặc `OUT_OF_STOCK`), bổ sung log audit vào `note`.
- Không tự ý hạ cấp (`No De-escalation`) khi phục hồi nhẹ để bảo đảm nhân viên kho xử lý triệt để.

---

## 2. Danh sách Công việc Chi tiết (WBS & TDD Steps)

### Bước 1: Viết Unit Test cho `AlertSeverityCalculator` (TDD Red)
- **File tạo mới:** `src/test/java/com/smartflow/smestocksensebackend/service/AlertSeverityCalculatorTest.java`
- **Kịch bản kiểm thử (5 test cases 100%):**
  1. `testCalculate_WhenQtyPositiveAndLowStock_ShouldReturnWarning`
  2. `testCalculate_WhenQtyZeroOrNegative_ShouldReturnCritical`
  3. `testEvaluateAndApplyEscalation_FromWarningToCritical_ShouldEscalateAndAppendNote`
  4. `testEvaluateAndApplyEscalation_WhenAlreadyCritical_ShouldNotChange`
  5. `testEvaluateAndApplyEscalation_RecoveryEdgeCase_ShouldNotDeescalate`

### Bước 2: Hoàn thiện logic trong `AlertSeverityCalculator` (TDD Green)
- **File cập nhật:** `src/main/java/com/smartflow/smestocksensebackend/service/AlertSeverityCalculator.java`
- Bổ sung chú thích (Note) tiếng Việt rõ ràng vào từng khối logic.
- Implement hai phương thức:
  - `public InventoryAlertSeverity calculate(int currentQuantity, String status)`
  - `public boolean evaluateAndApplyEscalation(InventoryAlert existingAlert, int newQuantity, String status)`

### Bước 3: Tích hợp `AlertSeverityCalculator` vào `InventoryAlertDetectionServiceImpl`
- **File cập nhật:** `src/main/java/com/smartflow/smestocksensebackend/service/impl/InventoryAlertDetectionServiceImpl.java`
- Inject `AlertSeverityCalculator` vào Service qua `@RequiredArgsConstructor`.
- Trong `createAndSaveAlert`: Gọi `alertSeverityCalculator.calculate(...)` thay thế cho đoạn check inline cứng.
- Trong `processAlertForStock`:
  - Gọi `alertSeverityCalculator.evaluateAndApplyEscalation(existingAlert, newQty, stock.getStatus())`.
  - Nếu `newQty < oldQty` HOẶC `escalated == true` -> Thực hiện `save(existingAlert)` và trả về `UPDATED`.

### Bước 4: Cập nhật & Bổ sung Unit Test cho `InventoryAlertDetectionServiceTest`
- **File cập nhật:** `src/test/java/com/smartflow/smestocksensebackend/service/impl/InventoryAlertDetectionServiceTest.java`
- Thêm kịch bản test kiểm chứng sự leo thang tự động khi quét lại (`testScanAndCreateAlerts_WithEscalation_ShouldEscalateToCritical`).
- Chạy toàn bộ bộ test bằng lệnh `mvnw test` đảm bảo 100% pass xanh mượt mà.

### Bước 5: Tạo Tài liệu Tối thiểu cho Task T180
- **File tạo mới:** `docs/README_T180.md`
- Tóm tắt ngắn gọn bằng tiếng Việt: Chức năng API, logic nghiệp vụ leo thang, cấu trúc JSON Request/Response theo yêu cầu của user.

---
**Tiêu chí hoàn thành (DoD):** Lệnh `./mvnw clean compile test` thực thi thành công (BUILD SUCCESS), không phá vỡ bất kỳ test case nào trước đó của T178 và T179.
