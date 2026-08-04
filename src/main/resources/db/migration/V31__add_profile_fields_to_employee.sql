CREATE TYPE "gioi_tinh" AS ENUM (
  'MALE',
  'FEMALE',
  'OTHER'
);

ALTER TABLE "nhan_vien"
  ADD COLUMN "avatar_url" VARCHAR(500),
  ADD COLUMN "avatar_public_id" VARCHAR(255),
  ADD COLUMN "gioi_tinh" "gioi_tinh",
  ADD COLUMN "ngay_sinh" DATE;
