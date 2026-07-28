# Báo cáo Triển khai Task T181 & T182: API Danh sách & Lọc Cảnh báo Tồn kho

**Mã Task:** T181 & T182 (Gộp chung)  
**Tác giả:** Antigravity AI & User (Phiên bản Balanced Architect)  
**Trạng thái:** Đã hoàn thành (100% Passed)

---

## 1. Chức năng API
API `GET /api/inventory-alerts` cung cấp danh sách các phiếu cảnh báo tồn kho (Low Stock Alerts) dành cho thủ kho. Tích hợp sẵn tính năng phân trang (Pagination) và bộ lọc đa chiều (Dynamic Filters).

---

## 2. Logic Nghiệp vụ Chính
1. **Phạm vi hiển thị mặc định:** Trả về các phiếu đang hoạt động (`OPEN`, `ACKNOWLEDGED`) nếu người dùng không chọn trạng thái cụ thể. Giúp che giấu các phiếu đã xử lý (`RESOLVED`) khỏi giao diện làm việc chính.
2. **Sắp xếp theo Business Priority:** Sử dụng Criteria API `cb.selectCase()` trong `InventoryAlertSpecification` để ép thứ tự `CRITICAL (0)` luôn nổi lên trên `WARNING (1)` (Bất chấp thứ tự Alphabet hay Ordinal). Nếu cùng mức độ, sắp xếp theo thời gian mới nhất (`createdAt DESC`).
3. **CQRS Lite:** Tách riêng `InventoryAlertQueryService` chỉ chuyên đọc dữ liệu và trả về DTO.
4. **Hiệu suất & Chống N+1:** Kết hợp `JpaSpecificationExecutor` với tính năng `@EntityGraph(attributePaths = {"product", "warehouse"})`. Dữ liệu được Join trực tiếp ở tầng Database thay vì phát sinh thêm 2 query phụ cho mỗi dòng kết quả.

---

## 3. Cấu trúc JSON API (Contract)

### 3.1. Request
- HTTP Method: `GET`
- Endpoint: `/api/inventory-alerts`
- Query Params:
  - `page` (int): Số trang (Bắt đầu từ 0).
  - `size` (int): Kích thước trang.
  - `warehouseId` (Long): Lọc theo ID kho.
  - `productId` (Long): Lọc theo ID sản phẩm.
  - `severity` (Enum): Lọc theo mức độ (`CRITICAL`, `WARNING`).
  - `status` (Enum List): Lọc theo trạng thái (vd: `?status=OPEN&status=ACKNOWLEDGED`).

### 3.2. Response
```json
{
  "content": [
    {
      "id": 1,
      "productId": 5,
      "productCode": "SP05",
      "productName": "Thép cuộn",
      "warehouseId": 2,
      "warehouseCode": "K02",
      "warehouseName": "Kho vật tư",
      "currentQuantity": 0,
      "minStock": 50,
      "severity": "CRITICAL",
      "status": "OPEN",
      "note": "Cảnh báo cạn kiệt",
      "handledBy": null,
      "createdAt": "2026-07-28T10:00:00",
      "updatedAt": "2026-07-28T10:00:00"
    }
  ],
  "pageNo": 0,
  "pageSize": 20,
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```
