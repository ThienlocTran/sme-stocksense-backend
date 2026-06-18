-- 1. Xóa dữ liệu cũ của các user này để đảm bảo không trùng lặp email
DELETE FROM nhan_vien
WHERE email IN (
                'admin@example.com',
                'tranthienloc21102005@gmail.com',
                'tranthienloc.nina@gmail.com',
                'thienloct.it@gmail.com'
    );

-- 2. Insert lại toàn bộ dữ liệu mới (chắc chắn không bị trùng vì đã xóa ở bước 1)
INSERT INTO nhan_vien (email, ho_ten, mat_khau, trang_thai, vai_tro_id)
VALUES
    ('admin@example.com', 'Admin Test', '$2a$10$PJ/YxavevUizEHQ3VAST2.HYuQ.TuBf3lcIm03NQEQYtXbBIUBjrC', 'HOAT_DONG', 1),
    ('tranthienloc21102005@gmail.com', 'Tran Thien Loc', '$2a$10$PJ/YxavevUizEHQ3VAST2.HYuQ.TuBf3lcIm03NQEQYtXbBIUBjrC', 'HOAT_DONG', 1),
    ('tranthienloc.nina@gmail.com', 'Tran Thien Loc Nina', '$2a$10$PJ/YxavevUizEHQ3VAST2.HYuQ.TuBf3lcIm03NQEQYtXbBIUBjrC', 'HOAT_DONG', 1),
    ('thienloct.it@gmail.com', 'Thien Loc IT', '$2a$10$PJ/YxavevUizEHQ3VAST2.HYuQ.TuBf3lcIm03NQEQYtXbBIUBjrC', 'HOAT_DONG', 1);

UPDATE nhan_vien
SET mat_khau = '$2a$10$iAbkC447zy2AIGQlWtabiuDrgdg7ydHaXHg5wr7hLhcuPv4uC9yce'
WHERE email = 'admin@example.com';