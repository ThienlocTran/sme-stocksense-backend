-- Align existing canh_bao_ton_kho table from V1 with the InventoryAlert entity.
-- Data is migrated in place; no table drop/truncate.

CREATE TABLE IF NOT EXISTS canh_bao_ton_kho (
    id BIGSERIAL PRIMARY KEY,
    san_pham_id BIGINT NOT NULL,
    kho_id BIGINT NOT NULL,
    so_luong_hien_tai INTEGER NOT NULL,
    ton_toi_thieu INTEGER,
    ton_toi_da INTEGER,
    muc_do VARCHAR(20) NOT NULL DEFAULT 'WARNING',
    trang_thai VARCHAR(20) NOT NULL DEFAULT 'OPEN',
    ghi_chu VARCHAR(500),
    nguoi_xu_ly VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    ngay_tao TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    ngay_cap_nhat TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    ngay_giai_quyet TIMESTAMP WITHOUT TIME ZONE
);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'canh_bao_ton_kho'
          AND column_name = 'ton_hien_tai'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'canh_bao_ton_kho'
          AND column_name = 'so_luong_hien_tai'
    ) THEN
        ALTER TABLE canh_bao_ton_kho RENAME COLUMN ton_hien_tai TO so_luong_hien_tai;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'canh_bao_ton_kho'
          AND column_name = 'noi_dung'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'canh_bao_ton_kho'
          AND column_name = 'ghi_chu'
    ) THEN
        ALTER TABLE canh_bao_ton_kho RENAME COLUMN noi_dung TO ghi_chu;
    END IF;

    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'canh_bao_ton_kho'
          AND column_name = 'ngay_xu_ly'
    ) AND NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'canh_bao_ton_kho'
          AND column_name = 'ngay_giai_quyet'
    ) THEN
        ALTER TABLE canh_bao_ton_kho RENAME COLUMN ngay_xu_ly TO ngay_giai_quyet;
    END IF;
END $$;

ALTER TABLE canh_bao_ton_kho
    ADD COLUMN IF NOT EXISTS so_luong_hien_tai INTEGER,
    ADD COLUMN IF NOT EXISTS ton_toi_thieu INTEGER,
    ADD COLUMN IF NOT EXISTS ton_toi_da INTEGER,
    ADD COLUMN IF NOT EXISTS ghi_chu VARCHAR(500),
    ADD COLUMN IF NOT EXISTS nguoi_xu_ly VARCHAR(100),
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS ngay_tao TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS ngay_cap_nhat TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    ADD COLUMN IF NOT EXISTS ngay_giai_quyet TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE canh_bao_ton_kho
    ALTER COLUMN ghi_chu TYPE VARCHAR(500),
    ALTER COLUMN nguoi_xu_ly TYPE VARCHAR(100);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'canh_bao_ton_kho'
          AND column_name = 'nguoi_xu_ly_id'
    ) THEN
        UPDATE canh_bao_ton_kho cb
        SET nguoi_xu_ly = nv.email
        FROM nhan_vien nv
        WHERE cb.nguoi_xu_ly_id = nv.id
          AND cb.nguoi_xu_ly IS NULL;
    END IF;
END $$;

UPDATE canh_bao_ton_kho
SET so_luong_hien_tai = 0
WHERE so_luong_hien_tai IS NULL;

UPDATE canh_bao_ton_kho
SET version = 0
WHERE version IS NULL;

UPDATE canh_bao_ton_kho
SET ngay_tao = NOW()
WHERE ngay_tao IS NULL;

UPDATE canh_bao_ton_kho
SET ngay_cap_nhat = COALESCE(ngay_giai_quyet, ngay_tao, NOW())
WHERE ngay_cap_nhat IS NULL;

ALTER TABLE canh_bao_ton_kho
    ALTER COLUMN muc_do DROP DEFAULT,
    ALTER COLUMN muc_do TYPE VARCHAR(20) USING (
        CASE
            WHEN muc_do::text IN ('CRITICAL', 'KHAN_CAP', 'CAO') THEN 'CRITICAL'
            ELSE 'WARNING'
        END
    ),
    ALTER COLUMN muc_do SET DEFAULT 'WARNING',
    ALTER COLUMN muc_do SET NOT NULL,
    ALTER COLUMN trang_thai DROP DEFAULT,
    ALTER COLUMN trang_thai TYPE VARCHAR(20) USING (
        CASE
            WHEN trang_thai::text IN ('RESOLVED', 'DA_XU_LY') THEN 'RESOLVED'
            WHEN trang_thai::text IN ('ACKNOWLEDGED', 'DA_XEM') THEN 'ACKNOWLEDGED'
            ELSE 'OPEN'
        END
    ),
    ALTER COLUMN trang_thai SET DEFAULT 'OPEN',
    ALTER COLUMN trang_thai SET NOT NULL,
    ALTER COLUMN so_luong_hien_tai SET NOT NULL,
    ALTER COLUMN version SET DEFAULT 0,
    ALTER COLUMN version SET NOT NULL,
    ALTER COLUMN ngay_tao SET DEFAULT NOW(),
    ALTER COLUMN ngay_tao SET NOT NULL,
    ALTER COLUMN ngay_cap_nhat SET DEFAULT NOW(),
    ALTER COLUMN ngay_cap_nhat SET NOT NULL;

CREATE INDEX IF NOT EXISTS idx_canh_bao_sp_kho_trang_thai ON canh_bao_ton_kho (san_pham_id, kho_id, trang_thai);
CREATE INDEX IF NOT EXISTS idx_canh_bao_kho_trang_thai_ngay ON canh_bao_ton_kho (kho_id, trang_thai, ngay_tao DESC);
CREATE INDEX IF NOT EXISTS idx_canh_bao_muc_do ON canh_bao_ton_kho (muc_do);
