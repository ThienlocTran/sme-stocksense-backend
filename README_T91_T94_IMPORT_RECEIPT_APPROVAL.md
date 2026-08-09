# T91-T94: API DUYỆT PHIẾU NHẬP KHO 2 CẤP

Tài liệu này mô tả chi tiết tính năng duyệt phiếu nhập kho theo quy trình 2 cấp duyệt (MANAGER/ADMIN).

---

## 1. Tổng quan (Overview)

Tính năng duyệt phiếu nhập kho cho phép người quản lý (MANAGER) hoặc quản trị viên (ADMIN) xem xét và phê duyệt các phiếu nhập kho đã được nhân viên gửi lên. Quy trình duyệt được thiết kế theo **2 cấp độ**:

- **Cấp 1 (CHO_DUYET_CAP_1)**: Phê duyệt sơ bộ
- **Cấp 2 (CHO_DUYET_CAP_2)**: Phê duyệt cuối cùng trước khi chuyển sang bước chờ hàng về

## 2. Luồng nghiệp vụ (Business Flow)

```
NHAP (Nháp)
    ↓ [Gửi duyệt - T83]
CHO_DUYET_CAP_1
    ↓ [Duyệt cấp 1 - T93]
CHO_DUYET_CAP_2
    ↓ [Duyệt cấp 2 - T93]
CHO_HANG_VE
    ↓ [Ghi nhận hàng về - T86]
CHO_KIEM_HANG
    ↓ [Kiểm hàng - T100]
HOAN_THANH (Hoàn tất - cộng tồn kho)

---

CHO_DUYET_CAP_1/CHO_DUYET_CAP_2
    ↓ [Từ chối - T94]
TU_CHOI (Có thể sửa và gửi lại)
```

## 3. Danh sách API Endpoints

### T91: Lấy danh sách phiếu chờ duyệt

**Endpoint:** `GET /api/import-receipts/pending-approval`

**Quyền truy cập:** MANAGER, ADMIN

**Query Parameters:**
- `status` (optional): Lọc theo trạng thái cụ thể (`CHO_DUYET_CAP_1` hoặc `CHO_DUYET_CAP_2`)
- `page` (optional, default=0): Số trang
- `size` (optional, default=20): Số bản ghi mỗi trang
- `sort` (optional): Trường sắp xếp (ví dụ: `createdAt,desc`)

**Response 200 OK:**
```json
{
  "content": [
    {
      "id": 1,
      "code": "PNK-20240001",
      "warehouseName": "Kho Trung Tâm",
      "supplierName": "Công ty TNHH ABC",
      "status": "CHO_DUYET_CAP_1",
      "totalAmount": 15000000,
      "createdByName": "Nguyễn Văn A",
      "submittedAt": "2024-06-22T10:00:00",
      "createdAt": "2024-06-22T09:00:00"
    }
  ],
  "totalElements": 10,
  "totalPages": 1,
  "size": 20,
  "number": 0
}
```

**Mô tả:**
- Trả về danh sách các phiếu nhập đang chờ duyệt cấp 1 hoặc cấp 2
- Nếu không truyền `status`, trả về tất cả phiếu chờ duyệt (cả 2 cấp)
- Kết quả được phân trang và có thể sắp xếp theo các trường

---

### T92: Xem chi tiết phiếu cần duyệt

**Endpoint:** `GET /api/import-receipts/{receiptId}/approval-detail`

**Quyền truy cập:** MANAGER, ADMIN

**Path Parameters:**
- `receiptId`: ID của phiếu nhập kho

**Response 200 OK:**
```json
{
  "id": 1,
  "code": "PNK-20240001",
  "warehouseName": "Kho Trung Tâm",
  "supplierName": "Công ty TNHH ABC",
  "status": "CHO_DUYET_CAP_1",
  "totalAmount": 15000000,
  "note": "Nhập hàng tháng 6",
  "createdByName": "Nguyễn Văn A",
  "submittedByName": "Nguyễn Văn A",
  "submittedAt": "2024-06-22T10:00:00",
  "createdAt": "2024-06-22T09:00:00",
  "items": [
    {
      "id": 1,
      "productCode": "SP001",
      "productName": "Cà phê hạt Arabica",
      "quantity": 100,
      "unitPrice": 150000,
      "totalPrice": 15000000,
      "note": "Hạn sử dụng: 2025-12-31"
    }
  ]
}
```

**Response 404 Not Found:**
```json
{
  "message": "Phieu nhap khong ton tai."
}
```

**Response 409 Conflict:**
```json
{
  "message": "Chi duoc xem chi tiet duyet voi phieu nhap o trang thai cho duyet."
}
```

**Mô tả:**
- Trả về thông tin chi tiết đầy đủ của phiếu nhập kho cần duyệt
- Bao gồm thông tin sản phẩm, số lượng, đơn giá
- Chỉ cho phép xem phiếu ở trạng thái chờ duyệt (CHO_DUYET_CAP_1 hoặc CHO_DUYET_CAP_2)

---

### T93: Duyệt phiếu nhập kho

**Endpoint:** `PUT /api/import-receipts/{receiptId}/approve`

**Quyền truy cập:** MANAGER, ADMIN

**Path Parameters:**
- `receiptId`: ID của phiếu nhập kho

**Request Body:** Không cần body

**Response 200 OK:**
```json
{
  "id": 1,
  "code": "PNK-20240001",
  "warehouseName": "Kho Trung Tâm",
  "supplierName": "Công ty TNHH ABC",
  "status": "CHO_DUYET_CAP_2",
  "totalAmount": 15000000,
  "note": "Nhập hàng tháng 6",
  "level1ApprovedByName": "Nguyễn Thị B",
  "level1ApprovedAt": "2024-06-22T11:00:00",
  "createdAt": "2024-06-22T09:00:00",
  "items": [...]
}
```

**Response 404 Not Found:**
```json
{
  "message": "Phieu nhap khong ton tai."
}
```

**Response 409 Conflict:**
```json
{
  "message": "Chi duoc duyet phieu nhap o trang thai cho duyet (CHO_DUYET_CAP_1 hoac CHO_DUYET_CAP_2)."
}
```

**Logic duyệt:**
- **CHO_DUYET_CAP_1** → **CHO_DUYET_CAP_2**: Ghi nhận người duyệt và thời gian duyệt cấp 1
- **CHO_DUYET_CAP_2** → **CHO_HANG_VE**: Ghi nhận người duyệt và thời gian duyệt cấp 2

**Lưu ý:**
- Không cộng tồn kho ở bước duyệt
- Tồn kho chỉ được cộng khi hoàn tất phiếu (T104)
- Lưu lịch sử duyệt vào bảng `import_receipt_history`

---

### T94: Từ chối phiếu nhập kho

**Endpoint:** `PUT /api/import-receipts/{receiptId}/reject`

**Quyền truy cập:** MANAGER, ADMIN

**Path Parameters:**
- `receiptId`: ID của phiếu nhập kho

**Request Body:**
```json
{
  "reason": "Thông tin nhà cung cấp không chính xác, yêu cầu kiểm tra lại"
}
```

**Validation:**
- `reason`: Bắt buộc, không được để trống, tối đa 500 ký tự

**Response 200 OK:**
```json
{
  "id": 1,
  "code": "PNK-20240001",
  "warehouseName": "Kho Trung Tâm",
  "supplierName": "Công ty TNHH ABC",
  "status": "TU_CHOI",
  "totalAmount": 15000000,
  "rejectionReason": "Thông tin nhà cung cấp không chính xác, yêu cầu kiểm tra lại",
  "createdAt": "2024-06-22T09:00:00",
  "items": [...]
}
```

**Response 400 Bad Request:**
```json
{
  "message": "Ly do tu choi khong duoc de trong."
}
```

**Response 404 Not Found:**
```json
{
  "message": "Phieu nhap khong ton tai."
}
```

**Response 409 Conflict:**
```json
{
  "message": "Chi duoc tu choi phieu nhap o trang thai cho duyet (CHO_DUYET_CAP_1 hoac CHO_DUYET_CAP_2)."
}
```

**Mô tả:**
- Từ chối phiếu nhập kho đang chờ duyệt
- Bắt buộc phải nhập lý do từ chối
- Phiếu chuyển sang trạng thái TU_CHOI
- Nhân viên có thể sửa lại và gửi duyệt lại phiếu bị từ chối

---

## 4. Cấu trúc Database

### Bảng `phieu_nhap_kho` (import_receipts)

Các trường liên quan đến duyệt:

| Trường | Kiểu | Mô tả |
|--------|------|-------|
| `trang_thai` | ENUM | Trạng thái phiếu (CHO_DUYET_CAP_1, CHO_DUYET_CAP_2, etc.) |
| `nguoi_gui_duyet_id` | BIGINT | FK đến `nhan_vien` - Người gửi duyệt |
| `ngay_gui_duyet` | TIMESTAMP | Thời gian gửi duyệt |
| `nguoi_duyet_cap_1_id` | BIGINT | FK đến `nhan_vien` - Người duyệt cấp 1 |
| `ngay_duyet_cap_1` | TIMESTAMP | Thời gian duyệt cấp 1 |
| `nguoi_duyet_cap_2_id` | BIGINT | FK đến `nhan_vien` - Người duyệt cấp 2 |
| `ngay_duyet_cap_2` | TIMESTAMP | Thời gian duyệt cấp 2 |
| `ly_do_tu_choi` | VARCHAR(500) | Lý do từ chối (nếu có) |

### Enum `ImportReceiptStatus`

```java
public enum ImportReceiptStatus {
    NHAP,              // Nháp
    CHO_DUYET_CAP_1,   // Chờ duyệt cấp 1
    CHO_DUYET_CAP_2,   // Chờ duyệt cấp 2
    CHO_HANG_VE,       // Chờ hàng về
    CHO_KIEM_HANG,     // Chờ kiểm hàng
    HOAN_THANH,        // Hoàn thành
    TU_CHOI,           // Từ chối
    HUY                // Hủy
}
```

---

## 5. Phân quyền (Authorization)

| Vai trò | Lấy danh sách chờ duyệt | Xem chi tiết | Duyệt | Từ chối |
|---------|-------------------------|--------------|-------|---------|
| ADMIN | ✅ | ✅ | ✅ | ✅ |
| MANAGER | ✅ | ✅ | ✅ | ✅ |
| EMPLOYEE | ❌ | ❌ | ❌ | ❌ |

**Lưu ý:**
- Chỉ ADMIN và MANAGER mới có quyền duyệt/từ chối phiếu
- EMPLOYEE chỉ có thể tạo và gửi duyệt phiếu, không thể tự duyệt

---

## 6. Quy tắc nghiệp vụ (Business Rules)

### 6.1 Quy tắc duyệt

1. **Trạng thái hợp lệ:**
   - Chỉ có thể duyệt phiếu ở trạng thái `CHO_DUYET_CAP_1` hoặc `CHO_DUYET_CAP_2`
   - Phiếu ở trạng thái khác sẽ báo lỗi `409 Conflict`

2. **Luồng duyệt tuần tự:**
   - `CHO_DUYET_CAP_1` → `CHO_DUYET_CAP_2`: Duyệt cấp 1
   - `CHO_DUYET_CAP_2` → `CHO_HANG_VE`: Duyệt cấp 2
   - Không được bỏ qua bất kỳ cấp nào

3. **Ghi nhận thông tin:**
   - Mỗi cấp duyệt đều ghi nhận người duyệt và thời gian duyệt
   - Lưu lịch sử duyệt vào bảng `import_receipt_history`

4. **Tính toàn vẹn:**
   - Sử dụng `@Version` (Optimistic Locking) để tránh conflict khi nhiều người duyệt cùng lúc
   - Sử dụng State Machine pattern để đảm bảo chuyển trạng thái hợp lệ

### 6.2 Quy tắc từ chối

1. **Lý do bắt buộc:**
   - Phải nhập lý do từ chối (không được null hoặc rỗng)
   - Lý do không vượt quá 500 ký tự

2. **Cho phép sửa lại:**
   - Phiếu bị từ chối chuyển sang trạng thái `TU_CHOI`
   - Nhân viên có thể sửa lại phiếu và gửi duyệt lại

3. **Lưu vết:**
   - Lý do từ chối được lưu vào trường `ly_do_tu_choi`
   - Lưu lịch sử từ chối vào bảng `import_receipt_history`

---

## 7. Cấu trúc mã nguồn (Source Code Structure)

### 7.1 Backend Files

```
src/main/java/com/smartflow/smestocksensebackend/
├── entity/
│   ├── ImportReceipt.java                  # Entity phiếu nhập kho
│   ├── ImportReceiptStatus.java            # Enum trạng thái
│   └── ImportReceiptAction.java            # Enum hành động (cho lịch sử)
├── repository/
│   └── ImportReceiptRepository.java        # Repository với query chờ duyệt
├── service/
│   ├── ImportReceiptService.java           # Interface khai báo methods
│   └── impl/
│       └── ImportReceiptServiceImpl.java   # Implementation logic duyệt
├── controller/
│   └── ImportReceiptController.java        # REST API endpoints
├── dto/
│   └── inbound/
│       ├── ImportReceiptPageResponse.java  # DTO danh sách phân trang
│       ├── ImportReceiptSummaryResponse.java # DTO tóm tắt phiếu
│       ├── ImportReceiptDraftResponse.java  # DTO chi tiết phiếu
│       └── RejectImportReceiptRequest.java  # DTO request từ chối
├── domain/
│   └── inbound/
│       └── ImportReceiptStatePolicy.java   # State Machine validation
└── config/
    └── SecurityConfig.java                  # Phân quyền endpoints
```

### 7.2 Key Methods

**ImportReceiptService.java:**
```java
// T91: Lấy danh sách chờ duyệt
ImportReceiptPageResponse listPendingApproval(String status, Pageable pageable);

// T92: Xem chi tiết phiếu duyệt
ImportReceiptDraftResponse getApprovalDetail(Long receiptId);

// T93: Duyệt phiếu
ImportReceiptDraftResponse approve(Long receiptId);

// T94: Từ chối phiếu
ImportReceiptDraftResponse reject(Long receiptId, RejectImportReceiptRequest request);
```

**ImportReceiptRepository.java:**
```java
// Tìm phiếu theo trạng thái (hỗ trợ phân trang)
@EntityGraph(attributePaths = {"warehouse", "supplier", "createdBy", "submittedBy"})
Page<ImportReceipt> findByStatus(ImportReceiptStatus status, Pageable pageable);

// Tìm phiếu theo nhiều trạng thái
@EntityGraph(attributePaths = {"warehouse", "supplier", "createdBy", "submittedBy"})
Page<ImportReceipt> findByStatusIn(Collection<ImportReceiptStatus> statuses, Pageable pageable);
```

---

## 8. Kiểm thử (Testing)

### 8.1 Chạy test

```bash
# Chạy tất cả test
./mvnw.cmd test

# Chạy test riêng cho approval
./mvnw.cmd test -Dtest=ImportReceiptApprovalTest
```

### 8.2 Test cases

#### T91: Lấy danh sách chờ duyệt
- ✅ Lấy tất cả phiếu chờ duyệt (cả 2 cấp)
- ✅ Lọc theo trạng thái cụ thể (CHO_DUYET_CAP_1)
- ✅ Lọc theo trạng thái cụ thể (CHO_DUYET_CAP_2)
- ✅ Phân trang đúng
- ❌ 403 Forbidden khi EMPLOYEE truy cập

#### T92: Xem chi tiết
- ✅ Xem chi tiết phiếu CHO_DUYET_CAP_1
- ✅ Xem chi tiết phiếu CHO_DUYET_CAP_2
- ❌ 404 Not Found khi phiếu không tồn tại
- ❌ 409 Conflict khi phiếu không ở trạng thái chờ duyệt
- ❌ 403 Forbidden khi EMPLOYEE truy cập

#### T93: Duyệt phiếu
- ✅ Duyệt cấp 1: CHO_DUYET_CAP_1 → CHO_DUYET_CAP_2
- ✅ Duyệt cấp 2: CHO_DUYET_CAP_2 → CHO_HANG_VE
- ✅ Ghi nhận người duyệt và thời gian
- ✅ Lưu lịch sử duyệt
- ❌ 409 Conflict khi phiếu không ở trạng thái chờ duyệt
- ❌ 409 Conflict khi có Optimistic Lock exception
- ❌ 403 Forbidden khi EMPLOYEE thực hiện

#### T94: Từ chối phiếu
- ✅ Từ chối phiếu CHO_DUYET_CAP_1
- ✅ Từ chối phiếu CHO_DUYET_CAP_2
- ✅ Lưu lý do từ chối
- ✅ Lưu lịch sử từ chối
- ❌ 400 Bad Request khi lý do để trống
- ❌ 409 Conflict khi phiếu không ở trạng thái chờ duyệt
- ❌ 403 Forbidden khi EMPLOYEE thực hiện

---

## 9. Các lưu ý khi triển khai

### 9.1 Security

1. **Phân quyền chặt chẽ:**
   ```java
   .requestMatchers(HttpMethod.GET, "/api/import-receipts/pending-approval").hasAnyRole("ADMIN", "MANAGER")
   .requestMatchers(HttpMethod.PUT, "/api/import-receipts/*/approve").hasAnyRole("ADMIN", "MANAGER")
   .requestMatchers(HttpMethod.PUT, "/api/import-receipts/*/reject").hasAnyRole("ADMIN", "MANAGER")
   ```

2. **Kiểm tra tài khoản active:**
   - Mọi method đều kiểm tra `actor.getStatus() == HOAT_DONG`
   - Tài khoản bị vô hiệu hóa không được phép duyệt

### 9.2 Performance

1. **Eager loading relationships:**
   - Sử dụng `@EntityGraph` để tránh N+1 query problem
   - Load trước `warehouse`, `supplier`, `createdBy`, `submittedBy`

2. **Pagination:**
   - Luôn hỗ trợ phân trang cho danh sách
   - Mặc định 20 bản ghi/trang

### 9.3 Concurrency

1. **Optimistic Locking:**
   - Sử dụng `@Version` để phát hiện conflict
   - Throw `ConflictException` khi có conflict

2. **State Machine:**
   - Sử dụng `ImportReceiptStatePolicy` để validate transition
   - Đảm bảo không có chuyển trạng thái không hợp lệ

---

## 10. Tích hợp với Frontend (T95-T98)

### 10.1 Màn hình danh sách chờ duyệt (T95)

**Component:** `PendingApprovalList.vue`

**Features:**
- Hiển thị bảng danh sách phiếu chờ duyệt
- Lọc theo cấp duyệt (Cấp 1 / Cấp 2)
- Phân trang
- Tìm kiếm theo mã phiếu, nhà cung cấp
- Action buttons: Xem chi tiết, Duyệt, Từ chối

**API Calls:**
```javascript
// Lấy danh sách
GET /api/import-receipts/pending-approval?page=0&size=20&status=CHO_DUYET_CAP_1
```

### 10.2 Modal chi tiết phiếu (T96)

**Component:** `ApprovalDetailModal.vue`

**Features:**
- Hiển thị thông tin phiếu đầy đủ
- Danh sách sản phẩm với số lượng, đơn giá
- Thông tin người tạo, người gửi duyệt
- Lịch sử duyệt (nếu có)

**API Call:**
```javascript
// Xem chi tiết
GET /api/import-receipts/{id}/approval-detail
```

### 10.3 Action duyệt phiếu (T97)

**Component:** `ApproveButton.vue`

**Features:**
- Nút "Duyệt" với confirm dialog
- Hiển thị thông báo thành công/lỗi
- Refresh danh sách sau khi duyệt

**API Call:**
```javascript
// Duyệt
PUT /api/import-receipts/{id}/approve
```

### 10.4 Action từ chối phiếu (T98)

**Component:** `RejectModal.vue`

**Features:**
- Form nhập lý do từ chối (required)
- Validation lý do (không trống, max 500 ký tự)
- Confirm trước khi từ chối
- Hiển thị thông báo thành công/lỗi

**API Call:**
```javascript
// Từ chối
PUT /api/import-receipts/{id}/reject
Body: {
  "reason": "Lý do từ chối..."
}
```

---

## 11. Changelog

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2024-06-22 | Backend Team | Initial implementation T91-T94 |
| 1.1 | 2024-07-11 | Backend Team | Fix approve logic - distinguish level 1 and level 2 |

---

## 12. Liên hệ & Hỗ trợ

- **Backend Lead:** [Your Name]
- **Repository:** https://github.com/ThienlocTran/sme-stocksense-backend
- **Branch:** `feature/T91-import-receipt-approval-api`
