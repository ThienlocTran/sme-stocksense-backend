# Tài liệu Tổng kết Task T176: Chuẩn Hóa Rule Cảnh Báo Tồn Kho Thấp

**Mã Task:** T176 (Sprint 4 - Low Stock Alert)  
**Tác giả:** Antigravity AI & User  
**Dự án:** SME StockSense Backend  
**Trạng thái:** Hoàn thành (100% Build Success & Test Passed)

---

## 1. Chức năng của API
Task T176 cung cấp và chuẩn hóa 2 API chính liên quan đến theo dõi trạng thái và cảnh báo tồn kho:
1. **API Danh sách tồn kho tổng quan (`GET /api/inventory`):**
   - Cho phép tra cứu, tìm kiếm và phân trang toàn bộ tồn kho.
   - Hỗ trợ lọc theo trạng thái tồn kho cụ thể thông qua tham số `stockStatus` (`LOW_STOCK`, `OUT_OF_STOCK`, `OVER_STOCK`, `NORMAL`), từ khóa, kho hàng, và trạng thái hoạt động của kho/sản phẩm.
2. **API Danh sách cảnh báo tồn kho thấp (`GET /api/inventory/low-stock`):**
   - Chuyên biệt hóa cho màn hình Cảnh báo (AlertsView trên Frontend).
   - Tự động gom nhóm trả về toàn bộ mặt hàng cần chú ý nhập bổ sung (bao gồm cả **LOW_STOCK - Sắp hết hàng** và **OUT_OF_STOCK - Hết hàng hoàn toàn**) để quản lý kho không bỏ sót rủi ro đứt gãy chuỗi cung ứng.

---

## 2. Logic Nghiệp vụ Chính (Single Source of Truth)

Toàn bộ logic phân loại trạng thái được xác định duy nhất (SSOT) tại tầng Database thông qua Repository SQL (`InventoryLevelRepository.java`), không tính toán lại ở tầng Service hay Controller để đảm bảo tính nhất quán tuyệt đối.

### Thứ tự Đánh giá Ưu tiên (Evaluation Order)
Cho mỗi cặp sản phẩm - kho hàng (`san_pham_id`, `kho_id`), trạng thái được đánh giá theo thứ tự ưu tiên tuyệt đối từ trên xuống dưới:
1. **`OUT_OF_STOCK` (Hết hàng - Critical):**
   - Điều kiện: `so_luong <= 0` (Bao gồm số lượng bằng 0 và số lượng âm do chênh lệch kiểm kê/xử lý vượt tồn).
2. **`LOW_STOCK` (Sắp hết hàng - Warning):**
   - Điều kiện: `ton_toi_thieu IS NOT NULL AND ton_toi_thieu > 0 AND so_luong <= ton_toi_thieu`.
   - *Lưu ý:* Trường hợp `ton_toi_thieu` chưa cấu hình (`NULL`) hoặc chủ động tắt (`0`) thì mặt hàng sẽ KHÔNG kích hoạt cảnh báo này.
3. **`OVER_STOCK` (Vượt định mức lưu trữ):**
   - Điều kiện: `ton_toi_da IS NOT NULL AND ton_toi_da > 0 AND so_luong > ton_toi_da`.
4. **`NORMAL` (Bình thường):**
   - Các trường hợp còn lại nằm trong định mức an toàn (`minStock < so_luong <= maxStock`).

---

## 3. Cấu trúc JSON Request & Response

### Request Query Parameters (`GET /api/inventory/low-stock`)
| Tham số | Kiểu dữ liệu | Bắt buộc | Mô tả |
| :--- | :--- | :---: | :--- |
| `warehouseId` | Long | Không | Lọc theo ID kho hàng cụ thể |
| `productId` | Long | Không | Lọc theo ID sản phẩm cụ thể |
| `keyword` | String | Không | Từ khóa tìm kiếm mã/tên sản phẩm, mã vạch, tên kho |
| `warehouseStatus`| String | Không | Lọc trạng thái kho (`HOAT_DONG`, `NGUNG_HOAT_DONG`) |
| `productStatus` | String | Không | Lọc trạng thái sản phẩm (`HOAT_DONG`, `NGUNG_HOAT_DONG`) |
| `page` | Integer | Không | Số trang (Mặc định: `0`) |
| `size` | Integer | Không | Kích thước trang (Mặc định: `20`, tối đa: `100`) |

### JSON Response Structure (`PageResponse<InventoryLevelResponse>`)
```json
{
  "content": [
    {
      "inventoryId": 105,
      "productId": 12,
      "productCode": "SP001",
      "productName": "Bàn phím cơ Keychron K8",
      "barcode": "893500123456",
      "warehouseId": 1,
      "warehouseCode": "KHO-HCM",
      "warehouse": "Kho Chính TP.HCM",
      "currentQuantity": 5,
      "minStock": 10,
      "maxStock": 100,
      "productStatus": "HOAT_DONG",
      "warehouseStatus": "HOAT_DONG",
      "status": "LOW_STOCK",
      "lastUpdatedAt": "2026-07-28T10:30:00"
    },
    {
      "inventoryId": 108,
      "productId": 15,
      "productCode": "SP005",
      "productName": "Chuột không dây Logitech G304",
      "barcode": "893500654321",
      "warehouseId": 1,
      "warehouseCode": "KHO-HCM",
      "warehouse": "Kho Chính TP.HCM",
      "currentQuantity": 0,
      "minStock": 20,
      "maxStock": 200,
      "productStatus": "HOAT_DONG",
      "warehouseStatus": "HOAT_DONG",
      "status": "OUT_OF_STOCK",
      "lastUpdatedAt": "2026-07-28T11:15:00"
    }
  ],
  "pageNumber": 0,
  "pageSize": 20,
  "totalElements": 2,
  "totalPages": 1,
  "last": true
}
```

---

## 4. Kiểm chứng & Chất lượng (Verification)
- **Chuẩn mã nguồn:** Đã bổ sung chú thích tiếng Việt (`Note: [T176 - Khối X]`) tại các file xử lý chính (`InventoryLevelRepository.java`, `InventoryServiceImpl.java`, `InventoryController.java`).
- **Build & Test:** Chạy tự động `mvnw test` đạt 100% BUILD SUCCESS (25/25 test cases passed).
