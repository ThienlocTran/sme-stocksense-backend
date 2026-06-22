# T65 - Chuẩn Bị Dữ Liệu Tồn Kho Mẫu

**Ngày**: 2026-06-21  
**Mục đích**: Tạo dữ liệu tồn kho mẫu để test các chức năng xem tồn, lọc, canh báo tồn thấp, lịch sử giao dịch (T64).

---

## 1. PLAN OVERVIEW (Kế Hoạch Tổng Quát)

### 1.1 Dữ Liệu Cần Tạo

```
Danh mục (Category)     → 5 danh mục
     ↓
Kho (Warehouse)         → 3 kho
     ↓
Sản phẩm (Product)      → 10-12 sản phẩm (khác nhau về ngưỡng)
     ↓
Tồn kho (ton_kho)       → 30-36 bản ghi (3 kho × 10-12 sản phẩm)
     ↓
Giao dịch (giao_dich_kho) → 20-30 bản ghi lịch sử (tùy chọn)
```

### 1.2 Chiến Lược Seed Data

**Danh mục (5 danh mục):**

1. Điện tử - Electronic devices
2. Linh kiện - Components & parts
3. Đồ dùng - Supplies
4. Thiết bị - Equipment
5. Phụ tùng - Accessories

**Kho (3 kho):**

1. Kho chính (Main warehouse) - TP.HCM
2. Kho chi nhánh - Hà Nội
3. Kho tạm (Temporary/Transit warehouse) - Đà Nẵng

**Sản phẩm (10 sản phẩm đa dạng):**

| STT | Tên               | Mã    | Danh Mục  | Đơn Vị | Giá        | Tồn Tối Thiểu | Tồn Tối Đa | Loại Tồn  |
| --- | ----------------- | ----- | --------- | ------ | ---------- | ------------- | ---------- | --------- |
| 1   | Máy tính xách tay | SP001 | Điện tử   | Cái    | 15,000,000 | 2             | 10         | Normal    |
| 2   | Chuột không dây   | SP002 | Linh kiện | Cái    | 250,000    | 50            | 200        | High      |
| 3   | Bàn phím cơ       | SP003 | Linh kiện | Cái    | 2,000,000  | 5             | 20         | Low       |
| 4   | Ổ cứng SSD 1TB    | SP004 | Điện tử   | Cái    | 3,500,000  | 10            | 50         | Zero      |
| 5   | RAM 16GB          | SP005 | Linh kiện | Cái    | 1,800,000  | 8             | 30         | Exact Min |
| 6   | Màn hình 27"      | SP006 | Điện tử   | Cái    | 8,000,000  | 1             | 8          | High      |
| 7   | Cáp USB-C         | SP007 | Đồ dùng   | Cái    | 150,000    | 100           | 500        | Normal    |
| 8   | Nguồn 500W        | SP008 | Thiết bị  | Cái    | 2,500,000  | 3             | 15         | Low       |
| 9   | Quạt tản nhiệt    | SP009 | Phụ tùng  | Cái    | 800,000    | 20            | 100        | Normal    |
| 10  | Adapter HDMI      | SP010 | Đồ dùng   | Cái    | 200,000    | 30            | 150        | High      |
| 11  | Router WiFi       | SP011 | Điện tử   | Cái    | 3,000,000  | 2             | 12         | Low       |
| 12  | Bộ quang học      | SP012 | Linh kiện | Cái    | 5,000,000  | 1             | 5          | Zero      |

---

## 2. INVENTORY DATA MATRIX (Ma Trận Dữ Liệu Tồn Kho)

### 2.1 Kho Chính (Main Warehouse - Kho 1)

Tồn kho tại Kho Chính:

| Sản Phẩm          | Mã    | Tối Thiểu | Tối Đa | Tồn Kho | Trạng Thái | Loại Test                         |
| ----------------- | ----- | --------- | ------ | ------- | ---------- | --------------------------------- |
| Máy tính xách tay | SP001 | 2         | 10     | 7       | ✅ Normal  | Normal (tối thiểu < tồn < tối đa) |
| Chuột không dây   | SP002 | 50        | 200    | 180     | ✅ High    | High (tồn gần tối đa)             |
| Bàn phím cơ       | SP003 | 5         | 20     | 3       | ⚠️ Low     | Low (tồn < tối thiểu)             |
| Ổ cứng SSD 1TB    | SP004 | 10        | 50     | 0       | ❌ Zero    | Zero (tồn = 0)                    |
| RAM 16GB          | SP005 | 8         | 30     | 8       | =          | Exact (tồn = tối thiểu)           |
| Màn hình 27"      | SP006 | 1         | 8      | 8       | ✅ High    | High (tồn = tối đa)               |
| Cáp USB-C         | SP007 | 100       | 500    | 250     | ✅ Normal  | Normal                            |
| Nguồn 500W        | SP008 | 3         | 15     | 1       | ⚠️ Low     | Low (tồn < tối thiểu)             |
| Quạt tản nhiệt    | SP009 | 20        | 100    | 45      | ✅ Normal  | Normal                            |
| Adapter HDMI      | SP010 | 30        | 150    | 140     | ✅ High    | High (tồn gần tối đa)             |
| Router WiFi       | SP011 | 2         | 12     | 1       | ⚠️ Low     | Low (tồn < tối thiểu)             |
| Bộ quang học      | SP012 | 1         | 5      | 0       | ❌ Zero    | Zero                              |

**Kho 1 Phân Tích:**

- ✅ Normal: 4 sản phẩm (SP001, SP007, SP009, SP006 - tối đa)
- ⚠️ Low Stock: 3 sản phẩm (SP003, SP008, SP011)
- ❌ Zero Stock: 2 sản phẩm (SP004, SP012)
- = Exact Min: 1 sản phẩm (SP005)
- ✅ High: 2 sản phẩm (SP002, SP010)

---

### 2.2 Kho Chi Nhánh (Branch Warehouse - Kho 2)

Tồn kho tại Kho Chi Nhánh (thường nhỏ hơn kho chính):

| Sản Phẩm          | Mã    | Tối Thiểu | Tối Đa | Tồn Kho | Trạng Thái | Ghi Chú                           |
| ----------------- | ----- | --------- | ------ | ------- | ---------- | --------------------------------- |
| Máy tính xách tay | SP001 | 2         | 10     | 4       | ✅ Normal  | Normal                            |
| Chuột không dây   | SP002 | 50        | 200    | 60      | ✅ Normal  | Normal (nhỏ hơn kho chính)        |
| Bàn phím cơ       | SP003 | 5         | 20     | 2       | ⚠️ Low     | Low                               |
| Ổ cứng SSD 1TB    | SP004 | 10        | 50     | 15      | ✅ Normal  | Chi nhánh có hàng (kho chính = 0) |
| RAM 16GB          | SP005 | 8         | 30     | 5       | ⚠️ Low     | Low (< tối thiểu)                 |
| Màn hình 27"      | SP006 | 1         | 8      | 6       | ✅ Normal  | Normal                            |
| Cáp USB-C         | SP007 | 100       | 500    | 200     | ✅ Normal  | Normal                            |
| Nguồn 500W        | SP008 | 3         | 15     | 0       | ❌ Zero    | Zero (kho chính = 1, kho này = 0) |
| Quạt tản nhiệt    | SP009 | 20        | 100    | 25      | ✅ Normal  | Normal (< kho chính)              |
| Adapter HDMI      | SP010 | 30        | 150    | 100     | ✅ Normal  | Normal                            |
| Router WiFi       | SP011 | 2         | 12     | 0       | ❌ Zero    | Zero                              |
| Bộ quang học      | SP012 | 1         | 5      | 2       | ✅ Normal  | Chi nhánh có hàng (kho chính = 0) |

**Kho 2 Phân Tích:**

- Tổng tồn thấp hơn Kho Chính (chi nhánh)
- Có 2 sản phẩm Zero (SP008, SP011)
- Có 2 sản phẩm Low (SP003, SP005)
- Khác biệt: SP004 và SP012 có tồn (kho chính = 0)

---

### 2.3 Kho Tạm (Transit Warehouse - Kho 3)

Tồn kho tại Kho Tạm (lưu trữ tạm thời):

| Sản Phẩm          | Mã    | Tối Thiểu | Tối Đa | Tồn Kho | Trạng Thái | Ghi Chú              |
| ----------------- | ----- | --------- | ------ | ------- | ---------- | -------------------- |
| Máy tính xách tay | SP001 | 2         | 10     | 0       | ❌ Zero    | Zero (kho tạm trống) |
| Chuột không dây   | SP002 | 50        | 200    | 150     | ✅ High    | High (kho tạm đầy)   |
| Bàn phím cơ       | SP003 | 5         | 20     | 0       | ❌ Zero    | Zero                 |
| Ổ cứng SSD 1TB    | SP004 | 10        | 50     | 35      | ✅ Normal  | Normal               |
| RAM 16GB          | SP005 | 8         | 30     | 20      | ✅ Normal  | Normal               |
| Màn hình 27"      | SP006 | 1         | 8      | 0       | ❌ Zero    | Zero                 |
| Cáp USB-C         | SP007 | 100       | 500    | 350     | ✅ High    | High                 |
| Nguồn 500W        | SP008 | 3         | 15     | 10      | ✅ Normal  | Normal               |
| Quạt tản nhiệt    | SP009 | 20        | 100    | 60      | ✅ Normal  | Normal               |
| Adapter HDMI      | SP010 | 30        | 150    | 120     | ✅ High    | High                 |
| Router WiFi       | SP011 | 2         | 12     | 5       | ✅ Normal  | Normal               |
| Bộ quang học      | SP012 | 1         | 5      | 0       | ❌ Zero    | Zero                 |

**Kho 3 Phân Tích:**

- Kho tạm có 4 sản phẩm Zero (SP001, SP003, SP006, SP012)
- Có tồn lớn cho Cáp/Adapter (SP002, SP007, SP010)
- Tương đối cân bằng cho linh kiện chính

---

## 3. DATA RELATIONSHIPS (Quan Hệ Dữ Liệu)

### 3.1 Entity Relationship Diagram

```
┌─────────────────────┐
│  danh_muc           │
│  (Category)         │
├─────────────────────┤
│ id (1-5)            │
│ ma_danh_muc         │
│ ten_danh_muc        │
│ trang_thai          │
└─────────────────────┘
         ▲
         │ (FK)
         │
┌────────┴────────────┐
│  san_pham           │
│  (Product)          │
│  SP001-SP012        │
├─────────────────────┤
│ id (1-12)           │
│ ma_san_pham         │
│ ten_san_pham        │
│ danh_muc_id (FK)    │
│ ton_toi_thieu       │
│ ton_toi_da          │
│ price               │
└─────────────────────┘
         ▲
         │ (FK x 12)
         │
    ┌────┴─────────────────┐
    │                      │
┌───┴──────────────┐  ┌───┴──────────────┐
│  ton_kho         │  │  ton_kho         │
│  (Kho 1)         │  │  (Kho 2)         │
│  (12 rows)       │  │  (12 rows)       │
│  san_pham_id: 1-12 │ │  san_pham_id: 1-12 │
│  kho_id: 1       │  │  kho_id: 2       │
└──────────────────┘  └──────────────────┘

┌──────────────────┐
│  ton_kho         │
│  (Kho 3)         │
│  (12 rows)       │
│  san_pham_id: 1-12 │
│  kho_id: 3       │
└──────────────────┘

     ┌─────────────────┐
     │  kho            │
     │  (Warehouse)    │
     ├─────────────────┤
     │ id (1-3)        │
     │ ma_kho          │
     │ ten_kho         │
     │ trang_thai      │
     └─────────────────┘
```

### 3.2 Referential Integrity

```
Product
  - danh_muc_id → Category.id (FK)
  - doi_tac_cung_cap_id → NULL (no partner data in seed)

ton_kho (Inventory Level)
  - san_pham_id → Product.id (FK) [12 references]
  - kho_id → Warehouse.id (FK) [3 references]
  - Unique: (san_pham_id, kho_id) → 36 unique combinations
```

---

## 4. SEED DATA CHECKLIST

### Bước 1: Tạo Danh Mục (Category) ✓

5 danh mục:

- [ ] DM001: Điện tử (Electronics)
- [ ] DM002: Linh kiện (Components)
- [ ] DM003: Đồ dùng (Supplies)
- [ ] DM004: Thiết bị (Equipment)
- [ ] DM005: Phụ tùng (Accessories)

### Bước 2: Tạo Kho (Warehouse) ✓

3 kho:

- [ ] K001: Kho Chính - TP.HCM
- [ ] K002: Kho Chi Nhánh - Hà Nội
- [ ] K003: Kho Tạm - Đà Nẵng

### Bước 3: Tạo Sản Phẩm (Product) ✓

12 sản phẩm (mỗi sản phẩm gán 1 danh mục):

- [ ] SP001 → DM001 (Máy tính)
- [ ] SP002 → DM002 (Chuột)
- [ ] SP003 → DM002 (Bàn phím)
- [ ] SP004 → DM001 (Ổ cứng)
- [ ] SP005 → DM002 (RAM)
- [ ] SP006 → DM001 (Màn hình)
- [ ] SP007 → DM003 (Cáp)
- [ ] SP008 → DM004 (Nguồn)
- [ ] SP009 → DM005 (Quạt)
- [ ] SP010 → DM003 (Adapter)
- [ ] SP011 → DM001 (Router)
- [ ] SP012 → DM002 (Quang học)

### Bước 4: Tạo Dữ Liệu Tồn Kho (ton_kho) ✓

36 bản ghi (3 kho × 12 sản phẩm):

- [ ] Kho 1: 12 bản ghi
- [ ] Kho 2: 12 bản ghi
- [ ] Kho 3: 12 bản ghi

**Phân bố trạng thái tồn kho:**

- 15+ bản ghi tồn bình thường (Normal)
- 5+ bản ghi tồn = 0 (Zero)
- 6+ bản ghi tồn < tối thiểu (Low)
- 2+ bản ghi tồn = tối thiểu (Exact Min)
- 5+ bản ghi tồn cao (High)

### Bước 5: Tạo Lịch Sử Giao Dịch (giao_dich_kho) - Tùy Chọn

Tạo ~20-30 giao dịch mẫu:

- [ ] Giao dịch nhập kho (NHAP_KHO)
- [ ] Giao dịch xuất kho (XUAT_KHO)
- [ ] Giao dịch import tồn đầu kỳ (NHAP_DAU_KY)
- [ ] Giao dịch điều chỉnh (DIEU_CHINH_TANG / DIEU_CHINH_GIAM)

---

## 5. DATA DISTRIBUTION SUMMARY

### 5.1 Tồn Kho Toàn Hệ Thống (All 3 Warehouses)

| Loại Tồn                | Số Lượng | Ví Dụ                                 |
| ----------------------- | -------- | ------------------------------------- |
| ✅ Bình thường (Normal) | ~16      | SP001(K1), SP007(K1), SP007(K3), v.v. |
| ⚠️ Tồn thấp (Low)       | ~6       | SP003(K1), SP008(K1), SP003(K2), v.v. |
| ❌ Tồn = 0 (Zero)       | ~7       | SP004(K1), SP012(K1), SP001(K3), v.v. |
| = Tồn = Min (Exact)     | 1        | SP005(K1)                             |
| ✅ Tồn cao (High)       | ~6       | SP002(K1), SP006(K1), SP002(K3), v.v. |
| **TOTAL**               | **36**   | 3 kho × 12 sản phẩm                   |

### 5.2 Tồn Kho Theo Kho

| Kho            | Tổng Sản Phẩm | Normal | Low   | Zero  | High  | Exact |
| -------------- | ------------- | ------ | ----- | ----- | ----- | ----- |
| K1 (Chính)     | 12            | 4      | 3     | 2     | 2     | 1     |
| K2 (Chi Nhánh) | 12            | 8      | 2     | 2     | 0     | 0     |
| K3 (Tạm)       | 12            | 5      | 0     | 4     | 3     | 0     |
| **TOTAL**      | **36**        | **17** | **5** | **8** | **5** | **1** |

---

## 6. TEST SCENARIOS (Kịch Bản Test)

### Scenario 1: Xem Tồn Kho

```
GET /api/inventory?warehouse=K001
→ Trả về 12 sản phẩm ở Kho Chính
→ Hiển thị: mã, tên, số lượng, tối thiểu, tối đa, trạng thái
```

### Scenario 2: Lọc Tồn Thấp

```
GET /api/inventory/low-stock?warehouse=K001
→ Trả về: SP003 (qty=3 < min=5), SP008 (qty=1 < min=3), SP011 (qty=1 < min=2)
→ Tổng 3 sản phẩm tồn thấp ở Kho Chính
```

### Scenario 3: Lọc Tồn = 0

```
GET /api/inventory?warehouse=K001&status=zero
→ Trả về: SP004, SP012 (2 sản phẩm)
```

### Scenario 4: Canh Báo Tồn Kho

```
GET /api/inventory/alerts
→ Trả về 8 canh báo (7 Zero + 6 Low - 1 overlapping) = ~13 alerts
→ Sắp xếp theo mức độ: KHAN_CAP (Zero), CAO (Low), TRUNG_BINH, THAP
```

### Scenario 5: Lịch Sử Giao Dịch (Nếu có giao dịch)

```
GET /api/inventory/SP001/K001/transactions
→ Trả về lịch sử biến động tồn kho của SP001 ở K001
→ Hiển thị: loại giao dịch, số lượng thay đổi, trước/sau, người tạo, ngày
```

---

## 7. SQL SCRIPT STRUCTURE (Cấu Trúc Script)

### Phase 1: Xóa Dữ Liệu Cũ

```sql
DELETE FROM ton_kho;
DELETE FROM san_pham;
DELETE FROM danh_muc;
DELETE FROM kho;
```

### Phase 2: Insert Danh Mục

```sql
INSERT INTO danh_muc (ma_danh_muc, ten_danh_muc, trang_thai, ngay_tao, ngay_cap_nhat)
VALUES (...)
```

### Phase 3: Insert Kho

```sql
INSERT INTO kho (ma_kho, ten_kho, dia_chi, trang_thai, ngay_tao, ngay_cap_nhat)
VALUES (...)
```

### Phase 4: Insert Sản Phẩm

```sql
INSERT INTO san_pham (ma_san_pham, ten_san_pham, don_vi_tinh, danh_muc_id,
                      ton_toi_thieu, ton_toi_da, price, trang_thai, ngay_tao, ngay_cap_nhat)
VALUES (...)
```

### Phase 5: Insert Tồn Kho

```sql
INSERT INTO ton_kho (san_pham_id, kho_id, so_luong, ngay_cap_nhat)
VALUES (...)
```

### Phase 6: Insert Giao Dịch (Tùy Chọn)

```sql
INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong,
                           so_luong_truoc, so_luong_sau, nguoi_tao_id, ngay_tao)
VALUES (...)
```

---

## 8. FILES TO BE GENERATED (Các File Sẽ Tạo)

Sau khi bạn xác nhận, tôi sẽ sinh:

1. 📄 **V9\_\_seed_inventory_data.sql** (Flyway Migration)
   - Seed dữ liệu tồn kho (ton_kho)
   - Seed sản phẩm (san_pham)
   - Seed kho (kho)
   - Seed danh mục (danh_muc)

2. 📄 **seed_inventory_transactions.sql** (Tùy Chọn)
   - Seed giao dịch mẫu (giao_dich_kho)
   - Tạo lịch sử cho test

3. 📋 **SEED_DATA_DETAILS.md** (Tài Liệu)
   - Danh sách đầy đủ dữ liệu sẽ insert
   - ID references
   - Test queries

---

## 9. EXECUTION COMPLETE ✅

Đã sinh toàn bộ dữ liệu seed:

✅ **File 1**: V9\_\_seed_inventory_data.sql (Flyway Migration)

- Location: `src/main/resources/db/migration/V9__seed_inventory_data.sql`
- Nội dung: INSERT 5 danh mục, 3 kho, 12 sản phẩm, 36 bản ghi tồn kho
- Includes: Verify queries tại cuối file

✅ **File 2**: SEED_DATA_DETAILS.md (Tài Liệu)

- Location: `docs/SEED_DATA_DETAILS.md`
- Nội dung: Chi tiết từng bản ghi, test queries, phân tích dữ liệu
- Includes: 6 SQL test queries mẫu

✅ **Khác**:

- Không modify logic hệ thống
- Dữ liệu seed sẽ tự chạy khi app khởi động (Flyway)
- Có thể rollback bằng reset database

---

**Status**: ✅ COMPLETED  
**Report Date**: 2026-06-21  
**Prepared By**: T65 Seed Data Execution
