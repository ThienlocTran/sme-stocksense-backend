# T247 - Excel Import Response

## Upload response

`POST /api/excel-imports`

Key fields for the two-step UI:

- `valid`: file has no validation errors
- `canConfirm`: UI may enable confirm
- `tongSoDong`: total business rows
- `soDongLoi`: rows with errors
- `trangThai`: import batch status

## Validate response

`POST /api/excel-imports/validate`

Returns:

- `valid`
- `canConfirm`
- `tongSoDong`
- `soDongHopLe`
- `soDongLoi`
- `errors[]` with sheet, row, column, raw value, message, suggestion

## Confirm rule

UI should show or enable confirm only when `canConfirm = true`. Backend still rejects confirm unless the batch status is `SAN_SANG_IMPORT`.
