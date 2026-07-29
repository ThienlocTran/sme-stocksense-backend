# T184 Design Spec: Gắn Hook Event Biến Động Tồn Kho & Auto-Resolve

## 1. Mục tiêu (Business Goal)
Tự động hóa hoàn toàn vòng đời của Cảnh báo tồn kho thấp (Low Stock Alert). Hệ thống sẽ tự động bắt lấy các biến động kho (nhập, xuất, kiểm kê) theo thời gian thực để:
- Tạo mới cảnh báo nếu kho bắt đầu thiếu hụt.
- Cập nhật số lượng nếu kho tiếp tục biến động trong ngưỡng thiếu.
- Tự động đóng (Auto-Resolve) cảnh báo nếu kho được bổ sung đủ hàng.

## 2. Phương pháp kỹ thuật (Architecture & Mechanisms)

### 2.1. Decoupling bằng Spring Event
```
Goods Issue / Goods Receipt
        ↓
InventoryService (cập nhật tồn kho)
        ↓
applicationEventPublisher.publishEvent(...)
        ↓
InventoryAlertEventListener (AFTER_COMMIT + @Async)
        ↓
InventoryAlertDetectionService.processInventoryChange(...)
        ↓
CREATE / UPDATE / RESOLVE
```
- `InventoryService` không biết và không phụ thuộc vào module Cảnh báo (Loose Coupling).

### 2.2. Event Contract (Hợp đồng sự kiện)
Định nghĩa rõ payload của event để developer không phải đoán:
```java
public record InventoryLevelChangedEvent(
    Long warehouseId,
    Long productId,
    Integer oldQuantity,
    Integer newQuantity,
    Integer minStock
) {}
```

### 2.3. Xử lý Bất đồng bộ (Async & Transactional Event)
- Event Listener sử dụng `@TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)`.
- Kết hợp `@Async`.
- **Lý do**: Chỉ chạy sau khi giao dịch kho ghi nhận thành công. Chạy ở thread nền, không làm chậm thao tác kho chính. Lỗi xử lý cảnh báo (nếu có) **KHÔNG được phép rollback giao dịch kho**.

## 3. Business Logic & Điều kiện kích hoạt

> **Quy tắc đối xứng (phải nhất quán):**
> - Điều kiện thiếu hụt: `currentQuantity <= minStock` → tạo/cập nhật cảnh báo.
> - Điều kiện an toàn: `currentQuantity > minStock` → Auto-Resolve.
>
> *Trường hợp biên: `quantity = minStock = 10` → vẫn coi là thiếu hụt (vì dùng `<=`).*

### 3.1. Khi tồn kho thiếu hụt (`newQuantity <= minStock`)
- Nếu **đã có** cảnh báo `OPEN`/`ACKNOWLEDGED` → Cập nhật `currentQuantity` (không tạo mới, tránh duplicate).
- Nếu **chưa có** → Tạo mới cảnh báo `OPEN`.

### 3.2. Khi tồn kho an toàn (`newQuantity > minStock`) — Auto-Resolve
- Tìm cảnh báo đang ở trạng thái `OPEN` hoặc `ACKNOWLEDGED` của `(productId, warehouseId)`.
- Nếu **có** → Cập nhật trạng thái thành `RESOLVED`, `handledBy` = `"System"` (chưa đổi schema, dùng "System" để phân biệt auto vs user).
- Nếu **không có** → Bỏ qua.

## 4. Acceptance Criteria (Tiêu chí nghiệm thu)

1. **Sinh cảnh báo tự động**: Xuất kho làm tồn giảm xuống `<= minStock` → Hệ thống tự sinh 1 cảnh báo `OPEN`. Giao dịch xuất kho vẫn thành công và **không bị delay**.
2. **Cập nhật số lượng**: Đã có cảnh báo `OPEN` (tồn = 5). Xuất thêm 2 → Cảnh báo tự cập nhật `currentQuantity` xuống 3. **Không tạo cảnh báo mới thứ hai**.
3. **Không duplicate**: Listener chạy nhiều lần cho cùng `(productId, warehouseId)` khi đang `OPEN` → Hệ thống chỉ giữ **1 cảnh báo** (cập nhật, không tạo thêm).
4. **Auto-Resolve**: Tồn kho đang `3`, `minStock = 10` (cảnh báo `ACKNOWLEDGED`). Nhập thêm 20 → Tồn = 23 (`> 10`) → Cảnh báo tự chuyển `RESOLVED`, `handledBy = "System"`.
5. **Không lỗi chéo**: Luồng ghi cảnh báo lỗi (VD: exception trong listener) → Giao dịch nhập/xuất kho chính **vẫn thành công**, không rollback.

## 5. Coding Plan

- **Bước 1**: Tạo `InventoryLevelChangedEvent` record với đầy đủ payload theo Event Contract ở mục 2.2.
- **Bước 2**: Publish event sau khi InventoryLevel đã được cập nhật thành công trong `InventoryServiceImpl`.
- **Bước 3**: Tạo `InventoryAlertEventListener` với `@TransactionalEventListener(phase = AFTER_COMMIT)` + `@Async`. Listener chỉ gọi `InventoryAlertDetectionService.processInventoryChange(event)`, **không chứa business logic**.
- **Bước 4**: Cập nhật `InventoryAlertDetectionService` (và impl) thêm hàm `processInventoryChange(...)` xử lý logic CREATE / UPDATE / RESOLVE. Kiểm tra `@EnableAsync` đã được cấu hình chưa.
- **Bước 5**: Viết Unit Test cho service (create, update, resolve, no-op) và Unit Test cho listener (verify gọi đúng service).

## 6. Out of Scope

- Không gửi Email / Notification.
- Không xử lý Distributed Event (Kafka, RabbitMQ).
- Không implement Retry Queue khi Async lỗi.
- Không xử lý Concurrent Duplicate (race condition giữa 2 thread cùng tạo alert) — đã có DB Unique Constraint từ T179 làm lưới an toàn cuối cùng.
