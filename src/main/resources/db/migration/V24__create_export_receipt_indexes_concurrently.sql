-- pragma: no transaction

CREATE INDEX CONCURRENTLY IF NOT EXISTS "idx_phieu_xuat_trang_thai" ON "phieu_xuat_kho" ("trang_thai");
CREATE INDEX CONCURRENTLY IF NOT EXISTS "idx_phieu_xuat_kho_id" ON "phieu_xuat_kho" ("kho_id");
CREATE INDEX CONCURRENTLY IF NOT EXISTS "idx_phieu_xuat_doi_tac_id" ON "phieu_xuat_kho" ("doi_tac_id");
CREATE INDEX CONCURRENTLY IF NOT EXISTS "idx_phieu_xuat_nguoi_tao_id" ON "phieu_xuat_kho" ("nguoi_tao_id");
CREATE INDEX CONCURRENTLY IF NOT EXISTS "idx_phieu_xuat_ngay_tao" ON "phieu_xuat_kho" ("ngay_tao");

ALTER TABLE "phieu_xuat_kho" VALIDATE CONSTRAINT "fk_phieu_xuat_nguoi_duyet_cap_1";
ALTER TABLE "phieu_xuat_kho" VALIDATE CONSTRAINT "fk_phieu_xuat_nguoi_duyet_cap_2";
ALTER TABLE "phieu_xuat_kho" VALIDATE CONSTRAINT "fk_phieu_xuat_nguoi_huy";
ALTER TABLE "phieu_xuat_kho" VALIDATE CONSTRAINT "fk_phieu_xuat_nguoi_hoan_thanh";
