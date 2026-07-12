CREATE TABLE dot_kiem_ke (
    id BIGSERIAL PRIMARY KEY,
    ma_dot VARCHAR(40) NOT NULL UNIQUE,
    kho_id BIGINT NOT NULL REFERENCES kho(id),
    trang_thai VARCHAR(20) NOT NULL CHECK (trang_thai IN ('NHAP','DANG_KIEM_KE','DA_CHOT','DA_HUY')),
    ghi_chu VARCHAR(500),
    nguoi_tao_id BIGINT NOT NULL REFERENCES nhan_vien(id),
    nguoi_chot_id BIGINT REFERENCES nhan_vien(id),
    ngay_chot TIMESTAMP,
    nguoi_huy_id BIGINT REFERENCES nhan_vien(id),
    ngay_huy TIMESTAMP,
    ly_do_huy VARCHAR(500),
    ngay_tao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE chi_tiet_kiem_ke (
    id BIGSERIAL PRIMARY KEY,
    dot_kiem_ke_id BIGINT NOT NULL REFERENCES dot_kiem_ke(id) ON DELETE CASCADE,
    san_pham_id BIGINT NOT NULL REFERENCES san_pham(id),
    so_luong_he_thong INTEGER NOT NULL CHECK (so_luong_he_thong >= 0),
    so_luong_thuc_te INTEGER CHECK (so_luong_thuc_te >= 0),
    chenh_lech INTEGER,
    ghi_chu VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_chi_tiet_kiem_ke_san_pham UNIQUE (dot_kiem_ke_id, san_pham_id)
);

CREATE INDEX idx_dot_kiem_ke_kho_trang_thai ON dot_kiem_ke(kho_id, trang_thai);
CREATE INDEX idx_chi_tiet_kiem_ke_dot ON chi_tiet_kiem_ke(dot_kiem_ke_id);
