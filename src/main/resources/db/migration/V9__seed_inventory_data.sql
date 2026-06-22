-- ============================================================================
-- V9__seed_inventory_data.sql
-- Flyway Migration: Seed dữ liệu tồn kho mẫu cho T65
-- ============================================================================
-- Dữ liệu: 5 danh mục, 3 kho, 12 sản phẩm, 36 bản ghi tồn kho
-- ============================================================================

-- ============================================================================
-- PHASE 1: Clean up (xóa dữ liệu cũ nếu tồn tại)
-- ============================================================================
-- DELETE FROM ton_kho;
-- DELETE FROM san_pham;
-- DELETE FROM kho;
-- DELETE FROM danh_muc;
-- Không xóa để tránh lỗi nếu migration chạy lại

-- ============================================================================
-- PHASE 2: Insert Danh Mục (Category) - 5 rows
-- ============================================================================

INSERT INTO "danh_muc" ("ma_danh_muc", "ten_danh_muc", "mo_ta", "trang_thai", "ngay_tao", "ngay_cap_nhat")
VALUES 
  ('DM001', 'Điện tử', 'Thiết bị điện tử: máy tính, màn hình, router', 'HOAT_DONG'::trang_thai_danh_muc, now(), now()),
  ('DM002', 'Linh kiện', 'Linh kiện máy tính: RAM, CPU, bàn phím, chuột', 'HOAT_DONG'::trang_thai_danh_muc, now(), now()),
  ('DM003', 'Đồ dùng', 'Đồ dùng văn phòng: cáp, adapter, dây điện', 'HOAT_DONG'::trang_thai_danh_muc, now(), now()),
  ('DM004', 'Thiết bị', 'Thiết bị máy tính: nguồn điện, quạt tản nhiệt', 'HOAT_DONG'::trang_thai_danh_muc, now(), now()),
  ('DM005', 'Phụ tùng', 'Phụ tùng phụ: bộ quang học, vít, keo', 'HOAT_DONG'::trang_thai_danh_muc, now(), now())
ON CONFLICT ("ma_danh_muc") DO UPDATE
SET "ten_danh_muc" = EXCLUDED."ten_danh_muc",
    "mo_ta" = EXCLUDED."mo_ta",
    "ngay_cap_nhat" = now();

-- ============================================================================
-- PHASE 3: Insert Kho (Warehouse) - 3 rows
-- ============================================================================

INSERT INTO "kho" ("ma_kho", "ten_kho", "dia_chi", "trang_thai", "ngay_tao", "ngay_cap_nhat")
VALUES 
  ('K001', 'Kho Chính TP.HCM', 'Số 123 Đường Nguyễn Hữu Cảnh, Phường Tân Phú, Quận 7, TP.HCM', 'HOAT_DONG'::trang_thai_kho, now(), now()),
  ('K002', 'Kho Chi Nhánh Hà Nội', 'Số 456 Đường Cầu Giấy, Quận Cầu Giấy, Hà Nội', 'HOAT_DONG'::trang_thai_kho, now(), now()),
  ('K003', 'Kho Tạm Đà Nẵng', 'Số 789 Đường Hải Phòng, Quận Hải Châu, Đà Nẵng', 'HOAT_DONG'::trang_thai_kho, now(), now())
ON CONFLICT ("ma_kho") DO UPDATE
SET "ten_kho" = EXCLUDED."ten_kho",
    "dia_chi" = EXCLUDED."dia_chi",
    "ngay_cap_nhat" = now();

-- ============================================================================
-- PHASE 4: Insert Sản Phẩm (Product) - 12 rows
-- ============================================================================

INSERT INTO "san_pham" (
  "ma_san_pham", "ten_san_pham", "sku", "ma_vach", "don_vi_tinh", 
  "danh_muc_id", "ton_toi_thieu", "ton_toi_da", "price", 
  "thoi_gian_giao_hang", "trang_thai", "ngay_tao", "ngay_cap_nhat"
)
SELECT 
  'SP001', 'Máy tính xách tay Dell XPS 13', 'SKU-DELL-XPS13', 'BARCODE-001', 'Cái',
  dm.id, 2, 10, 15000000.00, 7, 'HOAT_DONG'::trang_thai_san_pham, now(), now()
FROM "danh_muc" dm WHERE dm."ma_danh_muc" = 'DM001'
ON CONFLICT ("ma_san_pham") DO UPDATE
SET "ten_san_pham" = EXCLUDED."ten_san_pham",
    "ngay_cap_nhat" = now();

INSERT INTO "san_pham" (
  "ma_san_pham", "ten_san_pham", "sku", "ma_vach", "don_vi_tinh", 
  "danh_muc_id", "ton_toi_thieu", "ton_toi_da", "price", 
  "thoi_gian_giao_hang", "trang_thai", "ngay_tao", "ngay_cap_nhat"
)
SELECT 
  'SP002', 'Chuột không dây Logitech MX Master 3', 'SKU-LOGITECH-MX3', 'BARCODE-002', 'Cái',
  dm.id, 50, 200, 250000.00, 3, 'HOAT_DONG'::trang_thai_san_pham, now(), now()
FROM "danh_muc" dm WHERE dm."ma_danh_muc" = 'DM002'
ON CONFLICT ("ma_san_pham") DO UPDATE
SET "ten_san_pham" = EXCLUDED."ten_san_pham",
    "ngay_cap_nhat" = now();

INSERT INTO "san_pham" (
  "ma_san_pham", "ten_san_pham", "sku", "ma_vach", "don_vi_tinh", 
  "danh_muc_id", "ton_toi_thieu", "ton_toi_da", "price", 
  "thoi_gian_giao_hang", "trang_thai", "ngay_tao", "ngay_cap_nhat"
)
SELECT 
  'SP003', 'Bàn phím cơ Corsair K95 RGB Platinum', 'SKU-CORSAIR-K95', 'BARCODE-003', 'Cái',
  dm.id, 5, 20, 2000000.00, 5, 'HOAT_DONG'::trang_thai_san_pham, now(), now()
FROM "danh_muc" dm WHERE dm."ma_danh_muc" = 'DM002'
ON CONFLICT ("ma_san_pham") DO UPDATE
SET "ten_san_pham" = EXCLUDED."ten_san_pham",
    "ngay_cap_nhat" = now();

INSERT INTO "san_pham" (
  "ma_san_pham", "ten_san_pham", "sku", "ma_vach", "don_vi_tinh", 
  "danh_muc_id", "ton_toi_thieu", "ton_toi_da", "price", 
  "thoi_gian_giao_hang", "trang_thai", "ngay_tao", "ngay_cap_nhat"
)
SELECT 
  'SP004', 'Ổ cứng SSD Samsung 970 EVO 1TB', 'SKU-SAMSUNG-970-1TB', 'BARCODE-004', 'Cái',
  dm.id, 10, 50, 3500000.00, 3, 'HOAT_DONG'::trang_thai_san_pham, now(), now()
FROM "danh_muc" dm WHERE dm."ma_danh_muc" = 'DM001'
ON CONFLICT ("ma_san_pham") DO UPDATE
SET "ten_san_pham" = EXCLUDED."ten_san_pham",
    "ngay_cap_nhat" = now();

INSERT INTO "san_pham" (
  "ma_san_pham", "ten_san_pham", "sku", "ma_vach", "don_vi_tinh", 
  "danh_muc_id", "ton_toi_thieu", "ton_toi_da", "price", 
  "thoi_gian_giao_hang", "trang_thai", "ngay_tao", "ngay_cap_nhat"
)
SELECT 
  'SP005', 'RAM DDR4 Corsair Vengeance 16GB', 'SKU-CORSAIR-RAM-16GB', 'BARCODE-005', 'Cái',
  dm.id, 8, 30, 1800000.00, 4, 'HOAT_DONG'::trang_thai_san_pham, now(), now()
FROM "danh_muc" dm WHERE dm."ma_danh_muc" = 'DM002'
ON CONFLICT ("ma_san_pham") DO UPDATE
SET "ten_san_pham" = EXCLUDED."ten_san_pham",
    "ngay_cap_nhat" = now();

INSERT INTO "san_pham" (
  "ma_san_pham", "ten_san_pham", "sku", "ma_vach", "don_vi_tinh", 
  "danh_muc_id", "ton_toi_thieu", "ton_toi_da", "price", 
  "thoi_gian_giao_hang", "trang_thai", "ngay_tao", "ngay_cap_nhat"
)
SELECT 
  'SP006', 'Màn hình Dell UltraSharp 27" 4K', 'SKU-DELL-UP2720Q', 'BARCODE-006', 'Cái',
  dm.id, 1, 8, 8000000.00, 5, 'HOAT_DONG'::trang_thai_san_pham, now(), now()
FROM "danh_muc" dm WHERE dm."ma_danh_muc" = 'DM001'
ON CONFLICT ("ma_san_pham") DO UPDATE
SET "ten_san_pham" = EXCLUDED."ten_san_pham",
    "ngay_cap_nhat" = now();

INSERT INTO "san_pham" (
  "ma_san_pham", "ten_san_pham", "sku", "ma_vach", "don_vi_tinh", 
  "danh_muc_id", "ton_toi_thieu", "ton_toi_da", "price", 
  "thoi_gian_giao_hang", "trang_thai", "ngay_tao", "ngay_cap_nhat"
)
SELECT 
  'SP007', 'Cáp USB-C 2m Anker Powerline', 'SKU-ANKER-USB-C', 'BARCODE-007', 'Cái',
  dm.id, 100, 500, 150000.00, 2, 'HOAT_DONG'::trang_thai_san_pham, now(), now()
FROM "danh_muc" dm WHERE dm."ma_danh_muc" = 'DM003'
ON CONFLICT ("ma_san_pham") DO UPDATE
SET "ten_san_pham" = EXCLUDED."ten_san_pham",
    "ngay_cap_nhat" = now();

INSERT INTO "san_pham" (
  "ma_san_pham", "ten_san_pham", "sku", "ma_vach", "don_vi_tinh", 
  "danh_muc_id", "ton_toi_thieu", "ton_toi_da", "price", 
  "thoi_gian_giao_hang", "trang_thai", "ngay_tao", "ngay_cap_nhat"
)
SELECT 
  'SP008', 'Nguồn Seasonic Focus Gold 500W', 'SKU-SEASONIC-500W', 'BARCODE-008', 'Cái',
  dm.id, 3, 15, 2500000.00, 5, 'HOAT_DONG'::trang_thai_san_pham, now(), now()
FROM "danh_muc" dm WHERE dm."ma_danh_muc" = 'DM004'
ON CONFLICT ("ma_san_pham") DO UPDATE
SET "ten_san_pham" = EXCLUDED."ten_san_pham",
    "ngay_cap_nhat" = now();

INSERT INTO "san_pham" (
  "ma_san_pham", "ten_san_pham", "sku", "ma_vach", "don_vi_tinh", 
  "danh_muc_id", "ton_toi_thieu", "ton_toi_da", "price", 
  "thoi_gian_giao_hang", "trang_thai", "ngay_tao", "ngay_cap_nhat"
)
SELECT 
  'SP009', 'Quạt tản nhiệt Noctua NF-F12 PWM', 'SKU-NOCTUA-FAN-12', 'BARCODE-009', 'Cái',
  dm.id, 20, 100, 800000.00, 4, 'HOAT_DONG'::trang_thai_san_pham, now(), now()
FROM "danh_muc" dm WHERE dm."ma_danh_muc" = 'DM004'
ON CONFLICT ("ma_san_pham") DO UPDATE
SET "ten_san_pham" = EXCLUDED."ten_san_pham",
    "ngay_cap_nhat" = now();

INSERT INTO "san_pham" (
  "ma_san_pham", "ten_san_pham", "sku", "ma_vach", "don_vi_tinh", 
  "danh_muc_id", "ton_toi_thieu", "ton_toi_da", "price", 
  "thoi_gian_giao_hang", "trang_thai", "ngay_tao", "ngay_cap_nhat"
)
SELECT 
  'SP010', 'Adapter HDMI 2.1 4K60Hz', 'SKU-HDMI-ADAPTER-21', 'BARCODE-010', 'Cái',
  dm.id, 30, 150, 200000.00, 2, 'HOAT_DONG'::trang_thai_san_pham, now(), now()
FROM "danh_muc" dm WHERE dm."ma_danh_muc" = 'DM003'
ON CONFLICT ("ma_san_pham") DO UPDATE
SET "ten_san_pham" = EXCLUDED."ten_san_pham",
    "ngay_cap_nhat" = now();

INSERT INTO "san_pham" (
  "ma_san_pham", "ten_san_pham", "sku", "ma_vach", "don_vi_tinh", 
  "danh_muc_id", "ton_toi_thieu", "ton_toi_da", "price", 
  "thoi_gian_giao_hang", "trang_thai", "ngay_tao", "ngay_cap_nhat"
)
SELECT 
  'SP011', 'Router WiFi 6 TP-Link Archer AX6000', 'SKU-TP-LINK-AX6000', 'BARCODE-011', 'Cái',
  dm.id, 2, 12, 3000000.00, 5, 'HOAT_DONG'::trang_thai_san_pham, now(), now()
FROM "danh_muc" dm WHERE dm."ma_danh_muc" = 'DM001'
ON CONFLICT ("ma_san_pham") DO UPDATE
SET "ten_san_pham" = EXCLUDED."ten_san_pham",
    "ngay_cap_nhat" = now();

INSERT INTO "san_pham" (
  "ma_san_pham", "ten_san_pham", "sku", "ma_vach", "don_vi_tinh", 
  "danh_muc_id", "ton_toi_thieu", "ton_toi_da", "price", 
  "thoi_gian_giao_hang", "trang_thai", "ngay_tao", "ngay_cap_nhat"
)
SELECT 
  'SP012', 'Bộ quang học HD 2.5X', 'SKU-OPTICAL-2.5X', 'BARCODE-012', 'Cái',
  dm.id, 1, 5, 5000000.00, 7, 'HOAT_DONG'::trang_thai_san_pham, now(), now()
FROM "danh_muc" dm WHERE dm."ma_danh_muc" = 'DM002'
ON CONFLICT ("ma_san_pham") DO UPDATE
SET "ten_san_pham" = EXCLUDED."ten_san_pham",
    "ngay_cap_nhat" = now();

-- ============================================================================
-- PHASE 5: Insert Tồn Kho (ton_kho) - 36 rows (3 kho × 12 sản phẩm)
-- ============================================================================

-- Kho 1 (Kho Chính) - 12 sản phẩm
INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 7, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP001' AND k."ma_kho" = 'K001'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 180, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP002' AND k."ma_kho" = 'K001'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 3, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP003' AND k."ma_kho" = 'K001'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 0, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP004' AND k."ma_kho" = 'K001'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 8, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP005' AND k."ma_kho" = 'K001'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 8, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP006' AND k."ma_kho" = 'K001'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 250, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP007' AND k."ma_kho" = 'K001'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 1, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP008' AND k."ma_kho" = 'K001'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 45, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP009' AND k."ma_kho" = 'K001'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 140, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP010' AND k."ma_kho" = 'K001'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 1, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP011' AND k."ma_kho" = 'K001'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 0, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP012' AND k."ma_kho" = 'K001'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

-- Kho 2 (Kho Chi Nhánh) - 12 sản phẩm
INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 4, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP001' AND k."ma_kho" = 'K002'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 60, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP002' AND k."ma_kho" = 'K002'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 2, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP003' AND k."ma_kho" = 'K002'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 15, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP004' AND k."ma_kho" = 'K002'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 5, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP005' AND k."ma_kho" = 'K002'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 6, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP006' AND k."ma_kho" = 'K002'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 200, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP007' AND k."ma_kho" = 'K002'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 0, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP008' AND k."ma_kho" = 'K002'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 25, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP009' AND k."ma_kho" = 'K002'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 100, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP010' AND k."ma_kho" = 'K002'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 0, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP011' AND k."ma_kho" = 'K002'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 2, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP012' AND k."ma_kho" = 'K002'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

-- Kho 3 (Kho Tạm) - 12 sản phẩm
INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 0, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP001' AND k."ma_kho" = 'K003'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 150, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP002' AND k."ma_kho" = 'K003'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 0, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP003' AND k."ma_kho" = 'K003'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 35, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP004' AND k."ma_kho" = 'K003'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 20, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP005' AND k."ma_kho" = 'K003'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 0, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP006' AND k."ma_kho" = 'K003'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 350, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP007' AND k."ma_kho" = 'K003'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 10, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP008' AND k."ma_kho" = 'K003'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 60, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP009' AND k."ma_kho" = 'K003'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 120, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP010' AND k."ma_kho" = 'K003'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 5, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP011' AND k."ma_kho" = 'K003'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

INSERT INTO "ton_kho" ("san_pham_id", "kho_id", "so_luong", "ngay_cap_nhat")
SELECT sp.id, k.id, 0, now()
FROM "san_pham" sp JOIN "kho" k ON TRUE
WHERE sp."ma_san_pham" = 'SP012' AND k."ma_kho" = 'K003'
ON CONFLICT ("san_pham_id", "kho_id") DO UPDATE
SET "so_luong" = EXCLUDED."so_luong", "ngay_cap_nhat" = now();

-- ============================================================================
-- PHASE 6: Verify Data (Kiểm tra dữ liệu đã insert)
-- ============================================================================

-- Verify categories
/*
-- Verify categories
SELECT '=== DANH MỤC ===' as info;
SELECT "ma_danh_muc", "ten_danh_muc", "trang_thai" FROM "danh_muc" ORDER BY "ma_danh_muc";

-- Verify warehouses
SELECT '=== KHO ===' as info;
SELECT "ma_kho", "ten_kho", "trang_thai" FROM "kho" ORDER BY "ma_kho";

-- Verify products
SELECT '=== SẢN PHẨM ===' as info;
SELECT sp."ma_san_pham", sp."ten_san_pham", dm."ma_danh_muc", sp."ton_toi_thieu", sp."ton_toi_da" 
FROM "san_pham" sp 
LEFT JOIN "danh_muc" dm ON sp."danh_muc_id" = dm."id"
ORDER BY sp."ma_san_pham";

-- Verify inventory
SELECT '=== TỒN KHO ===' as info;
SELECT k."ma_kho", sp."ma_san_pham", sp."ten_san_pham", sp."ton_toi_thieu", sp."ton_toi_da", 
       tk."so_luong",
       CASE 
         WHEN tk."so_luong" = 0 THEN '❌ ZERO'
         WHEN tk."so_luong" < sp."ton_toi_thieu" THEN '⚠️ LOW'
         WHEN tk."so_luong" = sp."ton_toi_thieu" THEN '= EXACT_MIN'
         WHEN tk."so_luong" >= sp."ton_toi_da" THEN '✅ HIGH'
         ELSE '✅ NORMAL'
       END as status
FROM "ton_kho" tk
JOIN "san_pham" sp ON tk."san_pham_id" = sp."id"
JOIN "kho" k ON tk."kho_id" = k."id"
ORDER BY k."ma_kho", sp."ma_san_pham";

-- Count by status
SELECT '=== PHÂN BỐ TRẠNG THÁI TỒN ===' as info;
SELECT k."ma_kho",
       COUNT(*) FILTER (WHERE tk."so_luong" = 0) as "Zero",
       COUNT(*) FILTER (WHERE tk."so_luong" < sp."ton_toi_thieu") as "Low",
       COUNT(*) FILTER (WHERE tk."so_luong" = sp."ton_toi_thieu") as "ExactMin",
       COUNT(*) FILTER (WHERE tk."so_luong" >= sp."ton_toi_da") as "High",
       COUNT(*) FILTER (WHERE tk."so_luong" > sp."ton_toi_thieu" AND tk."so_luong" < sp."ton_toi_da") as "Normal",
       COUNT(*) as "Total"
FROM "ton_kho" tk
JOIN "san_pham" sp ON tk."san_pham_id" = sp."id"
JOIN "kho" k ON tk."kho_id" = k."id"
GROUP BY k."ma_kho"
ORDER BY k."ma_kho";
*/
