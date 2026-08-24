CREATE TABLE phieu_dieu_chinh_kiem_ke (
    id BIGSERIAL PRIMARY KEY,
    ma_phieu VARCHAR(50) NOT NULL UNIQUE,
    dot_kiem_ke_id BIGINT NOT NULL REFERENCES dot_kiem_ke(id),
    trang_thai VARCHAR(20) NOT NULL CHECK (trang_thai IN ('NHAP','CHO_DUYET','DA_DUYET','TU_CHOI','DA_AP_DUNG')),
    nguoi_tao_id BIGINT NOT NULL REFERENCES nhan_vien(id),
    nguoi_gui_duyet_id BIGINT REFERENCES nhan_vien(id),
    ngay_gui_duyet TIMESTAMP,
    nguoi_duyet_id BIGINT REFERENCES nhan_vien(id),
    ngay_duyet TIMESTAMP,
    ngay_ap_dung TIMESTAMP,
    ghi_chu VARCHAR(500),
    ly_do_tu_choi VARCHAR(500),
    ngay_tao TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ngay_cap_nhat TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE chi_tiet_dieu_chinh_kiem_ke (
    id BIGSERIAL PRIMARY KEY,
    phieu_dieu_chinh_id BIGINT NOT NULL REFERENCES phieu_dieu_chinh_kiem_ke(id) ON DELETE CASCADE,
    san_pham_id BIGINT NOT NULL REFERENCES san_pham(id),
    so_luong_he_thong INTEGER NOT NULL CHECK (so_luong_he_thong >= 0),
    so_luong_thuc_te INTEGER NOT NULL CHECK (so_luong_thuc_te >= 0),
    chenh_lech INTEGER NOT NULL,
    ly_do VARCHAR(255),
    ghi_chu VARCHAR(500),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_chi_tiet_dieu_chinh_kiem_ke_san_pham UNIQUE (phieu_dieu_chinh_id, san_pham_id),
    CONSTRAINT chk_ct_dieu_chinh_kiem_ke_chenh_lech CHECK (chenh_lech = so_luong_thuc_te - so_luong_he_thong)
);

CREATE INDEX idx_phieu_dieu_chinh_kiem_ke_dot ON phieu_dieu_chinh_kiem_ke(dot_kiem_ke_id);
CREATE INDEX idx_phieu_dieu_chinh_kiem_ke_trang_thai ON phieu_dieu_chinh_kiem_ke(trang_thai);
CREATE UNIQUE INDEX uk_phieu_dieu_chinh_kiem_ke_active
    ON phieu_dieu_chinh_kiem_ke(dot_kiem_ke_id)
    WHERE trang_thai IN ('NHAP','CHO_DUYET','DA_DUYET');
CREATE INDEX idx_ct_dieu_chinh_kiem_ke_phieu ON chi_tiet_dieu_chinh_kiem_ke(phieu_dieu_chinh_id);
