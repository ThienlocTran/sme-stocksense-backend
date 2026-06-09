-- Create enum type for warehouse status
CREATE TYPE "trang_thai_kho" AS ENUM (
  'HOAT_DONG',
  'NGUNG_HOAT_DONG'
);

-- Alter table "kho" to use the new status column
ALTER TABLE "kho" ADD COLUMN "trang_thai" trang_thai_kho NOT NULL DEFAULT 'HOAT_DONG';

-- Migrate existing records from "dang_hoat_dong" boolean to "trang_thai" enum
UPDATE "kho" SET "trang_thai" = 'NGUNG_HOAT_DONG' WHERE "dang_hoat_dong" = false;

-- Drop the old boolean column
ALTER TABLE "kho" DROP COLUMN "dang_hoat_dong";
