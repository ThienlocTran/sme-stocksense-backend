-- =====================================================================
-- T65: Seed dữ liệu nền (Kho, Danh mục, Sản phẩm) để làm móng cho ton_kho.
-- Idempotent: ON CONFLICT DO NOTHING dựa trên các cột UNIQUE (ma_kho,
-- ma_danh_muc, ma_san_pham). Chạy lại nhiều lần không nhân đôi dữ liệu.
-- =====================================================================

-- 1) Hai kho: Kho Tổng + Kho Chi Nhánh
INSERT INTO kho (ma_kho, ten_kho, dia_chi, trang_thai, ngay_tao, ngay_cap_nhat)
VALUES ('KHO_TONG', 'Kho Tong', 'Tru so chinh', 'HOAT_DONG', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
       ('KHO_CN1', 'Kho Chi Nhanh', 'Chi nhanh 1', 'HOAT_DONG', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (ma_kho) DO NOTHING;

-- 2) Một danh mục chung
INSERT INTO danh_muc (ma_danh_muc, ten_danh_muc, mo_ta, ngay_tao, ngay_cap_nhat)
VALUES ('DM_CHUNG', 'Danh muc chung', 'Danh muc mau cho seed', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP)
ON CONFLICT (ma_danh_muc) DO NOTHING;

-- 3) Ba sản phẩm thuộc danh mục DM_CHUNG (lấy danh_muc_id qua subquery cho an toàn FK)
INSERT INTO san_pham (ma_san_pham, ten_san_pham, sku, don_vi_tinh, danh_muc_id,
                      ton_toi_thieu, ton_toi_da, thoi_gian_giao_hang, price, trang_thai,
                      ngay_tao, ngay_cap_nhat)
SELECT v.ma_san_pham,
       v.ten_san_pham,
       v.sku,
       v.don_vi_tinh,
       dm.id,
       v.ton_toi_thieu,
       v.ton_toi_da,
       v.thoi_gian_giao_hang,
       v.price,
       'HOAT_DONG',
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
FROM (
         VALUES ('SP001', 'San pham mau A', 'SKU-SP001', 'Cai', 20, 1000, 3, 15000.00),
                ('SP002', 'San pham mau B', 'SKU-SP002', 'Hop', 30, 800, 5, 52000.00),
                ('SP003', 'San pham mau C', 'SKU-SP003', 'Thung', 10, 500, 7, 120000.00)
     ) AS v(ma_san_pham, ten_san_pham, sku, don_vi_tinh,
            ton_toi_thieu, ton_toi_da, thoi_gian_giao_hang, price)
         CROSS JOIN (SELECT id FROM danh_muc WHERE ma_danh_muc = 'DM_CHUNG') dm
ON CONFLICT (ma_san_pham) DO NOTHING;
