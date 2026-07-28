-- Tạo bảng canh_bao_ton_kho lưu trữ snapshot trạng thái và lịch sử xử lý tụt kho.
-- Sử dụng ON DELETE RESTRICT theo chuẩn Enterprise ERP để bảo toàn dấu vết kiểm toán (Audit Trail), cấm xóa sản phẩm/kho nếu đã có cảnh báo.
CREATE TABLE canh_bao_ton_kho (
    id BIGSERIAL PRIMARY KEY,
    san_pham_id BIGINT NOT NULL,
    kho_id BIGINT NOT NULL,
    so_luong_hien_tai INTEGER NOT NULL,
    ton_toi_thieu INTEGER,
    ton_toi_da INTEGER,
    muc_do VARCHAR(20) NOT NULL,       -- CRITICAL | WARNING
    trang_thai VARCHAR(20) NOT NULL,   -- OPEN | ACKNOWLEDGED | RESOLVED
    ghi_chu VARCHAR(500),
    nguoi_xu_ly VARCHAR(100),          -- Username nhân viên hoặc 'SYSTEM'/'SCHEDULER'
    version BIGINT NOT NULL DEFAULT 0, -- Optimistic Lock JPA chống xung đột khi cập nhật đồng thời
    ngay_tao TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    ngay_cap_nhat TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    ngay_giai_quyet TIMESTAMP WITHOUT TIME ZONE,
    
    CONSTRAINT fk_canh_bao_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham (id) ON DELETE RESTRICT,
    CONSTRAINT fk_canh_bao_kho FOREIGN KEY (kho_id) REFERENCES kho (id) ON DELETE RESTRICT
);

-- Tạo các Index phục vụ hiệu năng tra cứu danh sách trên Dashboard và hỗ trợ chống spam/tạo trùng lặp (T179).
CREATE INDEX idx_canh_bao_sp_kho_trang_thai ON canh_bao_ton_kho (san_pham_id, kho_id, trang_thai);
CREATE INDEX idx_canh_bao_kho_trang_thai_ngay ON canh_bao_ton_kho (kho_id, trang_thai, ngay_tao DESC);
CREATE INDEX idx_canh_bao_muc_do ON canh_bao_ton_kho (muc_do);
