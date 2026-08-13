# T142: Phân Tích Và Thiết Kế Rule Duyệt Phiếu Xuất 2 Cấp Cho ExportApproval

**Ngày tạo**: 2026-07-04  
**Task Type**: Backend - Business Analysis / Rule Design  
**Mục tiêu**: Phân tích và thiết kế rule duyệt phiếu xuất 2 cấp (ExportApproval) để chuẩn bị cho các task implement sau.

---

## 1. Tổng Quan Hệ Thống Phân Quyền Hiện Tại

### 1.1 Role Hiện Có

Hệ thống hiện có **3 role chính**:

| Role | Mã | Mô Tả |
|------|-----|-------|
| ADMIN | ADMIN | Quản trị viên, toàn quyền |
| MANAGER | MANAGER | Quản lý kho, có quyền duyệt và quản lý chung |
| EMPLOYEE | EMPLOYEE | Nhân viên, quyền hạn chế |

**Nguồn**: [RoleCode.java](../src/main/java/com/smartflow/smestocksensebackend/entity/RoleCode.java)

### 1.2 Quy Tắc Phân Quyền Chung (Từ ImportReceipt)

- **ADMIN**: Toàn quyền thao tác trên mọi phiếu
- **MANAGER**: Có quyền duyệt/từ chối phiếu, xem danh sách phiếu chờ duyệt, xem chi tiết phiếu bất kỳ
- **EMPLOYEE**: Chỉ được thao tác trên phiếu do chính mình tạo, xem danh sách phiếu cá nhân

**Nguồn**: [ImportReceiptServiceImpl.java - ensureCanApprove()](../src/main/java/com/smartflow/smestocksensebackend/service/impl/ImportReceiptServiceImpl.java#L788)

---

## 2. Phân Tích ImportReceipt Approval Flow (Reference)

### 2.1 Luồng Trạng Thái ImportReceipt Hiện Tại

```
NHAP (Nháp)
    ↓
CHO_DUYET_CAP_1 (Chờ duyệt cấp 1)
    ├─ Approve → CHO_HANG_VE (Chờ hàng về)
    │               ↓
    │           CHO_KIEM_HANG (Chờ kiểm hàng)
    │               ↓
    │           HOAN_THANH (Hoàn thành)
    │
    └─ Reject → TU_CHOI (Từ chối)
                    ↓
                  NHAP hoặc HUY

HUY (Hủy) - có thể từ NHAP hoặc TU_CHOI
```

**Lưu ý**: ImportReceipt hiện tại có trạng thái `CHO_DUYET_CAP_2` nhưng logic duyệt chỉ thực hiện 1 lần (không phân biệt 2 cấp). Duyệt phiếu từ `CHO_DUYET_CAP_1` hoặc `CHO_DUYET_CAP_2` đều chuyển sang `CHO_HANG_VE`.

**Nguồn**: 
- [ImportReceiptStatus.java](../src/main/java/com/smartflow/smestocksensebackend/entity/ImportReceiptStatus.java)
- [ImportReceiptStatePolicy.java](../src/main/java/com/smartflow/smestocksensebackend/domain/inbound/ImportReceiptStatePolicy.java)

### 2.2 Phân Quyền Duyệt ImportReceipt

| Chức Năng | ADMIN | MANAGER | EMPLOYEE |
|-----------|-------|---------|----------|
| Xem danh sách chờ duyệt | ✓ | ✓ | ✗ |
| Xem chi tiết phiếu để duyệt | ✓ | ✓ | ✗ |
| Duyệt phiếu | ✓ | ✓ | ✗ |
| Từ chối phiếu | ✓ | ✓ | ✗ |

**Nguồn**: [ImportReceiptServiceImpl.java - ensureCanApprove() & ensureCanListAllReceipts()](../src/main/java/com/smartflow/smestocksensebackend/service/impl/ImportReceiptServiceImpl.java#L765)

### 2.3 Trạng Thái Cho Phép Duyệt/Từ Chối

```java
PENDING_APPROVAL_STATUSES = {CHO_DUYET_CAP_1, CHO_DUYET_CAP_2}
```

Chỉ có thể duyệt hoặc từ chối khi phiếu ở trạng thái `CHO_DUYET_CAP_1` hoặc `CHO_DUYET_CAP_2`.

**Nguồn**: [ImportReceiptServiceImpl.java - line 102](../src/main/java/com/smartflow/smestocksensebackend/service/impl/ImportReceiptServiceImpl.java#L102)

---

## 3. Phân Tích Luồng ExportReceipt (Hiện Trạng)

### 3.1 Tình Trạng Implement Hiện Tại

- **Entity**: Chưa có Entity `ExportReceipt` riêng
- **Service**: Chưa có Service `ExportReceiptService` riêng
- **Repository**: Chưa có Repository `ExportReceiptRepository` riêng
- **Controller**: Chưa có Controller `ExportReceiptController` riêng
- **DTO**: Chưa có DTO cho ExportReceipt

**Tham chiếu chỉ có trong**:
- [InventoryTransaction.java](../src/main/java/com/smartflow/smestocksensebackend/entity/InventoryTransaction.java#L66) - có field `exportReceiptId`
- [InventoryTransactionResponse.java](../src/main/java/com/smartflow/smestocksensebackend/dto/inventory/InventoryTransactionResponse.java#L36) - có field `exportReceiptId`

---

## 4. Thiết Kế Rule Duyệt Phiếu Xuất 2 Cấp (ExportApproval)

### 4.1 Xác Định: Role Được Phép Xem Danh Sách Chờ Duyệt

**Yêu cầu**: Tìm role nào được xem danh sách phiếu xuất chờ duyệt.

**Quyết định**:
- ✓ **ADMIN**: Toàn quyền xem mọi phiếu chờ duyệt
- ✓ **MANAGER**: Có quyền xem danh sách phiếu xuất chờ duyệt (cấp 1 và cấp 2)
- ✗ **EMPLOYEE**: Không có quyền xem danh sách chờ duyệt chung

**Dựa trên**: ImportReceipt pattern - chỉ ADMIN/MANAGER được duyệt.

### 4.2 Xác Định: Role Được Duyệt Cấp 1

**Yêu cầu**: Tìm role nào được duyệt cấp 1.

**Quyết định**:
- ✓ **ADMIN**: Có quyền duyệt cấp 1
- ✓ **MANAGER**: Có quyền duyệt cấp 1
- ✗ **EMPLOYEE**: Không có quyền duyệt

**Mô tả cấp 1**: 
- Kiểm tra tính hợp lệ của phiếu xuất (kho, sản phẩm, số lượng, etc.)
- Xác nhận tồn kho đủ để xuất
- Chuyển phiếu sang trạng thái chờ duyệt cấp 2

**Quy tắc add-on**:
- Người duyệt cấp 1 khác với người gửi duyệt (nếu người gửi là MANAGER/ADMIN)
- Ghi lại `level1ApprovedBy` và `level1ApprovedAt`

### 4.3 Xác Định: Role Được Duyệt Cấp 2

**Yêu cầu**: Tìm role nào được duyệt cấp 2.

**Quyết định**:
- ✓ **ADMIN**: Có quyền duyệt cấp 2
- ✓ **MANAGER**: Có quyền duyệt cấp 2 (có thể là MANAGER khác hoặc cùng MANAGER nếu vượt qua cấp 1)
- ✗ **EMPLOYEE**: Không có quyền duyệt

**Mô tả cấp 2**:
- Kiểm tra lại tính hợp lệ của phiếu (review lại cấp 1)
- Xác nhận có thể xuất hàng
- Chuyển phiếu sang trạng thái sẵn sàng xuất/hoàn tất

**Quy tắc add-on**:
- Ghi lại `level2ApprovedBy` và `level2ApprovedAt`

### 4.4 Xác Định: Role Được Từ Chối

**Yêu cầu**: Tìm role nào được từ chối.

**Quyết định**:
- ✓ **ADMIN**: Có quyền từ chối ở cả cấp 1 và cấp 2
- ✓ **MANAGER**: Có quyền từ chối ở cả cấp 1 và cấp 2
- ✗ **EMPLOYEE**: Không có quyền từ chối

**Quy tắc**:
- Bắt buộc phải có "lý do từ chối" (`rejectionReason`)
- Phiếu chuyển sang trạng thái `TU_CHOI` (Từ chối)
- Lưu lại lý do trong `rejectionReason` field
- Ghi lại người từ chối và thời gian

---

## 5. Điều Kiện Chuyển Trạng Thái Phiếu Xuất

### 5.1 Từ Draft → Submitted for Approval

**Trạng thái**: NHAP → CHO_DUYET_CAP_1

**Điều kiện cho phép**:
- ✓ Phiếu đang ở trạng thái NHAP
- ✓ Phiếu có ít nhất 1 dòng sản phẩm hợp lệ
- ✓ Kho xuất hàng tồn tại và đang hoạt động
- ✓ Khách hàng (nếu có) tồn tại và đang hoạt động
- ✓ Số lượng từng sản phẩm ≤ tồn kho hiện tại
- ✓ Người submit có quyền (ADMIN/MANAGER/EMPLOYEE - người tạo)

**Người thực hiện**: EMPLOYEE (người tạo) hoặc ADMIN/MANAGER

**Kết quả**:
- `status` = CHO_DUYET_CAP_1
- `submittedBy` = Employee hiện tại
- `submittedAt` = LocalDateTime.now()

### 5.2 Từ CHO_DUYET_CAP_1 → Pending Level 2

**Trạng thái**: CHO_DUYET_CAP_1 → CHO_DUYET_CAP_2

**Điều kiện cho phép**:
- ✓ Phiếu đang ở trạng thái CHO_DUYET_CAP_1
- ✓ Người duyệt là ADMIN hoặc MANAGER
- ✓ Người duyệt không phải người gửi (nếu có thể)
- ✓ Tất cả check cấp 1 pass (kho, khách hàng, số lượng tồn kho)

**Người thực hiện**: ADMIN hoặc MANAGER (khác người gửi nếu có thể)

**Kết quả**:
- `status` = CHO_DUYET_CAP_2
- `level1ApprovedBy` = MANAGER/ADMIN duyệt cấp 1
- `level1ApprovedAt` = LocalDateTime.now()

### 5.3 Từ CHO_DUYET_CAP_2 → Approved / Ready to Export

**Trạng thái**: CHO_DUYET → DA_DUYET hoặc HOAN_THANH

**Điều kiện cho phép**:
- ✓ Phiếu đang ở trạng thái CHO_DUYET_CAP_2
- ✓ Người duyệt là ADMIN hoặc MANAGER
- ✓ Tất cả check cấp 2 pass

**Người thực hiện**: ADMIN hoặc MANAGER

**Kết quả**:
- `status` = DA_DUYET (hoặc HOAN_THANH tùy quy trình)
- `level2ApprovedBy` = MANAGER/ADMIN duyệt cấp 2
- `level2ApprovedAt` = LocalDateTime.now()
- Giảm tồn kho (nếu quy trình cho phép)

### 5.4 Từ Trạng Thái Pending → Rejected

**Trạng thái**: CHO_DUYET_CAP_1 hoặc CHO_DUYET_CAP_2 → TU_CHOI

**Điều kiện cho phép**:
- ✓ Phiếu đang ở trạng thái CHO_DUYET_CAP_1 hoặc CHO_DUYET_CAP_2
- ✓ Người từ chối là ADMIN hoặc MANAGER
- ✓ Bắt buộc có lý do từ chối (không được để trống)

**Người thực hiện**: ADMIN hoặc MANAGER

**Kết quả**:
- `status` = TU_CHOI
- `rejectionReason` = Lý do từ chối
- Ghi lại người từ chối và thời gian
- Không thay đổi tồn kho

### 5.5 Từ TU_CHOI → NHAP (Hoặc HUY)

**Trạng thái**: TU_CHOI → NHAP hoặc TU_CHOI → HUY

**Điều kiện cho phép**:
- ✓ Phiếu ở trạng thái TU_CHOI
- ✓ Người chỉnh sửa lại là người tạo hoặc ADMIN
- ✓ Muốn gửi duyệt lại: quay về NHAP → CHO_DUYET_CAP_1
- ✓ Muốn hủy: HUY (terminal)

**Kết quả**:
- Nếu chỉnh sửa lại: `status` = NHAP → có thể submit lại
- Nếu hủy: `status` = HUY (terminal, không thể thay đổi)

### 5.6 Từ NHAP → HUY

**Trạng thái**: NHAP → HUY

**Điều kiện cho phép**:
- ✓ Phiếu ở trạng thái NHAP (chưa gửi duyệt)
- ✓ Người hủy là người tạo hoặc ADMIN
- ✓ Phiếu chưa được phê duyệt

**Kết quả**:
- `status` = HUY (terminal)
- Ghi lại người hủy và thời gian

---

## 6. Điều Kiện Không Được Duyệt

### 6.1 Không Thể Duyệt Phiếu Nếu:

| Điều Kiện | Mô Tả | HTTP Status | Error Message |
|-----------|-------|-------------|---------------|
| Phiếu không tồn tại | ID phiếu không tìm thấy | 404 Not Found | "Phieu xuat khong ton tai" |
| Trạng thái không hợp lệ | Phiếu không ở CHO_DUYET_CAP_1 hoặc CHO_DUYET_CAP_2 | 409 Conflict | "Chi duoc duyet phieu xuat o trang thai cho duyet" |
| Người dùng không có quyền | Người dùng không phải ADMIN/MANAGER | 403 Forbidden | "Khong co quyen duyet phieu xuat" |
| Người dùng không hoạt động | Tài khoản bị khoá/deactivate | 403 Forbidden | "Tai khoan khong hoat dong" |
| Kho xuất không hoạt động | Kho xuất hàng đã deactivate | 400 Bad Request | "Kho xuat hang khong hoat dong" |
| Khách hàng không hoạt động | Khách hàng đã deactivate | 400 Bad Request | "Khach hang khong hoat dong" |
| Tồn kho không đủ (cấp 1) | Số lượng sản phẩm > tồn kho hiện tại | 400 Bad Request | "Ton kho khong du de xuat san pham X" |
| State transition không hợp lệ | State machine không cho phép transition | 409 Conflict | "Khong the chuyen trang thai tu {current} sang {next}" |

### 6.2 Các Tình Huống Khác

- **Phiếu đã bị hủy** (HUY): Không thể duyệt, từ chối, hay chỉnh sửa lại
- **Phiếu đã hoàn tất** (HOAN_THANH): Không thể chỉnh sửa hay từ chối, chỉ xem được
- **Phiếu ở trạng thái NHAP**: Không thể duyệt trực tiếp (phải submit trước)

---

## 7. Điều Kiện Không Được Từ Chối

### 7.1 Không Thể Từ Chối Nếu:

| Điều Kiện | Mô Tả | HTTP Status | Error Message |
|-----------|-------|-------------|---------------|
| Phiếu không tồn tại | ID phiếu không tìm thấy | 404 Not Found | "Phieu xuat khong ton tai" |
| Trạng thái không hợp lệ | Phiếu không ở CHO_DUYET_CAP_1 hoặc CHO_DUYET_CAP_2 | 409 Conflict | "Chi duoc tu choi phieu xuat o trang thai cho duyet" |
| Người dùng không có quyền | Người dùng không phải ADMIN/MANAGER | 403 Forbidden | "Khong co quyen tu choi phieu xuat" |
| Lý do từ chối trống | Field rejectionReason null hoặc empty | 400 Bad Request | "Ly do tu choi khong duoc de trong" |
| Người dùng không hoạt động | Tài khoản bị khoá/deactivate | 403 Forbidden | "Tai khoan khong hoat dong" |

### 7.2 Lưu Ý:

- Từ chối ở **cấp 1**: Phiếu quay về NHAP hoặc TU_CHOI, người tạo có thể chỉnh sửa lại
- Từ chối ở **cấp 2**: Phiếu quay về TU_CHOI, có thể chỉnh sửa lại từ NHAP
- Bắt buộc có lý do từ chối - không được để trống

---

## 8. State Machine Đầy Đủ Cho ExportReceipt

### 8.1 Sơ Đồ Trạng Thái

```
┌─────────────────────────────────────────────────────────────────┐
│                    Export Receipt State Machine                 │
└─────────────────────────────────────────────────────────────────┘

                              ┌─────────────────┐
                              │   NHAP (Nháp)   │◄──┐
                              └────────┬────────┘   │
                                       │            │ [Chỉnh sửa từ TU_CHOI]
                    ┌──────────────────┼──────────────────┐
                    │                  │                  │
         [Submit]   │                  │ [Cancel]         │
                    │                  │                  │
                    ▼                  ▼                  ▼
        ┌──────────────────┐ ┌───────────────┐  ┌──────────────┐
        │CHO_DUYET_CAP_1   │ │    HUY        │  │   TU_CHOI    │
        │ (Chờ duyệt cấp 1)│ │  (Hủy-Term)   │  │ (Từ chối)    │
        └────────┬─────────┘ └───────────────┘  └──────┬───────┘
                 │                                     │
     ┌───────────┼───────────┐         ┌───────────────┘
     │ [Approve] │ [Reject]  │         │ [Edit & Resubmit]
     │           │           │         │
     ▼           ▼           ▼         │
┌──────────────┐ ┌───────────────────┐│
│CHO_DUYET_CAP_2│ │  TU_CHOI          ││
│(Chờ duyệt    │ │  (Từ chối)        ││
│ cấp 2)       │ │                   ││
└────────┬─────┘ └───────────────────┘│
         │                            │
    ┌────┴─────────┐ ┌────────────────┘
    │ [Approve]    │ │
    │ [Reject]     │ │
    │              │ │
    ▼              ▼ │
┌──────────────┐ ┌──────────────────┐
│ DA_DUYET     │ │ TU_CHOI (Từ chối)│
│(Sẵn sàng xuất) │ └──────────────────┘
│ hoặc         │       │
│ HOAN_THANH   │       │ [Edit & Resubmit]
│(Hoàn tất)    │       │
└──────┬───────┘ ◄─────┘
       │
       │ [Export completed - Reduce inventory]
       ▼
   ┌─────────────┐
   │ HOAN_THANH  │ (Terminal - Read Only)
   │(Hoàn tất)   │
   └─────────────┘
```

### 8.2 Bảng Transition Được Phép

| Từ Trạng Thái | Sang Trạng Thái | Điều Kiện | Người Thực Hiện | Hành Động |
|---------------|-----------------|-----------|-----------------|-----------|
| NHAP | CHO_DUYET_CAP_1 | Có sản phẩm, kho/khách hoạt động | EMPLOYEE/MANAGER/ADMIN | Submit |
| NHAP | HUY | Phiếu nháp | Người tạo / ADMIN | Cancel |
| CHO_DUYET_CAP_1 | CHO_DUYET_CAP_2 | Approved by Mgr/Admin | MANAGER / ADMIN | Approve Level 1 |
| CHO_DUYET_CAP_1 | TU_CHOI | Có lý do | MANAGER / ADMIN | Reject Level 1 |
| CHO_DUYET | DA_DUYET/HOAN_THANH | Approved by Mgr/Admin | MANAGER / ADMIN | Approve |
| CHO_DUYET_CAP_2 | TU_CHOI | Có lý do | MANAGER / ADMIN | Reject Level 2 |
| TU_CHOI | NHAP | Người tạo muốn chỉnh sửa | EMPLOYEE/ADMIN | Edit & Go Back |
| TU_CHOI | CHO_DUYET_CAP_1 | Sau khi chỉnh sửa | EMPLOYEE/MANAGER/ADMIN | Resubmit |
| TU_CHOI | HUY | Từ bỏ phiếu | EMPLOYEE/ADMIN | Cancel |
| DA_DUYET/HOAN_THANH | (none) | Terminal state | - | Read Only |
| HUY | (none) | Terminal state | - | Read Only |

### 8.3 Các Trạng Thái

```java
public enum ExportReceiptStatus {
    NHAP,              // Nháp - chưa gửi duyệt
    CHO_DUYET_CAP_1,   // Chờ duyệt cấp 1 (MANAGER/ADMIN)
    CHO_DUYET_CAP_2,   // Chờ duyệt cấp 2 (MANAGER/ADMIN)
    DA_DUYET,          // Sẵn sàng xuất (sau duyệt)
    HOAN_THANH,        // Hoàn tất (đã xuất, giảm tồn kho)
    TU_CHOI,           // Từ chối (có thể sửa lại hoặc hủy)
    HUY                // Hủy (terminal - không thay đổi được)
}
```

---

## 9. ExportReceiptAction (Action Log)

Danh sách các action được ghi lại trong lịch sử phiếu xuất:

```java
public enum ExportReceiptAction {
    GUI_DUYET,       // Gửi duyệt
    DUYET_CAP_1,     // Duyệt cấp 1
    DUYET_CAP_2,     // Duyệt cấp 2
    TU_CHOI,         // Từ chối
    HUY              // Hủy
}
```

---

## 10. Danh Sách Field Cần Lưu Trong ExportReceipt Entity

### 10.1 Các Field Cơ Bản (Similar to ImportReceipt)

| Field | Type | Mô Tả | Nullable |
|-------|------|-------|----------|
| id | Long | PK | No |
| code | String | Mã phiếu xuất (unique) | No |
| warehouse | Warehouse FK | Kho xuất | No |
| customer | Partner FK | Khách hàng (loại KH) | Yes |
| status | ExportReceiptStatus | Trạng thái phiếu | No (default NHAP) |
| totalAmount | BigDecimal | Tổng tiền | Yes |
| note | String | Ghi chú | Yes |
| rejectionReason | String | Lý do từ chối | Yes |

### 10.2 Các Field Duyệt (Approval Tracking)

| Field | Type | Mô Tả |
|-------|------|-------|
| createdBy | Employee FK | Người tạo phiếu |
| createdAt | LocalDateTime | Ngày tạo |
| submittedBy | Employee FK | Người gửi duyệt |
| submittedAt | LocalDateTime | Ngày gửi duyệt |
| level1ApprovedBy | Employee FK | Người duyệt cấp 1 |
| level1ApprovedAt | LocalDateTime | Ngày duyệt cấp 1 |
| level2ApprovedBy | Employee FK | Người duyệt cấp 2 |
| level2ApprovedAt | LocalDateTime | Ngày duyệt cấp 2 |
| cancelledBy | Employee FK | Người hủy |
| cancelledAt | LocalDateTime | Ngày hủy |
| completedBy | Employee FK | Người hoàn tất |
| completedAt | LocalDateTime | Ngày hoàn tất |
| version | Long | Optimistic lock version |

### 10.3 Audit Timestamp

| Field | Type | Mô Tả |
|-------|------|-------|
| updatedAt | LocalDateTime | Cập nhật lần cuối |

---

## 11. Danh Sách API Cần Implement (Sau)

### 11.1 CRUD & Basic Operations

- `POST /api/export-receipts` - Tạo phiếu xuất nháp
- `GET /api/export-receipts/{id}` - Xem chi tiết phiếu xuất
- `GET /api/export-receipts` - Danh sách phiếu xuất (filters)
- `GET /api/export-receipts/my` - Danh sách phiếu do người dùng tạo
- `PUT /api/export-receipts/{id}/draft` - Lưu nháp
- `PUT /api/export-receipts/{id}` - Cập nhật (editable status)
- `PUT /api/export-receipts/{id}/cancel` - Hủy phiếu (NHAP → HUY)

### 11.2 Approval Flow Operations

- `GET /api/export-receipts/pending-approval` - Danh sách chờ duyệt (T91 equivalent)
- `GET /api/export-receipts/{id}/approval-detail` - Chi tiết để duyệt (T92 equivalent)
- `PUT /api/export-receipts/{id}/submit` - Gửi duyệt (NHAP → CHO_DUYET_CAP_1)
- `PUT /api/export-receipts/{id}/approve` - Duyệt (CHO_DUYET → DA_DUYET)
- `PUT /api/export-receipts/{id}/reject` - Từ chối (→ TU_CHOI)

### 11.3 Detail & History Operations

- `POST /api/export-receipts/{id}/items` - Thêm sản phẩm vào phiếu
- `GET /api/export-receipts/{id}/history` - Lịch sử thao tác phiếu

---

## 12. Danh Sách File Sẽ Phải Sửa Ở Các Task Sau

### 12.1 Entity & Enum

| File Path | Mục Đích | Task |
|-----------|---------|------|
| `entity/ExportReceipt.java` | Entity chính cho phiếu xuất | T143 |
| `entity/ExportReceiptStatus.java` | Enum trạng thái phiếu xuất | T143 |
| `entity/ExportReceiptAction.java` | Enum action cho lịch sử | T143 |
| `entity/ExportReceiptDetail.java` | Chi tiết dòng sản phẩm trong phiếu | T143 |
| `entity/ExportReceiptHistory.java` | Lịch sử thao tác phiếu xuất | T143 |

### 12.2 DTO (Request/Response)

| File Path | Mục Đích | Task |
|-----------|---------|------|
| `dto/outbound/CreateExportReceiptRequest.java` | Request tạo phiếu | T144 |
| `dto/outbound/ExportReceiptResponse.java` | Response phiếu chi tiết | T144 |
| `dto/outbound/ExportReceiptPageResponse.java` | Response phân trang | T144 |
| `dto/outbound/ExportReceiptSummaryResponse.java` | Response tóm tắt | T144 |
| `dto/outbound/ExportReceiptDraftResponse.java` | Response draft | T144 |
| `dto/outbound/SaveExportReceiptDraftRequest.java` | Request lưu draft | T144 |
| `dto/outbound/AddExportReceiptItemRequest.java` | Request thêm sản phẩm | T144 |
| `dto/outbound/RejectExportReceiptRequest.java` | Request từ chối (có reason) | T144 |
| `dto/outbound/ExportReceiptHistoryResponse.java` | Response lịch sử | T144 |

### 12.3 Repository

| File Path | Mục Đích | Task |
|-----------|---------|------|
| `repository/ExportReceiptRepository.java` | Repository chính | T145 |
| `repository/ExportReceiptDetailRepository.java` | Repository chi tiết | T145 |
| `repository/ExportReceiptHistoryRepository.java` | Repository lịch sử | T145 |

**Custom Queries cần**:
- `findByCreatedById(employeeId, pageable)` - Phiếu do nhân viên tạo
- `findByStatus(status, pageable)` - Phiếu theo trạng thái
- `findByStatusIn(statuses, pageable)` - Danh sách chờ duyệt
- `findByWarehouse(warehouse)` - Phiếu theo kho
- Entity Graph cho warehouse, supplier/customer, createdBy, etc.

### 12.4 Domain Policy

| File Path | Mục Đích | Task |
|-----------|---------|------|
| `domain/outbound/ExportReceiptStatePolicy.java` | State machine logic | T146 |
| `domain/outbound/ExportReceiptAmountCalculator.java` | Tính tổng tiền (tương tự ImportReceipt) | T146 |
| `domain/outbound/ExportReceiptItemValidator.java` | Validate sản phẩm, số lượng | T146 |

### 12.5 Service & Implementation

| File Path | Mục Đích | Task |
|-----------|---------|------|
| `service/ExportReceiptService.java` | Interface Service | T147 |
| `service/impl/ExportReceiptServiceImpl.java` | Implementation (Main business logic) | T147 |
| `service/ExportReceiptCodeGenerator.java` | Generate mã phiếu xuất (tương tự ImportReceipt) | T147 |

**Logic cần implement**:
- Tạo phiếu nháp
- Lưu draft
- Submit cho duyệt
- Duyệt cấp 1 & 2
- Từ chối (cấp 1 & 2)
- Hủy phiếu
- Xem danh sách (own / all / pending approval)
- Lịch sử phiếu

### 12.6 Controller

| File Path | Mục Đích | Task |
|-----------|---------|------|
| `controller/ExportReceiptController.java` | REST API endpoints | T148 |

**Endpoints**:
- POST /api/export-receipts
- GET /api/export-receipts/{id}
- GET /api/export-receipts
- GET /api/export-receipts/my
- PUT /api/export-receipts/{id}/draft
- PUT /api/export-receipts/{id}
- PUT /api/export-receipts/{id}/cancel
- GET /api/export-receipts/pending-approval
- GET /api/export-receipts/{id}/approval-detail
- PUT /api/export-receipts/{id}/submit
- PUT /api/export-receipts/{id}/approve
- PUT /api/export-receipts/{id}/reject
- POST /api/export-receipts/{id}/items
- GET /api/export-receipts/{id}/history

### 12.7 Exception Handling

| File Path | Mục Đích | Task |
|-----------|---------|------|
| `exception/ApiExceptionHandler.java` | Update exception handlers cho ExportReceipt | T149 |

**Exceptions sử dụng**:
- NotFoundException (phiếu không tồn tại)
- BadRequestException (dữ liệu không hợp lệ, trạng thái không được phép)
- ConflictException (state transition không hợp lệ)
- MissingRoleException (không có quyền)
- AccountInactiveException (tài khoản không hoạt động)

### 12.8 Frontend (Vue)

| File Path | Mục Đích | Task |
|-----------|---------|------|
| `services/exportReceiptService.js` | API calls cho export receipts | T150 |
| `views/StockOutView.vue` hoặc `StockDocumentDetailView.vue` | Danh sách/chi tiết phiếu xuất | T150-151 |
| `views/ExportApprovalsView.vue` | Danh sách chờ duyệt + form duyệt/từ chối | T151 |
| `router/index.js` | Thêm route cho export receipts | T150 |
| `stores/inventory.js` | Update Pinia store nếu cần | T150 |

---

## 13. Các Điểm Quan Trọng Cần Lưu Ý

### 13.1 Quy Tắc Phân Quyền Cấp 1 & 2

- **Cấp 1** (CHO_DUYET_CAP_1 → CHO_DUYET_CAP_2):
  - Kiểm tra tính hợp lệ cơ bản (kho, khách, sản phẩm, số lượng)
  - Xác nhận tồn kho đủ để xuất
  - Thường được MANAGER/ADMIN cấp quản lý trực tiếp phục vụ

- **Duyệt** (CHO_DUYET → DA_DUYET/HOAN_THANH):
  - Review lại tính hợp lệ từ cấp 1
  - Xác nhận có thể xuất hàng
  - Có thể là ADMIN hoặc MANAGER cấp cao hơn/khác

### 13.2 Tồn Kho Validation

- **Khi Submit** (NHAP → CHO_DUYET_CAP_1):
  - Check tồn kho ≥ yêu cầu
  - Nếu tồn kho thay đổi sau khi submit, cấp 1 duyệt phải check lại

- **Khi Duyệt Cấp 1** (CHO_DUYET_CAP_1 → CHO_DUYET_CAP_2):
  - Re-check tồn kho hiện tại
  - Nếu tồn kho < yêu cầu, có thể reject hoặc cho phép submit lại

- **Khi Duyệt** (CHO_DUYET → DA_DUYET):
  - Final check tồn kho
  - Lock pessimistic nếu cần để tránh race condition

- **Khi Hoàn Tất** (DA_DUYET → HOAN_THANH):
  - Giảm tồn kho cuối cùng

### 13.3 Từ Chối - Quy Tắc Quay Lại

- **Từ chối ở Cấp 1**: Phiếu → TU_CHOI (người tạo có thể sửa)
- **Từ chối ở Cấp 2**: Phiếu → TU_CHOI (người tạo có thể sửa)
- **Sửa từ TU_CHOI**: 
  - Quay về NHAP (chỉnh sửa)
  - Hoặc HUY (từ bỏ)
  - Sau khi sửa, có thể submit lại (NHAP → CHO_DUYET_CAP_1)

### 13.4 Cân Nhắc Độc Lập Cấp 1 & 2

Trong thiết kế này:
- Cấp 1 & 2 có thể được thực hiện bởi **cùng MANAGER** (không bắt buộc khác)
- Nhưng tuỳ theo quy tắc công ty có thể thêm constraint "phải là người khác"
- Hiện tại để flexible, không enforce "khác người" (có thể add sau)

### 13.5 Lịch Sử & Audit Trail

- Mỗi action (submit, approve cấp 1, approve cấp 2, reject, cancel) đều ghi vào ExportReceiptHistory
- Ghi lại: actor, action, timestamp, note (nếu có)
- Dùng để tracking đầy đủ quá trình phiếu

---

## 14. Tóm Tắt Phân Tích

### 14.1 Kết Luận

| Câu Hỏi | Câu Trả Lời |
|--------|-----------|
| **1. Role xem danh sách chờ duyệt** | ADMIN, MANAGER |
| **2. Role duyệt cấp 1** | ADMIN, MANAGER |
| **3. Role duyệt cấp 2** | ADMIN, MANAGER |
| **4. Role từ chối** | ADMIN, MANAGER |
| **5. Trạng thái đầy đủ** | NHAP, CHO_DUYET, DA_DUYET, HOAN_THANH, TU_CHOI, HUY |
| **6. Điều kiện không được duyệt** | Xem mục 6 - 7 |
| **7. Điều kiện không được từ chối** | Xem mục 7 |
| **8. State Machine** | Xem mục 8 - sơ đồ đầy đủ |
| **9. Danh sách file** | Xem mục 12 - 13 file cần tạo/sửa |

### 14.2 Quy Trình Tuần Tự Implement

```
T142 (Phân tích) ← [DONE]
    ↓
T143 (Entity, Enum, Status) 
    ↓
T144 (DTO)
    ↓
T145 (Repository)
    ↓
T146 (Domain Policy, Validator, Calculator)
    ↓
T147 (Service Implementation)
    ↓
T148 (Controller - API Endpoints)
    ↓
T149 (Exception Handling)
    ↓
T150-151 (Frontend - Service + Views)
    ↓
T152-155 (Test & Validation)
```

---

## 15. Tài Liệu Tham Khảo

### 15.1 Source Code hiện có

- [ImportReceiptServiceImpl.java](../src/main/java/com/smartflow/smestocksensebackend/service/impl/ImportReceiptServiceImpl.java) - Reference workflow
- [ImportReceiptStatePolicy.java](../src/main/java/com/smartflow/smestocksensebackend/domain/inbound/ImportReceiptStatePolicy.java) - State machine pattern
- [RoleCode.java](../src/main/java/com/smartflow/smestocksensebackend/entity/RoleCode.java) - Role definition
- [ImportReceipt.java](../src/main/java/com/smartflow/smestocksensebackend/entity/ImportReceipt.java) - Entity pattern
- [ImportReceiptController.java](../src/main/java/com/smartflow/smestocksensebackend/controller/ImportReceiptController.java) - API pattern

### 15.2 Database Schema References

- Hiện tại chưa có DB schema cho ExportReceipt → Cần tạo mới (T143/Migration)
- Tham khảo schema của `phieu_nhap_kho` từ DB hiện tại

---

## Kết Thúc T142

**Tài liệu này là output phân tích của T142 - Business Analysis / Rule Design cho ExportApproval.**

Không có code, không tạo Entity, không tạo API, không sửa Database. Chỉ là tài liệu hướng dẫn cho các task implement tiếp theo.

**Trạng thái**: ✓ Ready for T143
