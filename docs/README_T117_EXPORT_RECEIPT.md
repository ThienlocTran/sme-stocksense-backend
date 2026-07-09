# TÀI LIỆU TỔNG HỢP API HỦY PHIẾU XUẤT NHÁP (T117)

## 1. API Hủy Phiếu Xuất Nháp
- **Endpoint:** `DELETE /api/v1/export-receipts/{id}/draft`
- **Mục đích:** Hủy một Phiếu Xuất Kho đang ở trạng thái Nháp hoặc Từ chối.

## 2. Logic Nghiệp Vụ (Business Rules)
- **Hình thức hủy (Soft Delete):** Phiếu không bị xóa vĩnh viễn khỏi Database. Hệ thống sẽ giữ lại phiếu và chuyển `status` thành `HUY` để phục vụ tra cứu lịch sử và kiểm toán (Audit Trail).
- **Trạng thái hợp lệ:** 
  - Chỉ được hủy khi phiếu đang ở trạng thái `NHAP` (Draft) hoặc `TU_CHOI` (Rejected).
  - Trả về lỗi 409 Conflict nếu hủy phiếu đang ở trạng thái khác (ví dụ: đang chờ duyệt `CHO_DUYET_CAP_1`).
- **Phân quyền (Authorization):**
  - Người thực hiện phải là **người đã tạo ra phiếu đó** (`created_by`).
  - Hoặc người thực hiện phải có quyền quản trị tối cao (`RoleCode.ADMIN`).
  - Trả về lỗi 403 Forbidden / MissingRoleException nếu vi phạm phân quyền.

## 3. Cấu trúc Payload và Response

### 📦 Payload Yêu Cầu (Request)
- API không yêu cầu Body (Payload).
- Chỉ cần truyền tham số `id` của phiếu xuất trên URL.
  - VD: `DELETE /api/v1/export-receipts/10/draft`

### 📤 Phản Hồi Thành Công (Response)
- HTTP Status: `204 No Content` (Yêu cầu đã được xử lý thành công và không cần trả về dữ liệu).
- Body: `(Rỗng)`

### 🔴 Phản Hồi Thất Bại Thường Gặp
- `404 Not Found`: Phiếu xuất không tồn tại.
- `403 Forbidden`: Người dùng không có quyền hủy phiếu này.
- `409 Conflict`: Phiếu xuất đang ở trạng thái không cho phép hủy (ví dụ: đang chờ duyệt).
