# Báo cáo Task T184: Hook Event Biến Động Tồn Kho & Auto-Resolve

## 1. Chức năng
Tự động hóa vòng đời Cảnh báo tồn kho thấp bằng Spring ApplicationEvent (Event-Driven Architecture). Hệ thống tự động bắt mọi biến động kho (nhập/xuất) và quyết định tạo cảnh báo hoặc đóng cảnh báo mà không cần Batch Job hay thao tác thủ công từ người dùng.

## 2. Kiến trúc & Luồng hoạt động

```
Goods Issue / Receipt
        ↓
InventoryServiceImpl (cập nhật DB)
        ↓ publishEvent(InventoryLevelChangedEvent)
ApplicationEventPublisher
        ↓ [AFTER_COMMIT + @Async — chạy ở thread nền]
InventoryAlertEventListener
        ↓ processInventoryChange(event)
InventoryAlertDetectionServiceImpl
        ├── newQty <= minStock  → CREATE hoặc UPDATE cảnh báo
        └── newQty >  minStock  → AUTO-RESOLVE cảnh báo
```

**Điểm quan trọng:**
- `AFTER_COMMIT`: Cảnh báo chỉ được xử lý sau khi giao dịch kho đã commit thành công. Rollback kho → không sinh cảnh báo sai.
- `@Async`: Listener chạy nền, không làm chậm luồng xuất/nhập kho của người dùng.

## 3. Business Logic chính

| Điều kiện | Hành động |
|-----------|-----------|
| `newQty <= minStock` và đã có cảnh báo `OPEN`/`ACKNOWLEDGED` | Cập nhật `currentQuantity` (Deduplication - không tạo thêm) |
| `newQty <= minStock` và chưa có cảnh báo | Tạo mới cảnh báo `OPEN` |
| `newQty > minStock` và có cảnh báo `OPEN`/`ACKNOWLEDGED` | Auto-Resolve → `RESOLVED`, `handledBy = "System"` |
| `newQty > minStock` và không có cảnh báo | Bỏ qua (no-op) |

## 4. Event Contract

```java
public record InventoryLevelChangedEvent(
    Long warehouseId,
    Long productId,
    Integer oldQuantity,
    Integer newQuantity,
    Integer minStock
) {}
```

## 5. Các file thay đổi / tạo mới

| File | Vai trò |
|------|---------|
| `event/InventoryLevelChangedEvent.java` | Event record với đầy đủ payload |
| `event/InventoryAlertEventListener.java` | Listener (`@Async` + `AFTER_COMMIT`) |
| `service/impl/InventoryAlertDetectionServiceImpl.java` | Thêm `processInventoryChange()` + Auto-Resolve logic |
| `service/impl/InventoryServiceImpl.java` | Thêm `publishEvent()` sau khi cập nhật tồn kho |
| `SmeStocksenseBackendApplication.java` | Bật `@EnableAsync` |

## 6. Out of Scope
- Không gửi Email/Notification.
- Không xử lý Distributed Event (Kafka, RabbitMQ).
- Không implement Retry Queue khi Async lỗi.
- Concurrent Duplicate nằm ngoài scope — đã có DB Unique Constraint từ T179 làm lưới an toàn.
