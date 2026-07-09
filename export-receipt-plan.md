# ExportReceipt Model - Phiếu xuất nhân viên (Backend)

## Goal
Hoàn thiện các API phục vụ quy trình tạo, sửa, duyệt và hủy phiếu xuất kho theo thiết kế 2 cấp duyệt, áp dụng chiến lược Stacked Branches.

## Tasks
### Cụm 1: Core DB & Business Rules (Branch: `feature/cluster-1-export-receipt-core`)
- [ ] Task 1 (T112): Kiểm tra `ExportReceipt` model, schema và các enum trạng thái (Nháp, Chờ duyệt cấp 1, Chờ duyệt cấp 2, Từ chối, Hoàn thành, Hủy) → Verify: Model và DB Migration hợp lệ.
- [ ] Task 2 (T113): Chuẩn hóa rule chuyển trạng thái phiếu xuất 2 cấp trong service → Verify: Logic kiểm tra rule hợp lệ (chỉ trừ tồn sau 2 cấp duyệt).

### Cụm 2: Draft & Write APIs (Branch: `feature/cluster-2-export-receipt-write-api`)
- [ ] Task 3 (T114): Tạo API tạo phiếu xuất nháp (chưa trừ tồn) → Verify: Gọi POST API trả về 201 với trạng thái Nháp.
- [ ] Task 4 (T115): Tạo API cập nhật thông tin phiếu xuất nháp/từ chối → Verify: Gọi PUT/PATCH API thành công khi phiếu ở trạng thái Nháp/Từ chối, fail khi trạng thái khác.
- [ ] Task 5 (T116): Tạo API lưu nháp phiếu xuất (thông tin tối thiểu) → Verify: API lưu được thông tin thiếu sót mà không validate gắt gao.
- [ ] Task 6 (T117): Tạo API hủy phiếu xuất nháp → Verify: Gọi API hủy đổi trạng thái thành Hủy và không gửi duyệt được nữa.

### Cụm 3: Action & Read APIs (Branch: `feature/cluster-3-export-receipt-read-action-api`)
- [ ] Task 7 (T118): Tạo API gửi phiếu xuất duyệt → Verify: Check tồn kho, check chi tiết hợp lệ và chuyển trạng thái sang Chờ duyệt cấp 1.
- [ ] Task 8 (T119): Tạo API danh sách phiếu xuất của nhân viên → Verify: Trả về đúng danh sách phân trang kèm theo bộ lọc trạng thái.
- [ ] Task 9 (T120): Tạo API chi tiết phiếu xuất → Verify: Trả về thông tin chung và danh sách `ExportReceiptItem`.

## Done When
- [ ] Tất cả các API được tạo thành công, chạy qua Postman/Unit test hợp lệ.
- [ ] Flow chuyển trạng thái không gặp lỗi.

## Notes
- Các API cần chú ý chỉ trừ tồn kho khi đơn hàng đã hoàn thành (duyệt cấp 2 xong).
- Nhánh sau phải được checkout từ nhánh trước theo đúng thứ tự (Cụm 1 -> Cụm 2 -> Cụm 3).
