# SME StockSense — Backend Service

> Hệ thống quản lý và dự báo tồn kho thông minh dành cho doanh nghiệp vừa và nhỏ (SME).

## Tài liệu chi tiết theo phân hệ

| Phân hệ | File |
|---|---|
| Tổng quan backend & thiết lập môi trường | [README_backend.md](./README_backend.md) |
| Quản lý Đối tác (Khách hàng & Nhà cung cấp) | [README_partners.md](./README_partners.md) |
| Quản lý Kho hàng | [README_warehouses.md](./README_warehouses.md) |

---

## Luồng trạng thái Phiếu Nhập Kho

```
NHAP ──► CHO_DUYET_CAP_1 ──► CHO_DUYET_CAP_2 ──► CHO_HANG_VE ──► CHO_KIEM_HANG ──► [HOAN_THANH]
  │              │
  └──► HUY       └──► TU_CHOI (quay lại NHAP để sửa)
```

| Trạng thái | Ý nghĩa |
|---|---|
| `NHAP` | Phiếu vừa tạo, đang soạn thảo |
| `CHO_DUYET_CAP_1` | Đã gửi duyệt lần 1 |
| `CHO_DUYET_CAP_2` | Đã được duyệt lần 1, chờ duyệt lần 2 |
| `TU_CHOI` | Bị từ chối, có thể sửa và gửi lại |
| `CHO_HANG_VE` | Đã được duyệt hoàn toàn, đang chờ hàng về |
| `CHO_KIEM_HANG` | Hàng đã về, đang chờ kiểm đếm |
| `HOAN_THANH` | Kiểm hàng xong, nhập kho chính thức |
| `HUY` | Phiếu đã bị hủy |

---

## Phân quyền

| Role | Quyền hạn |
|---|---|
| `ADMIN` | Toàn quyền thao tác trên mọi phiếu |
| `EMPLOYEE` | Tạo, chỉnh sửa, gửi duyệt phiếu do chính mình tạo |
| `MANAGER` | Duyệt phiếu qua luồng approval (không tạo phiếu trực tiếp) |

---

## Các Tính Năng Đã Triển Khai

### T100 — Quản lý Phiếu Nháp (NHAP)

**Service**: [`ImportReceiptServiceImpl.java`](./src/main/java/com/smartflow/smestocksensebackend/service/impl/ImportReceiptServiceImpl.java)

| Method | Mô tả |
|---|---|
| `createDraft` | Tạo phiếu nhập mới ở trạng thái `NHAP`, sinh mã tự động |
| `addItem` | Thêm dòng sản phẩm vào phiếu, tự tính lại tổng tiền |
| `saveDraft` | Lưu/cập nhật toàn bộ phiếu khi đang ở trạng thái `NHAP` |
| `updateEditable` | Cập nhật phiếu ở trạng thái `NHAP` hoặc `TU_CHOI` (sau khi bị từ chối) |
| `cancelDraft` | Hủy phiếu, chuyển sang trạng thái `HUY` |
| `submitForApproval` | Gửi phiếu vào luồng duyệt (`NHAP` → `CHO_DUYET_CAP_1`) |
| `listMyReceipts` | Lấy danh sách phiếu của nhân viên đang đăng nhập (có phân trang) |
| `getDetail` | Xem chi tiết đầy đủ một phiếu nhập kèm danh sách sản phẩm |

**Các file liên quan:**
- DTO Request: [`CreateImportReceiptRequest`](./src/main/java/com/smartflow/smestocksensebackend/dto/inbound/CreateImportReceiptRequest.java), [`SaveImportReceiptDraftRequest`](./src/main/java/com/smartflow/smestocksensebackend/dto/inbound/SaveImportReceiptDraftRequest.java), [`AddImportReceiptItemRequest`](./src/main/java/com/smartflow/smestocksensebackend/dto/inbound/AddImportReceiptItemRequest.java)
- DTO Response: [`ImportReceiptDraftResponse`](./src/main/java/com/smartflow/smestocksensebackend/dto/inbound/ImportReceiptDraftResponse.java)
- Domain: [`ImportReceiptStatePolicy`](./src/main/java/com/smartflow/smestocksensebackend/domain/inbound/ImportReceiptStatePolicy.java) (kiểm soát luồng chuyển trạng thái), [`ImportReceiptAmountCalculator`](./src/main/java/com/smartflow/smestocksensebackend/domain/inbound/ImportReceiptAmountCalculator.java) (tính tổng tiền)

---

### T102 — Ghi Nhận Hàng Về (CHO_HANG_VE → CHO_KIEM_HANG)

**Method**: `recordArrival` trong [`ImportReceiptServiceImpl.java`](./src/main/java/com/smartflow/smestocksensebackend/service/impl/ImportReceiptServiceImpl.java)

**Mô tả**: Sau khi phiếu được duyệt hoàn toàn và hàng về thực tế, nhân viên kho ghi nhận ngày hàng về (`actualArrivalDate`) để chuyển phiếu sang trạng thái `CHO_KIEM_HANG`.

| Điều kiện | Mô tả |
|---|---|
| Trạng thái yêu cầu | Phiếu phải ở trạng thái `CHO_HANG_VE` |
| Quyền hạn | `ADMIN` hoặc `EMPLOYEE` |
| Đầu vào | `actualArrivalDate` — ngày hàng về thực tế |
| Đầu ra | Phiếu nhập với trạng thái `CHO_KIEM_HANG` kèm danh sách sản phẩm |

**DTO**: [`ImportReceiptArrivalRequest`](./src/main/java/com/smartflow/smestocksensebackend/dto/inbound/ImportReceiptArrivalRequest.java)

---

### T100 (Review Fixes) — Kiểm Hàng (Validation & SQL)

Các sửa đổi theo đề xuất CodeRabbit:

| File | Sửa đổi |
|---|---|
| [`InspectImportReceiptRequest.java`](./src/main/java/com/smartflow/smestocksensebackend/dto/inbound/InspectImportReceiptRequest.java) | Thêm `@NotNull` vào generic type: `List<@NotNull @Valid InspectImportReceiptItemRequest>` |
| [`V10__add_inspection_columns.sql`](./src/main/resources/db/migration/V10__add_inspection_columns.sql) | Thêm DB constraint: `CHECK ("trang_thai_dong" IN ('KHOP', 'CHENH_LECH'))` |

---

## Kiến Trúc & Pattern

### Package Structure
```
com.smartflow.smestocksensebackend/
├── config/          # SecurityConfig, CorsConfig, ...
├── controller/      # REST Controllers (ánh xạ HTTP endpoint)
├── domain/          # Domain logic thuần (Validator, Calculator, StatePolicy)
├── dto/             # Request/Response DTOs
│   └── inbound/     # DTOs cho phiếu nhập
├── entity/          # JPA Entities (ánh xạ bảng DB)
├── exception/       # Custom exceptions (BadRequest, NotFound, Conflict, ...)
├── repository/      # Spring Data JPA Repositories
└── service/         # Service interfaces + impl/
```

### Nguyên tắc Code

- **Validation**: Dùng Bean Validation (`@NotNull`, `@NotEmpty`, `@Valid`) ở tầng DTO.
- **Error Handling**: Dùng custom exception (`BadRequestException`, `NotFoundException`, `ConflictException`, `MissingRoleException`, `AccountInactiveException`).
- **Concurrency**: Dùng `OptimisticLockingFailureException` để phát hiện ghi đồng thời, `saveAndFlush` để flush ngay trong transaction.
- **Transaction**: Các method đọc dùng `@Transactional(readOnly = true)`, các method ghi dùng `@Transactional`.
- **Migration**: Dùng Flyway, `ddl-auto = none`. File đặt tại `src/main/resources/db/migration/`.

---

## Lệnh Phát Triển

```bash
# Chạy toàn bộ test suite
.\mvnw.cmd test

# Chạy riêng test cho phiếu nhập
.\mvnw.cmd test -Dtest=ImportReceiptDetailServiceTest

# Build (không chạy test)
.\mvnw.cmd package -DskipTests
```
