# T75 - Import Receipt Schema Check

> Historical pre-implementation check. Current T75 design and implementation decisions are documented in `docs/inbound-workflow.md`.

## Scope

Branch checked: `feature/T75-import-receipt-schema-check`.

Scope reviewed only:

- Flyway migrations related to `phieu_nhap_kho`, `phieu_mua_hang`, `chi_tiet_phieu_nhap`, product, warehouse, partner, employee.
- Existing Java entities/enums for employee, warehouse, partner and status concepts.
- No API, inventory update, approval flow, draft flow or total calculation was implemented.
- No versioned Flyway migration was created or modified.

## Tables Found

### `phieu_nhap_kho`

Found in `src/main/resources/db/migration/V1__init_schema.sql`.

Existing columns:

| Column | Notes |
| --- | --- |
| `id` | Primary key. |
| `ma_phieu_nhap` | Unique, not null. Equivalent to receipt code, but name differs from expected `ma_phieu`. |
| `doi_tac_id` | Nullable FK to `doi_tac(id)`. Can represent supplier/partner. |
| `kho_id` | Not null FK to `kho(id)`. |
| `trang_thai` | PostgreSQL enum `trang_thai_chung_tu_kho`, default `NHAP`. Current enum is not sufficient for T75 flow. |
| `tong_tien` | `decimal(15,2)`, nullable, no non-negative constraint. |
| `ghi_chu` | `varchar(255)`. |
| `nguoi_tao_id` | Nullable FK to `nhan_vien(id)`. |
| `ngay_tao` | Timestamp. |
| `nguoi_gui_duyet_id` | Nullable FK to `nhan_vien(id)`. |
| `ngay_gui_duyet` | Timestamp. |
| `nguoi_duyet_id` | Nullable FK to `nhan_vien(id)`. |
| `ngay_duyet` | Timestamp. |
| `ly_do_tu_choi` | `varchar(500)`. |
| `ngay_hoan_thanh` | Timestamp. |

### `chi_tiet_phieu_nhap`

Found in `V1__init_schema.sql`.

Existing columns:

| Column | Notes |
| --- | --- |
| `id` | Primary key. |
| `phieu_nhap_id` | Not null FK to `phieu_nhap_kho(id)`. |
| `san_pham_id` | Not null FK to `san_pham(id)`. |
| `so_luong` | `int not null`, no `> 0` constraint. |
| `don_gia` | `decimal(15,2)`, nullable, no `>= 0` constraint. |
| `thanh_tien` | `decimal(15,2)`, nullable, no `>= 0` constraint. |
| `ghi_chu` | `varchar(255)`. |
| `ngay_tao` | Timestamp. |

Indexes:

- Unique index on (`phieu_nhap_id`, `san_pham_id`). This prevents duplicate product lines inside one receipt.

### `phieu_mua_hang`

Not found in current migrations.

Current schema does not have a separate purchase order/purchase receipt table. `phieu_nhap_kho` is the only import receipt-like header table.

### Related Tables

| Table | Found | Notes |
| --- | --- | --- |
| `san_pham` | Yes, V1 | Has supplier-like column `doi_tac_cung_cap_id`, FK to `doi_tac(id)`. No Java entity currently exists. |
| `kho` | Yes, V1 and V3 | V1 creates `dang_hoat_dong`; V3 replaces it with enum `trang_thai`. Java `Warehouse` maps the post-V3 shape. |
| `doi_tac` | Yes, V1 and V4 | V1 creates `dang_hoat_dong`; V4 adds `nguoi_lien_he`, enum `trang_thai`, drops boolean. Java `Partner` maps the post-V4 shape. |
| `nhan_vien` | Yes, V1 | Employee table exists and maps to Java `Employee`. |

## Entities Found

Found Java entities:

- `Employee` -> `nhan_vien`
- `Warehouse` -> `kho`
- `Partner` -> `doi_tac`
- `Category` -> `danh_muc`
- `Role` -> `vai_tro`

Not found:

- No Java entity for `san_pham`.
- No Java entity for `phieu_nhap_kho`.
- No Java entity for `chi_tiet_phieu_nhap`.
- No Java entity for `phieu_mua_hang` because the table does not exist.
- No repositories for import receipt, purchase order/receipt, receipt detail or product.

Added safe Java enum:

- `ImportReceiptStatus` with T75-required codes only.
- This enum is not mapped to any entity/table yet and does not require a database migration.

## Existing Status Values

Current DB enum for stock document status: `trang_thai_chung_tu_kho`.

Existing values:

- `NHAP`
- `CHO_DUYET`
- `DA_DUYET`
- `TU_CHOI`
- `HOAN_THANH`
- `HUY`

Required T75 values:

- `NHAP`
- `CHO_DUYET_CAP_1`
- `CHO_DUYET_CAP_2`
- `CHO_HANG_VE`
- `CHO_KIEM_HANG`
- `HOAN_THANH`
- `TU_CHOI`
- `HUY`

Gap:

- Existing DB enum is missing `CHO_DUYET_CAP_1`, `CHO_DUYET_CAP_2`, `CHO_HANG_VE`, `CHO_KIEM_HANG`.
- Existing DB enum contains generic `CHO_DUYET` and `DA_DUYET`, which do not match T75-required flow names.
- A later migration must align the PostgreSQL enum with `ImportReceiptStatus` before mapped JPA enum persistence can be safe.

## Foreign-Key Readiness

Existing FKs for import receipt header:

- `phieu_nhap_kho.doi_tac_id` -> `doi_tac.id`
- `phieu_nhap_kho.kho_id` -> `kho.id`
- `phieu_nhap_kho.nguoi_tao_id` -> `nhan_vien.id`
- `phieu_nhap_kho.nguoi_gui_duyet_id` -> `nhan_vien.id`
- `phieu_nhap_kho.nguoi_duyet_id` -> `nhan_vien.id`

Existing FKs for detail:

- `chi_tiet_phieu_nhap.phieu_nhap_id` -> `phieu_nhap_kho.id`
- `chi_tiet_phieu_nhap.san_pham_id` -> `san_pham.id`

Readiness notes:

- Warehouse, supplier/partner, creator employee and submitter employee are represented.
- Supplier is modeled as generic `doi_tac_id`; there is no dedicated `nha_cung_cap_id` column.
- `doi_tac_id` and `nguoi_tao_id` are nullable. For employee import receipt flow they likely should be required.
- No DB-level restriction ensures `doi_tac_id` points to a partner with `loai_doi_tac` = `NHA_CUNG_CAP` or `CA_HAI`.

## Detail Support Check

`chi_tiet_phieu_nhap` can represent:

- product: yes, `san_pham_id`.
- quantity: yes, `so_luong`.
- unit price: yes, `don_gia`.
- line total: yes, `thanh_tien`.
- note: yes, `ghi_chu`.

Detail gaps:

- No check constraint for `so_luong > 0`.
- No check constraint for `don_gia >= 0`.
- No check constraint for `thanh_tien >= 0`.
- `don_gia` and `thanh_tien` are nullable; T76-T84 should decide whether draft lines can omit them.

## Header Support Check

`phieu_nhap_kho` supports:

- creator: yes, `nguoi_tao_id`, but nullable.
- warehouse: yes, `kho_id`, not null.
- supplier: yes, via `doi_tac_id`, but nullable and generic.
- draft status: yes, `NHAP` default.
- submitted timestamp/user: yes, `ngay_gui_duyet`, `nguoi_gui_duyet_id`.
- rejection reason: yes, `ly_do_tu_choi`.
- total amount: yes, `tong_tien`, but nullable/no non-negative constraint.

Header gaps:

- No `ngay_cap_nhat` column on `phieu_nhap_kho`.
- Status enum does not support the two approval levels, waiting-for-arrival or checking status.
- No check constraint for `tong_tien >= 0`.
- `doi_tac_id`, `nguoi_tao_id`, `tong_tien` are nullable.
- No separate purchase table (`phieu_mua_hang`) if the business wants purchase order and import receipt as distinct concepts.

## Mapping Inconsistencies

- `phieu_nhap_kho` exists in DB but has no Java entity mapping.
- `chi_tiet_phieu_nhap` exists in DB but has no Java entity mapping.
- `san_pham` exists in DB but has no Java entity mapping, so receipt detail cannot reference a `Product` entity yet.
- `phieu_mua_hang` does not exist as a table and has no entity.
- `trang_thai_chung_tu_kho` does not match T75-required `ImportReceiptStatus` values.
- `Warehouse` and `Partner` Java entities match the post-V3/post-V4 schema, not the initial V1 boolean columns. This is OK after all migrations are applied.
- Existing header code column is `ma_phieu_nhap`, not the expected generic `ma_phieu`.

## Inventory Rule Check

Current V1 comments say `phieu_nhap_kho` increases inventory only when `HOAN_THANH`.

No API/service/entity currently implements import receipt creation or completion in Java, so T75 does not introduce inventory changes. This is aligned with the rule that drafts and expected receipts must not increase inventory.

## T76 Readiness

T76 should not begin full API implementation safely against current DB mapping unless it is limited to planning/DTO-only work.

Reason:

- There is no Java entity for `phieu_nhap_kho` or `chi_tiet_phieu_nhap`.
- There is no Java entity for `san_pham`.
- DB enum cannot persist T75-required status values.
- Constraints required by T75 are missing.
- It is unclear whether `phieu_mua_hang` is required as a separate table or whether `phieu_nhap_kho` is the canonical header for employee flow.

## Exact Blockers For T76-T84

1. Add or align PostgreSQL enum for receipt statuses with:
   `NHAP`, `CHO_DUYET_CAP_1`, `CHO_DUYET_CAP_2`, `CHO_HANG_VE`, `CHO_KIEM_HANG`, `HOAN_THANH`, `TU_CHOI`, `HUY`.
2. Decide purchase concept:
   use `phieu_nhap_kho` only, or add separate `phieu_mua_hang` and define relationship to import receipt.
3. Add Java entities for `san_pham`, `phieu_nhap_kho`, `chi_tiet_phieu_nhap` only after schema is confirmed.
4. Add DB constraints for quantity, unit price, line total and header total.
5. Decide nullability for `doi_tac_id`, `nguoi_tao_id`, `tong_tien`, `don_gia`, `thanh_tien` in draft vs submitted states.
6. Add `ngay_cap_nhat` to receipt header if audit/update tracking is required.
7. Decide whether supplier must be restricted to `NHA_CUNG_CAP`/`CA_HAI` and enforce it in service or DB.
8. Confirm naming convention: `ma_phieu_nhap` vs generic `ma_phieu`, `kho_id` vs `kho_nhap_id`.

## Recommended Schema Approach After Teammate Migrations Are Merged

After pending teammate migrations are merged, add one coordinated migration that does all receipt schema alignment together:

1. Create or alter a dedicated receipt status PostgreSQL enum to match `ImportReceiptStatus` exactly.
2. Prefer keeping `phieu_nhap_kho` as the import receipt header if the employee flow does not need a separate purchase-order lifecycle.
3. Add `phieu_mua_hang` only if purchase order is a distinct upstream document with its own lifecycle.
4. Add `ngay_cap_nhat` to `phieu_nhap_kho`.
5. Add non-negative checks for `tong_tien`, `don_gia`, `thanh_tien` and positive check for `so_luong`.
6. Tighten `nguoi_tao_id` and supplier nullability after draft requirements are confirmed.
7. Add JPA entities only after the final table/enum names are stable.
