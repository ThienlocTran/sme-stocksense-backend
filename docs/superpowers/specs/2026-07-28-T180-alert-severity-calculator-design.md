# Kỹ thuật Đặc tả Thiết kế (Design Specification) - Task T180: Tính Mức Độ Cảnh Báo Tồn Kho Đơn Giản & Chính Sách Leo Thang (Alert Severity Calculator)

**Mã Task:** T180 (Sprint 4 - Low Stock Alert System)  
**Tác giả:** Antigravity AI & User (Phiên bản Balanced Architect 10/10)  
**Ngày tạo:** 2026-07-28  
**Trạng thái:** Chờ phê duyệt (Pending Approval - 10/10 Revision)

---

## 1. Bối cảnh & Mục tiêu Nghiệp vụ (Business Context)
Trong quản lý tồn kho SME, các mặt hàng dưới định mức tối thiểu không có mức độ nghiêm trọng giống nhau:
- **Mặt hàng chạm định mức tối thiểu (0 < tồn kho <= minStock):** Cần cảnh báo để lập kế hoạch mua sắm từ nhà cung cấp (Mức `WARNING`).
- **Mặt hàng cạn kiệt hoàn toàn (tồn kho <= 0 hoặc OUT_OF_STOCK):** Là tình trạng khẩn cấp, đe dọa trực tiếp đến hoạt động sản xuất và bán hàng, yêu cầu thủ kho và quản lý xử lý ngay lập tức (Mức `CRITICAL`).

Khi kho hàng vận hành, số lượng tồn kho liên tục biến động do các lệnh xuất kho (Spot Check) hoặc qua các chu kỳ quét (Batch Scan). Nếu một phiếu cảnh báo cũ đang ở mức `WARNING` nhưng tồn kho tiếp tục tụt xuống 0, hệ thống phải tự động **leo thang mức độ nghiêm trọng (Escalate)** lên `CRITICAL` để gây chú ý, thay vì tạo phiếu trùng lặp hoặc bỏ qua.

Task T180 xây dựng cơ chế tính toán và leo thang tự động này, tuân thủ nguyên tắc **Separation of Concerns** đã chốt từ T179: T179 lo việc nhận diện và chống trùng lặp, còn T180 lo việc tính toán và ra quyết định nâng cấp mức độ cảnh báo.

---

## 2. Các Quyết định Kiến trúc đã Chốt (Architectural Decisions 10/10)

### 2.1. Quyết định 1: Thang mức độ & Định mức phân loại (2 Mức chuẩn theo SSOT T176)
- **Kiên định với nguyên tắc Ponytail & YAGNI:** Không bổ sung các mức độ phức tạp chưa cần thiết như `NOTICE`, `LOW`, hay `OVERSTOCK`.
- Sử dụng chính xác enum `InventoryAlertSeverity` hiện có với 2 mức:
  - **`WARNING`:** Khi `currentQuantity > 0` và trạng thái không phải cạn kho.
  - **`CRITICAL`:** Khi `currentQuantity <= 0` HOẶC trạng thái kho từ SSOT T176 là `OUT_OF_STOCK`.

### 2.2. Quyết định 2: Chính sách Leo thang Tự động (Auto-Escalation Policy)
- Khi thực hiện quét lại (Batch Scan hoặc Spot Check) và tìm thấy phiếu cảnh báo cũ đang hoạt động (`OPEN` hoặc `ACKNOWLEDGED`):
  - Nếu `severity` hiện tại của phiếu là `WARNING` và mức độ mới tính toán được là `CRITICAL` (do hàng tiếp tục tụt xuống <= 0): Hệ thống **tự động leo thang (Auto-Escalate)** bằng cách cập nhật `existingAlert.setSeverity(CRITICAL)`, đồng thời thêm ghi chú vào `note` để lưu dấu vết audit.
  - Việc leo thang áp dụng cho **cả 2 trạng thái `OPEN` và `ACKNOWLEDGED`**: Đảm bảo nhân viên kho đang xử lý phiếu (`ACKNOWLEDGED`) vẫn nhận biết được mức độ khẩn cấp mới nhất của mặt hàng mà không bị ngắt quãng luồng làm việc.

### 2.3. Quyết định 3: Không Hạ cấp Tự động (No Auto De-escalation trong T180)
- **Xử lý Edge Case Phục hồi nhẹ:** Giả sử mặt hàng đang `CRITICAL` (tồn = 0), sau đó có giao dịch nhập kho nhỏ lẻ lên tồn = 5 (vẫn dưới `minStock = 10`, tức mức `WARNING`).
- Hệ thống **KHÔNG hạ cấp** (`CRITICAL -> WARNING`). Phiếu vẫn được giữ ở mức `CRITICAL` cho đến khi tồn kho được nhập vượt hẳn định mức tối thiểu (`minStock`), lúc này quy trình tự động giải quyết (Auto-Resolve) sẽ được thực hiện chuyên biệt tại **Task T183/T184**.

### 2.4. Quyết định 4: Component Chuyên biệt `AlertSeverityCalculator`
- Không viết code tính toán phân tán trong Service hay Entity. Khởi tạo một Spring Bean chuyên biệt `@Component` mang tên `AlertSeverityCalculator` nằm tại package `service`.
- Lợi ích: Tách biệt hoàn toàn logic kinh doanh (Business Rules), giúp viết Unit Test cô lập 100% với tốc độ thực thi mili-giây, dễ dàng mở rộng chính sách leo thang trong tương lai.

---

## 3. Thiết kế Chi tiết Component & Phương thức (Detailed Design)

### 3.1. Interface / Component: `AlertSeverityCalculator`
```java
package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.entity.InventoryAlert;
import com.smartflow.smestocksensebackend.entity.InventoryAlertSeverity;
import org.springframework.stereotype.Component;

@Component
public class AlertSeverityCalculator {

    /**
     * Tính toán mức độ cảnh báo dựa trên số lượng hiện tại và trạng thái từ SSOT.
     */
    public InventoryAlertSeverity calculate(int currentQuantity, String stockStatus) { ... }

    /**
     * Đánh giá và áp dụng leo thang mức độ nghiêm trọng cho phiếu cảnh báo hiện có.
     * @return true nếu đã xảy ra leo thang (WARNING -> CRITICAL), false nếu giữ nguyên.
     */
    public boolean evaluateAndApplyEscalation(InventoryAlert existingAlert, int newQuantity, String stockStatus) { ... }
}
```

### 3.2. Logic Nghiệp vụ Chi tiết
1. **`calculate(int currentQuantity, String stockStatus)`:**
   - Nếu `currentQuantity <= 0` HOẶC `"OUT_OF_STOCK".equalsIgnoreCase(stockStatus)` -> Trả về `CRITICAL`.
   - Ngược lại -> Trả về `WARNING`.

2. **`evaluateAndApplyEscalation(InventoryAlert existingAlert, int newQuantity, String stockStatus)`:**
   - Tính toán mức độ mới `newSeverity = calculate(newQuantity, stockStatus)`.
   - Kiểm tra điều kiện leo thang: Chỉ leo thang khi `oldSeverity == WARNING` VÀ `newSeverity == CRITICAL`.
   - Nếu đủ điều kiện:
     - Cập nhật: `existingAlert.setSeverity(InventoryAlertSeverity.CRITICAL);`
     - Bổ sung lịch sử vào `note`: `" | [Auto-Escalate] Mức độ nâng từ WARNING lên CRITICAL do tồn kho cạn kiệt (SL: " + newQuantity + ") - " + LocalDateTime.now()`.
     - Trả về `true`.
   - Nếu không đủ điều kiện (giữ nguyên hoặc phục hồi nhẹ): Trả về `false`.

---

## 4. Tích hợp vào Luồng Deduplication hiện có (T179 Integration)

Trong phương thức `processAlertForStock` của `InventoryAlertDetectionServiceImpl`:
- **Khi tạo phiếu mới (`createNew`):**
  - Gọi `alertSeverityCalculator.calculate(stock.getCurrentQuantity(), stock.getStatus())` để gán `severity` ban đầu.
- **Khi phát hiện phiếu cũ đã tồn tại (`handleExisting`):**
  - Kiểm tra xem tồn kho có tụt sâu hơn (`newQty < oldQty`) hoặc cần leo thang không.
  - Gọi `boolean escalated = alertSeverityCalculator.evaluateAndApplyEscalation(existingAlert, newQty, stock.getStatus());`
  - Nếu số lượng tụt sâu hơn HOẶC có xảy ra leo thang (`escalated == true`), tiến hành gọi `inventoryAlertRepository.save(existingAlert)` và trả về `UPDATED`.

---

## 5. Kế hoạch Kiểm chứng & Testing Strategy (TDD 10/10)

Bổ sung class kiểm thử cô lập `AlertSeverityCalculatorTest` bằng Mockito / JUnit 5, kiểm chứng 100% 5 kịch bản:
1. `testCalculate_WhenQtyPositiveAndLowStock_ShouldReturnWarning`: Tồn > 0 và trạng thái `LOW_STOCK` -> `WARNING`.
2. `testCalculate_WhenQtyZeroOrNegative_ShouldReturnCritical`: Tồn <= 0 hoặc trạng thái `OUT_OF_STOCK` -> `CRITICAL`.
3. `testEvaluateEscalation_FromWarningToCritical_ShouldEscalateAndAppendNote`: Phiếu cũ `WARNING`, tụt xuống 0 -> Nâng lên `CRITICAL`, bổ sung note, trả về `true`.
4. `testEvaluateEscalation_WhenAlreadyCritical_ShouldNotChange`: Phiếu cũ đã là `CRITICAL` -> Giữ nguyên, trả về `false`.
5. `testEvaluateEscalation_RecoveryEdgeCase_ShouldNotDeescalate`: Phiếu cũ là `CRITICAL`, nhập kho lên 5 (vẫn dưới `minStock`) -> KHÔNG hạ cấp xuống `WARNING`, trả về `false`.

---
**Tài liệu này đã chốt 100% theo câu trả lời trắc nghiệm của Tech Lead. Vui lòng phê duyệt (Approve) để bắt đầu bước lập kế hoạch triển khai chi tiết (Plan Mode / Writing Plans).**
