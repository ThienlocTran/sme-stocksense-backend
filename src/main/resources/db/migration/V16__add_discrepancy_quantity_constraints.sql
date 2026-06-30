DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_ct_bbcl_qty_non_negative'
    ) THEN
        ALTER TABLE "chi_tiet_bien_ban_chenh_lech"
            ADD CONSTRAINT "ck_ct_bbcl_qty_non_negative"
            CHECK ("so_luong_chung_tu" >= 0 AND "so_luong_thuc_te" >= 0);
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'ck_ct_bbcl_qty_delta'
    ) THEN
        ALTER TABLE "chi_tiet_bien_ban_chenh_lech"
            ADD CONSTRAINT "ck_ct_bbcl_qty_delta"
            CHECK ("so_luong_lech" = "so_luong_thuc_te" - "so_luong_chung_tu");
    END IF;
END $$;
