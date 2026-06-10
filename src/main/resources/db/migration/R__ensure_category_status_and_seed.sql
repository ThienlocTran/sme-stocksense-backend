DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'trang_thai_danh_muc') THEN
        CREATE TYPE "trang_thai_danh_muc" AS ENUM ('HOAT_DONG', 'NGUNG_HOAT_DONG');
    END IF;
END $$;

ALTER TABLE "danh_muc"
    ADD COLUMN IF NOT EXISTS "trang_thai" trang_thai_danh_muc NOT NULL DEFAULT 'HOAT_DONG';

INSERT INTO "danh_muc" ("ma_danh_muc", "ten_danh_muc", "mo_ta", "trang_thai", "ngay_tao", "ngay_cap_nhat")
VALUES
    ('DM001', 'Nguyên liệu', 'Nhóm nguyên liệu sản xuất.', 'HOAT_DONG'::trang_thai_danh_muc, now(), now()),
    ('DM002', 'Thành phẩm', 'Nhóm sản phẩm hoàn thiện.', 'HOAT_DONG'::trang_thai_danh_muc, now(), now()),
    ('DM003', 'Phụ kiện', 'Nhóm phụ kiện đi kèm.', 'HOAT_DONG'::trang_thai_danh_muc, now(), now())
ON CONFLICT ("ma_danh_muc") DO UPDATE
SET "ten_danh_muc" = EXCLUDED."ten_danh_muc",
    "mo_ta" = EXCLUDED."mo_ta",
    "ngay_cap_nhat" = now();
