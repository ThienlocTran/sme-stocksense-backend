-- =============================================================================
-- V53: Danh dau model thang cuoc (competition) trong ai.thong_tin_mo_hinh.
-- Tu nay moi lan huan luyen (dat >= MIN_HISTORY_DAYS) ghi 3 dong (XGBOOST/PROPHET/ETS),
-- cot nay danh dau dong nao la model co sMAPE thap nhat (duoc dung de du bao thuc te).
-- Mac dinh true de cac dong lich su (chi co 1 model) van dung ve mat y nghia.
-- =============================================================================

ALTER TABLE ai.thong_tin_mo_hinh ADD COLUMN la_mo_hinh_thang BOOLEAN NOT NULL DEFAULT true;

CREATE INDEX IF NOT EXISTS idx_ttmh_sp_kho_thang ON ai.thong_tin_mo_hinh (san_pham_id, kho_id, la_mo_hinh_thang);
