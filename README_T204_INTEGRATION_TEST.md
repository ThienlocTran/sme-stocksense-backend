# T204 INTEGRATION TEST: Kiểm Kê → Cảnh Báo → Dashboard

Tài liệu này hướng dẫn chạy Integration Test cho luồng T204 - Kiểm tra việc kiểm kê giảm tồn kho tự động tạo cảnh báo và cập nhật Dashboard.

---

## 📋 Tổng quan

**Luồng test T204** kiểm tra xuyên suốt 3 module:
1. **Inventory/Kiểm kê**: Tạo phiếu kiểm kê giảm số lượng sản phẩm
2. **Alert/Cảnh báo**: Hệ thống tự động phát hiện tồn kho thấp (LOW_STOCK)
3. **Dashboard**: Số lượng cảnh báo trên Dashboard tăng lên

---

## 🎯 Mục tiêu kiểm thử

### Kịch bản test:
```
┌─────────────────────────────────────────────────┐
│ TRƯỚC kiểm kê                                   │
│ • Sản phẩm A: Tồn kho = 50                      │
│ • minStock = 10                                 │
│ • Trạng thái: NORMAL (50 > 10)                  │
│ • Số cảnh báo LOW_STOCK: X                      │
└─────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────┐
│ KIỂM KÊ: Phát hiện chênh lệch                  │
│ • Chứng từ: Nhập 20 cái                         │
│ • Thực tế kiểm đếm: Chỉ nhận được 8 cái        │
│ • Chênh lệch: -12 cái (thiếu hàng)             │
└─────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────┐
│ TẠO BIÊN BẢN CHÊNH LỆCH                        │
│ • Mã: BBCL-PNK-XXX                              │
│ • Lý do: Nhà cung cấp giao thiếu                │
│ • Hành động: Yêu cầu giao bù                    │
└─────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────┐
│ CẬP NHẬT TỒN KHO                                │
│ • Tồn mới: 50 + 8 = 58                          │
│ • Logic: Cộng số lượng THỰC NHẬN (8)           │
└─────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────┐
│ (MÔ PHỎNG) GIẢM TỒN XUỐNG DƯỚI NGƯỠNG          │
│ • Tồn = 8 (< minStock = 10)                     │
│ • Trạng thái: LOW_STOCK ⚠️                      │
└─────────────────────────────────────────────────┘
              ↓
┌─────────────────────────────────────────────────┐
│ KIỂM TRA CẢNH BÁO & DASHBOARD                  │
│ ✅ Sản phẩm A xuất hiện trong /low-stock        │
│ ✅ Số cảnh báo tăng: X → X+1                    │
└─────────────────────────────────────────────────┘
```

---

## 🚀 Cách chạy Integration Test (JUnit + MockMvc)

### 1. Yêu cầu

- **Java 21+**
- **Maven 3.9+**
- **PostgreSQL** (hoặc H2 Database cho test profile)
- **Spring Boot 3.x**

### 2. Cấu hình Database Test

Tạo file `src/test/resources/application-test.yml`:

```yaml
spring:
  datasource:
    url: jdbc:h2:mem:testdb
    driver-class-name: org.h2.Driver
    username: sa
    password:
  jpa:
    hibernate:
      ddl-auto: create-drop
    show-sql: true
  flyway:
    enabled: false  # Tắt Flyway cho test
```

### 3. Chạy test

```bash
# Chạy tất cả test
./mvnw test

# Chạy riêng T204 Integration Test
./mvnw test -Dtest=T204InventoryAlertIntegrationTest

# Chạy với output chi tiết
./mvnw test -Dtest=T204InventoryAlertIntegrationTest -X
```

### 4. Kết quả mong đợi

```
========== TRƯỚC KHI KIỂM KÊ ==========
Tồn kho ban đầu: 50 (trạng thái: NORMAL)
Ngưỡng cảnh báo (minStock): 10
Số lượng cảnh báo LOW_STOCK ban đầu: 0

========== BƯỚC 1: KIỂM HÀNG ==========
Chứng từ ghi: Nhập 20 cái
Thực tế kiểm đếm: Chỉ nhận được 8 cái
⚠️ CHÊNH LỆCH: -12 cái (thiếu hàng)

========== BƯỚC 2: TẠO BIÊN BẢN CHÊNH LỆCH ==========
Biên bản đã tạo: BBCL-PNK-TEST-001
Số lượng chênh lệch: -12 cái

========== BƯỚC 3: HOÀN TẤT PHIẾU NHẬP ==========
Tồn kho trước: 50
Thực nhập: +8
Tồn kho sau: 50 + 8 = 58

========== MÔ PHỎNG: GIẢM TỒN XUỐNG 8 ==========
Đã giảm tồn kho xuống: 8
Ngưỡng cảnh báo: 10
⚠️ Kỳ vọng: Trạng thái = LOW_STOCK

========== BƯỚC 5: KIỂM TRA CẢNH BÁO ==========
✅ Trạng thái tồn kho: LOW_STOCK

========== BƯỚC 6: DANH SÁCH CẢNH BÁO LOW_STOCK ==========
Số lượng cảnh báo LOW_STOCK sau khi giảm tồn: 1
Số lượng cảnh báo ban đầu: 0
Chênh lệch: +1

========== BƯỚC 7: DASHBOARD - SỐ LƯỢNG CẢNH BÁO ==========
✅ Tổng số cảnh báo trên Dashboard: 1
✅ Đã tăng: +1 cảnh báo

========== KẾT QUẢ TEST T204 ==========
✅ PASS: Kiểm kê giảm tồn kho thành công
✅ PASS: Hệ thống tự động phát hiện LOW_STOCK
✅ PASS: Sản phẩm xuất hiện trong danh sách cảnh báo
✅ PASS: Số lượng cảnh báo trên Dashboard tăng lên
==========================================
```

---

## 📮 Cách chạy Postman Collection

### 1. Import Collection vào Postman

1. Mở Postman
2. Click **Import** → **Choose Files**
3. Chọn file `T204_Inventory_Alert_Test.postman_collection.json`
4. Click **Import**

### 2. Cấu hình biến môi trường

Tạo Environment mới với các biến:

```json
{
  "base_url": "http://localhost:8080",
  "product_id": "1",
  "warehouse_id": "1",
  "import_receipt_id": "1"
}
```

**Lưu ý:** 
- `product_id`: ID của sản phẩm cần test (có `minStock` được set)
- `warehouse_id`: ID của kho hàng
- `import_receipt_id`: ID của phiếu nhập ở trạng thái `CHO_KIEM_HANG`

### 3. Chạy Collection

1. **Login trước:** Chạy request "0. Setup - Login" để lấy access token
2. **Chạy tuần tự:** Click **Run** → Run collection theo thứ tự
3. **Xem kết quả:** Check tab **Test Results** để xem assertions

### 4. Các bước trong Collection

| Bước | Request | Mục đích |
|------|---------|----------|
| 0 | `POST /api/auth/login` | Đăng nhập, lấy access token |
| 1 | `GET /api/inventory` | Kiểm tra tồn kho ban đầu |
| 2 | `GET /api/inventory/low-stock` | Đếm số cảnh báo ban đầu |
| 3 | `PUT /api/import-receipts/{id}/inspect` | Kiểm hàng, ghi nhận chênh lệch |
| 4 | `POST /api/import-receipts/{id}/discrepancy-report` | Tạo biên bản chênh lệch |
| 5 | `PUT /api/import-receipts/{id}/hoan-tat` | Hoàn tất phiếu, cập nhật tồn |
| 6 | `GET /api/inventory` | Kiểm tra tồn kho sau cập nhật |
| 7 | `GET /api/inventory/low-stock` | Kiểm tra cảnh báo mới |
| 8 | `GET /api/inventory/low-stock` | Dashboard count (final) |

---

## 🔍 Chi tiết Implementation

### Logic tính toán trạng thái LOW_STOCK

Trạng thái được tính động trong SQL query:

```sql
CASE 
  WHEN t.so_luong = 0 THEN 'OUT_OF_STOCK'
  WHEN t.so_luong <= sp.ton_toi_thieu THEN 'LOW_STOCK'
  WHEN sp.ton_toi_da IS NOT NULL AND t.so_luong >= sp.ton_toi_da THEN 'OVER_STOCK'
  ELSE 'NORMAL'
END AS status
```

**Điều kiện LOW_STOCK:**
- `currentQuantity <= minStock` → Trạng thái = `LOW_STOCK`

### Các API liên quan

| API | Method | Mô tả |
|-----|--------|-------|
| `/api/inventory` | GET | Lấy danh sách tồn kho với filter |
| `/api/inventory/low-stock` | GET | Lấy danh sách tồn thấp (status=LOW_STOCK) |
| `/api/import-receipts/{id}/inspect` | PUT | Kiểm hàng, ghi nhận số lượng thực tế |
| `/api/import-receipts/{id}/discrepancy-report` | POST | Tạo biên bản chênh lệch |
| `/api/import-receipts/{id}/hoan-tat` | PUT | Hoàn tất phiếu, cập nhật tồn kho |

---

## ⚠️ Lưu ý quan trọng

### 1. Hệ thống CHƯA có Dashboard API chính thức

Hiện tại, số lượng cảnh báo được lấy từ:
```
GET /api/inventory/low-stock?page=0&size=100
→ Response: { "totalElements": X }
```

**Để có Dashboard thực sự**, cần tạo:
1. `DashboardController` + `DashboardService`
2. Query count số lượng LOW_STOCK:
   ```java
   @Query("SELECT COUNT(*) FROM InventoryLevel i WHERE i.quantity <= i.product.minStock")
   long countLowStockProducts();
   ```

### 2. KHÔNG có cơ chế tự động tạo Alert

Hệ thống **KHÔNG có bảng `alerts`** và **KHÔNG có trigger tự động**.

Trạng thái LOW_STOCK được tính **real-time** mỗi lần gọi API, không lưu vào database.

Nếu cần alert persistence, cần:
1. Tạo entity `Alert` (id, productId, warehouseId, type, createdAt)
2. Tạo Service `AlertService` với method `createLowStockAlert()`
3. Gọi trong `InventoryService.increaseInventory()` hoặc `decreaseInventory()`

### 3. Logic nghiệp vụ: Cộng số lượng THỰC NHẬN

Khi hoàn tất phiếu nhập:
```
Tồn mới = Tồn cũ + Số lượng THỰC NHẬN (actualReceivedQuantity)
```

**KHÔNG phải:**
```
Tồn mới = Tồn cũ + Số lượng CHỨNG TỪ (expectedQuantity) ❌
```

---

## 🐛 Troubleshooting

### Test thất bại: "Phieu nhap khong o trang thai CHO_KIEM_HANG"

**Nguyên nhân:** Phiếu nhập chưa ở đúng trạng thái

**Giải pháp:**
```java
importReceipt.setStatus(ImportReceiptStatus.CHO_KIEM_HANG);
importReceiptRepository.save(importReceipt);
```

### Test thất bại: "Status vẫn là NORMAL thay vì LOW_STOCK"

**Nguyên nhân:** Tồn kho chưa giảm xuống dưới `minStock`

**Kiểm tra:**
- `currentQuantity` <= `minStock` ?
- `minStock` đã được set đúng chưa?

### Postman: 401 Unauthorized

**Nguyên nhân:** Token hết hạn hoặc chưa login

**Giải pháp:**
1. Chạy lại request "0. Setup - Login"
2. Kiểm tra biến `{{access_token}}` đã được set chưa

---

## 📊 Metrics & Assertions

### Integration Test assertions:

```java
// Assert 1: Tồn kho giảm xuống
assertThat(currentQuantity).isLessThanOrEqualTo(minStock);

// Assert 2: Trạng thái = LOW_STOCK
assertThat(stockStatus).isEqualTo("LOW_STOCK");

// Assert 3: Sản phẩm xuất hiện trong danh sách cảnh báo
assertThat(lowStockList).contains(productA);

// Assert 4: Số lượng cảnh báo tăng lên
assertThat(finalLowStockCount).isGreaterThan(initialLowStockCount);
```

### Postman Test Scripts:

```javascript
// Test 1: Status code
pm.test("Status code is 200", function () {
    pm.response.to.have.status(200);
});

// Test 2: Trạng thái LOW_STOCK
pm.test("Status is LOW_STOCK", function () {
    var jsonData = pm.response.json();
    pm.expect(jsonData.content[0].status).to.eql("LOW_STOCK");
});

// Test 3: Số lượng cảnh báo tăng
pm.test("Alert count increased", function () {
    var initialCount = pm.collectionVariables.get("initial_low_stock_count");
    var finalCount = pm.response.json().totalElements;
    pm.expect(finalCount).to.be.above(initialCount);
});
```

---

## 📚 Tham khảo

- **README T101-T104:** Tài liệu chi tiết về Biên bản chênh lệch
- **InventoryServiceImpl:** Logic cộng tồn kho
- **InventoryLevelRepository:** Query tính toán trạng thái
- **ImportReceiptServiceImpl:** Logic kiểm hàng và hoàn tất phiếu

---

## ✅ Checklist

Trước khi chạy test, đảm bảo:

- [ ] Database đã có dữ liệu sản phẩm với `minStock` > 0
- [ ] Có phiếu nhập ở trạng thái `CHO_KIEM_HANG`
- [ ] Tồn kho hiện tại > `minStock` (để test giảm xuống)
- [ ] Backend đang chạy tại `http://localhost:8080`
- [ ] Đã login và có access token hợp lệ

---

**✨ TEST T204 SẴN SÀNG! Chúc bạn test thành công!** 🚀
