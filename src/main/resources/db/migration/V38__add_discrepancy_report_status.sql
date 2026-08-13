ALTER TABLE "bien_ban_chenh_lech"
  ADD COLUMN IF NOT EXISTS "trang_thai" varchar(20);

UPDATE "bien_ban_chenh_lech"
SET "trang_thai" = 'CHO_DUYET'
WHERE "trang_thai" IS NULL;

ALTER TABLE "bien_ban_chenh_lech"
  ALTER COLUMN "trang_thai" SET DEFAULT 'CHO_DUYET',
  ALTER COLUMN "trang_thai" SET NOT NULL;

ALTER TABLE "bien_ban_chenh_lech"
  ADD CONSTRAINT "ck_bbcl_trang_thai"
  CHECK ("trang_thai" IN ('CHO_DUYET', 'DA_DUYET', 'TU_CHOI', 'HUY'));

CREATE INDEX IF NOT EXISTS "idx_bb_cl_trang_thai"
  ON "bien_ban_chenh_lech" ("trang_thai");
