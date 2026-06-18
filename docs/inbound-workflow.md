# Inbound Workflow

## Design Decision

T75 keeps the MVP inbound flow as one aggregate: `phieu_nhap_kho` plus `chi_tiet_phieu_nhap`.

- Draft purchase request and official purchase document are the same document. Approval changes the status; it does not create another table.
- Goods receipt/inspection is not a separate table in T75. Actual receipt data is stored on the expected detail line so the MVP can compare planned and received quantities without adding a second aggregate.
- `phieu_mua_hang` is not created for MVP. Add it later only if the product needs a separate purchase-order lifecycle, multiple receipts per order, invoice matching, or supplier debt tracking.
- `DA_DUYET` is not added because it would duplicate `CHO_HANG_VE` for this workflow.

This keeps the database simple while still matching the approved inbound steps for MVP: purchase planning, two-level approval, waiting for goods, inspection, completion, and stock update in later service tasks.

## Approved Inbound Step Mapping

The current T75 model maps the inbound process as follows:

| Process step | T75 model |
| --- | --- |
| Lập phiếu mua hàng | `phieu_nhap_kho` in `NHAP`. At this point it plays the role of a planned purchase request, not a real stock receipt. |
| Duyệt cấp 1 | `CHO_DUYET_CAP_1 -> CHO_DUYET_CAP_2`; store `nguoi_duyet_cap_1_id` and `ngay_duyet_cap_1` in later service logic. |
| Duyệt cấp 2 | `CHO_DUYET_CAP_2 -> CHO_HANG_VE`; store `nguoi_duyet_cap_2_id` and `ngay_duyet_cap_2` in later service logic. |
| Phiếu mua hàng chính thức | Same `phieu_nhap_kho` once it reaches `CHO_HANG_VE`. No separate `phieu_mua_hang` table is needed for MVP. |
| Hàng về | `CHO_HANG_VE -> CHO_KIEM_HANG`. This means goods have arrived and can be inspected. |
| Kiểm hàng | `CHO_KIEM_HANG`; expected line quantity stays in `so_luong`, actual received quantity is written to `so_luong_thuc_nhan`. |
| Có/không có chênh lệch | T75 can compare `so_luong` and `so_luong_thuc_nhan`, but discrepancy handling/records are out of scope. |
| Cộng tồn | Out of scope for T75. Later completion uses `so_luong_thuc_nhan` only when moving to `HOAN_THANH`. |
| Tạo giao dịch kho | Out of scope for T75. Later completion creates `giao_dich_kho` in the same transaction as stock update. |
| Đổi trạng thái | `ImportReceiptStatePolicy` defines allowed transitions; service/API enforcement starts in T76+. |

The document starts being treated as an actual inbound stock receipt at `CHO_KIEM_HANG`, because real receiving/inspection data can be recorded then. It only affects inventory after `HOAN_THANH`.

## Why There Is No Separate `phieu_mua_hang`

The approved MVP flow does not require an independent purchase-order aggregate yet. A single document can represent these business meanings by status:

- `NHAP`: planned purchase request.
- `CHO_DUYET_CAP_1` and `CHO_DUYET_CAP_2`: purchase request under approval.
- `CHO_HANG_VE`: official purchase document waiting for goods.
- `CHO_KIEM_HANG`: actual inbound receipt under inspection.
- `HOAN_THANH`: completed inbound receipt, ready for inventory mutation in later tasks.

A separate `phieu_mua_hang` table becomes necessary only if one purchase document must support multiple independent receiving events, invoice/debt matching, supplier confirmation lifecycle, or partial deliveries that need their own receipt numbers and statuses.

## Multiple Receiving Events

No clear requirement currently says one purchase document can be received in multiple batches. Therefore T75 keeps the one-aggregate MVP model.

If the approved workflow later requires partial/multiple receipts for one purchase document, this becomes a schema blocker: split the model into `phieu_mua_hang` for the purchase order and `phieu_nhap_kho` or a receipt table for each receiving event.

## Tables

### `phieu_nhap_kho`

Inbound document header. It stores document code, warehouse, supplier, status, totals, note, rejection reason, workflow actors, audit timestamps, and optimistic-lock `version`.

Important mappings:

- `ma_phieu_nhap` -> document code
- `kho_id` -> inbound warehouse
- `doi_tac_id` -> supplier partner
- `tong_tien` -> expected total amount, calculated by backend later
- `trang_thai` -> `ImportReceiptStatus`

### `chi_tiet_phieu_nhap`

Expected and actual line data for each product.

- `so_luong` is expected quantity.
- `so_luong_thuc_nhan` is actual received quantity after inspection.
- `don_gia` and `thanh_tien` are expected unit price and expected line total.

The unique index on `(phieu_nhap_id, san_pham_id)` prevents duplicate product lines in one document.

## Relationships

- `phieu_nhap_kho.kho_id` -> `kho.id`
- `phieu_nhap_kho.doi_tac_id` -> `doi_tac.id`
- Workflow employee columns -> `nhan_vien.id`
- `chi_tiet_phieu_nhap.phieu_nhap_id` -> `phieu_nhap_kho.id`
- `chi_tiet_phieu_nhap.san_pham_id` -> `san_pham.id`

Service validation in later API tasks must ensure:

- warehouse is active and valid for inbound
- supplier is active and has type `NHA_CUNG_CAP` or `CA_HAI`
- product is active

## State Machine

Statuses:

```text
NHAP
CHO_DUYET_CAP_1
CHO_DUYET_CAP_2
CHO_HANG_VE
CHO_KIEM_HANG
HOAN_THANH
TU_CHOI
HUY
```

Allowed transitions:

```text
NHAP -> CHO_DUYET_CAP_1
NHAP -> HUY

TU_CHOI -> NHAP
TU_CHOI -> HUY

CHO_DUYET_CAP_1 -> CHO_DUYET_CAP_2
CHO_DUYET_CAP_1 -> TU_CHOI

CHO_DUYET_CAP_2 -> CHO_HANG_VE
CHO_DUYET_CAP_2 -> TU_CHOI

CHO_HANG_VE -> CHO_KIEM_HANG
CHO_KIEM_HANG -> HOAN_THANH
```

Terminal statuses:

- `HOAN_THANH`
- `HUY`

Editable statuses:

- `NHAP`
- `TU_CHOI`

There is no dedicated discrepancy or no-receipt status in T75. The current state machine can represent the happy path and rejection/cancel path, but it does not model discrepancy resolution or a final no-receipt decision. Those rules should be added in a later Sprint task only if the backlog requires them.

## Roles By Step

- Employee creates and edits draft documents.
- Employee submits `NHAP -> CHO_DUYET_CAP_1`.
- Manager approves or rejects at `CHO_DUYET_CAP_1` and `CHO_DUYET_CAP_2` according to later role rules.
- Warehouse employee records inspection at `CHO_KIEM_HANG`.
- Backend service completes the document and updates stock only at `CHO_KIEM_HANG -> HOAN_THANH`.

Exact role enforcement belongs to API/service tasks after T75.

## Edit And Cancel Rules

- Header and expected detail data can be changed only in `NHAP` or `TU_CHOI`.
- `TU_CHOI` must keep a rejection reason.
- `HUY` and `HOAN_THANH` cannot be edited.
- T75 allows `TU_CHOI -> HUY` because the Sprint 2 rules allow it when backlog permits; API tasks can remove that transition if PB04 later forbids it.

## Inventory Rule

T75 does not update inventory.

Later completion logic must update inventory only when transitioning `CHO_KIEM_HANG -> HOAN_THANH`.

The stock increase must use `so_luong_thuc_nhan`, not `so_luong`.

Each stock change must create a `giao_dich_kho` row.

## Complete-Twice Protection

T76+ service implementation should combine:

- optimistic locking through `version`
- a transaction around status change, inventory update, and inventory transaction creation
- status guard: complete only when current DB status is `CHO_KIEM_HANG`
- optional row lock or conditional update for the completion command
- optional unique guard on inventory transaction if completion is implemented as an insert per product line

If a second completion request arrives after the first one commits, it must return `409` because the document is already `HOAN_THANH`.

## Out Of Scope For T75

- No API/controller.
- No UI.
- No inventory mutation.
- No stock capacity update.
- No `giao_dich_kho` creation.
- No generated document numbers.
- No discrepancy report or discrepancy approval workflow.
- No no-receipt handling workflow.
- No quality or expiry-date inspection data.
- No damaged/rejected quantity columns because PB04 did not require them yet.
- No separate receipt table until backlog requires multiple receiving events or inspection lifecycle.
- No commit/push unless all requested validation passes.

Potential later additions:

- discrepancy table/report for differences between expected and actual received quantities
- `KHONG_NHAN_HANG`, `CHO_XU_LY_CHENH_LECH`, or equivalent statuses if backlog needs explicit no-receipt/discrepancy resolution
- quality, expiry-date, damaged quantity, rejected quantity fields
- inventory update, stock transaction creation, and warehouse capacity update during completion
