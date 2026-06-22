-- Thêm cột version để Spring Boot làm Optimistic Locking
ALTER TABLE ton_kho ADD COLUMN version BIGINT NOT NULL DEFAULT 0;

-- Chặn đứng tình trạng âm tồn kho ở tầng Database
ALTER TABLE ton_kho ADD CONSTRAINT chk_ton_kho_so_luong_non_negative CHECK (so_luong >= 0);

-- Tiện tay ép lịch sử giao dịch phải có thời gian chuẩn (Cú pháp chuẩn của Postgres)
ALTER TABLE giao_dich_kho ALTER COLUMN ngay_tao SET NOT NULL;