# T192-T193 - Gợi ý nhu cầu nhập hàng

## Quy tắc

Service chỉ đọc các dòng tồn của kho và sản phẩm đang hoạt động khi `currentStock <= minStock`.

- `shortageQuantity = max(minStock - currentStock, 0)`.
- Có `maxStock` hợp lệ: `suggestedQuantity = maxStock - currentStock`.
- Chưa có `maxStock`: gợi ý bằng khoảng thiếu và trả `MAX_STOCK_NOT_CONFIGURED`.
- Khoảng min/max không hợp lệ: gợi ý bằng khoảng thiếu và trả `INVALID_STOCK_RANGE`.

Kết quả chỉ tham khảo, không tạo phiếu nhập, không thay đổi tồn kho và không tạo giao dịch kho.

## API

```http
GET /api/replenishment-suggestions
```

Quyền: `ADMIN`, `MANAGER`, `EMPLOYEE`.

Query parameters: `warehouseId`, `productId`, `keyword`, `page`, `size` (tối đa 100).

Response sử dụng cấu trúc phân trang chuẩn và trả tồn hiện tại, min/max, khoảng thiếu, lượng gợi ý, nguyên nhân, ưu tiên và cảnh báo cấu hình.

## Phân loại

- `OUT_OF_STOCK` / `CRITICAL`: tồn bằng 0.
- `BELOW_MINIMUM` / `HIGH`: tồn thấp hơn min.
- `AT_MINIMUM` / `MEDIUM`: tồn bằng min.
