# T246 - Excel Import Upload Validation

## API

`POST /api/excel-imports`

Multipart fields:

- `file`: required `.xlsx`, max 10MB
- `loaiImport`: `PRODUCT_ONLY` or `PRODUCT_WITH_OPENING_STOCK`
- `khoId`: optional

## Behavior

Upload now creates the import batch, validates the uploaded workbook immediately, persists validation errors, and stores row counters/status on the batch.

Status after upload:

- `SAN_SANG_IMPORT`: workbook is valid
- `CO_LOI`: workbook has validation errors

Validation stays in backend. The Excel file does not use formula validation.
