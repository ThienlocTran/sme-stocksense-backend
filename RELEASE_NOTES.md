# Release v1.3.0

## Summary
- Warehouse workflow documentation source of truth added.
- Cloudinary avatar config handling fixed for local IntelliJ and Maven runs.
- Export receipt status cleanup and complete flow covered by targeted tests.
- Import receipt approval and discrepancy workflow covered by targeted tests.
- Inventory count workflow docs added, backend roles enforced, finalize applies stock adjustments.

## Validation
Backend:
- `git diff --check`: pass
- `DashboardServiceImplTest`: pass, 8 tests
- `EmployeeProfileServiceTest`: pass, 9 tests
- `ExportReceiptApprovalServiceTest`: pass, 23 tests
- `InventoryCountServiceImplTest,InventoryCountControllerTest`: pass, 11 tests
- `ImportReceiptDiscrepancyReportControllerTest,ImportReceiptDetailServiceTest,ImportReceiptCompleteServiceTest`: pass, 50 tests

Frontend:
- See frontend release PR for build validation.

## Known Risks
- Full backend suite not run.
- Manual app/API smoke not run in this release task.
- Cloudinary runtime still requires valid local or deployed environment variables.

## Release Tag
- RC tag not created yet.
- Final tag will be created only after merge into `main`.
