# SPEC: T194 & T195 - API Dashboard Tổng Quan & Thống Kê Chờ Duyệt

## 1. Tổng Quan Kiến Trúc
- **Mục tiêu:** Cung cấp duy nhất 1 endpoint để trả về toàn bộ dữ liệu thống kê tổng quan (số sản phẩm, số kho, tổng tồn) và danh sách các tác vụ đang chờ xử lý (phiếu nhập, phiếu xuất, cảnh báo).
- **Phương pháp tiếp cận:**
  - Gom chung T194 và T195 vào **1 API duy nhất**.
  - Tính toán **Real-time** trực tiếp bằng COUNT/SUM trong SQL để đảm bảo tính chính xác tức thì. Không cần dùng Cache hay Materialized View đối với quy mô SME.
  - Tự động detect vai trò (Role) từ Security Context (`UserPrincipal`).
  - Repository bám chuẩn tên method tiếng Anh (ví dụ: `countByStatus`, `countByCreatedById`).

## 2. API Contract

**GET `/api/dashboard/overview`**

**Response JSON:**
```json
{
  "code": 200,
  "message": "Thành công",
  "data": {
    "overview": {
      "totalProducts": 150,
      "totalWarehouses": 3,
      "totalStock": 15420
    },
    "pendingTasks": {
      "importReceipts": 5,
      "exportReceipts": 2,
      "inventoryAlerts": 11
    }
  }
}
```

## 3. Phân Quyền & Scoping Dữ Liệu

### 3.1 Global Metrics (Tổng Quan)
Các chỉ số: `totalProducts`, `totalWarehouses`, `totalStock`.
- **Áp dụng cho mọi Role (ADMIN, MANAGER, EMPLOYEE)**: Đều có thể xem vì đây chỉ là KPI tổng quan của hệ thống.

### 3.2 Pending Tasks (Chờ Xử Lý)
Đếm số phiếu / cảnh báo cần xử lý theo Role:
- **ADMIN & MANAGER:** Trả về số liệu Pending trên **toàn bộ hệ thống**.
- **EMPLOYEE:** 
  - Phiếu Nhập & Phiếu Xuất: Chỉ đếm các phiếu **do Employee này tạo ra** (dựa vào `createdBy.id = principal.getId()`).
  - Cảnh báo tồn kho: Trả về **Global** (đếm toàn bộ) vì `InventoryAlert` không có trường `createdBy`.

### 3.3 Trạng thái "Chờ xử lý" (Pending Statuses)
- **Phiếu Nhập (Import Receipts):** `DA_TAO`, `DANG_KIEM_TRA`
- **Phiếu Xuất (Export Receipts):** `DA_TAO`, `DANG_LAY_HANG`
- **Cảnh báo tồn kho (Inventory Alerts):** `OPEN`, `ACKNOWLEDGED`
*(Lưu ý: Không dùng logic `NOT COMPLETED` để tránh đếm nhầm các phiếu `FAILED`, `CANCELLED`, `REJECTED`).*

### 3.4 Error Handling
- Nếu `Authentication == null` hoặc Role không hợp lệ -> Lập tức văng `403 Forbidden` (ném `AccessDeniedException` hoặc chặn từ tầng Filter). Không để request lọt sâu vào Service.

## 4. Kế Hoạch Triển Khai (Coding Plan)

### Bước 1: Khởi tạo DTOs
- `DashboardOverviewResponse`
- `OverviewMetricsDTO`
- `PendingTasksDTO`

### Bước 2: Tạo Custom Repository Queries
Thêm các custom query method vào các repositories:
- `ProductRepository.countByStatus(ProductStatus status)`
- `WarehouseRepository.countByStatus(WarehouseStatus status)`
- `InventoryLevelRepository`: Thêm `@Query("SELECT COALESCE(SUM(i.quantity), 0) FROM InventoryLevel i")` để đảm bảo không bị lỗi trả về `null` khi chưa có dữ liệu tồn kho.
- `ImportReceiptRepository`:
  - `countByStatusIn(List<ImportReceiptStatus> statuses)`
  - `countByStatusInAndCreatedById(List<ImportReceiptStatus> statuses, Long createdById)`
- `ExportReceiptRepository`:
  - `countByStatusIn(List<ExportReceiptStatus> statuses)`
  - `countByStatusInAndCreatedById(List<ExportReceiptStatus> statuses, Long createdById)`
- `InventoryAlertRepository.countByStatusIn(List<InventoryAlertStatus> statuses)`

### Bước 3: Xây dựng DashboardService
- Signature: `DashboardOverviewResponse getOverview(UserPrincipal principal)`
- Xử lý rẽ nhánh Role:
  - Lấy Role từ `principal`.
  - Nếu `ADMIN` / `MANAGER`: Gọi các hàm query Global.
  - Nếu `EMPLOYEE`: Gọi các hàm query theo `createdById = principal.getId()`.
- Chạy tính toán tuần tự theo đúng tinh thần tinh gọn (`@ponytail`).

### Bước 4: Tạo DashboardController
- Inject `DashboardService`.
- Endpoint `GET /overview`.
- Sử dụng `@AuthenticationPrincipal CustomUserDetails user` để lấy context.

### Bước 5: Viết Unit Tests (`DashboardServiceImplTest`)
Bao phủ đầy đủ các case:
- **Admin**: Nhận số liệu toàn hệ thống.
- **Manager**: Nhận số liệu toàn hệ thống (logic tương tự Admin).
- **Employee**: Nhận pending task theo `currentUserId`.
- **Tổng tồn kho rỗng**: Hàm `SUM(quantity)` phải xử lý tốt và trả về `0`.
- **Không có task pending**: Hàm đếm phải trả về `0`, không văng lỗi hay NullPointerException.
