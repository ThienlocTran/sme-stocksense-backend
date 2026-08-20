-- 1. Them cot the_tich_don_vi_m3 vao bang san_pham
ALTER TABLE san_pham ADD COLUMN the_tich_don_vi_m3 NUMERIC(12,6);
ALTER TABLE san_pham ADD CONSTRAINT chk_the_tich_don_vi_m3 CHECK (the_tich_don_vi_m3 > 0);

-- 2. Them cot suc_chua_toi_da_m3 vao bang kho voi mac dinh la 1500.000
ALTER TABLE kho ADD COLUMN suc_chua_toi_da_m3 NUMERIC(12,3) NOT NULL DEFAULT 1500.000;
ALTER TABLE kho ADD CONSTRAINT chk_suc_chua_toi_da_m3 CHECK (suc_chua_toi_da_m3 > 0);

-- 3. Tao bang cau_hinh_ton_kho
CREATE TABLE cau_hinh_ton_kho (
    id BIGSERIAL PRIMARY KEY,
    san_pham_id BIGINT NOT NULL REFERENCES san_pham(id) ON DELETE CASCADE,
    kho_id BIGINT NOT NULL REFERENCES kho(id) ON DELETE CASCADE,
    min_stock INTEGER NOT NULL DEFAULT 0,
    ngay_tao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_san_pham_kho UNIQUE (san_pham_id, kho_id),
    CONSTRAINT chk_min_stock CHECK (min_stock >= 0)
);

-- 4. Migrate du lieu min stock hien co tu san_pham
INSERT INTO cau_hinh_ton_kho (san_pham_id, kho_id, min_stock, ngay_tao, ngay_cap_nhat)
SELECT DISTINCT tk.san_pham_id, tk.kho_id, COALESCE(sp.ton_toi_thieu, 0), CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM ton_kho tk
JOIN san_pham sp ON sp.id = tk.san_pham_id
ON CONFLICT (san_pham_id, kho_id) DO NOTHING;

-- 5. Drop cac cot cu trong san_pham
ALTER TABLE san_pham DROP COLUMN ton_toi_thieu;
ALTER TABLE san_pham DROP COLUMN ton_toi_da;

-- 6. Tao bang canh_bao_suc_chua_kho
CREATE TABLE canh_bao_suc_chua_kho (
    id BIGSERIAL PRIMARY KEY,
    kho_id BIGINT NOT NULL REFERENCES kho(id) ON DELETE CASCADE,
    used_capacity_m3 NUMERIC(12,3) NOT NULL,
    max_capacity_m3 NUMERIC(12,3) NOT NULL,
    usage_percentage NUMERIC(5,2) NOT NULL,
    severity VARCHAR(50) NOT NULL,
    status VARCHAR(50) NOT NULL,
    message VARCHAR(500),
    ngay_tao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_giai_quyet TIMESTAMP,
    nguoi_giai_quyet VARCHAR(100)
);

CREATE INDEX idx_canh_bao_suc_chua_kho_id ON canh_bao_suc_chua_kho(kho_id);
CREATE UNIQUE INDEX uq_active_warehouse_capacity_alert ON canh_bao_suc_chua_kho (kho_id) WHERE (status != 'RESOLVED');

-- 7. Snapshot nguong duyet phieu nhap tai thoi diem gui duyet
ALTER TABLE phieu_nhap_kho ADD COLUMN nguong_duyet_ap_dung NUMERIC(15,2);
ALTER TABLE phieu_nhap_kho ADD COLUMN so_cap_duyet_yeu_cau SMALLINT;

-- 8. Chuan hoa business config + audit lich su thay doi
INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('IMPORT_RECEIPT_SECOND_APPROVAL_THRESHOLD', '50000000', 'Nguong phe duyet cap 2 cho phieu nhap kho (VND)')
ON CONFLICT (setting_key) DO NOTHING;

CREATE TABLE system_setting_history (
    id BIGSERIAL PRIMARY KEY,
    setting_key VARCHAR(100) NOT NULL,
    old_value VARCHAR(500),
    new_value VARCHAR(500) NOT NULL,
    reason VARCHAR(500) NOT NULL,
    changed_by_id BIGINT NOT NULL REFERENCES nhan_vien(id),
    changed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_system_setting_history_key_time ON system_setting_history(setting_key, changed_at DESC);
