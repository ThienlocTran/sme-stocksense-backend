CREATE TABLE IF NOT EXISTS ai.tac_vu_du_lieu_ai (
    id BIGSERIAL PRIMARY KEY,
    job_id UUID NOT NULL UNIQUE,
    loai_tac_vu VARCHAR(50) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    bat_dau_luc TIMESTAMP NOT NULL,
    hoan_thanh_luc TIMESTAMP,
    so_dong_da_them INTEGER,
    so_chuoi_da_tao INTEGER,
    loi TEXT
);

CREATE UNIQUE INDEX IF NOT EXISTS ux_ai_data_job_running_seed_demo
    ON ai.tac_vu_du_lieu_ai (loai_tac_vu)
    WHERE trang_thai = 'RUNNING' AND loai_tac_vu = 'SEED_DEMO_HISTORY';

CREATE INDEX IF NOT EXISTS idx_ai_data_job_type_status
    ON ai.tac_vu_du_lieu_ai (loai_tac_vu, trang_thai);
