# Báo cáo kỹ thuật: Task T194 & T195 (Dashboard Overview)

## 1. Chức năng
- Tạo **một API duy nhất** trả về dữ liệu tổng quan cho trang Dashboard, phục vụ chung cho tất cả các vai trò (Admin, Quản lý, Nhân viên).
- Dữ liệu trả về bao gồm:
  - **Global Metrics**: Tổng số sản phẩm, tổng số kho hàng, tổng số lượng tồn kho (áp dụng cho mọi Role).
  - **Pending Tasks**: Thống kê số lượng công việc đang chờ xử lý, giúp người dùng nắm bắt công việc cần ưu tiên trong ngày.
- **Phân quyền hiển thị (Role-based)**:
  - Backend tự động nhận dạng Role từ JWT (`@AuthenticationPrincipal Employee`). Frontend không cần truyền tham số `role`.
  - **Admin / Manager**: Xem số lượng Phiếu Nhập và Phiếu Xuất đang chờ duyệt trên **toàn hệ thống**.
  - **Employee**: Chỉ xem số lượng Phiếu Nhập và Phiếu Xuất **do chính mình tạo** (dựa vào `createdById`).
  - **Inventory Alert**: Xem trên toàn hệ thống (Global) cho mọi Role vì bảng `InventoryAlert` không lưu thông tin người tạo (`createdBy`).

## 2. Quy chuẩn Trạng thái (Statuses)
Các công việc đang chờ xử lý (Pending Tasks) được tính toán dựa trên các Enum trạng thái chuẩn của hệ thống:
- **Phiếu Nhập (Import)**: `CHO_DUYET_CAP_1`, `CHO_DUYET_CAP_2`, `CHO_HANG_VE`, `CHO_KIEM_HANG`
- **Phiếu Xuất (Export)**: `CHO_DUYET`, `DA_DUYET`
- **Cảnh báo Tồn kho (Alert)**: `OPEN`, `ACKNOWLEDGED`

## 3. API Endpoint
- **URL**: `GET /api/dashboard/overview`
- **Security**: Yêu cầu xác thực JWT. Các Role hợp lệ: `ADMIN`, `MANAGER`, `EMPLOYEE`.
- **Logic thực thi (Service)**:
  1. Xác thực và trích xuất Role từ `Employee`.
  2. Query `Global Metrics` từ DB (sử dụng `ProductStatus.HOAT_DONG`, `WarehouseStatus.HOAT_DONG` và `COALESCE(SUM(quantity), 0)` cho tồn kho).
  3. Tuỳ biến query `Pending Tasks` dựa trên Role (Dùng `countByStatusIn` cho Admin/Manager và `countByStatusInAndCreatedById` cho Employee).
  4. Build DTO trả về cho Controller.

## 4. JSON Request / Response
**Request**:
- API không nhận tham số trên URL hoặc Body.
- Headers bắt buộc: `Authorization: Bearer <token>`

**Response (Thành công)**:
*(Lưu ý: API dùng trực tiếp `ResponseEntity<DashboardOverviewResponse>`, không bọc trong wrapper `ApiResponse<T>` chung).*
```json
{
  "overview": {
    "totalProducts": 100,
    "totalWarehouses": 5,
    "totalStock": 5000
  },
  "pendingTasks": {
    "importReceipts": 10,
    "exportReceipts": 5,
    "inventoryAlerts": 3
  }
}
```

## 5. Kiểm thử Unit Test (DashboardServiceImplTest)
- Đã cover toàn bộ các cases chính:
  - Role **Admin** và **Manager**: Xác nhận Service gọi Repository method `countByStatusIn()`.
  - Role **Employee**: Xác nhận Service gọi Repository method `countByStatusInAndCreatedById()` cho import/export.
  - Xử lý các case giá trị trả về rỗng (0) từ Inventory/Pending.
  - Ném ngoại lệ chính xác (`AccessDeniedException`) khi `RoleCode` là `null` hoặc người dùng chưa đăng nhập.
- Tất cả các tests stub trực tiếp vào logic Enum `ProductStatus.HOAT_DONG` và `WarehouseStatus.HOAT_DONG` để đảm bảo code service không tự ý đổi trạng thái truy vấn.
