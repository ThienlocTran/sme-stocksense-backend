ALTER TABLE san_pham ADD COLUMN IF NOT EXISTS ton_toi_thieu_mac_dinh INTEGER;

ALTER TABLE san_pham
    ADD CONSTRAINT chk_san_pham_ton_toi_thieu_mac_dinh
    CHECK (ton_toi_thieu_mac_dinh IS NULL OR ton_toi_thieu_mac_dinh >= 0);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'cau_hinh_ton_kho' AND column_name = 'min_stock'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'cau_hinh_ton_kho' AND column_name = 'ton_toi_thieu_ghi_de'
    ) THEN
        ALTER TABLE cau_hinh_ton_kho RENAME COLUMN min_stock TO ton_toi_thieu_ghi_de;
    END IF;
END $$;

ALTER TABLE cau_hinh_ton_kho
    ALTER COLUMN ton_toi_thieu_ghi_de DROP NOT NULL;

ALTER TABLE cau_hinh_ton_kho
    ADD CONSTRAINT chk_cau_hinh_ton_kho_ton_toi_thieu_ghi_de
    CHECK (ton_toi_thieu_ghi_de IS NULL OR ton_toi_thieu_ghi_de >= 0);
