-- Xoá tài khoản test bị lỡ insert ở V2__seed_base_roles.sql
-- vì team dùng chung Cloud DB, không được tồn tại data rác.
-- KHÔNG sửa V2 để giữ nguyên checksum Flyway, dùng migration mới để dọn dẹp.

DELETE FROM "nhan_vien"
WHERE "email" = 'admin@example.com';
