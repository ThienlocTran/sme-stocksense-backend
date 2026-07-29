# Tài liệu Tổng kết Task T180: Tính toán Mức độ Nghiêm trọng & Chính sách Leo thang (Severity & Escalation Policy)

**Mã Task:** T180 (Sprint 4 - Inventory Alert System)  
**Tác giả:** Antigravity AI & User (Phiên bản Balanced Architect 10/10)  
**Dự án:** SME StockSense Backend  
**Trạng thái:** Hoàn thành (100% Build Success & Verified)

---

## 1. Mục đích và Vai trò (Purpose)
Task T180 xây dựng và tích hợp component chuyên biệt `AlertSeverityCalculator` vào luồng phát hiện cảnh báo tồn kho (`InventoryAlertDetectionServiceImpl`). Component này chịu trách nhiệm:
1. **Phân loại Mức độ Nghiêm trọng Ban đầu (Initial Severity Determination):** Xác định phiếu cảnh báo mới nên ở mức `WARNING` hay `CRITICAL` khi vừa tạo.
2. **Chính sách Leo thang Tự động (Auto-Escalation Policy):** Theo dõi diễn biến của các phiếu đang hoạt động (`OPEN`, `ACKNOWLEDGED`) trong các lần quét tiếp theo để tự động nâng cấp độ nguy cấp nếu tình trạng kho ngày càng tồi tệ hơn, đồng thời lưu vết lại trong log audit.
3. **Tuân thủ Triệt để Separation of Concerns:** Logic phân loại mức độ và leo thang được cô lập hoàn toàn trong Spring Bean `AlertSeverityCalculator`, giúp mã nguồn dễ bảo trì và kiểm thử tự động 100% không phụ thuộc DB (Ponytail Principle).

---

## 2. Logic Nghiêm vụ Chính (Core Business Logic)

### 2.1. Nguồn Chân lý Đơn nhất (Single Source of Truth - SSOT)
T180 không tự ý định nghĩa lại ngưỡng `CRITICAL`, mà tuyệt đối tuân thủ kết quả phân loại từ câu truy vấn SQL SSOT của Task T176 thông qua cột `status` trong `InventoryLevelProjection`:
- **Khẩn cấp (`CRITICAL`):** Khi số lượng thực tế `<= 0` hoặc trạng thái mặt hàng từ SSOT là `"OUT_OF_STOCK"`.
- **Cảnh báo (`WARNING`):** Khi số lượng thực tế `> 0` và trạng thái mặt hàng là `"LOW_STOCK"` (tồn dưới định mức tối thiểu `minStock`).

### 2.2. Chính sách Leo thang tự động (Auto-Escalation Policy)
Trong quá trình quét lô định kỳ (Batch Scan) hoặc kiểm tra tức thời (Spot Check), khi phát hiện một phiếu cảnh báo cũ đang hoạt động (`OPEN` hoặc `ACKNOWLEDGED`):

```text
Phiếu Cũ Đang Hoạt Động (OPEN / ACKNOWLEDGED)
                      │
                      ▼
         Kiểm tra Mức độ Nghiêm trọng
                      │
         ┌────────────┴────────────┐
         ▼                         ▼
  [Đang là WARNING]         [Đang là CRITICAL]
         │                         │
  Tồn kho mới <= 0?                ├──> Có nhập kho nhỏ lẻ phục hồi lên > 0?
         │                         │    (Nhưng vẫn dưới định mức minStock)
    ┌────┴────┐                    │
    ▼         ▼                    ▼
  [Yes]      [No]          [No De-escalation]
    │         │                    │
    ▼         ▼                    ▼
Leo thang   Giữ nguyên          Giữ nguyên
CRITICAL    WARNING              CRITICAL
 + Note
```

- **Quy tắc Leo thang (`WARNING -> CRITICAL`):** Nếu phiếu cũ ban đầu ở mức `WARNING`, nhưng do xuất kho liên tục khiến tồn kho tụt xuống cạn kiệt (`<= 0`), hệ thống sẽ:
  - Tự động nâng trường `severity` lên `CRITICAL`.
  - Bổ sung log audit vào trường `note`: ` | [Auto-Escalate] Mức độ nâng từ WARNING lên CRITICAL do tồn kho cạn kiệt (SL: 0) - yyyy-MM-dd HH:mm:ss`.
  - Cập nhật bản ghi vào cơ sở dữ liệu và ghi nhận số liệu `existingAlertsUpdated++`.
- **Quy tắc Không Hạ Cấp (No De-escalation):** Nếu một phiếu đã bị đẩy lên mức `CRITICAL`, nhưng sau đó kho có nhập thêm hàng với số lượng nhỏ (ví dụ từ `0` lên `5`, vẫn dưới định mức tối thiểu `minStock`), hệ thống **tuyệt đối KHÔNG tự ý hạ cấp** xuống `WARNING`. Lý do: đảm bảo thủ kho phải nhận thức được mức độ nghiêm trọng và xử lý nhập kho triệt để cho đến khi đạt chuẩn tối thiểu (được giải quyết bởi T183/T184).

---

## 3. Cấu trúc JSON Request và Response (API Specifications)

Vì T180 là lõi xử lý được tích hợp trực tiếp vào API phát hiện cảnh báo của T178/T179, cấu trúc giao tiếp REST API của hệ thống tiếp tục giữ vững tính thống nhất:

### 3.1. API Quét Lô (Batch Scan Endpoint)
- **Method & Path:** `POST /api/v1/inventory-alerts/scan?warehouseId={id}`
- **Request Body:** Không có (Trống).
- **JSON Response:** Trả về đối tượng DTO tóm tắt `AlertDetectionResultResponse`:
```json
{
  "totalScanned": 50,
  "newAlertsCreated": 2,
  "existingAlertsUpdated": 1,
  "existingAlertsUnchanged": 47,
  "existingAlertsSkipped": 47,
  "raceConditionIgnored": 0,
  "timestamp": "2026-07-28T21:15:00"
}
```
*(Ghi chú: Trường `existingAlertsUpdated` tăng lên `1` thể hiện có phiếu vừa được cập nhật số lượng hoặc tự động leo thang severity).*

### 3.2. API Kiểm Tra Tức Thời (Spot Check Endpoint)
- **Method & Path:** `POST /api/v1/inventory-alerts/check?productId={pid}&warehouseId={wid}`
- **Request Body:** Không có.
- **JSON Response:** Trả về giá trị `boolean` thuần túy:
  - `true`: Đã tạo phiếu mới hoặc phát hiện cần cập nhật số lượng/leo thang severity cho phiếu cũ.
  - `false`: Phiếu cũ giữ nguyên trạng thái và mức độ nghiêm trọng, không có thay đổi nào cần lưu DB.

---

## 4. Kiểm Thử và Đảm Bảo Chất Lượng (Quality Assurance & Testing)

Hệ thống được bảo chứng chất lượng với các bộ test TDD đạt chuẩn 100%:
1. **`AlertSeverityCalculatorTest`:** Kiểm thử cô lập 5 kịch bản phân loại (`LOW_STOCK -> WARNING`, `OUT_OF_STOCK -> CRITICAL`, số lượng âm) và kiểm chứng quy tắc No De-escalation khi phục hồi nhẹ.
2. **`InventoryAlertDetectionServiceTest`:** Kiểm chứng sự phối hợp mượt mà giữa Service và Calculator, đảm bảo khi quét phát hiện tụt hàng cạn kiệt, hệ thống cập nhật đúng severity và lưu chú thích leo thang vào DB.
3. **Đồng bộ Index DB:** Không làm ảnh hưởng đến cơ chế phòng thủ 2 lớp và Partial Unique Index (`idx_unique_active_alert`) của T179.
