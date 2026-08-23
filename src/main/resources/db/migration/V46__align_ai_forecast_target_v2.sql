CREATE SCHEMA IF NOT EXISTS ai;

ALTER TABLE ai.lich_su_ban_hang
    ADD COLUMN IF NOT EXISTS gia_ban_binh_quan NUMERIC(15,2),
    ADD COLUMN IF NOT EXISTS nguon_du_lieu VARCHAR(50),
    ADD COLUMN IF NOT EXISTS tham_chieu_nguon VARCHAR(255),
    ADD COLUMN IF NOT EXISTS ngay_cap_nhat TIMESTAMP;

UPDATE ai.lich_su_ban_hang
SET nguon_du_lieu = CASE
        WHEN nguon = 'SEED' THEN 'SEED_DEMO'
        WHEN nguon IS NULL THEN 'SEED_DEMO'
        ELSE nguon
    END,
    ngay_cap_nhat = COALESCE(ngay_cap_nhat, ngay_tao, now())
WHERE nguon_du_lieu IS NULL OR ngay_cap_nhat IS NULL;

ALTER TABLE ai.lich_su_ban_hang
    ALTER COLUMN so_luong SET DEFAULT 0,
    ALTER COLUMN nguon_du_lieu SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_ai_lich_su_so_luong_non_negative'
    ) THEN
        ALTER TABLE ai.lich_su_ban_hang
            ADD CONSTRAINT chk_ai_lich_su_so_luong_non_negative CHECK (so_luong >= 0);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ai_lich_su_sp_kho ON ai.lich_su_ban_hang (san_pham_id, kho_id);
CREATE INDEX IF NOT EXISTS idx_ai_lich_su_ngay ON ai.lich_su_ban_hang (ngay);
CREATE INDEX IF NOT EXISTS idx_ai_lich_su_nguon ON ai.lich_su_ban_hang (nguon_du_lieu);

ALTER TABLE ai.thong_tin_mo_hinh
    ADD COLUMN IF NOT EXISTS kieu_tap_du_lieu VARCHAR(30),
    ADD COLUMN IF NOT EXISTS ngay_bat_dau_du_lieu DATE,
    ADD COLUMN IF NOT EXISTS ngay_ket_thuc_du_lieu DATE,
    ADD COLUMN IF NOT EXISTS mae NUMERIC(18,4),
    ADD COLUMN IF NOT EXISTS rmse NUMERIC(18,4),
    ADD COLUMN IF NOT EXISTS tham_so_mo_hinh JSONB,
    ADD COLUMN IF NOT EXISTS dac_trung_su_dung JSONB,
    ADD COLUMN IF NOT EXISTS ngay_huan_luyen TIMESTAMP;

UPDATE ai.thong_tin_mo_hinh
SET kieu_tap_du_lieu = COALESCE(kieu_tap_du_lieu, CASE WHEN che_do = 'COLD_START_AVG' THEN 'COLD_START' ELSE 'LEGACY_UNKNOWN' END),
    ngay_huan_luyen = COALESCE(ngay_huan_luyen, ngay_tao, now())
WHERE kieu_tap_du_lieu IS NULL OR ngay_huan_luyen IS NULL;

ALTER TABLE ai.thong_tin_mo_hinh
    ALTER COLUMN smape DROP NOT NULL,
    ALTER COLUMN kieu_tap_du_lieu SET NOT NULL,
    ALTER COLUMN ngay_huan_luyen SET NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_model_sp_kho_version
    ON ai.thong_tin_mo_hinh (san_pham_id, kho_id, phien_ban);
CREATE INDEX IF NOT EXISTS idx_ai_model_sp_kho_time
    ON ai.thong_tin_mo_hinh (san_pham_id, kho_id, ngay_huan_luyen);

ALTER TABLE ai.ket_qua_du_bao
    ADD COLUMN IF NOT EXISTS thong_tin_mo_hinh_id BIGINT,
    ADD COLUMN IF NOT EXISTS horizon_days SMALLINT,
    ADD COLUMN IF NOT EXISTS ngay_moc_du_bao DATE,
    ADD COLUMN IF NOT EXISTS nhu_cau_trung_binh_ngay NUMERIC(18,4);

UPDATE ai.ket_qua_du_bao kq
SET thong_tin_mo_hinh_id = COALESCE(kq.thong_tin_mo_hinh_id, md.id),
    horizon_days = COALESCE(kq.horizon_days, kq.so_ngay_du_bao::SMALLINT),
    ngay_moc_du_bao = COALESCE(kq.ngay_moc_du_bao, kq.ngay_du_bao - kq.so_ngay_du_bao),
    nhu_cau_trung_binh_ngay = COALESCE(kq.nhu_cau_trung_binh_ngay, kq.so_luong_du_bao)
FROM ai.thong_tin_mo_hinh md
WHERE md.san_pham_id = kq.san_pham_id
  AND md.kho_id = kq.kho_id
  AND md.phien_ban = kq.phien_ban;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_ai_forecast_horizon'
    ) THEN
        ALTER TABLE ai.ket_qua_du_bao
            ADD CONSTRAINT chk_ai_forecast_horizon CHECK (horizon_days IS NULL OR horizon_days IN (7, 14, 30));
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'chk_ai_forecast_avg_non_negative'
    ) THEN
        ALTER TABLE ai.ket_qua_du_bao
            ADD CONSTRAINT chk_ai_forecast_avg_non_negative CHECK (nhu_cau_trung_binh_ngay IS NULL OR nhu_cau_trung_binh_ngay >= 0);
    END IF;
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_ai_forecast_summary_model'
    ) THEN
        ALTER TABLE ai.ket_qua_du_bao
            ADD CONSTRAINT fk_ai_forecast_summary_model FOREIGN KEY (thong_tin_mo_hinh_id) REFERENCES ai.thong_tin_mo_hinh(id);
    END IF;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_forecast_horizon
    ON ai.ket_qua_du_bao (thong_tin_mo_hinh_id, horizon_days)
    WHERE thong_tin_mo_hinh_id IS NOT NULL AND horizon_days IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_ai_forecast_ngay_moc
    ON ai.ket_qua_du_bao (ngay_moc_du_bao);

CREATE TABLE IF NOT EXISTS ai.ket_qua_du_bao_hang_ngay (
    id BIGSERIAL PRIMARY KEY,
    thong_tin_mo_hinh_id BIGINT NOT NULL REFERENCES ai.thong_tin_mo_hinh(id),
    ngay_du_bao DATE NOT NULL,
    so_luong_du_bao NUMERIC(18,4) NOT NULL,
    ngay_tao TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT chk_ai_forecast_daily_quantity_non_negative CHECK (so_luong_du_bao >= 0)
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_ai_forecast_daily
    ON ai.ket_qua_du_bao_hang_ngay (thong_tin_mo_hinh_id, ngay_du_bao);
CREATE INDEX IF NOT EXISTS idx_ai_forecast_daily_ngay
    ON ai.ket_qua_du_bao_hang_ngay (ngay_du_bao);

ALTER TABLE ai.nhat_ky_lech_mo_hinh
    ADD COLUMN IF NOT EXISTS thong_tin_mo_hinh_id BIGINT,
    ADD COLUMN IF NOT EXISTS rolling_smape NUMERIC(10,4),
    ADD COLUMN IF NOT EXISTS so_ngay_doi_chieu INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS can_huan_luyen_lai BOOLEAN,
    ADD COLUMN IF NOT EXISTS ngay_kiem_tra TIMESTAMP;

UPDATE ai.nhat_ky_lech_mo_hinh n
SET thong_tin_mo_hinh_id = COALESCE(n.thong_tin_mo_hinh_id, md.id),
    rolling_smape = COALESCE(n.rolling_smape, n.smape_thuc_te),
    can_huan_luyen_lai = COALESCE(n.can_huan_luyen_lai, n.can_train_lai),
    ngay_kiem_tra = COALESCE(n.ngay_kiem_tra, n.ngay_phat_hien, now())
FROM ai.thong_tin_mo_hinh md
WHERE md.san_pham_id = n.san_pham_id
  AND md.kho_id = n.kho_id
  AND md.phien_ban = (
      SELECT MAX(m2.phien_ban)
      FROM ai.thong_tin_mo_hinh m2
      WHERE m2.san_pham_id = n.san_pham_id AND m2.kho_id = n.kho_id
  );

UPDATE ai.nhat_ky_lech_mo_hinh
SET can_huan_luyen_lai = COALESCE(can_huan_luyen_lai, false),
    ngay_kiem_tra = COALESCE(ngay_kiem_tra, ngay_phat_hien, now());

ALTER TABLE ai.nhat_ky_lech_mo_hinh
    ALTER COLUMN can_huan_luyen_lai SET NOT NULL,
    ALTER COLUMN ngay_kiem_tra SET NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_ai_drift_model'
    ) THEN
        ALTER TABLE ai.nhat_ky_lech_mo_hinh
            ADD CONSTRAINT fk_ai_drift_model FOREIGN KEY (thong_tin_mo_hinh_id) REFERENCES ai.thong_tin_mo_hinh(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_ai_drift_model ON ai.nhat_ky_lech_mo_hinh (thong_tin_mo_hinh_id);
CREATE INDEX IF NOT EXISTS idx_ai_drift_time ON ai.nhat_ky_lech_mo_hinh (ngay_kiem_tra);

CREATE TABLE IF NOT EXISTS yeu_cau_nhap_hang_ai (
    id BIGSERIAL PRIMARY KEY,
    ma_yeu_cau VARCHAR(50) NOT NULL UNIQUE,
    thong_tin_mo_hinh_id BIGINT NOT NULL REFERENCES ai.thong_tin_mo_hinh(id),
    san_pham_id BIGINT NOT NULL REFERENCES san_pham(id),
    kho_id BIGINT NOT NULL REFERENCES kho(id),
    horizon_days SMALLINT NOT NULL,
    so_luong_ai_goi_y INTEGER NOT NULL,
    so_luong_yeu_cau INTEGER NOT NULL,
    nguoi_gui_id BIGINT NOT NULL REFERENCES nhan_vien(id),
    nguoi_nhan_id BIGINT NOT NULL REFERENCES nhan_vien(id),
    noi_dung VARCHAR(1000),
    trang_thai VARCHAR(30) NOT NULL DEFAULT 'DA_GUI',
    trang_thai_email VARCHAR(30) NOT NULL DEFAULT 'CHO_GUI',
    ngay_gui_email TIMESTAMP,
    loi_gui_email VARCHAR(500),
    phieu_nhap_id BIGINT REFERENCES phieu_nhap_kho(id),
    ngay_tao TIMESTAMP NOT NULL DEFAULT now(),
    ngay_tiep_nhan TIMESTAMP,
    ngay_cap_nhat TIMESTAMP,
    CONSTRAINT chk_yc_ai_horizon CHECK (horizon_days IN (7, 14, 30)),
    CONSTRAINT chk_yc_ai_suggested_quantity CHECK (so_luong_ai_goi_y >= 0),
    CONSTRAINT chk_yc_ai_requested_quantity CHECK (so_luong_yeu_cau >= 0)
);

CREATE INDEX IF NOT EXISTS idx_yc_ai_nguoi_nhan_trang_thai
    ON yeu_cau_nhap_hang_ai (nguoi_nhan_id, trang_thai);
CREATE INDEX IF NOT EXISTS idx_yc_ai_model
    ON yeu_cau_nhap_hang_ai (thong_tin_mo_hinh_id);
CREATE INDEX IF NOT EXISTS idx_yc_ai_sp_kho
    ON yeu_cau_nhap_hang_ai (san_pham_id, kho_id);
CREATE INDEX IF NOT EXISTS idx_yc_ai_phieu_nhap
    ON yeu_cau_nhap_hang_ai (phieu_nhap_id);
