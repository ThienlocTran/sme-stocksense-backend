INSERT INTO "vai_tro" ("ma_vai_tro", "ten_vai_tro", "mo_ta", "ngay_tao", "ngay_cap_nhat")
VALUES
  (
    'ADMIN'::ma_vai_tro_he_thong,
    'Quản trị viên',
    'Has full system administration permission.',
    now(),
    now()
  ),
  ('MANAGER'::ma_vai_tro_he_thong, 'Quản lý kho', 'Manages inventory operations and approvals.', now(), now()),
  ('EMPLOYEE'::ma_vai_tro_he_thong, 'Nhân viên kho', 'Handles warehouse import/export requests.', now(), now())
ON CONFLICT ("ma_vai_tro") DO UPDATE
SET "ten_vai_tro" = EXCLUDED."ten_vai_tro",
    "mo_ta" = EXCLUDED."mo_ta",
    "ngay_cap_nhat" = now();


INSERT INTO "nhan_vien"
("ho_ten", "email", "mat_khau", "vai_tro_id", "trang_thai", "ngay_tao", "ngay_cap_nhat")
SELECT
    'Admin Test',
    'admin@example.com',
    '$2a$10$PJ/YxavevUizEHQ3VAST2.HYuQ.TuBf3lcIm03NQEQYtXbBIUBjrC',
    "id",
    'HOAT_DONG'::trang_thai_nhan_vien,
    now(),
    now()
FROM "vai_tro"
WHERE "ma_vai_tro" = 'ADMIN'::ma_vai_tro_he_thong
ON CONFLICT ("email") DO UPDATE
    SET "mat_khau" = EXCLUDED."mat_khau",
        "vai_tro_id" = EXCLUDED."vai_tro_id",
        "trang_thai" = EXCLUDED."trang_thai",
        "ngay_cap_nhat" = now();

select
    nv.id,
    nv.email,
    nv.ho_ten,
    nv.trang_thai,
    vt.ma_vai_tro,
    nv.mat_khau
from nhan_vien nv
         join vai_tro vt on vt.id = nv.vai_tro_id
where nv.email = 'admin@example.com';

