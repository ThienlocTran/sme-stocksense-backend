ALTER TABLE "phieu_xuat_kho"
    ADD COLUMN IF NOT EXISTS "nguoi_duyet_cap_1_id" bigint,
    ADD COLUMN IF NOT EXISTS "ngay_duyet_cap_1" timestamp,
    ADD COLUMN IF NOT EXISTS "nguoi_duyet_cap_2_id" bigint,
    ADD COLUMN IF NOT EXISTS "ngay_duyet_cap_2" timestamp,
    ADD COLUMN IF NOT EXISTS "nguoi_huy_id" bigint,
    ADD COLUMN IF NOT EXISTS "ngay_huy" timestamp,
    ADD COLUMN IF NOT EXISTS "nguoi_hoan_thanh_id" bigint,
    ADD COLUMN IF NOT EXISTS "ngay_cap_nhat" timestamp,
    ADD COLUMN IF NOT EXISTS "version" bigint NOT NULL DEFAULT 0;

ALTER TABLE "chi_tiet_phieu_xuat"
    ADD COLUMN IF NOT EXISTS "ngay_cap_nhat" timestamp,
    ADD COLUMN IF NOT EXISTS "version" bigint NOT NULL DEFAULT 0;

ALTER TABLE "phieu_xuat_kho"
    ALTER COLUMN "trang_thai" SET DEFAULT 'NHAP';

ALTER TABLE "phieu_xuat_kho"
    ADD CONSTRAINT "chk_phieu_xuat_trang_thai_t75"
        CHECK ("trang_thai"::text IN (
            'NHAP',
            'CHO_DUYET_CAP_1',
            'CHO_DUYET_CAP_2',
            'HOAN_THANH',
            'TU_CHOI',
            'HUY'
        )) NOT VALID,
    ADD CONSTRAINT "chk_phieu_xuat_tong_tien_non_negative"
        CHECK ("tong_tien" IS NULL OR "tong_tien" >= 0) NOT VALID;

ALTER TABLE "chi_tiet_phieu_xuat"
    ADD CONSTRAINT "chk_ct_phieu_xuat_so_luong_positive"
        CHECK ("so_luong" > 0) NOT VALID,
    ADD CONSTRAINT "chk_ct_phieu_xuat_don_gia_non_negative"
        CHECK ("don_gia" IS NULL OR "don_gia" >= 0) NOT VALID,
    ADD CONSTRAINT "chk_ct_phieu_xuat_thanh_tien_non_negative"
        CHECK ("thanh_tien" IS NULL OR "thanh_tien" >= 0) NOT VALID;

ALTER TABLE "phieu_xuat_kho"
    ADD CONSTRAINT "fk_phieu_xuat_nguoi_duyet_cap_1"
        FOREIGN KEY ("nguoi_duyet_cap_1_id") REFERENCES "nhan_vien" ("id") DEFERRABLE INITIALLY IMMEDIATE,
    ADD CONSTRAINT "fk_phieu_xuat_nguoi_duyet_cap_2"
        FOREIGN KEY ("nguoi_duyet_cap_2_id") REFERENCES "nhan_vien" ("id") DEFERRABLE INITIALLY IMMEDIATE,
    ADD CONSTRAINT "fk_phieu_xuat_nguoi_huy"
        FOREIGN KEY ("nguoi_huy_id") REFERENCES "nhan_vien" ("id") DEFERRABLE INITIALLY IMMEDIATE,
    ADD CONSTRAINT "fk_phieu_xuat_nguoi_hoan_thanh"
        FOREIGN KEY ("nguoi_hoan_thanh_id") REFERENCES "nhan_vien" ("id") DEFERRABLE INITIALLY IMMEDIATE;

CREATE INDEX IF NOT EXISTS "idx_phieu_xuat_trang_thai" ON "phieu_xuat_kho" ("trang_thai");
CREATE INDEX IF NOT EXISTS "idx_phieu_xuat_kho_id" ON "phieu_xuat_kho" ("kho_id");
CREATE INDEX IF NOT EXISTS "idx_phieu_xuat_doi_tac_id" ON "phieu_xuat_kho" ("doi_tac_id");
CREATE INDEX IF NOT EXISTS "idx_phieu_xuat_nguoi_tao_id" ON "phieu_xuat_kho" ("nguoi_tao_id");
CREATE INDEX IF NOT EXISTS "idx_phieu_xuat_ngay_tao" ON "phieu_xuat_kho" ("ngay_tao");
