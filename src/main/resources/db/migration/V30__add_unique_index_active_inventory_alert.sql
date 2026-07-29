-- V30__add_unique_index_active_inventory_alert.sql
-- Thêm chỉ mục duy nhất một phần (Partial Unique Index) tại tầng DB để chống tạo trùng lặp phiếu cảnh báo (Race Condition).
-- Chỉ áp dụng với các phiếu đang ở trạng thái OPEN hoặc ACKNOWLEDGED.
-- Khi phiếu chuyển sang RESOLVED, chỉ mục không còn cản trở, cho phép tạo phiếu OPEN mới cho chu kỳ tiếp theo.

CREATE UNIQUE INDEX IF NOT EXISTS idx_unique_active_alert 
ON canh_bao_ton_kho (san_pham_id, kho_id) 
WHERE trang_thai IN ('OPEN', 'ACKNOWLEDGED');
