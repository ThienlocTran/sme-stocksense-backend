ALTER TABLE "phieu_xuat_kho"
    DROP CONSTRAINT IF EXISTS "chk_phieu_xuat_trang_thai_t75";

ALTER TABLE "phieu_xuat_kho"
    ADD CONSTRAINT "chk_phieu_xuat_trang_thai_t75"
        CHECK ("trang_thai"::text IN (
            'NHAP',
            'CHO_DUYET',
            'DA_DUYET',
            'HOAN_THANH',
            'TU_CHOI',
            'HUY'
        )) NOT VALID;
