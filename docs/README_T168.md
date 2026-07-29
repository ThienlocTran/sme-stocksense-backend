# T168-T175 - Kiểm kê tồn kho định kỳ

## Phạm vi

Module `InventoryCount` tạo snapshot tồn kho để nhập số lượng thực tế, tính chênh lệch và chốt hoặc hủy đợt kiểm kê. Chốt kiểm kê không tự điều chỉnh tồn kho và không tạo giao dịch kho.

## API

- `POST /api/inventory-counts`: tạo đợt theo kho; `productIds` rỗng sẽ snapshot toàn bộ sản phẩm đang có tồn tại kho.
- `GET /api/inventory-counts`: danh sách, hỗ trợ `warehouseId`, `status`, `page`, `size`.
- `GET /api/inventory-counts/{id}`: chi tiết và các dòng snapshot.
- `PUT /api/inventory-counts/{id}/details/{detailId}`: ghi số lượng thực tế và tự tính `differenceQuantity`.
- `POST /api/inventory-counts/{id}/finalize`: chốt khi mọi dòng đã có số lượng thực tế.
- `POST /api/inventory-counts/{id}/cancel`: hủy với lý do bắt buộc.

## Quy tắc

- Trạng thái: `NHAP`, `DANG_KIEM_KE`, `DA_CHOT`, `DA_HUY`.
- Chênh lệch = số lượng thực tế - số lượng snapshot hệ thống.
- Không cho số lượng âm, sản phẩm trùng hoặc sản phẩm/kho ngừng hoạt động.
- Một kho chỉ có một đợt `NHAP`/`DANG_KIEM_KE` tại một thời điểm.
- Sau khi chốt/hủy không được sửa.
- `version` bắt buộc khi ghi số thực tế, chốt hoặc hủy để chống cập nhật đồng thời.

## Ví dụ tạo

```json
{
  "warehouseId": 11,
  "productIds": [44],
  "note": "Kiểm kê định kỳ"
}
```

## Ví dụ ghi thực tế

```json
{
  "actualQuantity": 2,
  "note": "Thiếu 1",
  "version": 0
}
```
