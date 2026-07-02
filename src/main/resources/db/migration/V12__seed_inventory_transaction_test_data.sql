-- ============================================================================
-- V12__seed_inventory_transaction_test_data.sql
-- Flyway migration: Seed deterministic inventory transaction history data for T72 UI test
-- ============================================================================

-- NOTE: This script inserts only into giao_dich_kho. It uses lookup subqueries for
--       san_pham.id, kho.id, and nhan_vien.id based on existing seed data.
--
-- Rollback guidance:
--   DELETE FROM giao_dich_kho
--   WHERE nguoi_tao_id IN (
--     (SELECT id FROM nhan_vien WHERE email = 'admin@example.com'),
--     (SELECT id FROM nhan_vien WHERE email = 'tranthienloc21102005@gmail.com'),
--     (SELECT id FROM nhan_vien WHERE email = 'tranthienloc.nina@gmail.com')
--   )
--   AND ngay_tao BETWEEN TIMESTAMP '2026-04-28 00:00:00' AND TIMESTAMP '2026-06-30 23:59:59';

-- ============================================================================
-- Product / Warehouse / Employee coverage
--   Warehouses: K001, K002, K003
--   Products: SP001, SP002, SP003, SP004, SP005, SP006, SP007, SP008, SP009, SP010
--   Employees: admin@example.com, tranthienloc21102005@gmail.com, tranthienloc.nina@gmail.com
--   Transaction types: NHAP_DAU_KY, NHAP_KHO, XUAT_KHO, DIEU_CHINH_TANG, DIEU_CHINH_GIAM
-- ============================================================================

-- ============================================================================
-- 1. SP001 @ K001
-- ============================================================================
INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'NHAP_DAU_KY'::loai_giao_dich_kho, 10, 0, 10, 'Nhập đầu kỳ', e.id, TIMESTAMP '2026-04-28 08:00:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP001' AND k.ma_kho = 'K001' AND e.email = 'admin@example.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'XUAT_KHO'::loai_giao_dich_kho, 5, 10, 5, 'Xuất bình thường', e.id, TIMESTAMP '2026-04-30 09:30:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP001' AND k.ma_kho = 'K001' AND e.email = 'admin@example.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'NHAP_KHO'::loai_giao_dich_kho, 2, 5, 7, 'Nhập bổ sung', e.id, TIMESTAMP '2026-05-02 08:45:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP001' AND k.ma_kho = 'K001' AND e.email = 'admin@example.com';

-- ============================================================================
-- 2. SP002 @ K001
-- ============================================================================
INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'NHAP_DAU_KY'::loai_giao_dich_kho, 180, 0, 180, 'Nhập đầu kỳ', e.id, TIMESTAMP '2026-04-29 08:15:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP002' AND k.ma_kho = 'K001' AND e.email = 'tranthienloc21102005@gmail.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'XUAT_KHO'::loai_giao_dich_kho, 50, 180, 130, 'Xuất lớn', e.id, TIMESTAMP '2026-05-01 10:00:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP002' AND k.ma_kho = 'K001' AND e.email = 'tranthienloc21102005@gmail.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'DIEU_CHINH_TANG'::loai_giao_dich_kho, 70, 130, 200, 'Điều chỉnh tăng tồn', e.id, TIMESTAMP '2026-05-03 09:15:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP002' AND k.ma_kho = 'K001' AND e.email = 'tranthienloc21102005@gmail.com';

-- ============================================================================
-- 3. SP006 @ K003
-- ============================================================================
INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'NHAP_DAU_KY'::loai_giao_dich_kho, 7, 0, 7, 'Nhập đầu kỳ', e.id, TIMESTAMP '2026-04-30 08:20:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP006' AND k.ma_kho = 'K003' AND e.email = 'tranthienloc.nina@gmail.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'XUAT_KHO'::loai_giao_dich_kho, 5, 7, 2, 'Xuất gần hết', e.id, TIMESTAMP '2026-05-02 11:00:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP006' AND k.ma_kho = 'K003' AND e.email = 'tranthienloc.nina@gmail.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'DIEU_CHINH_GIAM'::loai_giao_dich_kho, 2, 2, 0, 'Điều chỉnh giảm do kiểm kê', e.id, TIMESTAMP '2026-05-04 09:10:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP006' AND k.ma_kho = 'K003' AND e.email = 'tranthienloc.nina@gmail.com';

-- ============================================================================
-- 4. SP003 @ K002
-- ============================================================================
INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'NHAP_KHO'::loai_giao_dich_kho, 2, 0, 2, 'Nhập bổ sung', e.id, TIMESTAMP '2026-05-01 08:30:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP003' AND k.ma_kho = 'K002' AND e.email = 'admin@example.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'XUAT_KHO'::loai_giao_dich_kho, 1, 2, 1, 'Xuất nhỏ', e.id, TIMESTAMP '2026-05-03 08:50:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP003' AND k.ma_kho = 'K002' AND e.email = 'admin@example.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'NHAP_KHO'::loai_giao_dich_kho, 1, 1, 2, 'Nhập bổ sung', e.id, TIMESTAMP '2026-05-05 08:30:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP003' AND k.ma_kho = 'K002' AND e.email = 'admin@example.com';

-- ============================================================================
-- 5. SP004 @ K002
-- ============================================================================
INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'NHAP_KHO'::loai_giao_dich_kho, 10, 0, 10, 'Nhập bổ sung lớn', e.id, TIMESTAMP '2026-05-04 08:15:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP004' AND k.ma_kho = 'K002' AND e.email = 'tranthienloc21102005@gmail.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'XUAT_KHO'::loai_giao_dich_kho, 3, 10, 7, 'Xuất bình thường', e.id, TIMESTAMP '2026-05-06 09:05:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP004' AND k.ma_kho = 'K002' AND e.email = 'tranthienloc21102005@gmail.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'DIEU_CHINH_TANG'::loai_giao_dich_kho, 8, 7, 15, 'Điều chỉnh tăng để bù thiếu', e.id, TIMESTAMP '2026-05-08 10:10:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP004' AND k.ma_kho = 'K002' AND e.email = 'tranthienloc21102005@gmail.com';

-- ============================================================================
-- 6. SP005 @ K002
-- ============================================================================
INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'NHAP_KHO'::loai_giao_dich_kho, 8, 0, 8, 'Nhập bổ sung', e.id, TIMESTAMP '2026-05-05 08:00:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP005' AND k.ma_kho = 'K002' AND e.email = 'tranthienloc.nina@gmail.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'XUAT_KHO'::loai_giao_dich_kho, 2, 8, 6, 'Xuất nhỏ', e.id, TIMESTAMP '2026-05-07 08:20:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP005' AND k.ma_kho = 'K002' AND e.email = 'tranthienloc.nina@gmail.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'DIEU_CHINH_GIAM'::loai_giao_dich_kho, 1, 6, 5, 'Điều chỉnh giảm do kiểm kê', e.id, TIMESTAMP '2026-05-09 09:20:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP005' AND k.ma_kho = 'K002' AND e.email = 'tranthienloc.nina@gmail.com';

-- ============================================================================
-- 7. SP007 @ K003
-- ============================================================================
INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'NHAP_KHO'::loai_giao_dich_kho, 400, 0, 400, 'Nhập số lượng lớn', e.id, TIMESTAMP '2026-05-06 08:10:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP007' AND k.ma_kho = 'K003' AND e.email = 'admin@example.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'XUAT_KHO'::loai_giao_dich_kho, 30, 400, 370, 'Xuất nhiều lần liên tiếp', e.id, TIMESTAMP '2026-05-08 08:25:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP007' AND k.ma_kho = 'K003' AND e.email = 'admin@example.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'XUAT_KHO'::loai_giao_dich_kho, 20, 370, 350, 'Xuất nhiều lần liên tiếp', e.id, TIMESTAMP '2026-05-10 09:00:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP007' AND k.ma_kho = 'K003' AND e.email = 'admin@example.com';

-- ============================================================================
-- 8. SP008 @ K003
-- ============================================================================
INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'NHAP_KHO'::loai_giao_dich_kho, 12, 0, 12, 'Nhập bổ sung nhỏ', e.id, TIMESTAMP '2026-05-07 08:05:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP008' AND k.ma_kho = 'K003' AND e.email = 'admin@example.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'XUAT_KHO'::loai_giao_dich_kho, 1, 12, 11, 'Xuất nhỏ', e.id, TIMESTAMP '2026-05-09 08:40:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP008' AND k.ma_kho = 'K003' AND e.email = 'admin@example.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'XUAT_KHO'::loai_giao_dich_kho, 1, 11, 10, 'Xuất lặp lại', e.id, TIMESTAMP '2026-05-11 09:10:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP008' AND k.ma_kho = 'K003' AND e.email = 'admin@example.com';

-- ============================================================================
-- 9. SP009 @ K001
-- ============================================================================
INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'NHAP_KHO'::loai_giao_dich_kho, 45, 0, 45, 'Nhập số lượng vừa đủ', e.id, TIMESTAMP '2026-05-08 09:00:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP009' AND k.ma_kho = 'K001' AND e.email = 'tranthienloc21102005@gmail.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'XUAT_KHO'::loai_giao_dich_kho, 5, 45, 40, 'Xuất bình thường', e.id, TIMESTAMP '2026-05-12 09:20:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP009' AND k.ma_kho = 'K001' AND e.email = 'tranthienloc21102005@gmail.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'DIEU_CHINH_TANG'::loai_giao_dich_kho, 5, 40, 45, 'Điều chỉnh tăng sau kiểm kê', e.id, TIMESTAMP '2026-05-14 10:40:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP009' AND k.ma_kho = 'K001' AND e.email = 'tranthienloc21102005@gmail.com';

-- ============================================================================
-- 10. SP010 @ K001
-- ============================================================================
INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'NHAP_KHO'::loai_giao_dich_kho, 160, 0, 160, 'Nhập số lượng lớn', e.id, TIMESTAMP '2026-05-09 09:30:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP010' AND k.ma_kho = 'K001' AND e.email = 'tranthienloc.nina@gmail.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'XUAT_KHO'::loai_giao_dich_kho, 15, 160, 145, 'Xuất liên tiếp', e.id, TIMESTAMP '2026-05-13 08:50:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP010' AND k.ma_kho = 'K001' AND e.email = 'tranthienloc.nina@gmail.com';

INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong, so_luong_truoc, so_luong_sau, ghi_chu, nguoi_tao_id, ngay_tao)
SELECT sp.id, k.id, 'DIEU_CHINH_GIAM'::loai_giao_dich_kho, 5, 145, 140, 'Điều chỉnh giảm do kiểm kê', e.id, TIMESTAMP '2026-05-15 09:45:00'
FROM san_pham sp, kho k, nhan_vien e
WHERE sp.ma_san_pham = 'SP010' AND k.ma_kho = 'K001' AND e.email = 'tranthienloc.nina@gmail.com';
