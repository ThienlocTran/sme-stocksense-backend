# Kỹ thuật Đặc tả Thiết kế (Design Specification) - Task T181 & T182: API Danh sách & Lọc cảnh báo tồn kho

**Mã Task:** T181 & T182 (Sprint 4 - Low Stock Alert System)  
**Tác giả:** Antigravity AI & User (Phiên bản Balanced Architect)  
**Trạng thái:** Chờ phê duyệt (Pending Approval)

---

## 1. Bối cảnh & Mục tiêu Nghiệp vụ (Business Context)
Để thủ kho có thể theo dõi và xử lý các mặt hàng sắp hết hoặc đã cạn kiệt, hệ thống cần cung cấp một API danh sách (List API) kết hợp phân trang (Pagination) và các bộ lọc (Filters). 
Thay vì tách riêng 2 API cho danh sách (T181) và lọc (T182), thiết kế này gộp chung thành một API RESTful duy nhất để tăng tính tiện dụng và nhất quán.

---

## 2. Các Quyết định Kiến trúc đã Chốt (Architectural Decisions)

Dựa trên kết quả phỏng vấn qua `/grill-me`, các nguyên tắc sau đã được thống nhất:

### 2.1. Phạm vi hiển thị mặc định (Default Scope & Status Filter)
- Nếu Frontend không truyền tham số `status`: API **chỉ trả về các phiếu đang hoạt động** (`OPEN` và `ACKNOWLEDGED`). Điều này giúp thủ kho tập trung 100% vào các công việc cần xử lý ngay, không bị rác bởi các phiếu đã xử lý xong.
- Nếu Frontend muốn xem lịch sử: Cần chủ động truyền mảng `status=RESOLVED` hoặc mảng rỗng `status=` (tuỳ thiết kế filter, ở đây ta sẽ nhận danh sách `status` qua param).

### 2.2. Sắp xếp Mặc định (Default Sorting)
- **Ưu tiên 1:** Mức độ nghiêm trọng theo Business Priority (`CRITICAL` trước, `WARNING` sau). Không phụ thuộc vào thứ tự alphabet hay ordinal của Enum. Trong truy vấn (ví dụ JPQL/Specification) sẽ dùng logic `CASE WHEN severity = 'CRITICAL' THEN 0 WHEN 'WARNING' THEN 1 END ASC`.
- **Ưu tiên 2:** Thời gian tạo (`createdAt DESC`) để các phiếu mới nhất được ưu tiên hiển thị.

### 2.3. Cấu trúc dữ liệu & Chống N+1 (DTO & N+1 Prevention)
- **Tối ưu truy vấn:** Chốt chiến lược sử dụng `JpaSpecificationExecutor` + `@EntityGraph(attributePaths = {"product", "warehouse"})`. Việc dùng Specification cho phép mở rộng linh hoạt các bộ lọc động (dynamic filters) cho T183 sau này (lọc thêm theo handledBy, date...). Kết hợp với EntityGraph giúp giải quyết triệt để lỗi N+1 mà vẫn an toàn với phân trang Pageable (do là ManyToOne).
- **Data Transfer Object (DTO):** Mapping sang `InventoryAlertResponse` chứa đầy đủ thông tin để UI hiển thị trực tiếp. Bổ sung trường `handledBy` để sẵn sàng cho T183/T184.

---

## 3. Cấu trúc API (API Contract)

**Endpoint:** `GET /api/inventory-alerts`

**Query Parameters (Optional):**
- `page` (int): Trang hiện tại (Mặc định: 0)
- `size` (int): Số dòng/trang (Mặc định: 20)
- `warehouseId` (Long): Lọc theo Kho
- `productId` (Long): Lọc theo Sản phẩm
- `severity` (InventoryAlertSeverity): Lọc theo Mức độ (`WARNING`, `CRITICAL`)
- `status` (List<InventoryAlertStatus>): Lọc theo Trạng thái (Ví dụ: `?status=OPEN&status=ACKNOWLEDGED`). Spring sẽ tự động bind sang Enum an toàn. Nếu bỏ trống, backend tự gán mặc định `OPEN` và `ACKNOWLEDGED`.

**Response (JSON):**
```json
{
  "content": [
    {
      "id": 100,
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

---

## 4. Kế hoạch Triển khai (Implementation Plan)

1. **DTO:** Tạo `InventoryAlertResponse`.
2. **Repository:** Extends `JpaSpecificationExecutor` và định nghĩa `InventoryAlertSpecification` (Specification Builder).
3. **Service:** Tạo `InventoryAlertQueryService` (Tách biệt Read/Write theo CQRS lite) để xử lý logic filter (gắn default status) và map Entity -> DTO.
4. **Controller:** Thêm `GET /api/inventory-alerts` vào REST Controller mới `InventoryAlertController`.
5. **Testing:** Viết Unit Test cho Service đảm bảo filter hoạt động đúng, đặc biệt là default status và sorting (Business priority).
