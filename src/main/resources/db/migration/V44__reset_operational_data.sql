-- =============================================================================
-- V44__reset_operational_data.sql
-- Module: Database Optimization & Official Seed Preparation
-- Mục đích: Dọn dẹp dữ liệu nghiệp vụ và giao dịch cũ theo thứ tự ràng buộc FK
-- Tuyệt đối bảo toàn:
--   - Bảng vai_tro, quyen, vai_tro_quyen (RBAC metadata)
--   - Bảng system_settings (Cấu hình hệ thống)
--   - Bảng nhan_vien (3 tài khoản chính thức được bảo toàn)
-- =============================================================================

-- 1. Xóa dữ liệu phân hệ AI Forecast
DELETE FROM ai.nhat_ky_lech_mo_hinh;
DELETE FROM ai.thong_tin_mo_hinh;
DELETE FROM ai.ket_qua_du_bao;
DELETE FROM ai.lich_su_ban_hang;

-- 2. Xóa lịch sử thay đổi cấu hình hệ thống
DELETE FROM system_setting_history;

-- 3. Xóa dữ liệu Cảnh báo & Cấu hình tồn kho
DELETE FROM canh_bao_suc_chua_kho;
DELETE FROM canh_bao_ton_kho;
DELETE FROM cau_hinh_ton_kho;

-- 4. Xóa dữ liệu Kiểm kê kho
DELETE FROM chi_tiet_kiem_ke;
DELETE FROM dot_kiem_ke;

-- 5. Xóa dữ liệu Giao dịch kho & Sổ kho
DELETE FROM giao_dich_kho;

-- 6. Xóa dữ liệu Import Excel
DELETE FROM loi_import_excel;
DELETE FROM lan_import_excel;

-- 7. Xóa dữ liệu Biên bản kiểm hàng / chênh lệch & Lịch sử phiếu
DELETE FROM chi_tiet_bien_ban_chenh_lech;
DELETE FROM bien_ban_chenh_lech;
DELETE FROM phieu_xuat_kho_lich_su;
DELETE FROM phieu_nhap_kho_lich_su;

-- 8. Xóa dữ liệu Chi tiết & Phiếu Nhập / Xuất kho
DELETE FROM chi_tiet_phieu_xuat;
DELETE FROM phieu_xuat_kho;
DELETE FROM chi_tiet_phieu_nhap;
DELETE FROM phieu_nhap_kho;

-- 9. Xóa dữ liệu Tồn kho tổng hợp
DELETE FROM ton_kho;

-- 10. Xóa dữ liệu Danh mục Sản phẩm, Đối tác, Kho, Danh mục
DELETE FROM san_pham;
DELETE FROM doi_tac;
DELETE FROM kho;
DELETE FROM danh_muc;

-- 11. Dọn dẹp tài khoản cũ không thuộc danh sách 3 tài khoản chính thức
DELETE FROM nhan_vien
WHERE email NOT IN (
    'tranthienloc21102005@gmail.com',
    'tranthienloc.nina@gmail.com',
    'thienloct.it@gmail.com'
);
