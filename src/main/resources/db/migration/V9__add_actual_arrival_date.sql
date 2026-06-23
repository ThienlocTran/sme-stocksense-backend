-- Migration để thêm cột ngày hàng về thực tế cho phiếu nhập kho
ALTER TABLE "phieu_nhap_kho"
    ADD COLUMN IF NOT EXISTS "ngay_hang_ve" timestamp;

COMMENT ON COLUMN "phieu_nhap_kho"."ngay_hang_ve" IS 'Ngày hàng về thực tế của phiếu nhập kho';
