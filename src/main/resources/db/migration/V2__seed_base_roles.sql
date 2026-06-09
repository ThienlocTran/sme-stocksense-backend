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
