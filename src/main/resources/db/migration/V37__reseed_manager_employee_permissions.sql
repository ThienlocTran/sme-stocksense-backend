-- V37: Re-seed correct MANAGER and EMPLOYEE permissions per quy-trinh-kho.html
-- V36 cleared all MANAGER and EMPLOYEE permissions - this migration restores them correctly.

-- MANAGER permissions: approve/reject receipts, view all receipts, view import history/template
INSERT INTO "vai_tro_quyen" ("vai_tro_id", "quyen_id")
SELECT vt.id, q.id
FROM "vai_tro" vt, "quyen" q
WHERE vt.ma_vai_tro = 'MANAGER'
  AND q.ma_quyen IN (
    'VIEW_IMPORT_ALL',
    'APPROVE_IMPORT_L1',
    'APPROVE_IMPORT_L2',
    'REJECT_IMPORT',
    'CANCEL_IMPORT',
    'APPROVE_DISCREPANCY',
    'VIEW_EXPORT_ALL',
    'APPROVE_EXPORT',
    'REJECT_EXPORT',
    'VIEW_DASHBOARD',
    'VIEW_INVENTORY',
    'MANAGE_WAREHOUSE',
    'MANAGE_CATEGORY',
    'MANAGE_PARTNER',
    'MANAGE_PRODUCT'
)
ON CONFLICT DO NOTHING;

-- EMPLOYEE permissions: create/submit/complete receipts, view own receipts, import Excel, view inventory
INSERT INTO "vai_tro_quyen" ("vai_tro_id", "quyen_id")
SELECT vt.id, q.id
FROM "vai_tro" vt, "quyen" q
WHERE vt.ma_vai_tro = 'EMPLOYEE'
  AND q.ma_quyen IN (
    'VIEW_IMPORT_OWN',
    'CREATE_IMPORT',
    'UPDATE_IMPORT',
    'SUBMIT_IMPORT',
    'CANCEL_IMPORT',
    'INSPECT_IMPORT',
    'COMPLETE_IMPORT',
    'CREATE_DISCREPANCY',
    'VIEW_EXPORT_OWN',
    'CREATE_EXPORT',
    'UPDATE_EXPORT',
    'SUBMIT_EXPORT',
    'COMPLETE_EXPORT',
    'EXCEL_UPLOAD_CONFIRM',
    'VIEW_DASHBOARD',
    'VIEW_INVENTORY'
)
ON CONFLICT DO NOTHING;
