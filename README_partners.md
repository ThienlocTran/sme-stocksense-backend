# Module Đối Tác (Partners) - Backend API

Tài liệu tổng hợp các thay đổi và thông số kỹ thuật cho Module Đối Tác được phát triển trong task **T48**.

## 1. Module đã làm
- **Entity**: `Partner` ánh xạ bảng `doi_tac` trong database.
- **Enums**:
  - `PartnerType` (`NHA_CUNG_CAP`, `KHACH_HANG`, `CA_HAI`) - Phân loại đối tác.
  - `PartnerStatus` (`HOAT_DONG`, `NGUNG_HOAT_DONG`) - Trạng thái hoạt động.
- **Repository**: `PartnerRepository` kế thừa `JpaRepository` và `JpaSpecificationExecutor` để thực hiện truy vấn động.
- **Service**: `PartnerService` & `PartnerServiceImpl` triển khai các bộ lọc tìm kiếm động bằng `Specification`.
- **Controller**: `PartnerController` tiếp nhận yêu cầu HTTP.

---

## 2. API chính
### GET `/api/partners`
- **Mô tả**: Lấy danh sách đối tác có lọc động.
- **Query Parameter (Tùy chọn)**:
  - `keyword` (String): Tìm kiếm tương đối không phân biệt chữ hoa/thường theo mã, tên, người liên hệ, số điện thoại, email hoặc địa chỉ.
  - `loaiDoiTac` (String): Lọc theo loại đối tác (`NHA_CUNG_CAP`, `KHACH_HANG`, `CA_HAI`).
  - `trangThai` (String): Lọc theo trạng thái hoạt động (`HOAT_DONG`, `NGUNG_HOAT_DONG`).
- **Response Format**:
  ```json
  [
    {
      "id": 1,
      "maDoiTac": "NCC001",
      "tenDoiTac": "Công ty cung cấp A",
      "loaiDoiTac": "NHA_CUNG_CAP",
      "nguoiLienHe": "Nguyễn Văn A",
      "soDienThoai": "0912345678",
      "email": "ncc_a@example.com",
      "diaChi": "Hà Nội",
      "trangThai": "HOAT_DONG"
    }
  ]
  ```

---

## 3. Quy tắc DB
- **Tên bảng**: `doi_tac`
- **Các cột**: `id`, `ma_doi_tac`, `ten_doi_tac`, `loai_doi_tac`, `nguoi_lien_he`, `so_dien_thoai`, `email`, `dia_chi`, `trang_thai`, `ngay_tao`, `ngay_cap_nhat`
- **Quy tắc nghiệp vụ**:
  - Hệ thống **không xóa vật lý đối tác** để bảo toàn lịch sử giao dịch, chỉ thay đổi trạng thái sang `NGUNG_HOAT_DONG`.
  - Loại đối tác chỉ nhận 3 giá trị: `NHA_CUNG_CAP` (Nhà cung cấp), `KHACH_HANG` (Khách hàng), và `CA_HAI` (Cả hai) để phân loại luồng giao dịch nhập xuất kho.

---

## 4. Quy tắc Security
- **Phân quyền truy cập**: Đường dẫn `GET /api/partners` yêu cầu người dùng phải đăng nhập và có một trong các vai trò:
  - `ADMIN` (Admin / IT)
  - `MANAGER` (Quản lý kho)
  - `EMPLOYEE` (Nhân viên thủ kho)

---

## 5. Cách test
- **Chạy ứng dụng backend**: Chạy cổng mặc định `http://localhost:8080`.
- **Gọi endpoint**: Sử dụng Postman hoặc Curl để thực hiện request `GET http://localhost:8080/api/partners` kèm header `Authorization: Bearer <token>`.
- **Chạy UnitTest**: Chạy test suite `PartnerServiceImplTest` trong mã nguồn.
