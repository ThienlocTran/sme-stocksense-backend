# Tài liệu Tổng kết Task T179: Chống Tạo Trùng Phiếu Cảnh Báo Mở (Deduplication Logic)

**Mã Task:** T179 (Sprint 4 - Inventory Alert System)  
**Tác giả:** Antigravity AI & User (Phiên bản Balanced Architect 10/10)  
**Dự án:** SME StockSense Backend  
**Trạng thái:** Hoàn thành (100% Build Success & 5/5 Tests Passed)

---

## 1. Mục đích và Vai trò (Purpose)
Task T179 nâng cấp dịch vụ `InventoryAlertDetectionService` (từ T178) với cơ chế **Phòng thủ 2 lớp (Dual-Layer Defense)** chống tạo trùng lặp phiếu cảnh báo tồn kho ở trạng thái `OPEN` hoặc `ACKNOWLEDGED`.
Cơ chế này loại bỏ hoàn toàn hiện tượng rác dữ liệu (Spam Alerts) do Cron Job quét lô lặp đi lặp lại hoặc xung đột truy cập đồng thời (Race Condition) từ các giao dịch xuất kho liên tục, đồng thời tuân thủ tuyệt đối nguyên tắc **Separation of Concerns** đã chốt với Tech Lead.

---

## 2. Kiến trúc Phòng thủ 2 Lớp (Dual-Layer Defense Architecture)

### 2.1. Lớp 1: Tầng Application (Service Deduplication & Quantity Update)
Trước khi quyết định tạo phiếu mới cho một mặt hàng tụt định mức (`LOW_STOCK`), Service thực hiện kiểm tra trạng thái hoạt động:
```text
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
- **Separation of Concerns:** T179 chỉ chịu trách nhiệm điều phối Deduplication và cập nhật số lượng `currentQuantity` khi mặt hàng bị tụt kho sâu hơn trước (`newQty < oldQty`). Toàn bộ luật tính toán hoặc leo thang mức độ cảnh báo (`WARNING -> CRITICAL`) được giữ nguyên không chạm tới để giao trọn vẹn cho `AlertSeverityCalculator` ở **Task T180**.
- **Không cộng gộp Ghi chú vô hạn:** Loại bỏ ý tưởng nối chuỗi ghi chú (`append note`) để tránh phình to dữ liệu trường text sau nhiều tháng vận hành.

### 2.2. Lớp 2: Tầng Database (Partial Unique Index V30)
Để phòng ngừa tuyệt đối các tình huống Race Condition (ví dụ 2 nhân viên kho cùng xuất hàng tại cùng 1 mili-giây, cùng vượt qua lệnh check ở tầng Application), hệ thống thiết lập migration **V30** (`V30__add_unique_index_active_inventory_alert.sql`):
```sql
CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_alert 
ON canh_bao_ton_kho (san_pham_id, kho_id) 
WHERE trang_thai IN ('OPEN', 'ACKNOWLEDGED');
```
- **Phạm vi bảo vệ:** Chỉ mục duy nhất một phần (Partial Unique Index) chỉ khóa đối với phiếu đang hoạt động (`OPEN`, `ACKNOWLEDGED`). Khi phiếu cũ được giải quyết nhập đủ hàng (`RESOLVED`), chỉ mục tự động giải phóng, cho phép sinh phiếu `OPEN` mới cho chu kỳ tiếp theo.
- **Xử lý Ngoại lệ Chuyên sâu:** Service bắt chính xác ngoại lệ `DataIntegrityViolationException` và `ObjectOptimisticLockingFailureException` (không dùng catch chung chung), chuyển vào nhóm `raceConditionIgnored` và ghi log cảnh báo, đảm bảo luồng quét theo lô không bao giờ bị gián đoạn.

---

## 3. Cấu trúc DTO Kết quả SIÊU MINH BẠCH (`AlertDetectionResultResponse`)

DTO kết quả được chia nhỏ thành 6 thuộc tính rõ ràng, giúp Dashboard và log hệ thống phản ánh trung thực bản chất từng xử lý:

```json
{
  "totalScanned": 50,
  "newAlertsCreated": 2,
  "existingAlertsUpdated": 3,
  "existingAlertsUnchanged": 44,
  "raceConditionIgnored": 1,
  "timestamp": "2026-07-28T20:19:50.556"
}
```

**Ý nghĩa các trường:**
- `totalScanned`: Tổng số mặt hàng dưới định mức được quét.
- `newAlertsCreated`: Số phiếu `OPEN` mới hoàn toàn được tạo.
- `existingAlertsUpdated`: Số phiếu cũ được cập nhật lại `currentQuantity` do kho tụt sâu hơn (`newQty < oldQty`).
- `existingAlertsUnchanged`: Số phiếu cũ giữ nguyên do số lượng không đổi hoặc có chiều hướng phục hồi nhẹ.
- `raceConditionIgnored`: Số kịch bản bị DB cản lại do xung đột Race Condition hoặc khóa lạc quan (@Version).
- `timestamp`: Thời điểm hoàn tất lệnh quét.

---

## 4. Kiểm chứng Chất lượng (Verification & Testing)
Bộ test `InventoryAlertDetectionServiceTest` đã được mở rộng kiểm chứng toàn diện 5 kịch bản và đạt kết quả 100%:
1. `testScanAndCreateAlerts_WithNewAndExistingAlerts`: Kiểm chứng vừa tạo phiếu mới vừa nhận diện đúng phiếu cũ giữ nguyên (`unchanged`).
2. `testDeduplication_WhenStockDropsFurther_ShouldUpdateQuantity`: Kiểm chứng tự động cập nhật `currentQuantity` khi số lượng tụt từ 15 xuống 4 (`updated`).
3. `testDeduplication_RaceCondition_ShouldCatchExceptionAndIgnore`: Giả lập DB ném `DataIntegrityViolationException`, xác minh catch thành công và đếm `raceConditionIgnored`.
4. `testScan_NoLowStock_ReturnEmptyResult`: Kiểm chứng trả về rỗng `(0, 0, 0, 0, 0)` khi kho an toàn.
5. `testCheckAndCreateAlert_LowStockAndNormal`: Kiểm chứng luồng Spot Check cho 3 tình huống (tụt kho tạo mới, kho bình thường, tụt kho nhưng trùng lặp không đổi).
