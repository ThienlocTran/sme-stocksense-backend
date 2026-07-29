# Tài liệu Tổng kết Task T178: Service Phát Hiện Tồn Kho Thấp (Low Stock Detection Service)

**Mã Task:** T178 (Sprint 4 - Inventory Alert Foundation)  
**Tác giả:** Antigravity AI & User (Phiên bản Balanced Architect 10/10)  
**Dự án:** SME StockSense Backend  
**Trạng thái:** Hoàn thành (100% Build Success & Test Passed)

---

## 1. Mục đích và Vai trò (Purpose)
Task T178 xây dựng dịch vụ phát hiện và sinh phiếu cảnh báo tự động (`InventoryAlertDetectionService`) làm cầu nối giữa dữ liệu tồn kho thực tế (SSOT T176) và khối lưu trữ sự kiện phiếu cảnh báo (T177).
Dịch vụ này đóng vai trò là "động cơ quét" (Detection Engine), đảm bảo mọi tình trạng tụt định mức hoặc hết hàng đều được ghi nhận kịp thời thành phiếu cảnh báo `OPEN` mà không sinh ra dữ liệu trùng lặp.

---

## 2. Luồng Nghiệp vụ Chính (Flow & Separation of Concerns)

### 2.1. Hai Cơ chế Quét Linh hoạt (Batch Scan & Spot Check)
Service cung cấp 2 phương thức hoạt động bổ trợ cho nhau:
- **Quét theo lô (`scanAndCreateAlerts`):** Quét định kỳ (thông qua Cron Job ban đêm hoặc nút nhấn thủ công từ Dashboard quản lý kho). Hỗ trợ quét toàn bộ hệ thống (`warehouseId = null`) hoặc lọc theo từng kho cụ thể.
- **Kiểm tra điểm (`checkAndCreateAlert`):** Kích hoạt tức thời khi có giao dịch làm thay đổi số lượng tồn kho (ví dụ: xuất kho, bán hàng, kiểm kê lệch giảm), giúp phát hiện rủi ro ngay lập tức mà không cần chờ đến đợt quét lô tiếp theo.

### 2.2. Separation of Concerns (Chỉ CREATE, Không UPDATE/RESOLVE)
Theo chuẩn kiến trúc đã chốt (10/10), T178 tuân thủ tuyệt đối nguyên tắc đơn nhiệm:
```text
Quét tồn kho tụt định mức (LOW_STOCK / OUT_OF_STOCK)
       ↓
Kiểm tra Deduplication (Đã có phiếu OPEN / ACKNOWLEDGED chưa?)
       │
       ├── Có ──> Bỏ qua (Skip), không tạo trùng
       │
       └── Chưa ──> Khởi tạo & Lưu phiếu mới [OPEN] (Create)
```
- **Không** tự động giải quyết (Resolve) phiếu cũ khi tồn kho tăng trở lại (để dành cho T183/T184).
- **Không** tự động cập nhật lại số lượng hay mức độ cảnh báo (Severity) của các phiếu đang mở (để dành cho T180/T183).

### 2.3. Zero N+1 Query & Tối ưu Dependency (Ponytail Architecture)
- **Tận dụng SSOT:** Sử dụng duy nhất câu truy vấn chuẩn hóa từ `InventoryLevelRepository.findInventory(...)` (T176), tự động lọc bỏ mặt hàng và kho hàng ngừng hoạt động (`HOAT_DONG`).
- **Zero Extra Lookups:** Do projection đã trả về đầy đủ `productId`, `productCode`, `productName`, `warehouseId`, `warehouseCode`, `warehouseName`, Service khởi tạo Proxy Object cho `Product` và `Warehouse` để lưu khóa ngoại vào DB mà **không cần** inject `ProductRepository` hay `WarehouseRepository`.

---

## 3. Cấu trúc DTO Kết quả (Summary DTO)

Thay vì trả về danh sách toàn bộ các phiếu cảnh báo vừa sinh (gây tốn bộ nhớ khi quét lô lớn hàng nghìn mặt hàng), API/Service trả về một DTO tổng kết gọn nhẹ (`AlertDetectionResultResponse`):

```json
{
  "totalScanned": 58,
  "newAlertsCreated": 2,
  "existingAlertsSkipped": 5,
  "timestamp": "2026-07-28T19:59:02.159"
}
```

**Ý nghĩa các trường:**
- `totalScanned`: Tổng số mặt hàng bị phát hiện nằm dưới định mức tối thiểu hoặc hết hàng trong lần quét.
- `newAlertsCreated`: Số lượng phiếu cảnh báo mới (`OPEN`) thực tế được tạo và lưu vào DB.
- `existingAlertsSkipped`: Số lượng mặt hàng bị bỏ qua do đã có phiếu cảnh báo đang xử lý (`OPEN` hoặc `ACKNOWLEDGED`) từ trước.
- `timestamp`: Thời điểm hoàn tất quá trình quét.

---

## 4. Định hướng Mở rộng trong Các Task Tiếp theo (Future Tasks)
Nền tảng phát hiện T178 đã hoàn chỉnh và sẵn sàng tích hợp với các module tiếp theo trong Sprint 4:
- **T179 (Deduplication Logic):** Xây dựng Job kiểm tra chuyên sâu và làm sạch các rủi ro trùng lặp nâng cao.
- **T180 (Severity Calculation):** Chuẩn hóa công thức tính toán mức độ cảnh báo (`CRITICAL`, `WARNING`, `NOTICE`) dựa trên tỷ lệ tụt kho thực tế.
- **T181 & T182 (Alert List & Detail APIs):** Cung cấp endpoint REST API cho Frontend hiển thị danh sách phiếu cảnh báo, lọc theo trạng thái, kho hàng và xem chi tiết phiếu.
- **T183 & T184 (Manual Acknowledge & Auto-Resolve):** Xử lý quy trình nhân viên ghi nhận phiếu và cơ chế tự động đóng phiếu khi nhận hàng nạp lại vào kho.
