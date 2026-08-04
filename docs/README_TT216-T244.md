# Tính năng: Quản lý Hồ sơ Cá nhân (Employee Profile)

## 1. Overview
Tính năng cho phép nhân viên (Employee, Manager, Admin) xem và cập nhật thông tin cá nhân của mình, bao gồm họ tên, ngày sinh, số điện thoại, giới tính và ảnh đại diện (avatar). Các thông tin nhạy cảm như trạng thái, vai trò, email không được tự ý thay đổi.

## 2. Business Rule
- **Không tự đổi Email/Role/Status**: Các trường này được hệ thống quản lý, tài khoản cá nhân chỉ được xem.
- **Tính tuổi (Age) ở Client**: Không lưu trường `age` trong DB. `dateOfBirth` được lưu để đảm bảo tính nhất quán.
- **Trạng thái tài khoản**: Chỉ tài khoản đang `HOAT_DONG` mới được xem và cập nhật hồ sơ. `TAM_KHOA` hoặc `NGUNG_HOAT_DONG` sẽ bị chặn (ném `AccountInactiveException`).

## 3. API
### Lấy hồ sơ cá nhân
- `GET /api/employees/profile/me`: Lấy dữ liệu hồ sơ cá nhân hiện tại.

### Cập nhật hồ sơ
- `PUT /api/employees/profile/me`: Cập nhật thông tin (Họ tên, SĐT, Ngày sinh, Giới tính).

### Upload ảnh đại diện
- `POST /api/employees/profile/me/avatar`
- **Content-Type**: `multipart/form-data`
- **Request**: `file=<binary>`
- **Response**: Trả về trực tiếp JSON hồ sơ sau khi cập nhật thành công (giúp FE hiển thị ngay lập tức)
```json
{
  "id": 1,
  "avatarUrl": "https://res.cloudinary.com/...",
  "fullName": "Nguyen Van A",
  "email": "a@gmail.com",
  "phone": "0912345678",
  "role": "EMPLOYEE",
  "status": "HOAT_DONG",
  "gender": "MALE",
  "dateOfBirth": "2000-01-01"
}
```

## 4. Validation Dữ liệu
- `fullName`: Bắt buộc (`@NotBlank`).
- `phone`: Backend tự động loại bỏ khoảng trắng và chuyển đổi đầu số `+84` thành `0`. Sau đó kiểm tra regex chuẩn VN `^(\+84|0)[3|5|7|8|9][0-9]{8}$`. Đảm bảo số không bị trùng với tài khoản khác.
- `dateOfBirth`: Chỉ nhận ngày trong quá khứ (`@Past`).
- `gender`: Phải thuộc ENUM (`MALE`, `FEMALE`, `OTHER`).
- **Avatar Upload**: 
  - Kích thước file <= 2MB.
  - Kích thước ảnh (pixel) <= 4000x4000 (chống spam RAM/OOM).
  - Backend sử dụng `ImageIO.read()` để đọc byte file thực sự, chặn tuyệt đối việc rename `.exe` thành `.jpg`.

## 5. Error Code
| HTTP | Ý nghĩa (Meaning) |
| ---- | ----------------- |
| 400  | Kích thước file > 2MB hoặc kích thước pixel ảnh quá lớn (> 4000x4000). |
| 400  | Số điện thoại không hợp lệ / Ngày sinh không hợp lệ. |
| 401  | Chưa xác thực (Chưa có token hoặc token hết hạn). |
| 403  | Tài khoản đang bị khóa (`TAM_KHOA` hoặc `NGUNG_HOAT_DONG`). |
| 404  | Không tìm thấy nhân viên trong hệ thống. |
| 409  | Số điện thoại đã được sử dụng bởi tài khoản khác. |
| 415  | Unsupported Media Type (File gửi lên không phải ảnh hợp lệ, fail qua `ImageIO`). |

## 6. Security
- API áp dụng `.anyRequest().authenticated()`. User chỉ có thể thao tác với chính tài khoản của mình (`me`).
- Token JWT (`Authorization: Bearer <token>`) là bắt buộc.
- Backend không bao giờ expose `avatarPublicId` hay `passwordHash` ra API Response, tránh lộ internal ID của Cloudinary.

## 7. Thiết kế Transaction & Xử lý Cloudinary
- **Cấu hình**: Key được cấu hình trong `application.yml` liên kết với biến môi trường (`${CLOUDINARY_API_KEY}`). Tuyệt đối KHÔNG hardcode.
- **Thư mục lưu trữ**: Phân tách rõ ràng môi trường `sme-stocksense/{env}/avatars`.
- **Tên file (Public ID)**: Được đặt tên có cấu trúc `employee_{id}_{timestamp}` thay vì dùng uuid ngẫu nhiên.
- **Luồng xử lý Orphan Image (Quản lý giao dịch)**: Cloudinary là External Service (không rollback tự động theo Spring Transaction). Luồng hoạt động:
  1. **Upload Cloudinary**: Tải ảnh mới lên nhận `secure_url` và `new_public_id`.
  2. **Save DB**: Lưu vào database (`saveAndFlush`) để trigger commit.
  3. **Nếu DB Lưu thất bại**: Hệ thống sẽ tự động bắt exception và gửi lệnh xóa ngay lập tức `new_public_id` trên Cloudinary, không để lại file rác. Đồng thời trả về `400 BadRequest`.
  4. **Nếu DB Lưu thành công**: Lúc này avatar mới đã an toàn. Hệ thống gọi API Cloudinary để xóa ảnh cũ (nếu gọi xóa cũ bị lỗi timeout, hệ thống ghi log và giữ lại ảnh cũ rác, API vẫn trả 200 OK bình thường để không gián đoạn trải nghiệm người dùng, orphan image cũ có thể dọn qua job định kỳ).

## 8. Frontend Integration
- **ProfileView**: Giao diện chia mode View và Edit trực quan. Form data reactive.
- **Avatar component**: Chứa một button ẩn cho phép người dùng click để chọn ảnh thay thế. Kiểm tra dung lượng và loại file trực tiếp ngay tại trình duyệt để tránh lãng phí băng thông server.
- **Service (`profileService.js`)**: Nhận response từ API update và đồng bộ thẳng vào State (`authStore`), UI sẽ update lập tức mà không cần F5.
