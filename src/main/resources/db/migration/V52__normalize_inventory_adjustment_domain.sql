ALTER TABLE chi_tiet_kiem_ke
    ADD COLUMN IF NOT EXISTS ly_do_chenh_lech VARCHAR(255);

CREATE UNIQUE INDEX IF NOT EXISTS uk_phieu_dieu_chinh_kiem_ke_dot
    ON phieu_dieu_chinh_kiem_ke(dot_kiem_ke_id);
