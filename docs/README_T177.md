# Tài liệu Tổng kết Task T177: Thiết kế Bảng và Khối Lõi Phiếu Cảnh Báo Tồn Kho (Inventory Alert Foundation)

**Mã Task:** T177 (Sprint 4 - Inventory Alert Foundation)  
**Tác giả:** Antigravity AI & User (Phiên bản Balanced Architect 10/10)  
**Dự án:** SME StockSense Backend  
**Trạng thái:** Hoàn thành (100% Build Success & Test Passed)

---

## 1. Chức năng và Vai trò của Khối Lõi (Foundation)
Task T177 xây dựng toàn bộ nền tảng dữ liệu (Database Schema, Entity, Enums, Repository) cho hệ thống **Phiếu Cảnh báo Tồn kho (`inventory_alert`)**. 
Thay vì chỉ cảnh báo thụ động qua câu truy vấn tức thời, hệ thống tạo ra một **lưu trữ sự kiện cố định (Alert Snapshot & Audit Trail)** giúp:
- **Theo dõi vòng đời xử lý rủi ro:** Ghi nhận từ lúc phát hiện tụt kho đến lúc nhân viên lên đơn và khi kho được nạp lại đủ định mức.
- **Duy trì lịch sử kiểm toán:** Không mất dấu vết cảnh báo ngay cả khi định mức tồn kho (`minStock`, `maxStock`) bị thay đổi trong tương lai.
- **Tạo nền tảng cho Dashboard KPI:** Hỗ trợ thống kê nhanh số lượng cảnh báo đang mở, nghiêm trọng hoặc đã xử lý cho các API T181 - T184.

---

## 2. Logic Nghiệp vụ Chính (Balanced Architect 10/10)

### 2.1. Cơ chế Snapshot Định mức (Audit-Proof)
Khi phiếu cảnh báo được sinh ra, hệ thống lưu ngay giá trị snapshot tại thời điểm phát sinh:
- `current_quantity`: Số lượng tồn kho thực tế lúc xảy ra cảnh báo (cho phép giá trị âm nếu xuất kho vượt mức hoặc lệch kiểm kê).
- `min_stock` & `max_stock`: Định mức tối thiểu và tối đa tại thời điểm đó.
> **Lý do thiết kế:** Đảm bảo khi Audit sau 3 tháng, số liệu trong phiếu cảnh báo hoàn toàn khớp với ngữ cảnh lịch sử, không bị ảnh hưởng nếu Admin sửa định mức sản phẩm ở hiện tại.

### 2.2. Máy Trạng thái Vòng đời (State Machine & Guard Rules)
Phiếu cảnh báo tuân thủ quy tắc chuyển trạng thái chặt chẽ được đóng gói trong Entity `InventoryAlert.java`:
```text
[OPEN] ──(Ghi nhận / Acknowledge)──> [ACKNOWLEDGED] ──(Giải quyết / Resolve)──> [RESOLVED]
  │                                                                                  ▲
  └────────────────────────────(Tự động giải quyết khi nhập kho)─────────────────────┘
```
- **Quy tắc bảo vệ (Guard Rules):**
  - Chỉ phiếu ở trạng thái `OPEN` mới được chuyển sang `ACKNOWLEDGED` thông qua `canAcknowledge()`.
  - Phiếu ở trạng thái `OPEN` hoặc `ACKNOWLEDGED` có thể chuyển sang `RESOLVED` thông qua `canResolve()`.
  - Không cho phép cập nhật ngược từ `RESOLVED` về trạng thái trước đó.

### 2.3. Khóa Lạc quan & Định danh Người xử lý (Actor Resilience)
- **Optimistic Locking (`@Version`):** Bảng được trang bị trường `version` BIGINT. Khi hai quản lý kho cùng bấm "Ghi nhận" một phiếu cảnh báo tại cùng một mili-giây, JPA sẽ ném ngoại lệ `ObjectOptimisticLockingFailureException`, ngăn chặn race condition và bảo vệ tính toàn vẹn dữ liệu.
- **Định danh linh hoạt (`nguoi_xu_ly VARCHAR(100)`):** Thay vì khóa cứng khóa ngoại vào bảng `Employee`, hệ thống lưu chuỗi định danh. Điều này cho phép lưu cả tài khoản nhân viên (`emp_001`), tiến trình tự động (`CRON_JOB`, `SYSTEM`), hoặc AI Agent trong tương lai.

### 2.4. Tối ưu Deduplication (Chống trùng lặp phiếu)
Trong tầng Repository (`InventoryAlertRepository.java`), hệ thống cung cấp phương thức chuyên biệt:
```java
boolean existsByProductIdAndWarehouseIdAndStatusIn(Long productId, Long warehouseId, Collection<InventoryAlertStatus> statuses);
```
- **Mục đích:** Trước khi Job tự động quét tồn kho sinh phiếu mới (T179), hệ thống gọi hàm này kiểm tra nếu đã tồn tại phiếu `OPEN` hoặc `ACKNOWLEDGED` cho cặp Sản phẩm + Kho hàng thì **bỏ qua**, tránh tạo rác dữ liệu và làm phiền người dùng.

---

## 3. Cấu trúc JSON Request và Response (Dự kiến cho API T181 - T184)

Dưới đây là chuẩn giao ước dữ liệu JSON dựa trên nền tảng T177 để Frontend và các Task tiếp theo tích hợp.

### 3.1. JSON Response - Danh sách Phiếu cảnh báo (`GET /api/inventory-alerts`)
```json
{
  "success": true,
  "code": 200,
  "message": "Lấy danh sách phiếu cảnh báo thành công",
  "data": {
    "content": [
      {
        "id": 1,
        "productId": 105,
        "productCode": "SP_IPHONE15",
        "productName": "iPhone 15 Pro Max 256GB",
        "warehouseId": 2,
        "warehouseName": "Kho Tổng TP.HCM",
        "currentQuantity": 2,
        "minStock": 10,
        "maxStock": 50,
        "severity": "CRITICAL",
        "status": "OPEN",
        "notes": null,
        "handledBy": null,
        "createdAt": "2026-07-28T10:15:30",
        "version": 0
      },
      {
        "id": 2,
        "productId": 204,
        "productCode": "SP_MACBOOK",
        "productName": "MacBook Air M3 16GB",
        "warehouseId": 2,
        "warehouseName": "Kho Tổng TP.HCM",
        "currentQuantity": 5,
        "minStock": 8,
        "maxStock": 30,
        "severity": "WARNING",
        "status": "ACKNOWLEDGED",
        "notes": "Đã liên hệ nhà cung cấp Apple Việt Nam, dự kiến giao hàng trong 2 ngày tới.",
        "handledBy": "nguyenvana_kho",
        "createdAt": "2026-07-27T14:20:00",
        "version": 1
      }
    ],
    "pageNumber": 0,
    "pageSize": 20,
    "totalElements": 2,
    "totalPages": 1
  }
}
```

### 3.2. JSON Request - Ghi nhận xử lý cảnh báo (`PATCH /api/inventory-alerts/{id}/acknowledge`)
```json
{
  "notes": "Đã tạo đơn đặt hàng nhập kho số #PO-20260728-01, chờ phê duyệt."
}
```

### 3.3. JSON Response - Sau khi Ghi nhận / Giải quyết thành công
```json
{
  "success": true,
  "code": 200,
  "message": "Ghi nhận xử lý phiếu cảnh báo thành công",
  "data": {
    "id": 1,
    "status": "ACKNOWLEDGED",
    "notes": "Đã tạo đơn đặt hàng nhập kho số #PO-20260728-01, chờ phê duyệt.",
    "handledBy": "tranvanb_kho",
    "updatedAt": "2026-07-28T19:40:12",
    "version": 1
  }
}
```

---

## 4. Các File Đã Xây Dựng & Kiểm Chứng
1. **Migration SQL:** `src/main/resources/db/migration/V29__create_inventory_alert_table.sql` (Index tối ưu cho `product_id`, `warehouse_id`, `status`).
2. **Enums:** `InventoryAlertStatus.java`, `InventoryAlertSeverity.java`.
3. **Entity:** `InventoryAlert.java` (Tích hợp State Machine, Guard Rules, `@Version`).
4. **Repository:** `InventoryAlertRepository.java` (Tích hợp `existsBy...`, `findFirstBy...`, `countBy...`, JpaSpecificationExecutor).
5. **Unit & Contract Tests:** 
   - `InventoryAlertTest.java` (6/6 test cases passed - State machine verification).
   - `InventoryAlertRepositoryTest.java` (4/4 test cases passed - Deduplication & Query contract verification).
