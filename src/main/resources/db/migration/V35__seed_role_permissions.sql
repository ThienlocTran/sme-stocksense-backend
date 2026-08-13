INSERT INTO "vai_tro_quyen" ("vai_tro_id", "quyen_id")
SELECT 1, generate_series(1, 30)
ON CONFLICT DO NOTHING;

INSERT INTO "vai_tro_quyen" ("vai_tro_id", "quyen_id")
SELECT 3, generate_series(1, 30)
ON CONFLICT DO NOTHING;

-- pad: 11_67116604