# SME StockSense - Backend Service

Hệ thống quản lý và dự báo tồn kho thông minh dành cho doanh nghiệp vừa và nhỏ (SME).

## 📌 Tổng Quan Dự Án
Repository này chứa mã nguồn Backend cho hệ thống **SME StockSense**, được phát triển dựa trên framework **Spring Boot** cùng kiến trúc Clean Architecture/Domain-Driven Design rút gọn, tích hợp cơ sở dữ liệu **PostgreSQL (Neon)**.

### Tài Liệu Chi Tiết Các Phân Hệ
* 📦 **Tổng quan Backend & Thiết lập**: [README_backend.md](./README_backend.md)
* 🤝 **Quản lý Đối tác (Khách hàng & Nhà cung cấp)**: [README_partners.md](./README_partners.md)
* 🏢 **Quản lý Kho hàng**: [README_warehouses.md](./README_warehouses.md)

---

## 🚀 Luồng Nghiệp Vụ Mới Nhất: Kiểm Hàng (T100)

Tính năng **Kiểm hàng** hỗ trợ nhân viên kho thực hiện đối chiếu hàng hóa thực tế nhận từ nhà cung cấp so với chứng từ dự kiến ban đầu, ghi nhận các chênh lệch và tình trạng vật lý trước khi chính thức đưa hàng vào kho.

### Các File Code Chính Tham Gia Luồng Kiểm Hàng
1. **DTO Requests**:
   * [InspectImportReceiptRequest.java](./src/main/java/com/smartflow/smestocksensebackend/dto/inbound/InspectImportReceiptRequest.java) - Chứa thông tin danh sách sản phẩm cần kiểm hàng (được bổ sung Validation kiểm tra giá trị null/rỗng).
   * [InspectImportReceiptItemRequest.java](./src/main/java/com/smartflow/smestocksensebackend/dto/inbound/InspectImportReceiptItemRequest.java) - Biểu diễn thông tin thực nhận cụ thể cho từng sản phẩm (ID, số lượng thực nhận, tình trạng vật lý, hạn sử dụng).
2. **Logic Nghiệp vụ (Service)**:
   * [ImportReceiptServiceImpl.java](./src/main/java/com/smartflow/smestocksensebackend/service/impl/ImportReceiptServiceImpl.java) - Thực hiện xác thực trạng thái phiếu nhập (`CHO_KIEM_HANG`), đối chiếu chênh lệch, phân loại dòng thành `KHOP` hoặc `CHENH_LECH`, và lưu lại kết quả kiểm đếm thực tế.
3. **Database Migration**:
   * [V10__add_inspection_columns.sql](./src/main/resources/db/migration/V10__add_inspection_columns.sql) - Thêm các cột phục vụ kiểm hàng (`tinh_trang`, `han_su_dung`, `trang_thai_dong`) kèm theo ràng buộc kiểm tra giá trị (`CHECK constraint`) ở tầng DB.
4. **Kiểm Thử (Unit Tests)**:
   * [ImportReceiptDetailServiceTest.java](./src/test/java/com/smartflow/smestocksensebackend/service/impl/ImportReceiptDetailServiceTest.java) - Bộ kiểm thử tự động xác minh toàn bộ các kịch bản thành công (khớp/chênh lệch) và các kịch bản lỗi (sai trạng thái phiếu, trùng ID sản phẩm đầu vào).

---

## 🛠️ Hướng Dẫn Phát Triển & Chạy Test

### Yêu Cầu Hệ Thống
* Java JDK 21
* Maven 3.9+ (hoặc sử dụng `./mvnw` trên Linux/Mac, `.\mvnw.cmd` trên Windows)

### Lệnh Chạy Bộ Test Tự Động
Chạy riêng các test case cho luồng kiểm hàng và chi tiết phiếu nhập:
```bash
./mvnw test -pl . -Dtest=ImportReceiptDetailServiceTest
```

Chạy toàn bộ test suite của dự án:
```bash
./mvnw test
```
*(Lưu ý: Một số integration test yêu cầu cấu hình kết nối thực tế đến PostgreSQL để khởi chạy ứng dụng thành công).*
