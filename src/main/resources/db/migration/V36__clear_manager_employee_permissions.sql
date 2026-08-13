DELETE FROM "vai_tro_quyen" WHERE "vai_tro_id" IN (
    SELECT id FROM "vai_tro" WHERE "ma_vai_tro" IN ('MANAGER', 'EMPLOYEE')
);

-- pad: 1_239803643