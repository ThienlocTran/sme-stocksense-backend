ALTER TABLE "phieu_xuat_kho"
    ADD COLUMN IF NOT EXISTS "nguoi_tu_choi_id" bigint,
    ADD COLUMN IF NOT EXISTS "ngay_tu_choi" timestamp;

ALTER TABLE "phieu_xuat_kho"
    ADD CONSTRAINT "fk_phieu_xuat_nguoi_tu_choi"
        FOREIGN KEY ("nguoi_tu_choi_id") REFERENCES "nhan_vien" ("id") DEFERRABLE INITIALLY IMMEDIATE NOT VALID;
