# T248 - Excel Import Frontend Contract

## Flow

1. Download template.
2. Upload file. Backend validates immediately.
3. If `canConfirm = true`, enable confirm.
4. Confirm import batch.
5. Apply import with the same file when backend requires file checksum matching.

Do not import data when `valid = false` or `canConfirm = false`.

## Download Template

`GET /api/excel-imports/template`

Response:

- `200`
- Content-Type: `application/vnd.openxmlformats-officedocument.spreadsheetml.sheet`
- Attachment filename: `SME_StockSense_Import_Template_v1.xlsx`

Template sheets:

- `01_SanPham`
- `02_TonDauKy`

## Upload And Validate

`POST /api/excel-imports`

Content-Type: `multipart/form-data`

Fields:

- `file`: required `.xlsx`, max 10MB
- `loaiImport`: `PRODUCT_ONLY` or `PRODUCT_WITH_OPENING_STOCK`
- `khoId`: optional

Response fields:

- `id`: import batch ID
- `tenFile`: uploaded filename
- `loaiImport`: import mode
- `trangThai`: `SAN_SANG_IMPORT` or `CO_LOI`
- `valid`: file has no validation errors
- `canConfirm`: UI may enable confirm
- `tongSoDong`: total business rows
- `soDongHopLe`: valid rows
- `soDongLoi`: rows with errors
- `createdAt`: created time

## Validate Only

`POST /api/excel-imports/validate`

Same multipart fields as upload. Does not create or update an import batch.

Response:

- `valid`
- `canConfirm`
- `loaiImport`
- `tongSoDong`
- `soDongHopLe`
- `soDongLoi`
- `errors[]`

Error item:

- `sheetName`
- `rowNumber`
- `columnName`
- `rawValue`
- `message`
- `suggestion`

## Saved Errors

`GET /api/excel-imports/{id}/errors?page=0&size=20`

Use this after upload when `soDongLoi > 0`.

## Confirm

`POST /api/excel-imports/{id}/confirm`

Enable the button only when:

- `valid = true`
- `canConfirm = true`
- `soDongLoi = 0`
- `trangThai = SAN_SANG_IMPORT`

Backend rejects confirm when the batch is not ready.

## Common Errors

- `400`: bad file, unsupported import mode, invalid structure, invalid confirm state
- `401`: not authenticated
- `403`: not Admin
- `404`: import batch not found

## UI Rules

- Never rely on Excel formulas for validation.
- Show row and column from `errors[]`.
- Keep confirm disabled until `canConfirm = true`.
- Do not auto-confirm after upload.
