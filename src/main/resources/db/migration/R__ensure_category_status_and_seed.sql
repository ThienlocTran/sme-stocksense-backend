-- =============================================================================
-- R__ensure_category_status_and_seed.sql
-- Repeatable Migration: Đảm bảo 3 tài khoản chính thức và phân quyền RBAC
-- Idempotent & Non-destructive: Không xóa và cấp phát lại ID nhằm bảo toàn FK
-- =============================================================================

-- 1. Loại bỏ tài khoản không còn sử dụng
DELETE FROM nhan_vien WHERE email = 'admin@example.com';

-- 2. Đảm bảo 3 tài khoản chính thức tồn tại với vai trò và mật khẩu chuẩn BCrypt ('12345678')
INSERT INTO nhan_vien (email, ho_ten, mat_khau, trang_thai, vai_tro_id, gioi_tinh, ngay_sinh, ngay_tao, ngay_cap_nhat)
VALUES
    (
        'thienloct.it@gmail.com',
        'Tran Thien Loc (Admin)',
        '$2a$10$.xiBOVMplpx5Jib0zFLvkOo80WelbiGSN5yh/vt1jl3/8VY.ypGWS',
        'HOAT_DONG'::trang_thai_nhan_vien,
        (SELECT id FROM vai_tro WHERE ma_vai_tro = 'ADMIN'),
        'MALE'::gioi_tinh, '2000-01-01'::DATE, now(),
        now()
    ),
    (
        'tranthienloc.nina@gmail.com',
        'Tran Thien Loc (Manager)',
        '$2a$10$.xiBOVMplpx5Jib0zFLvkOo80WelbiGSN5yh/vt1jl3/8VY.ypGWS',
        'HOAT_DONG'::trang_thai_nhan_vien,
        (SELECT id FROM vai_tro WHERE ma_vai_tro = 'MANAGER'),
        'MALE'::gioi_tinh, '2000-01-01'::DATE, now(),
        now()
    ),
    (
        'tranthienloc21102005@gmail.com',
        'Tran Thien Loc (Employee)',
        '$2a$10$.xiBOVMplpx5Jib0zFLvkOo80WelbiGSN5yh/vt1jl3/8VY.ypGWS',
        'HOAT_DONG'::trang_thai_nhan_vien,
        (SELECT id FROM vai_tro WHERE ma_vai_tro = 'EMPLOYEE'),
        'MALE'::gioi_tinh, '2000-01-01'::DATE, now(),
        now()
    )
ON CONFLICT (email) DO UPDATE
SET ho_ten = EXCLUDED.ho_ten,
    mat_khau = EXCLUDED.mat_khau,
    trang_thai = EXCLUDED.trang_thai,
    vai_tro_id = EXCLUDED.vai_tro_id,
    gioi_tinh = EXCLUDED.gioi_tinh, ngay_sinh = EXCLUDED.ngay_sinh, ngay_cap_nhat = now();

-- 3. Đồng bộ phân quyền ADMIN (Toàn bộ 30 quyền trong hệ thống)
INSERT INTO vai_tro_quyen (vai_tro_id, quyen_id)
SELECT vt.id, q.id
FROM vai_tro vt, quyen q
WHERE vt.ma_vai_tro = 'ADMIN'
ON CONFLICT DO NOTHING;

-- 4. Đồng bộ phân quyền MANAGER (15 quyền quản lý & phê duyệt theo chuẩn V37)
INSERT INTO vai_tro_quyen (vai_tro_id, quyen_id)
SELECT vt.id, q.id
FROM vai_tro vt, quyen q
WHERE vt.ma_vai_tro = 'MANAGER'
  AND q.ma_quyen IN (
    'VIEW_IMPORT_ALL',
    'APPROVE_IMPORT_L1',
    'APPROVE_IMPORT_L2',
    'REJECT_IMPORT',
    'CANCEL_IMPORT',
    'APPROVE_DISCREPANCY',
    'VIEW_EXPORT_ALL',
    'APPROVE_EXPORT',
    'REJECT_EXPORT',
    'VIEW_DASHBOARD',
    'VIEW_INVENTORY',
    'MANAGE_WAREHOUSE',
    'MANAGE_CATEGORY',
    'MANAGE_PARTNER',
    'MANAGE_PRODUCT'
)
ON CONFLICT DO NOTHING;

-- 5. Đồng bộ phân quyền EMPLOYEE (16 quyền thao tác nghiệp vụ theo chuẩn V37)
INSERT INTO vai_tro_quyen (vai_tro_id, quyen_id)
SELECT vt.id, q.id
FROM vai_tro vt, quyen q
WHERE vt.ma_vai_tro = 'EMPLOYEE'
  AND q.ma_quyen IN (
    'VIEW_IMPORT_OWN',
    'CREATE_IMPORT',
    'UPDATE_IMPORT',
    'SUBMIT_IMPORT',
    'CANCEL_IMPORT',
    'INSPECT_IMPORT',
    'COMPLETE_IMPORT',
    'CREATE_DISCREPANCY',
    'VIEW_EXPORT_OWN',
    'CREATE_EXPORT',
    'UPDATE_EXPORT',
    'SUBMIT_EXPORT',
    'COMPLETE_EXPORT',
    'EXCEL_UPLOAD_CONFIRM',
    'VIEW_DASHBOARD',
    'VIEW_INVENTORY'
)
ON CONFLICT DO NOTHING;