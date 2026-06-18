ALTER TABLE "phieu_nhap_kho"
    ADD COLUMN IF NOT EXISTS "nguoi_duyet_cap_1_id" bigint,
    ADD COLUMN IF NOT EXISTS "ngay_duyet_cap_1" timestamp,
    ADD COLUMN IF NOT EXISTS "nguoi_duyet_cap_2_id" bigint,
    ADD COLUMN IF NOT EXISTS "ngay_duyet_cap_2" timestamp,
    ADD COLUMN IF NOT EXISTS "nguoi_huy_id" bigint,
    ADD COLUMN IF NOT EXISTS "ngay_huy" timestamp,
    ADD COLUMN IF NOT EXISTS "nguoi_hoan_thanh_id" bigint,
    ADD COLUMN IF NOT EXISTS "ngay_cap_nhat" timestamp,
    ADD COLUMN IF NOT EXISTS "version" bigint NOT NULL DEFAULT 0;

ALTER TABLE "chi_tiet_phieu_nhap"
    ADD COLUMN IF NOT EXISTS "so_luong_thuc_nhan" int,
    ADD COLUMN IF NOT EXISTS "ngay_cap_nhat" timestamp,
    ADD COLUMN IF NOT EXISTS "version" bigint NOT NULL DEFAULT 0;

ALTER TABLE "phieu_nhap_kho"
    ALTER COLUMN "trang_thai" SET DEFAULT 'NHAP';

ALTER TABLE "phieu_nhap_kho"
    ADD CONSTRAINT "chk_phieu_nhap_trang_thai_t75"
        CHECK ("trang_thai"::text IN (
            'NHAP',
            'CHO_DUYET_CAP_1',
            'CHO_DUYET_CAP_2',
            'CHO_HANG_VE',
            'CHO_KIEM_HANG',
            'HOAN_THANH',
            'TU_CHOI',
            'HUY'
        )) NOT VALID,
    ADD CONSTRAINT "chk_phieu_nhap_tong_tien_non_negative"
        CHECK ("tong_tien" IS NULL OR "tong_tien" >= 0) NOT VALID;

ALTER TABLE "chi_tiet_phieu_nhap"
    ADD CONSTRAINT "chk_ct_phieu_nhap_so_luong_positive"
        CHECK ("so_luong" > 0) NOT VALID,
    ADD CONSTRAINT "chk_ct_phieu_nhap_so_luong_thuc_nhan_non_negative"
        CHECK ("so_luong_thuc_nhan" IS NULL OR "so_luong_thuc_nhan" >= 0) NOT VALID,
    ADD CONSTRAINT "chk_ct_phieu_nhap_don_gia_non_negative"
        CHECK ("don_gia" IS NULL OR "don_gia" >= 0) NOT VALID,
    ADD CONSTRAINT "chk_ct_phieu_nhap_thanh_tien_non_negative"
        CHECK ("thanh_tien" IS NULL OR "thanh_tien" >= 0) NOT VALID;

ALTER TABLE "phieu_nhap_kho"
    ADD CONSTRAINT "fk_phieu_nhap_nguoi_duyet_cap_1"
        FOREIGN KEY ("nguoi_duyet_cap_1_id") REFERENCES "nhan_vien" ("id") DEFERRABLE INITIALLY IMMEDIATE,
    ADD CONSTRAINT "fk_phieu_nhap_nguoi_duyet_cap_2"
        FOREIGN KEY ("nguoi_duyet_cap_2_id") REFERENCES "nhan_vien" ("id") DEFERRABLE INITIALLY IMMEDIATE,
    ADD CONSTRAINT "fk_phieu_nhap_nguoi_huy"
        FOREIGN KEY ("nguoi_huy_id") REFERENCES "nhan_vien" ("id") DEFERRABLE INITIALLY IMMEDIATE,
    ADD CONSTRAINT "fk_phieu_nhap_nguoi_hoan_thanh"
        FOREIGN KEY ("nguoi_hoan_thanh_id") REFERENCES "nhan_vien" ("id") DEFERRABLE INITIALLY IMMEDIATE;

CREATE INDEX IF NOT EXISTS "idx_phieu_nhap_trang_thai" ON "phieu_nhap_kho" ("trang_thai");
CREATE INDEX IF NOT EXISTS "idx_phieu_nhap_kho_id" ON "phieu_nhap_kho" ("kho_id");
CREATE INDEX IF NOT EXISTS "idx_phieu_nhap_doi_tac_id" ON "phieu_nhap_kho" ("doi_tac_id");
CREATE INDEX IF NOT EXISTS "idx_phieu_nhap_nguoi_tao_id" ON "phieu_nhap_kho" ("nguoi_tao_id");
CREATE INDEX IF NOT EXISTS "idx_phieu_nhap_ngay_tao" ON "phieu_nhap_kho" ("ngay_tao");

COMMENT ON COLUMN "chi_tiet_phieu_nhap"."so_luong" IS 'So luong du kien cua dong phieu nhap.';
COMMENT ON COLUMN "chi_tiet_phieu_nhap"."so_luong_thuc_nhan" IS 'So luong thuc nhan sau khi kiem hang. Chi dung khi hoan thanh de tang ton kho.';
