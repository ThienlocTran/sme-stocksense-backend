# SEED_DATA_DETAILS.md

**Tài Liệu Chi Tiết Dữ Liệu Seed - T65**  
**Ngày**: 2026-06-21  
**File**: V9\_\_seed_inventory_data.sql

---

## 📊 OVERVIEW

Dữ liệu seed cho tồn kho mẫu:

- **5 Danh mục** (Category)
- **3 Kho** (Warehouse)
- **12 Sản phẩm** (Product)
- **36 Bản ghi tồn kho** (Inventory Level - ton_kho)

---

## 1. DANH MỤC (Category)

| ID  | Mã    | Tên       | Mô Tả                                         | Trạng Thái |
| --- | ----- | --------- | --------------------------------------------- | ---------- |
| 1   | DM001 | Điện tử   | Thiết bị điện tử: máy tính, màn hình, router  | HOAT_DONG  |
| 2   | DM002 | Linh kiện | Linh kiện máy tính: RAM, CPU, bàn phím, chuột | HOAT_DONG  |
| 3   | DM003 | Đồ dùng   | Đồ dùng văn phòng: cáp, adapter, dây điện     | HOAT_DONG  |
| 4   | DM004 | Thiết bị  | Thiết bị máy tính: nguồn điện, quạt tản nhiệt | HOAT_DONG  |
| 5   | DM005 | Phụ tùng  | Phụ tùng phụ: bộ quang học, vít, keo          | HOAT_DONG  |

---

## 2. KHO (Warehouse)

| ID  | Mã   | Tên                  | Địa Chỉ                                                      | Trạng Thái |
| --- | ---- | -------------------- | ------------------------------------------------------------ | ---------- |
| 1   | K001 | Kho Chính TP.HCM     | Số 123 Đường Nguyễn Hữu Cảnh, Phường Tân Phú, Quận 7, TP.HCM | HOAT_DONG  |
| 2   | K002 | Kho Chi Nhánh Hà Nội | Số 456 Đường Cầu Giấy, Quận Cầu Giấy, Hà Nội                 | HOAT_DONG  |
| 3   | K003 | Kho Tạm Đà Nẵng      | Số 789 Đường Hải Phòng, Quận Hải Châu, Đà Nẵng               | HOAT_DONG  |

---

## 3. SẢN PHẨM (Product)

| ID  | Mã    | Tên                                  | SKU                  | Đơn Vị | Danh Mục | Tối Thiểu | Tối Đa | Giá        | Ghi Chú         |
| --- | ----- | ------------------------------------ | -------------------- | ------ | -------- | --------- | ------ | ---------- | --------------- |
| 1   | SP001 | Máy tính xách tay Dell XPS 13        | SKU-DELL-XPS13       | Cái    | DM001    | 2         | 10     | 15,000,000 | Laptop cao cấp  |
| 2   | SP002 | Chuột không dây Logitech MX Master 3 | SKU-LOGITECH-MX3     | Cái    | DM002    | 50        | 200    | 250,000    | Chuột game      |
| 3   | SP003 | Bàn phím cơ Corsair K95 RGB Platinum | SKU-CORSAIR-K95      | Cái    | DM002    | 5         | 20     | 2,000,000  | Bàn phím gaming |
| 4   | SP004 | Ổ cứng SSD Samsung 970 EVO 1TB       | SKU-SAMSUNG-970-1TB  | Cái    | DM001    | 10        | 50     | 3,500,000  | SSD NVMe        |
| 5   | SP005 | RAM DDR4 Corsair Vengeance 16GB      | SKU-CORSAIR-RAM-16GB | Cái    | DM002    | 8         | 30     | 1,800,000  | Memory          |
| 6   | SP006 | Màn hình Dell UltraSharp 27" 4K      | SKU-DELL-UP2720Q     | Cái    | DM001    | 1         | 8      | 8,000,000  | Monitor 4K      |
| 7   | SP007 | Cáp USB-C 2m Anker Powerline         | SKU-ANKER-USB-C      | Cái    | DM003    | 100       | 500    | 150,000    | Cáp sạc         |
| 8   | SP008 | Nguồn Seasonic Focus Gold 500W       | SKU-SEASONIC-500W    | Cái    | DM004    | 3         | 15     | 2,500,000  | Power supply    |
| 9   | SP009 | Quạt tản nhiệt Noctua NF-F12 PWM     | SKU-NOCTUA-FAN-12    | Cái    | DM004    | 20        | 100    | 800,000    | Fan cooling     |
| 10  | SP010 | Adapter HDMI 2.1 4K60Hz              | SKU-HDMI-ADAPTER-21  | Cái    | DM003    | 30        | 150    | 200,000    | Adapter video   |
| 11  | SP011 | Router WiFi 6 TP-Link Archer AX6000  | SKU-TP-LINK-AX6000   | Cái    | DM001    | 2         | 12     | 3,000,000  | WiFi router     |
| 12  | SP012 | Bộ quang học HD 2.5X                 | SKU-OPTICAL-2.5X     | Cái    | DM002    | 1         | 5      | 5,000,000  | Optical device  |

---

## 4. TỒN KHO THEO KHO (Inventory Details)

### 4.1 Kho 1 (K001 - Kho Chính TP.HCM)

| Sản Phẩm          | Mã    | Tối Thiểu | Tối Đa | Tồn | Trạng Thái  | Ghi Chú           |
| ----------------- | ----- | --------- | ------ | --- | ----------- | ----------------- |
| Máy tính xách tay | SP001 | 2         | 10     | 7   | ✅ NORMAL   | (2 < 7 < 10)      |
| Chuột không dây   | SP002 | 50        | 200    | 180 | ✅ HIGH     | (180 gần max)     |
| Bàn phím cơ       | SP003 | 5         | 20     | 3   | ⚠️ LOW      | (3 < 5)           |
| Ổ cứng SSD 1TB    | SP004 | 10        | 50     | 0   | ❌ ZERO     | (0 = 0)           |
| RAM 16GB          | SP005 | 8         | 30     | 8   | = EXACT_MIN | (8 = min)         |
| Màn hình 27"      | SP006 | 1         | 8      | 8   | ✅ HIGH     | (8 = max)         |
| Cáp USB-C         | SP007 | 100       | 500    | 250 | ✅ NORMAL   | (100 < 250 < 500) |
| Nguồn 500W        | SP008 | 3         | 15     | 1   | ⚠️ LOW      | (1 < 3)           |
| Quạt tản nhiệt    | SP009 | 20        | 100    | 45  | ✅ NORMAL   | (20 < 45 < 100)   |
| Adapter HDMI      | SP010 | 30        | 150    | 140 | ✅ HIGH     | (140 gần max)     |
| Router WiFi       | SP011 | 2         | 12     | 1   | ⚠️ LOW      | (1 < 2)           |
| Bộ quang học      | SP012 | 1         | 5      | 0   | ❌ ZERO     | (0 = 0)           |

**Tóm tắt K001:**

- ✅ NORMAL: 4 sản phẩm
- ✅ HIGH: 2 sản phẩm
- ⚠️ LOW: 3 sản phẩm
- ❌ ZERO: 2 sản phẩm
- = EXACT_MIN: 1 sản phẩm
- **Total: 12 sản phẩm**

---

### 4.2 Kho 2 (K002 - Kho Chi Nhánh Hà Nội)

| Sản Phẩm          | Mã    | Tối Thiểu | Tối Đa | Tồn | Trạng Thái | Ghi Chú                          |
| ----------------- | ----- | --------- | ------ | --- | ---------- | -------------------------------- |
| Máy tính xách tay | SP001 | 2         | 10     | 4   | ✅ NORMAL  | (2 < 4 < 10) - chi nhánh ít hàng |
| Chuột không dây   | SP002 | 50        | 200    | 60  | ✅ NORMAL  | (50 < 60 < 200)                  |
| Bàn phím cơ       | SP003 | 5         | 20     | 2   | ⚠️ LOW     | (2 < 5)                          |
| Ổ cứng SSD 1TB    | SP004 | 10        | 50     | 15  | ✅ NORMAL  | (10 < 15 < 50) - K1 = 0          |
| RAM 16GB          | SP005 | 8         | 30     | 5   | ⚠️ LOW     | (5 < 8)                          |
| Màn hình 27"      | SP006 | 1         | 8      | 6   | ✅ NORMAL  | (1 < 6 < 8)                      |
| Cáp USB-C         | SP007 | 100       | 500    | 200 | ✅ NORMAL  | (100 < 200 < 500)                |
| Nguồn 500W        | SP008 | 3         | 15     | 0   | ❌ ZERO    | (0 = 0) - K1 = 1                 |
| Quạt tản nhiệt    | SP009 | 20        | 100    | 25  | ✅ NORMAL  | (20 < 25 < 100)                  |
| Adapter HDMI      | SP010 | 30        | 150    | 100 | ✅ NORMAL  | (30 < 100 < 150)                 |
| Router WiFi       | SP011 | 2         | 12     | 0   | ❌ ZERO    | (0 = 0)                          |
| Bộ quang học      | SP012 | 1         | 5      | 2   | ✅ NORMAL  | (1 < 2 < 5) - K1 = 0             |

**Tóm tắt K002:**

- ✅ NORMAL: 8 sản phẩm
- ⚠️ LOW: 2 sản phẩm
- ❌ ZERO: 2 sản phẩm
- **Total: 12 sản phẩm**

---

### 4.3 Kho 3 (K003 - Kho Tạm Đà Nẵng)

| Sản Phẩm          | Mã    | Tối Thiểu | Tối Đa | Tồn | Trạng Thái | Ghi Chú                     |
| ----------------- | ----- | --------- | ------ | --- | ---------- | --------------------------- |
| Máy tính xách tay | SP001 | 2         | 10     | 0   | ❌ ZERO    | (0 = 0) - kho tạm trống     |
| Chuột không dây   | SP002 | 50        | 200    | 150 | ✅ HIGH    | (150 gần max) - kho tạm đầy |
| Bàn phím cơ       | SP003 | 5         | 20     | 0   | ❌ ZERO    | (0 = 0)                     |
| Ổ cứng SSD 1TB    | SP004 | 10        | 50     | 35  | ✅ NORMAL  | (10 < 35 < 50)              |
| RAM 16GB          | SP005 | 8         | 30     | 20  | ✅ NORMAL  | (8 < 20 < 30)               |
| Màn hình 27"      | SP006 | 1         | 8      | 0   | ❌ ZERO    | (0 = 0)                     |
| Cáp USB-C         | SP007 | 100       | 500    | 350 | ✅ HIGH    | (350 gần max)               |
| Nguồn 500W        | SP008 | 3         | 15     | 10  | ✅ NORMAL  | (3 < 10 < 15)               |
| Quạt tản nhiệt    | SP009 | 20        | 100    | 60  | ✅ NORMAL  | (20 < 60 < 100)             |
| Adapter HDMI      | SP010 | 30        | 150    | 120 | ✅ HIGH    | (120 gần max)               |
| Router WiFi       | SP011 | 2         | 12     | 5   | ✅ NORMAL  | (2 < 5 < 12)                |
| Bộ quang học      | SP012 | 1         | 5      | 0   | ❌ ZERO    | (0 = 0)                     |

**Tóm tắt K003:**

- ✅ NORMAL: 5 sản phẩm
- ✅ HIGH: 3 sản phẩm
- ❌ ZERO: 4 sản phẩm
- **Total: 12 sản phẩm**

---

## 5. PHÂN BỐ TRẠNG THÁI TỒN KHO TOÀN HỆ THỐNG

| Loại Tồn    | Số Lượng | Phần Trăm | Ví Dụ                                                 |
| ----------- | -------- | --------- | ----------------------------------------------------- |
| ✅ NORMAL   | 17       | 47%       | SP001(K1,K2), SP007(K1), SP007(K3), ...               |
| ⚠️ LOW      | 5        | 14%       | SP003(K1,K2), SP005(K2), SP008(K1), SP011(K1)         |
| ❌ ZERO     | 8        | 22%       | SP004(K1), SP008(K2), SP012(K1,K2,K3), SP001(K3), ... |
| = EXACT_MIN | 1        | 3%        | SP005(K1)                                             |
| ✅ HIGH     | 5        | 14%       | SP002(K1), SP006(K1), SP002(K3), SP007(K3), SP010(K3) |
| **TOTAL**   | **36**   | **100%**  | 3 kho × 12 sản phẩm                                   |

---

## 6. TEST QUERIES

### 6.1 Xem Tất Cả Tồn Kho Kho Chính

```sql
SELECT sp."ma_san_pham", sp."ten_san_pham", sp."don_vi_tinh",
       sp."ton_toi_thieu", sp."ton_toi_da", tk."so_luong"
FROM "ton_kho" tk
JOIN "san_pham" sp ON tk."san_pham_id" = sp."id"
JOIN "kho" k ON tk."kho_id" = k."id"
WHERE k."ma_kho" = 'K001'
ORDER BY sp."ten_san_pham";
```

**Kết quả mong đợi**: 12 sản phẩm ở Kho K001

---

### 6.2 Lọc Tồn Thấp (Low Stock)

```sql
SELECT sp."ma_san_pham", sp."ten_san_pham", sp."ton_toi_thieu", tk."so_luong",
       (sp."ton_toi_thieu" - tk."so_luong") as "thieu_bao_nhieu"
FROM "ton_kho" tk
JOIN "san_pham" sp ON tk."san_pham_id" = sp."id"
JOIN "kho" k ON tk."kho_id" = k."id"
WHERE tk.so_luong < sp."ton_toi_thieu"
  AND k."ma_kho" = 'K001'
ORDER BY "thieu_bao_nhieu" DESC;
```

**Kết quả mong đợi**: 3 sản phẩm (SP003, SP008, SP011)

---

### 6.3 Lọc Tồn = 0 (Zero Stock)

```sql
SELECT sp."ma_san_pham", sp."ten_san_pham", k."ma_kho"
FROM "ton_kho" tk
JOIN "san_pham" sp ON tk."san_pham_id" = sp."id"
JOIN "kho" k ON tk."kho_id" = k."id"
WHERE tk."so_luong" = 0
ORDER BY k."ma_kho", sp."ma_san_pham";
```

**Kết quả mong đợi**: 8 sản phẩm Zero stock:

- K001: SP004, SP012
- K002: SP008, SP011
- K003: SP001, SP003, SP006, SP012

---

### 6.4 Thống Kê Tồn Kho Theo Kho

```sql
SELECT
  k."ma_kho" as "Kho",
  COUNT(*) as "Tong_SP",
  SUM(CASE WHEN tk."so_luong" = 0 THEN 1 ELSE 0 END) as "Zero",
  SUM(CASE WHEN tk."so_luong" < sp."ton_toi_thieu" THEN 1 ELSE 0 END) as "Low",
  SUM(CASE WHEN tk."so_luong" = sp."ton_toi_thieu" THEN 1 ELSE 0 END) as "Exact",
  SUM(CASE WHEN tk."so_luong" >= sp."ton_toi_da" THEN 1 ELSE 0 END) as "High",
  SUM(CASE WHEN tk."so_luong" > sp."ton_toi_thieu" AND tk."so_luong" < sp."ton_toi_da" THEN 1 ELSE 0 END) as "Normal"
FROM "ton_kho" tk
JOIN "san_pham" sp ON tk."san_pham_id" = sp."id"
JOIN "kho" k ON tk."kho_id" = k."id"
GROUP BY k."ma_kho"
ORDER BY k."ma_kho";
```

**Kết quả mong đợi**:

```
| Kho  | Tong_SP | Zero | Low | Exact | High | Normal |
|------|---------|------|-----|-------|------|--------|
| K001 | 12      | 2    | 3   | 1     | 2    | 4      |
| K002 | 12      | 2    | 2   | 0     | 0    | 8      |
| K003 | 12      | 4    | 0   | 0     | 3    | 5      |
```

---

### 6.5 Canh Báo Tồn Thấp (Alert)

```sql
SELECT
  sp."ma_san_pham",
  sp."ten_san_pham",
  k."ma_kho",
  k."ten_kho",
  tk."so_luong" as "ton_hien_tai",
  sp."ton_toi_thieu",
  (sp."ton_toi_thieu" - tk."so_luong") as "thieu",
  CASE
    WHEN tk."so_luong" = 0 THEN 'KHAN_CAP'
    WHEN tk."so_luong" <= sp."ton_toi_thieu" * 0.5 THEN 'CAO'
    WHEN tk."so_luong" <= sp."ton_toi_thieu" THEN 'TRUNG_BINH'
    ELSE 'THAP'
  END as "muc_do"
FROM "ton_kho" tk
JOIN "san_pham" sp ON tk."san_pham_id" = sp."id"
JOIN "kho" k ON tk."kho_id" = k."id"
WHERE tk."so_luong" <= sp."ton_toi_thieu"
ORDER BY muc_do DESC, k."ma_kho", sp."ma_san_pham";
```

**Kết quả mong đợi**: 13 canh báo (8 Zero + 5 Low)

---

## 7. MIGRATION EXECUTION

### Chạy Migration

Migration sẽ tự động chạy khi ứng dụng khởi động:

```bash
# Nếu dùng Maven
mvn spring-boot:run

# Nếu dùng Docker
docker-compose up
```

### Verify Data

Sau khi chạy migration, kiểm tra dữ liệu:

```bash
# Connect tới database
psql -U postgres -d sme_stocksense_db

# Chạy SQL verify queries (có trong V9)
SELECT COUNT(*) FROM danh_muc;      -- Kỳ vọng: 5
SELECT COUNT(*) FROM kho;           -- Kỳ vọng: 3
SELECT COUNT(*) FROM san_pham;      -- Kỳ vọng: 12
SELECT COUNT(*) FROM ton_kho;       -- Kỳ vọng: 36
```

---

## 8. DATA RELATIONSHIPS

### Entity Diagram

```
danh_muc (5 rows)
    ↓ (FK: danh_muc_id)
san_pham (12 rows)
    ↓ (FK: san_pham_id)
ton_kho (36 rows) ← kho (FK: kho_id - 3 rows)
```

### Foreign Key References

| Table    | Column      | References   | Expected Rows            |
| -------- | ----------- | ------------ | ------------------------ |
| san_pham | danh_muc_id | danh_muc(id) | 12 × 1 = 12              |
| ton_kho  | san_pham_id | san_pham(id) | 36 (3 kho × 12 sản phẩm) |
| ton_kho  | kho_id      | kho(id)      | 36                       |

**Unique Constraint**: ton_kho(san_pham_id, kho_id)

- Expected: 36 unique combinations (mỗi sản phẩm chỉ có 1 bản ghi tồn ở mỗi kho)

---

## 9. NOTES

### Dữ Liệu Đảm Bảo Test Scenarios

1. **Xem tồn kho**: ✅ Có dữ liệu đầy đủ ở 3 kho
2. **Lọc tồn bình thường**: ✅ 17 bản ghi NORMAL
3. **Lọc tồn thấp**: ✅ 5 bản ghi LOW + 8 bản ghi ZERO = 13 canh báo
4. **Lọc tồn = 0**: ✅ 8 bản ghi ZERO
5. **Lọc tồn cao**: ✅ 5 bản ghi HIGH
6. **Thống kê theo kho**: ✅ Phân bố khác nhau giữa 3 kho

### Không Modify Logic

- Chỉ seed dữ liệu
- Không thay đổi constraint, index
- Không modify Entity, Service, Controller
- Dữ liệu sẽ xóa khi reset database (`DELETE FROM ton_kho` trong migration)

---

## 10. ROLLBACK

Nếu cần rollback, không có migration lùi. Chỉ có thể:

```sql
-- Xóa dữ liệu seed
DELETE FROM ton_kho;
DELETE FROM san_pham;
DELETE FROM kho;
DELETE FROM danh_muc;

-- Hoặc reset database
DROP SCHEMA public CASCADE;
CREATE SCHEMA public;
-- Chạy lại tất cả migration từ V1
```

---

**Status**: ✅ COMPLETED  
**File Location**: `src/main/resources/db/migration/V9__seed_inventory_data.sql`  
**Prepared By**: T65 Seed Data Execution  
**Date**: 2026-06-21
