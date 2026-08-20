CREATE TABLE system_settings (
    setting_key VARCHAR(100) PRIMARY KEY,
    setting_value VARCHAR(255) NOT NULL,
    description VARCHAR(500)
);

INSERT INTO system_settings (setting_key, setting_value, description)
VALUES ('import_receipt_threshold', '50000000', 'Ngưỡng phê duyệt cấp 2 cho phiếu nhập kho (VND)');
