# Kỹ thuật Đặc tả Thiết kế (Design Specification) - Task T179: Chống Tạo Trùng Phiếu Cảnh Báo Mở (Deduplication Logic)

**Mã Task:** T179 (Sprint 4 - Low Stock Alert System)  
**Tác giả:** Antigravity AI & User (Phiên bản Balanced Architect 10/10)  
**Ngày tạo:** 2026-07-28  
**Trạng thái:** Chờ phê duyệt (Pending Approval - 10/10 Revision)

---

## 1. Bối cảnh & Mục tiêu Nghiệp vụ (Business Context)
Trong hệ thống quản lý kho thực tế, tình trạng tụt định mức có thể phát sinh từ nhiều nguồn đồng thời:
- Quét định kỳ theo lô (Batch Scan Cron Job ban đêm hoặc khi quản lý kho bấm nút quét toàn hệ thống).
- Giao dịch kiểm tra điểm (Spot Check) phát sinh liên tục khi nhân viên kho lập phiếu xuất kho, bán hàng, hoặc điều chỉnh kiểm kê giảm.

Nếu không có cơ chế chống trùng lặp (Deduplication) chặt chẽ và phòng thủ nhiều tầng, hệ thống sẽ phải đối mặt với hai rủi ro nghiêm trọng:
1. **Rác dữ liệu & Báo động giả (Spam Alerts):** Một mặt hàng tụt kho bị sinh ra hàng chục phiếu cảnh báo `OPEN` giống hệt nhau, khiến quản lý kho bối rối và làm sai lệch thống kê Dashboard.
2. **Xung đột truy cập đồng thời (Race Condition):** Hai giao dịch xuất kho xảy ra tại cùng một mili-giây cùng kiểm tra thấy chưa có phiếu -> cả hai cùng chèn phiếu mới vào DB.

Task T179 thiết lập một cơ chế **Phòng thủ 2 lớp (Dual-Layer Defense)** kết hợp giữa tầng Application và chỉ mục duy nhất một phần (Partial Unique Index) tại tầng Database, bảo đảm 100% không tạo rác dữ liệu.

---

## 2. Các Quyết định Kiến trúc đã Chốt (Architectural Decisions 10/10)

### 2.1. Quyết định 1: Phòng thủ 2 lớp (Application Check + DB Partial Unique Index)
- **Tầng Application:** Trước khi tạo phiếu, Service gọi truy vấn `findFirstByProductIdAndWarehouseIdAndStatusIn(productId, warehouseId, List.of(OPEN, ACKNOWLEDGED))` từ `InventoryAlertRepository` để kiểm tra phiếu đang xử lý.
- **Tầng Database (Thắt lưng buộc bụng tuyệt đối):** Tạo migration `V30__add_unique_index_active_inventory_alert.sql` bổ sung chỉ mục:
  ```sql
  CREATE UNIQUE INDEX idx_unique_active_alert 
  ON canh_bao_ton_kho(san_pham_id, kho_id) 
  WHERE trang_thai IN ('OPEN', 'ACKNOWLEDGED');
  ```
- **Xử lý Ngoại lệ Chuyên sâu:** Khi xảy ra Race Condition (2 thread cùng lọt qua bước check và cùng gọi `save()`), giao dịch thứ hai sẽ bị DB từ chối. Service bắt chính xác ngoại lệ `DataIntegrityViolationException` (tuyệt đối không catch `Exception` chung để tránh che giấu các lỗi hệ thống khác), ghi log thông báo nhẹ và cộng vào biến đếm `raceConditionIgnored`, đảm bảo không bao giờ sập luồng Batch Scan.

### 2.2. Quyết định 2: Separation of Concerns - Chỉ Orchestrate Deduplication & Quantity Update (Không lấn sân T180)
Để tránh chồng chéo trách nhiệm giữa các task trong Sprint 4:
- **T179 (Deduplication):** Chỉ tập trung trả lời câu hỏi: *“Đã có phiếu chưa? Nếu có rồi và số lượng tụt sâu hơn trước (`newQty < oldQty`), thì cập nhật `currentQuantity` mới nhất vào phiếu cũ.”* Tuyệt đối **không** thực hiện cộng gộp ghi chú vô hạn (`append note`) để tránh bùng nổ dữ liệu trường text sau nhiều tháng vận hành. Nếu sau này cần audit trail, sẽ phát triển bảng `inventory_alert_history` độc lập.
- **T180 (Severity):** Toàn bộ luật tính toán hoặc leo thang mức độ cảnh báo (`WARNING -> CRITICAL`, `NOTICE -> WARNING`, v.v.) được tách bạch và giao trọn vẹn cho `AlertSeverityCalculator` / `InventoryAlertEscalationPolicy` ở Task T180.

### 2.3. Quyết định 3: Phạm vi Trạng thái Trùng lặp (Duplicate State Boundary)
- Chỉ cản trở tạo mới khi phiếu cũ đang ở trạng thái **`OPEN`** (Mở) hoặc **`ACKNOWLEDGED`** (Đã ghi nhận, đang xử lý).
- Nếu phiếu cũ đã chuyển sang **`RESOLVED`** (đã nhập bù hàng xong), vòng đời phiếu cũ đã kết thúc. Nếu sau đó kho lại tiếp tục xuất hàng làm tụt định mức, hệ thống cho phép sinh một phiếu `OPEN` mới hoàn toàn cho vòng đời mới.

---

## 3. Cải tiến DTO Tổng kết (`AlertDetectionResultResponse`)
Để Dashboard và log hệ thống phản ánh đúng bản chất của từng tình huống xử lý (không bị nhập nhằng trong một biến `skipped` chung chung), DTO được tái cấu trúc minh bạch thành 6 thuộc tính:

```java
public record AlertDetectionResultResponse(
        int totalScanned,
        int newAlertsCreated,
        int existingAlertsUpdated,   // Phiếu cũ được cập nhật do số lượng tụt sâu hơn (newQty < oldQty)
        int existingAlertsUnchanged, // Phiếu cũ giữ nguyên do số lượng không đổi hoặc tăng nhẹ (phục hồi)
        int raceConditionIgnored,    // Trường hợp bị DB từ chối do Race Condition (DataIntegrityViolationException)
        LocalDateTime timestamp
)
```

Ví dụ log sau đợt quét lô ban đêm:
```text
Scan completed: Scanned=50, Created=2, Updated=3, Unchanged=44, RaceIgnored=1
```

---

## 4. Thiết kế Luồng Xử lý (Processing Flow)

```text
Quét mặt hàng LOW_STOCK / OUT_OF_STOCK (SSOT T176)
                      ↓
Tìm phiếu cũ: findFirstByProductIdAndWarehouseIdAndStatusIn(..., OPEN, ACKNOWLEDGED)
                      │
        ┌─────────────┴─────────────┐
        ▼                           ▼
   [Tồn tại Phiếu]            [Chưa Có Phiếu]
        │                           │
  Kiểm tra số lượng:                ├──> Gọi save(newAlert)
  newQty < oldQty?                  │          │
        │                           │          ├─(Thành công)─> newAlertsCreated++
   ┌────┴────┐                      │          │
   ▼         ▼                      │          └─(DataIntegrityViolationException)
 [Yes]      [No]                    │                                   │
   │         │                      │                                   ▼
   ▼         ▼                      ▼                         raceConditionIgnored++
Update DB  Skip ────────────────────┘
   │         │
   ▼         ▼
updated++ unchanged++
```

---

## 5. Phân tích Các Trường hợp Biên (Edge Cases & Resilience)
1. **Trường hợp Phục hồi nhẹ (`newQty >= oldQty`):**
   - Ví dụ: Phiếu `OPEN` đang ghi nhận tồn kho = 5. Kho nhập bù thêm 3 chiếc -> tồn kho mới là 8 (vẫn dưới minStock = 20).
   - **Xử lý:** Không cập nhật số lượng `5 -> 8` (vì đây là chiều phục hồi, thuộc phạm vi giải quyết của T183/T184). Hệ thống xếp vào nhóm `existingAlertsUnchanged`.
2. **Trường hợp Phiếu đang ở trạng thái `ACKNOWLEDGED`:**
   - Nếu tồn kho tiếp tục tụt khi nhân viên đang đi mua hàng bù (`newQty < oldQty`), hệ thống vẫn cập nhật `currentQuantity` mới nhất để nhân viên nắm được con số thực tế.
3. **Trường hợp Xung đột Khóa Lạc quan (`@Version`):**
   - Khi cập nhật số lượng cho phiếu cũ, nếu có nhân viên kho vừa bấm Acknowledge cùng lúc, JPA ném `ObjectOptimisticLockingFailureException`. Service bắt ngoại lệ này, ghi log cảnh báo và chuyển vào nhóm `raceConditionIgnored`, không làm gián đoạn luồng quét.

---

## 6. Tiêu chí Nghiệm thu (Verification Criteria)
- **V30 Migration:** Tạo bảng thành công index unique một phần trên `(san_pham_id, kho_id)` với điều kiện `trang_thai IN ('OPEN', 'ACKNOWLEDGED')`.
- **Unit Test 1 (`testDeduplication_WhenAlertExists_NoChangeOrIncrease_ShouldRecordUnchanged`):** Khi tồn kho bằng hoặc lớn hơn số cũ, giữ nguyên phiếu và đếm `unchanged`.
- **Unit Test 2 (`testDeduplication_WhenStockDropsFurther_ShouldUpdateQuantity`):** Khi tồn kho tụt sâu hơn (10 -> 4), cập nhật `currentQuantity = 4` và đếm `updated`.
- **Unit Test 3 (`testDeduplication_RaceCondition_ShouldCatchDataIntegrityViolationAndIgnore`):** Giả lập `save()` ném `DataIntegrityViolationException`, xác minh bắt đúng lỗi và đếm `raceConditionIgnored`.
