ALTER TABLE "phieu_nhap_kho"
    ADD COLUMN IF NOT EXISTS "ngay_hang_ve" timestamp;

COMMENT ON COLUMN "phieu_nhap_kho"."ngay_hang_ve" IS 'Ngay hang ve thuc te cua phieu nhap kho';
