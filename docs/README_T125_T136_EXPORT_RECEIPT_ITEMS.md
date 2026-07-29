# Sprint 3 — ExportReceiptItem & Stock Availability (T125–T136)

## Phạm vi hoàn thành

- Chuẩn hóa dòng phiếu xuất theo `chi_tiet_phieu_xuat`, liên kết phiếu, sản phẩm và kho.
- Chỉ cho phép thêm/sửa/xóa khi phiếu ở trạng thái `NHAP` hoặc `TU_CHOI`.
- Sản phẩm phải đang hoạt động, số lượng phải lớn hơn 0 và không vượt tồn tại kho của phiếu.
- Thành tiền dòng = `quantity * unitPrice`; tổng tiền phiếu được tính lại sau mỗi thay đổi.
- Tồn kho được kiểm tra lại khi gửi duyệt và khi duyệt cấp 2; chưa trừ tồn ở bước nhập liệu.
- Form phiếu xuất hiển thị danh sách dòng, tổng tiền, tồn hiện tại và cảnh báo vượt tồn.

## API

Base path: `/api/export-receipts`

| Method | Endpoint | Chức năng |
|---|---|---|
| GET | `/{id}` | Chi tiết phiếu kèm tồn hiện tại/cảnh báo |
| GET | `/{id}/items` | Danh sách dòng sản phẩm |
| POST | `/{id}/items` | Thêm dòng |
| PUT | `/{id}/items/{itemId}` | Cập nhật số lượng, giá, ghi chú |
| DELETE | `/{id}/items/{itemId}` | Xóa dòng ở trạng thái cho phép |
| GET | `/{id}/availability/{productId}` | Tra tồn khả dụng theo sản phẩm/kho |
| PUT | `/{id}/submit` | Kiểm tra lại tồn và gửi duyệt |
| GET | `/{id}/history` | Lịch sử gửi duyệt, duyệt, từ chối và hủy phiếu xuất |

Lịch sử phiếu xuất được lưu riêng trong `phieu_xuat_kho_lich_su`; không dùng chung ID hoặc dữ liệu với lịch sử phiếu nhập.

### Request thêm/cập nhật dòng

```json
{
  "productId": 10,
  "quantity": 5,
  "unitPrice": 125000,
  "note": "Giao đợt 1"
}
```

### Response dòng

```json
{
  "id": 25,
  "productId": 10,
  "productCode": "SP-001",
  "productName": "Sản phẩm A",
  "unit": "Cái",
  "quantity": 5,
  "unitPrice": 125000,
  "lineTotal": 625000,
  "note": "Giao đợt 1",
  "availableStock": 20,
  "exceedsAvailableStock": false
}
```

## Quy ước lỗi

- `400`: dữ liệu không hợp lệ, sản phẩm ngừng hoạt động hoặc vượt tồn.
- `404`: phiếu, dòng hoặc sản phẩm không tồn tại.
- `409`: trùng sản phẩm hoặc trạng thái/xung đột cập nhật.
- `401/403`: chưa xác thực hoặc không có quyền.
