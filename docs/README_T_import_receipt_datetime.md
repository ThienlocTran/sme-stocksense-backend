# T_import_receipt_datetime: Đồng bộ và Chuẩn hóa Thời gian Phiếu nhập theo Giờ Việt Nam

## 1. Vấn đề phát hiện
- Chi tiết phiếu nhập (`GET /api/import-receipts/{id}`) trả về DTO `ImportReceiptDraftResponse` bị thiếu trường `createdAt` (thời gian tạo phiếu thực tế), khiến frontend/báo cáo phải fallback sang `actualArrivalDate` (ngày hàng về - thường là 00:00:00) hoặc `updatedAt`, dẫn đến thời gian phiếu nhập bị sai lệch so với phiếu xuất (`ExportReceiptDetailResponse` có trường `createdAt`).
- Hàm xuất chứng từ PDF/Excel cho phiếu nhập (`StockDocumentExportServiceImpl`) sử dụng `firstDate(receipt.actualArrivalDate(), receipt.updatedAt())` thay vì ưu tiên `receipt.createdAt()`.
- Thiếu thiết lập Timezone mặc định toàn cục `Asia/Ho_Chi_Minh` (GMT+7) khi ứng dụng Spring Boot khởi động và trong cấu hình Jackson.

## 2. Giải pháp thực hiện
1. **Cấu hình Timezone hệ thống**:
   - Thêm `@PostConstruct` trong `SmeStocksenseBackendApplication` thiết lập `TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"))`.
   - Cấu hình `spring.jackson.time-zone: Asia/Ho_Chi_Minh` trong `application.yml`.
2. **Bổ sung trường `createdAt` vào DTO `ImportReceiptDraftResponse`**:
   - Thêm `LocalDateTime createdAt` vào record `ImportReceiptDraftResponse`.
   - Cập nhật phương thức `from(ImportReceipt, List<ImportReceiptDetail>)` để gán `receipt.getCreatedAt()`.
   - Giữ nguyên các constructor nạp chồng để tương thích ngược.
3. **Cập nhật Logic xuất PDF/Excel**:
   - Cập nhật `buildImportDocument` trong `StockDocumentExportServiceImpl` ưu tiên lấy `receipt.createdAt()` đồng bộ tương tự như phiếu xuất.

## 3. Cấu trúc JSON API Chi tiết Phiếu nhập (`GET /api/import-receipts/{id}`)
```json
{
  "id": 1,
  "code": "PN-20260902-001",
  "warehouseId": 1,
  "warehouseName": "Kho Chính",
  "supplierId": 2,
  "supplierName": "Nhà cung cấp A",
  "createdById": 1,
  "createdByName": "Nguyễn Văn A",
  "submittedById": null,
  "submittedByName": null,
  "submittedAt": null,
  "actualArrivalDate": "2026-09-02T15:00:00",
  "status": "NHAP",
  "totalAmount": 15000000.00,
  "note": "Nhập hàng theo kế hoạch",
  "rejectionReason": null,
  "details": [],
  "detailCount": 0,
  "createdAt": "2026-09-02T20:15:00",
  "updatedAt": "2026-09-02T20:15:00",
  "version": 0
}
```
