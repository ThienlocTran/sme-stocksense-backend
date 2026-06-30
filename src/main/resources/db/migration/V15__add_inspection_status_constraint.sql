DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_ctpn_trang_thai_dong'
    ) THEN
        ALTER TABLE "chi_tiet_phieu_nhap"
            ADD CONSTRAINT "ck_ctpn_trang_thai_dong"
            CHECK ("trang_thai_dong" IS NULL OR "trang_thai_dong" IN ('KHOP', 'CHENH_LECH'));
    END IF;
END $$;
