-- =====================================================================
-- T65: Seed dữ liệu mẫu cho module Tồn kho (ton_kho + giao_dich_kho).
-- Móc ID qua ma_san_pham / ma_kho (JOIN) -> luôn đúng FK dù id là IDENTITY.
-- Phụ thuộc V10 (kho, danh muc, san pham). Idempotent.
-- =====================================================================

-- 1) Tồn kho hiện tại: so_luong random 50..500, version = 0 (Optimistic Lock)
INSERT INTO ton_kho (san_pham_id, kho_id, so_luong, version, ngay_cap_nhat)
SELECT sp.id,
       k.id,
       floor(random() * (500 - 50 + 1) + 50)::int AS so_luong,
       0                                          AS version,
       CURRENT_TIMESTAMP                          AS ngay_cap_nhat
FROM (
         VALUES ('SP001', 'KHO_TONG'),
                ('SP002', 'KHO_TONG'),
                ('SP003', 'KHO_TONG'),
                ('SP001', 'KHO_CN1'),
                ('SP002', 'KHO_CN1')
     ) AS c(ma_san_pham, ma_kho)
         JOIN san_pham sp ON sp.ma_san_pham = c.ma_san_pham
         JOIN kho k ON k.ma_kho = c.ma_kho
ON CONFLICT (san_pham_id, kho_id) DO NOTHING;

-- 2) Sổ cái giao dịch khớp tồn kho: NHAP_DAU_KY, truoc = 0, sau = ton hien tai
INSERT INTO giao_dich_kho (san_pham_id, kho_id, loai_giao_dich, so_luong,
                           so_luong_truoc, so_luong_sau, ghi_chu, ngay_tao)
SELECT tk.san_pham_id,
       tk.kho_id,
       'NHAP_DAU_KY',
       tk.so_luong,
       0,
       tk.so_luong,
       'Seed ton dau ky T65',
       CURRENT_TIMESTAMP
FROM ton_kho tk
WHERE NOT EXISTS (
    SELECT 1
    FROM giao_dich_kho g
    WHERE g.san_pham_id = tk.san_pham_id
      AND g.kho_id = tk.kho_id
      AND g.loai_giao_dich = 'NHAP_DAU_KY'
);
