-- =============================================================================
-- scripts/validate_database.sql
-- Module: Database Integrity & Business Consistency Verification Suite
-- Mục đích: Kiểm định toàn bộ các tiêu chí toàn vẹn theo chuẩn schema V1 -> V43
-- Mục tiêu chuẩn: TẤT CẢ CÁC CÂU TRUY VẤN LỖI PHẢI TRẢ VỀ 0 RECORD (PASS)
-- =============================================================================

\echo '============================================================================='
\echo 'BẮT ĐẦU BỘ KIỂM ĐỊNH TOÀN DIỆN CƠ SỞ DỮ LIỆU CHÍNH THỨC SMESTOCKSENSE'
\echo '============================================================================='

-- 1. Mã sản phẩm (Không duplicate)
\echo '1. Kiểm tra trùng lặp mã sản phẩm (ma_san_pham):'
SELECT ma_san_pham, COUNT(*) AS count_duplicate 
FROM san_pham 
GROUP BY ma_san_pham 
HAVING COUNT(*) > 1;

-- 2. SKU (Không duplicate)
\echo '2. Kiểm tra trùng lặp SKU sản phẩm:'
SELECT sku, COUNT(*) AS count_duplicate 
FROM san_pham 
WHERE sku IS NOT NULL 
GROUP BY sku 
HAVING COUNT(*) > 1;

-- 3. Barcode (Không duplicate nếu có giá trị)
\echo '3. Kiểm tra trùng lặp Barcode:'
SELECT ma_vach, COUNT(*) AS count_duplicate 
FROM san_pham 
WHERE ma_vach IS NOT NULL 
GROUP BY ma_vach 
HAVING COUNT(*) > 1;

-- 4. Danh mục (Không có sản phẩm mồ côi danh mục)
\echo '4. Kiểm tra sản phẩm mồ côi (thiếu danh mục hợp lệ):'
SELECT sp.id, sp.ma_san_pham, sp.ten_san_pham 
FROM san_pham sp 
LEFT JOIN danh_muc dm ON dm.id = sp.danh_muc_id 
WHERE dm.id IS NULL;

-- 5. Nhà cung cấp (Không có sản phẩm mồ côi đối tác cung cấp)
\echo '5. Kiểm tra sản phẩm thiếu đối tác cung cấp hợp lệ:'
SELECT sp.id, sp.ma_san_pham, sp.ten_san_pham 
FROM san_pham sp 
LEFT JOIN doi_tac dt ON dt.id = sp.doi_tac_cung_cap_id 
WHERE dt.id IS NULL;

-- 6. Kho hàng (Không có sức chứa <= 0 hoặc null)
\echo '6. Kiểm tra sức chứa tối đa kho không hợp lệ:'
SELECT id, ma_kho, ten_kho, suc_chua_toi_da_m3 
FROM kho 
WHERE suc_chua_toi_da_m3 IS NULL OR suc_chua_toi_da_m3 <= 0;

-- 7. Sản phẩm (Không có price < 0, the_tich <= 0, ton_toi_thieu_mac_dinh < 0)
\echo '7. Kiểm tra giá, thể tích m3 và tồn tối thiểu mặc định sản phẩm không hợp lệ:'
SELECT id, ma_san_pham, price, the_tich_don_vi_m3, ton_toi_thieu_mac_dinh 
FROM san_pham 
WHERE price IS NULL OR price < 0 
   OR the_tich_don_vi_m3 IS NULL OR the_tich_don_vi_m3 <= 0
   OR ton_toi_thieu_mac_dinh < 0;

-- 8. Tồn kho (Không có số lượng âm trong bảng ton_kho)
\echo '8. Kiểm tra tồn kho bị âm (Negative stock):'
SELECT id, san_pham_id, kho_id, so_luong 
FROM ton_kho 
WHERE so_luong < 0;

-- 9. Ledger vs Tồn kho (SUM giao_dich_kho phải bằng ton_kho.so_luong)
\echo '9. Đối soát cân bằng sổ kho (SUM giao_dich_kho vs ton_kho.so_luong):'
SELECT tk.san_pham_id, tk.kho_id, tk.so_luong AS ton_kho_so_luong,
       COALESCE(SUM(CASE WHEN gd.loai_giao_dich IN ('NHAP_KHO','NHAP_DAU_KY','DIEU_CHINH_TANG') THEN gd.so_luong ELSE -gd.so_luong END), 0) AS ledger_so_luong,
       (tk.so_luong - COALESCE(SUM(CASE WHEN gd.loai_giao_dich IN ('NHAP_KHO','NHAP_DAU_KY','DIEU_CHINH_TANG') THEN gd.so_luong ELSE -gd.so_luong END), 0)) AS chenh_lech
FROM ton_kho tk
LEFT JOIN giao_dich_kho gd ON gd.san_pham_id = tk.san_pham_id AND gd.kho_id = tk.kho_id
GROUP BY tk.san_pham_id, tk.kho_id, tk.so_luong
HAVING tk.so_luong != COALESCE(SUM(CASE WHEN gd.loai_giao_dich IN ('NHAP_KHO','NHAP_DAU_KY','DIEU_CHINH_TANG') THEN gd.so_luong ELSE -gd.so_luong END), 0);

-- 10. Phiếu nhập (Tổng tiền phải bằng tổng chi tiết)
\echo '10. Kiểm tra lệch tổng tiền phiếu nhập:'
SELECT pn.id, pn.ma_phieu_nhap, pn.tong_tien, SUM(ct.thanh_tien) AS chi_tiet_total
FROM phieu_nhap_kho pn
JOIN chi_tiet_phieu_nhap ct ON ct.phieu_nhap_id = pn.id
GROUP BY pn.id, pn.ma_phieu_nhap, pn.tong_tien
HAVING pn.tong_tien != SUM(ct.thanh_tien);

-- 11. Phiếu xuất (Tổng tiền phải bằng tổng chi tiết)
\echo '11. Kiểm tra lệch tổng tiền phiếu xuất:'
SELECT px.id, px.ma_phieu_xuat, px.tong_tien, SUM(ct.thanh_tien) AS chi_tiet_total
FROM phieu_xuat_kho px
JOIN chi_tiet_phieu_xuat ct ON ct.phieu_xuat_id = px.id
GROUP BY px.id, px.ma_phieu_xuat, px.tong_tien
HAVING px.tong_tien != SUM(ct.thanh_tien);

-- 12. Phiếu nhập HOAN_THANH nhưng thiếu giao dịch kho
\echo '12. Phiếu nhập HOAN_THANH thiếu giao dịch kho:'
SELECT pn.id, pn.ma_phieu_nhap
FROM phieu_nhap_kho pn
WHERE pn.trang_thai = 'HOAN_THANH'
  AND NOT EXISTS (SELECT 1 FROM giao_dich_kho gd WHERE gd.phieu_nhap_id = pn.id);

-- 13. Phiếu xuất HOAN_THANH nhưng thiếu giao dịch kho
\echo '13. Phiếu xuất HOAN_THANH thiếu giao dịch kho:'
SELECT px.id, px.ma_phieu_xuat
FROM phieu_xuat_kho px
WHERE px.trang_thai = 'HOAN_THANH'
  AND NOT EXISTS (SELECT 1 FROM giao_dich_kho gd WHERE gd.phieu_xuat_id = px.id);

-- 14. Transaction mồ côi (Giao dịch kho không rõ SP hoặc Kho)
\echo '14. Giao dịch kho mồ côi SP hoặc Kho:'
SELECT gd.id, gd.loai_giao_dich
FROM giao_dich_kho gd
LEFT JOIN san_pham sp ON sp.id = gd.san_pham_id
LEFT JOIN kho k ON k.id = gd.kho_id
WHERE sp.id IS NULL OR k.id IS NULL;

-- 15. Kiểm kê (Chênh lệch phải tính đúng: thực tế - hệ thống)
\echo '15. Chênh lệch kiểm kê tính sai công thức (thực tế - hệ thống):'
SELECT ct.id, ct.dot_kiem_ke_id, ct.so_luong_he_thong, ct.so_luong_thuc_te, ct.chenh_lech
FROM chi_tiet_kiem_ke ct
WHERE ct.so_luong_thuc_te IS NOT NULL
  AND ct.chenh_lech != (ct.so_luong_thuc_te - ct.so_luong_he_thong);

-- 16. Alert tồn kho (Không duplicate active alert theo Partial Unique Index V30)
\echo '16. Trùng lặp cảnh báo tồn kho active (OPEN/ACKNOWLEDGED):'
SELECT san_pham_id, kho_id, COUNT(*) AS count_active_alerts
FROM canh_bao_ton_kho
WHERE trang_thai IN ('OPEN', 'ACKNOWLEDGED')
GROUP BY san_pham_id, kho_id
HAVING COUNT(*) > 1;

-- 17. Alert tồn kho (Không tạo cảnh báo nếu ton_hien_tai > ton_toi_thieu)
\echo '17. Cảnh báo tồn kho bất hợp lý (OPEN nhưng tồn > min_stock):'
SELECT cb.id, cb.san_pham_id, cb.kho_id, cb.so_luong_hien_tai, cb.ton_toi_thieu, cb.muc_do, cb.trang_thai
FROM canh_bao_ton_kho cb
WHERE cb.trang_thai = 'OPEN' AND cb.so_luong_hien_tai > cb.ton_toi_thieu;

-- 18. Capacity (Phải tính đúng từ tồn kho x thể tích)
\echo '18. Đối soát sức chứa kho đã dùng (used_capacity_m3 vs SUM ton_kho * the_tich):'
SELECT cb.kho_id, cb.used_capacity_m3,
       COALESCE(SUM(tk.so_luong * sp.the_tich_don_vi_m3), 0) AS calculated_capacity
FROM canh_bao_suc_chua_kho cb
JOIN kho k ON k.id = cb.kho_id
LEFT JOIN ton_kho tk ON tk.kho_id = cb.kho_id
LEFT JOIN san_pham sp ON sp.id = tk.san_pham_id
GROUP BY cb.kho_id, cb.used_capacity_m3
HAVING ABS(cb.used_capacity_m3 - COALESCE(SUM(tk.so_luong * sp.the_tich_don_vi_m3), 0)) > 0.001;

-- 19. AI (Không duplicate ngày bán SP/Kho/Ngày)
\echo '19. Trùng lặp chuỗi thời gian AI (SP, Kho, Ngày):'
SELECT san_pham_id, kho_id, ngay, COUNT(*) AS count_duplicate
FROM ai.lich_su_ban_hang
GROUP BY san_pham_id, kho_id, ngay
HAVING COUNT(*) > 1;

-- 20. AI (Không có số lượng âm)
\echo '20. Số lượng bán AI bị âm:'
SELECT id, san_pham_id, kho_id, ngay, so_luong
FROM ai.lich_su_ban_hang
WHERE so_luong < 0;

-- 21. Account (Đúng 3 email chính thức và mật khẩu hash BCrypt)
\echo '21. Kiểm tra 3 tài khoản chính thức và mã hóa BCrypt:'
SELECT nv.id, nv.email, vt.ma_vai_tro, nv.mat_khau
FROM nhan_vien nv
JOIN vai_tro vt ON vt.id = nv.vai_tro_id
WHERE (nv.email = 'thienloct.it@gmail.com' AND vt.ma_vai_tro != 'ADMIN')
   OR (nv.email = 'tranthienloc.nina@gmail.com' AND vt.ma_vai_tro != 'MANAGER')
   OR (nv.email = 'tranthienloc21102005@gmail.com' AND vt.ma_vai_tro != 'EMPLOYEE')
   OR nv.mat_khau NOT LIKE '$2a$%';

-- 22. RBAC (Không thay đổi quyền hiện tại, EMPLOYEE không bị cấp quyền quản trị)
\echo '22. Kiểm tra phân quyền Employee an toàn:'
SELECT vt.ma_vai_tro, q.ma_quyen
FROM vai_tro_quyen vq
JOIN vai_tro vt ON vt.id = vq.vai_tro_id
JOIN quyen q ON q.id = vq.quyen_id
WHERE vt.ma_vai_tro = 'EMPLOYEE'
  AND q.ma_quyen IN ('MANAGE_EMPLOYEE', 'MANAGE_ROLE_PERMISSION', 'APPROVE_IMPORT_L1', 'APPROVE_IMPORT_L2');

\echo '============================================================================='
\echo 'HOÀN TẤT KIỂM ĐỊNH. NẾU KHÔNG CÓ DÒNG LỖI NÀO XUẤT HIỆN: TOÀN BỘ CHECK PASS!'
\echo '============================================================================='
