# Module Đối Tác (Partners) - Backend API

Tài liệu tổng hợp các thay đổi và thông số kỹ thuật cho Module Đối Tác được phát triển trong task **T48** và **T49**.

## 1. Module đã làm
- **Entity**: `Partner` ánh xạ bảng `doi_tac` trong database.
- **Enums**:
  - `PartnerType` (`NHA_CUNG_CAP`, `KHACH_HANG`, `CA_HAI`) - Phân loại đối tác.
  - `PartnerStatus` (`HOAT_DONG`, `NGUNG_HOAT_DONG`) - Trạng thái hoạt động.
- **Repository**: `PartnerRepository` kế thừa `JpaRepository` và `JpaSpecificationExecutor` để thực hiện truy vấn động.
- **Service**: `PartnerService` & `PartnerServiceImpl` triển khai các bộ lọc tìm kiếm động bằng `Specification`, cùng các phương thức `createPartner` và `updatePartner`.
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

### POST `/api/partners`
- **Mô tả**: Tạo đối tác mới trong hệ thống.
- **Request Body (JSON)**:
  ```json
  {
    "maDoiTac": "GP001", // Tùy chọn. Nếu rỗng, hệ thống tự sinh mã
    "tenDoiTac": "Công ty Gia Phát", // Bắt buộc
    "loaiDoiTac": "NHA_CUNG_CAP", // Bắt buộc (NHA_CUNG_CAP, KHACH_HANG, CA_HAI)
    "nguoiLienHe": "Nguyễn Văn A",
    "soDienThoai": "0901234567",
    "email": "contact@example.com", // Phải đúng định dạng nếu truyền
    "diaChi": "TP.HCM",
    "trangThai": "HOAT_DONG" // Tùy chọn, mặc định HOAT_DONG
  }
  ```
- **Response Format**: DDTO PartnerResponse tương tự như GET API.

### PUT `/api/partners/{id}`
- **Mô tả**: Cập nhật thông tin đối tác dựa trên ID.
- **Quy tắc nghiệp vụ**: Không cho phép cập nhật Mã đối tác (`maDoiTac`) để bảo toàn tính toàn vẹn của dữ liệu chứng từ.
- **Request Body (JSON)**:
  ```json
  {
    "tenDoiTac": "Công ty Gia Phát cập nhật", // Bắt buộc
    "loaiDoiTac": "CA_HAI", // Bắt buộc
    "nguoiLienHe": "Nguyễn Văn B",
    "soDienThoai": "0909999999",
    "email": "update@example.com",
    "diaChi": "Bình Dương",
    "trangThai": "NGUNG_HOAT_DONG" // Bắt buộc
  }
  ```
- **Response Format**: DTO PartnerResponse tương tự như GET API.

---

## 3. Quy tắc DB
- **Tên bảng**: `doi_tac`
- **Các cột**: `id`, `ma_doi_tac`, `ten_doi_tac`, `loai_doi_tac`, `nguoi_lien_he`, `so_dien_thoai`, `email`, `dia_chi`, `trang_thai`, `ngay_tao`, `ngay_cap_nhat`
- **Quy tắc nghiệp vụ**:
  - Hệ thống **không xóa vật lý đối tác** để bảo toàn lịch sử giao dịch, chỉ thay đổi trạng thái sang `NGUNG_HOAT_DONG`.
  - Loại đối tác chỉ nhận 3 giá trị: `NHA_CUNG_CAP` (Nhà cung cấp), `KHACH_HANG` (Khách hàng), và `CA_HAI` (Cả hai) để phân loại luồng giao dịch nhập xuất kho.
  - **Validate Loại đối tác (Task T50)**:
    - Bắt buộc điền loại đối tác (`loaiDoiTac`), không được để trống hoặc `null`. Nếu gửi trống/null, hệ thống trả về lỗi: `"Loại đối tác không được để trống."` (HTTP Status 400).
    - Chỉ chấp nhận 3 giá trị: `NHA_CUNG_CAP`, `KHACH_HANG`, `CA_HAI`. Bất kỳ giá trị nào khác (ví dụ: `VENDOR`, `SUPPLIER`, `CUSTOMER`, `ABC`, `OTHER`) đều bị hệ thống từ chối và trả về lỗi: `"Loại đối tác chỉ nhận NHA_CUNG_CAP, KHACH_HANG hoặc CA_HAI."` (HTTP Status 400).
    - Kiểm tra tính hợp lệ này được thực hiện song song cả ở mức DTO (Spring Validation) và tầng Service (Exception handling) để đảm bảo an toàn dữ liệu tuyệt đối trước khi xử lý hoặc lưu xuống DB.

---

## 4. Quy tắc Security
- **Quy tắc đọc**: `GET /api/partners` yêu cầu vai trò `ADMIN`, `MANAGER` hoặc `EMPLOYEE`.
- **Quy tắc ghi (Tạo/Sửa)**: `POST /api/partners` và `PUT /api/partners/{id}` chỉ cho phép vai trò `ADMIN` hoặc `MANAGER`. Nhân viên thủ kho (`EMPLOYEE`) bị chặn và trả về `403 Forbidden`.

---

## 5. Cách test
- **Chạy ứng dụng backend**: Chạy cổng mặc định `http://localhost:8080`.
- **Gọi endpoint**: Sử dụng Postman hoặc Curl để thực hiện request kèm header `Authorization: Bearer <token>`.
- **Chạy UnitTest**: Chạy test suite `PartnerServiceImplTest` trong mã nguồn.
