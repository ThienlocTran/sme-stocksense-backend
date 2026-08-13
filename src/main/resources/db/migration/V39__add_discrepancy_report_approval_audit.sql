ALTER TABLE "bien_ban_chenh_lech"
  ADD COLUMN IF NOT EXISTS "nguoi_duyet_id" bigint,
  ADD COLUMN IF NOT EXISTS "ngay_duyet" timestamp,
  ADD COLUMN IF NOT EXISTS "nguoi_tu_choi_id" bigint,
  ADD COLUMN IF NOT EXISTS "ngay_tu_choi" timestamp,
  ADD COLUMN IF NOT EXISTS "ly_do_tu_choi" varchar(500);

ALTER TABLE "bien_ban_chenh_lech"
  ADD CONSTRAINT "fk_bbcl_nguoi_duyet"
  FOREIGN KEY ("nguoi_duyet_id") REFERENCES "nhan_vien" ("id");

ALTER TABLE "bien_ban_chenh_lech"
  ADD CONSTRAINT "fk_bbcl_nguoi_tu_choi"
  FOREIGN KEY ("nguoi_tu_choi_id") REFERENCES "nhan_vien" ("id");
