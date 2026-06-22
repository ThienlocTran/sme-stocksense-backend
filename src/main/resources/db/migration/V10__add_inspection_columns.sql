ALTER TABLE "chi_tiet_phieu_nhap"
    ADD COLUMN IF NOT EXISTS "tinh_trang" varchar(255),
    ADD COLUMN IF NOT EXISTS "han_su_dung" timestamp,
    ADD COLUMN IF NOT EXISTS "trang_thai_dong" varchar(50);

COMMENT ON COLUMN "chi_tiet_phieu_nhap"."tinh_trang" IS 'Tinh trang vat ly cua hang thuc nhan (vd: Binh thuong, Hong,...)';
COMMENT ON COLUMN "chi_tiet_phieu_nhap"."han_su_dung" IS 'Han su dung thuc te cua hang khi kiem hang';
COMMENT ON COLUMN "chi_tiet_phieu_nhap"."trang_thai_dong" IS 'Trang thai doi chieu kiem hang: KHOP hoac CHENH_LECH';

ALTER TABLE "chi_tiet_phieu_nhap" ADD CONSTRAINT "ck_ctpn_trang_thai_dong" CHECK ("trang_thai_dong" IS NULL OR "trang_thai_dong" IN ('KHOP', 'CHENH_LECH'));
