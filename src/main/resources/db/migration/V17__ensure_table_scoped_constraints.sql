DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_ctpn_trang_thai_dong'
          AND conrelid = 'chi_tiet_phieu_nhap'::regclass
          AND contype = 'c'
    ) THEN
        ALTER TABLE "chi_tiet_phieu_nhap"
            ADD CONSTRAINT "ck_ctpn_trang_thai_dong"
            CHECK ("trang_thai_dong" IS NULL OR "trang_thai_dong" IN ('KHOP', 'CHENH_LECH'));
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_ct_bbcl_qty_non_negative'
          AND conrelid = 'chi_tiet_bien_ban_chenh_lech'::regclass
          AND contype = 'c'
    ) THEN
        ALTER TABLE "chi_tiet_bien_ban_chenh_lech"
            ADD CONSTRAINT "ck_ct_bbcl_qty_non_negative"
            CHECK ("so_luong_chung_tu" >= 0 AND "so_luong_thuc_te" >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_ct_bbcl_qty_delta'
          AND conrelid = 'chi_tiet_bien_ban_chenh_lech'::regclass
          AND contype = 'c'
    ) THEN
        ALTER TABLE "chi_tiet_bien_ban_chenh_lech"
            ADD CONSTRAINT "ck_ct_bbcl_qty_delta"
            CHECK ("so_luong_lech" = "so_luong_thuc_te" - "so_luong_chung_tu");
    END IF;
END $$;
