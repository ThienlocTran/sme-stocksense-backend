# T245 - Excel Import Template

## API

`GET /api/excel-imports/template`

Returns `SME_StockSense_Import_Template_v1.xlsx`.

## Workbook

Sheets:

- `01_SanPham`
- `02_TonDauKy`

Both sheets contain headers only. The workbook does not include reference, rule, research, instruction, formula validation, or sample data sheets.

## Validation

Backend upload validation remains responsible for required fields, format checks, product/category/warehouse existence, and business relations.
