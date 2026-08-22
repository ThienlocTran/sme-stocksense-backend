ALTER TABLE ai.lich_su_ban_hang
    DROP CONSTRAINT IF EXISTS uk_lshb_sp_kho_ngay;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint
        WHERE conname = 'uk_lshb_sp_kho_ngay_nguon'
    ) THEN
        ALTER TABLE ai.lich_su_ban_hang
            ADD CONSTRAINT uk_lshb_sp_kho_ngay_nguon
            UNIQUE (san_pham_id, kho_id, ngay, nguon_du_lieu);
    END IF;
END $$;
