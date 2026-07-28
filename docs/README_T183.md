# Báo cáo Task T183: API Đánh dấu Cảnh báo Đã Tiếp Nhận (Acknowledge Alert)

## 1. Chức năng
- Cung cấp endpoint cho phép thủ kho (hoặc quản lý) đánh dấu một cảnh báo tồn kho từ trạng thái `OPEN` (Mở) sang `ACKNOWLEDGED` (Đã tiếp nhận).
- Lưu lại thông tin người đã tiếp nhận cảnh báo vào trường `handledBy`.
- Xử lý Idempotent: Nếu cảnh báo đã được ai đó tiếp nhận (`ACKNOWLEDGED`), hệ thống sẽ trả về thành công nhưng không ghi đè lại dữ liệu (giữ nguyên người tiếp nhận đầu tiên) để tránh xung đột.

## 2. Logic Nghiệp vụ chính
- Xác thực và lấy tên người dùng hiện tại từ Spring Security Context.
- Tách biệt logic Write/Action thông qua interface `InventoryAlertActionService`, tuân thủ CQRS pattern.
- State Transition:
  - `OPEN` -> Cập nhật trạng thái thành `ACKNOWLEDGED`, set `handledBy` bằng tên User, và `updatedAt` tự động sinh ra nhờ JPA Auditing. Lưu xuống cơ sở dữ liệu.
  - `ACKNOWLEDGED` -> Bỏ qua, không lưu lại cơ sở dữ liệu (Idempotent), trả về HTTP 200 OK bình thường.
  - `RESOLVED` -> Ném lỗi `InvalidAlertStateException` (Kế thừa từ `BadRequestException`), map thành HTTP 400 Bad Request để ngăn chặn việc quay ngược vòng đời của cảnh báo.

## 3. Cấu trúc JSON Request / Response

**Method**: `PUT /api/inventory-alerts/{id}/acknowledge`

**Request**: Trống (Không yêu cầu Request Body).

**Response**:
Trả về object `InventoryAlertResponse` đã được cập nhật.

```json
{
  "id": 1,
  "productId": 101,
  "productCode": "SP-001",
  "productName": "Sản phẩm A",
  "warehouseId": 2,
  "warehouseCode": "KHO-02",
  "warehouseName": "Kho Hàng B",
  "currentQuantity": 5,
  "minStock": 20,
  "severity": "CRITICAL",
  "status": "ACKNOWLEDGED",
  "note": null,
  "handledBy": "thukho_01",
  "createdAt": "2026-07-28T10:00:00",
  "updatedAt": "2026-07-28T15:45:00"
}
```

## 4. Kiến trúc và Tái cấu trúc (Refactor)
- **InventoryAlertMapper**: Logic convert DTO được tách từ `InventoryAlertQueryServiceImpl` thành `InventoryAlertMapper.toResponse(entity)`, chia sẻ dùng chung cho cả ActionService.
- **Custom Exception**: Định nghĩa thêm class `InvalidAlertStateException` thuộc họ Bad Request để quản lý các case state bị lỗi.
- **Unit Test**: Viết 4 test cases sử dụng Mockito cho toàn bộ scenario trong service. Test đã Pass 100%.
