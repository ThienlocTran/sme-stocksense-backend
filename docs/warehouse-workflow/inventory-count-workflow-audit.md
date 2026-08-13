# Inventory Count Workflow Audit

## Source of truth
- `source_new/quy-trinh-kho.html` da bo sung nghiep vu kiem ke kho.

## Expected workflow
- Trang thai: `DANG_KIEM_KE -> DA_CHOT`; luong huy: `DANG_KIEM_KE -> DA_HUY`.
- Ton kho chi duoc cap nhat khi chot kiem ke. Huy kiem ke khong doi ton kho. Sau khi chot/huy thi khoa sua.
- Chenh lech = so luong thuc te - ton he thong.
- Khong xoa dot kiem ke de giu audit trail.
- ADMIN/MANAGER: xem, tao, chot, huy, xem ket qua. EMPLOYEE: xem dot dang xu ly, nhap/cap nhat so luong thuc te, ghi chu, khong chot/huy.

## Backend capability
- API hien co:
  - `POST /api/inventory-counts`: tao dot kiem ke.
  - `GET /api/inventory-counts`: xem danh sach, filter `warehouseId`, `status`.
  - `GET /api/inventory-counts/{id}`: xem chi tiet.
  - `PUT /api/inventory-counts/{id}/details/{detailId}`: nhap/cap nhat so luong thuc te va ghi chu.
  - `POST /api/inventory-counts/{id}/finalize`: chot kiem ke.
  - `POST /api/inventory-counts/{id}/cancel`: huy kiem ke.
- Role hien co: controller gan `@PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")` o class, khong tach role theo action. Thuc te ADMIN, MANAGER, EMPLOYEE deu goi duoc create/list/detail/update/finalize/cancel neu qua duoc security chung.
- Trang thai hien co: `NHAP`, `DANG_KIEM_KE`, `DA_CHOT`, `DA_HUY`. `NHAP` dang nam trong tap `OPEN` nhung create dat thang `DANG_KIEM_KE`.
- Inventory impact: finalize chi set status `DA_CHOT`, `finalizedBy`, `finalizedAt`. Chua cap nhat `InventoryLevel`.
- Log/giao dich: finalize chua goi `InventoryTransactionService`, chua ghi giao dich dieu chinh/log rieng.
- Cancel: chi set status `DA_HUY`, reason, cancelledBy, cancelledAt; khong doi ton kho.
- Gaps:
  - Backend role qua rong: EMPLOYEE co the tao/chot/huy.
  - Finalize chua can bang ton kho theo so thuc te.
  - Finalize chua ghi giao dich dieu chinh/log.
  - Co trang thai `NHAP` ngoai workflow tai lieu.

## Frontend capability
- Man hinh hien co:
  - `InventoryCountListView.vue`: danh sach, filter, nut tao dot kiem ke, modal tao.
  - `InventoryCountDetailView.vue`: chi tiet, nhap thuc te, ghi chu, luu dong, chot, huy.
- Action hien co: frontend da co API create/update actual/finalize/cancel trong `inventoryCountService.js`.
- Permission hien co:
  - `canManageInventoryCounts(role)` tra ve true cho `ADMIN`, `MANAGER`.
  - Route `/inventory-counts` va `/inventory-counts/:id` cho `ADMIN`, `MANAGER`, `EMPLOYEE`.
- Ly do MANAGER chi thay "Xem chi tiet": list/detail tinh `canManage = computed(() => canManageInventoryCounts())`, khong truyen role. Ham fallback sang `getCurrentRoleCode()` tu localStorage. Neu localStorage chua co `stocksense_current_user` dung format, role normalize ra rong nen nut tao/chot/huy bi an. Nut "Xem chi tiet" khong phu thuoc `canManage`.
- Role normalization: `normalizeRole()` ho tro `MANAGER`, `ROLE_MANAGER`, object co `roleCode`, `role`, `roleName`, `authority`, `authorities`. Chua thay ho tro object role long nhau kieu `{ role: { code/name } }` neu backend tra shape do.
- Current UI la bug hay missing requirement: co ca hai. UI actions da implement, nen truong hop MANAGER that su dang nhap ma nut bi an la bug permission/runtime role. Tuy nhien backend finalize chua doi ton kho/log, nen workflow hoan chinh van la missing requirement backend.

## Decision
- Current UI chi xem la chua du neu user la MANAGER hop le.
- Can sua frontend de truyen/lay role on dinh cho `canManageInventoryCounts`, kiem tra shape role thuc te tu login response/localStorage.
- Can sua backend de gioi han role theo endpoint va them logic finalize cap nhat ton kho + log/giao dich dieu chinh.

## Recommended next task
Sua Inventory Count code: frontend truyen role hien tai vao `canManageInventoryCounts` va xu ly role object `{code,name}` neu co; backend tach permission create/finalize/cancel cho ADMIN/MANAGER, update actual cho ADMIN/EMPLOYEE theo rule, va finalize cap nhat `InventoryLevel` kem giao dich dieu chinh/log audit.
