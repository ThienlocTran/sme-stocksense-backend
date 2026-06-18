-- Create enum type for partner status
CREATE TYPE "trang_thai_doi_tac" AS ENUM (
  'HOAT_DONG',
  'NGUNG_HOAT_DONG'
);

-- Alter table "doi_tac" to add contact person and the new status column
ALTER TABLE "doi_tac" ADD COLUMN "nguoi_lien_he" varchar(150);
ALTER TABLE "doi_tac" ADD COLUMN "trang_thai" trang_thai_doi_tac NOT NULL DEFAULT 'HOAT_DONG';

-- Migrate existing records from "dang_hoat_dong" boolean to "trang_thai" enum
UPDATE "doi_tac" SET "trang_thai" = 'NGUNG_HOAT_DONG' WHERE "dang_hoat_dong" = false;

-- Drop the old boolean column
ALTER TABLE "doi_tac" DROP COLUMN "dang_hoat_dong";
