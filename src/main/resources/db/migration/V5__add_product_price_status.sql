-- Thêm enum trạng thái sản phẩm
CREATE TYPE "trang_thai_san_pham" AS ENUM (
  'HOAT_DONG',
  'NGUNG_HOAT_DONG'
);

-- Thêm cột giá bán và trạng thái vào bảng san_pham
ALTER TABLE "san_pham"
    ADD COLUMN "price" decimal(15, 2),
    ADD COLUMN "trang_thai" trang_thai_san_pham NOT NULL DEFAULT 'HOAT_DONG';

-- Migrate dữ liệu từ boolean dang_hoat_dong sang enum trang_thai
UPDATE "san_pham" SET "trang_thai" = 'NGUNG_HOAT_DONG' WHERE "dang_hoat_dong" = false;

-- Xoá cột cũ
ALTER TABLE "san_pham" DROP COLUMN "dang_hoat_dong";
