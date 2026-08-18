-- =============================================================================
-- V40: AI Forecast module - schema "ai" rieng biet cho du bao ton kho bang XGBoost
-- Khong chua du lieu seed (du lieu demo duoc sinh qua endpoint rieng, khong qua migration)
-- =============================================================================

CREATE SCHEMA IF NOT EXISTS ai;

-- Lich su ban hang: du lieu chuoi thoi gian dung de huan luyen model (seed demo hoac thuc te sau nay)
CREATE TABLE IF NOT EXISTS ai.lich_su_ban_hang (
    id BIGSERIAL PRIMARY KEY,
    san_pham_id BIGINT NOT NULL REFERENCES san_pham(id),
    kho_id BIGINT NOT NULL REFERENCES kho(id),
    ngay DATE NOT NULL,
    so_luong INTEGER NOT NULL,
    nguon VARCHAR(20) NOT NULL DEFAULT 'SEED',
    ngay_tao TIMESTAMP NOT NULL DEFAULT now(),
    CONSTRAINT uk_lshb_sp_kho_ngay UNIQUE (san_pham_id, kho_id, ngay)
);

-- Ket qua du bao: du bao 7/14/30 ngay cho tung san pham/kho
CREATE TABLE IF NOT EXISTS ai.ket_qua_du_bao (
    id BIGSERIAL PRIMARY KEY,
    san_pham_id BIGINT NOT NULL REFERENCES san_pham(id),
    kho_id BIGINT NOT NULL REFERENCES kho(id),
    ngay_du_bao DATE NOT NULL,
    so_ngay_du_bao INTEGER NOT NULL CHECK (so_ngay_du_bao IN (7, 14, 30)),
    so_luong_du_bao NUMERIC(12, 2) NOT NULL,
    phien_ban INTEGER NOT NULL DEFAULT 1,
    ngay_tao TIMESTAMP NOT NULL DEFAULT now()
);

-- Thong tin mo hinh: metadata cua lan huan luyen (sMAPE, phien ban, che do)
CREATE TABLE IF NOT EXISTS ai.thong_tin_mo_hinh (
    id BIGSERIAL PRIMARY KEY,
    san_pham_id BIGINT NOT NULL REFERENCES san_pham(id),
    kho_id BIGINT NOT NULL REFERENCES kho(id),
    smape NUMERIC(7, 4) NOT NULL,
    phien_ban INTEGER NOT NULL DEFAULT 1,
    so_ngay_du_lieu INTEGER NOT NULL,
    che_do VARCHAR(20) NOT NULL DEFAULT 'XGBOOST',
    ngay_tao TIMESTAMP NOT NULL DEFAULT now()
);

-- Nhat ky lech mo hinh (drift log): so sanh du bao da luu voi thuc te
CREATE TABLE IF NOT EXISTS ai.nhat_ky_lech_mo_hinh (
    id BIGSERIAL PRIMARY KEY,
    san_pham_id BIGINT NOT NULL REFERENCES san_pham(id),
    kho_id BIGINT NOT NULL REFERENCES kho(id),
    smape_thuc_te NUMERIC(7, 4) NOT NULL,
    nguong_smape NUMERIC(7, 4) NOT NULL DEFAULT 20.0,
    can_train_lai BOOLEAN NOT NULL DEFAULT false,
    ngay_phat_hien TIMESTAMP NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_lshb_sp_kho_ngay ON ai.lich_su_ban_hang (san_pham_id, kho_id, ngay);
CREATE INDEX IF NOT EXISTS idx_kqdb_sp_kho_ngay ON ai.ket_qua_du_bao (san_pham_id, kho_id, ngay_du_bao);
CREATE INDEX IF NOT EXISTS idx_ttmh_sp_kho_phien ON ai.thong_tin_mo_hinh (san_pham_id, kho_id, phien_ban DESC);
CREATE INDEX IF NOT EXISTS idx_nklm_sp_kho ON ai.nhat_ky_lech_mo_hinh (san_pham_id, kho_id);
