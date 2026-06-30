# SME StockSense - Tổng Hợp Hệ Thống Backend

> For current AI/project operating rules, follow `AGENTS.md` and `rules/`.

## Safe Environment Setup

Do not put real DB credentials in committed `application.yml`.

For Neon/local development:
1. Copy `.env.example` to `.env`.
2. Copy `src/main/resources/application-neon.yml.example` to `src/main/resources/application-neon.yml`.
3. Fill real credentials in ignored local files or IDE environment variables.
4. Run with profile `neon`.
5. Never commit `.env` or `application-neon.yml`.

Chào mừng bạn đến với tài liệu tổng hợp hệ thống Backend của dự án **SME StockSense** – Hệ thống quản lý tồn kho thông minh và dự báo nhu cầu dành cho doanh nghiệp vừa và nhỏ (SME).

Tài liệu này tổng hợp toàn bộ các module tính năng, kiến trúc mã nguồn, cơ sở dữ liệu và các liên kết hướng dẫn chi tiết của dự án bằng tiếng Việt.

---

## 📌 Các Tài Liệu Chi Tiết

Để xem hướng dẫn chi tiết của từng module cụ thể, vui lòng truy cập các liên kết dưới đây:

* ⚙️ **[Hướng dẫn cài đặt & Cấu trúc gốc](./README_backend.md)**: Chi tiết cấu trúc thư mục, môi trường phát triển, Docker PostgreSQL, và lộ trình triển khai tổng thể.
* 📦 **[Module Quản lý Kho hàng (Warehouses)](./README_warehouses.md)**: Các API Danh sách, Tạo mới, Cập nhật, và Kích hoạt/Hủy kích hoạt kho hàng.
* 🤝 **[Module Quản lý Đối tác (Partners)](./README_partners.md)**: Các API quản lý Nhà cung cấp/Khách hàng, ràng buộc kiểm tra loại đối tác, phân quyền truy cập.
* ⚖️ **[Tính năng Lập Biên bản Chênh lệch (T101)](./README_T101.md)**: Quy trình kiểm hàng thực tế (T100), lập biên bản chênh lệch (T101), thiết kế database và ví dụ JSON.
* 📜 **[Ghi Giao dịch Nhập kho (T103)](./README_T103.md)**: Thiết kế và logic ghi log giao dịch kho loại `NHAP_KHO` sau khi hoàn tất, liên kết phiếu nhập và theo dõi tồn kho trước/sau.

---

## 🚀 Các Tính Năng Đã Triển Khai (Features)

Hệ thống đã hoàn thiện các nghiệp vụ cốt lõi sau:

### 1. Xác Thực & Phân Quyền (Security & Auth)
* Tích hợp **Spring Security** bảo mật các REST API endpoint.
* Phân quyền chặt chẽ dựa trên 3 vai trò chính: `ADMIN` (Quản trị viên), `MANAGER` (Quản lý), và `EMPLOYEE` (Nhân viên/Thủ kho).
* Xác thực trạng thái hoạt động của tài khoản nhân viên trước khi xử lý nghiệp vụ.

### 2. Quản Lý Dữ Liệu Nền (Master Data)
* **Danh mục sản phẩm (Categories)**: Xem danh sách, thêm, sửa, ngưng hoạt động danh mục.
* **Đối tác (Partners)**: Lưu trữ đối tác với các phân loại `NHA_CUNG_CAP` (Nhà cung cấp), `KHACH_HANG` (Khách hàng), và `CA_HAI` (Cả hai).
* **Kho hàng (Warehouses)**: Hỗ trợ tạo và chỉnh sửa thông tin kho chứa, tự động khóa/mở kho.

### 3. Quy Trình Kiểm Hàng & Biên Bản Chênh Lệch (Inbound Inspection & Discrepancies)
Quy trình này gồm hai bước nghiệp vụ chặt chẽ:
1. **Kiểm hàng thực tế (T100 - Inspect)**:
   * Ghi nhận số lượng thực tế nhận được (`actualReceivedQuantity`), tình trạng vật lý sản phẩm (`physicalStatus`), và hạn sử dụng (`expiryDate`).
   * Tự động đối chiếu số lượng thực tế với số lượng trên chứng từ gốc của `ImportReceipt`.
   * Đánh dấu dòng sản phẩm là `KHOP` hoặc `CHENH_LECH`.
2. **Lập biên bản chênh lệch (T101 - Discrepancy Report)**:
   * Cho phép nhân viên lập biên bản đối với các phiếu nhập có dòng trạng thái `CHENH_LECH`.
   * Ghi nhận lý do chênh lệch (`reason`) và hướng xử lý đề xuất (`action`) cho từng dòng sản phẩm bị lệch.
   * Tự động sinh mã biên bản: `BBCL-[Mã Phiếu Nhập]`.

### 4. Giao Dịch Nhập Kho (Inventory Transaction Logs - T103)
* Ghi nhận giao dịch biến động tồn kho (`giao_dich_kho`) loại `NHAP_KHO` khi hoàn tất nhập kho.
* Tự động đo lường và ghi vết số lượng tồn kho trước/sau (`so_luong_truoc`, `so_luong_sau`), người tạo, liên kết phiếu nhập và ghi chú đối soát.
* Kế thừa từ thiết kế Task T73 để phục vụ nghiệp vụ đối soát.

---

## 🛠️ Công Nghệ Sử Dụng

* **Core**: Java 21, Spring Boot 4.0.6 (Spring Web MVC, Spring Security).
* **Database**: PostgreSQL (Neon Cloud / Docker local), JPA/Hibernate.
* **Database Migration**: Flyway Migration.
* **Tiện ích & Validation**: Lombok, Jakarta Validation.
* **Kiểm thử**: JUnit 5, Mockito, Spring Boot Test.

---

## 📂 Cấu Trúc Mã Nguồn Quan Trọng

```text
sme-stocksense-backend/
├── src/main/java/com/smartflow/smestocksensebackend/
│   ├── config/             # Cấu hình Spring Security, JWT, CORS...
│   ├── controller/         # Các REST Controller định nghĩa API endpoint
│   │   └── ImportReceiptController.java     # API Kiểm hàng & Lập biên bản chênh lệch
│   ├── dto/                # Các đối tượng truyền dữ liệu (Request/Response)
│   │   └── inbound/        # DTO cho luồng Nhập kho
│   ├── entity/             # Các JPA Entity ánh xạ với Database
│   │   ├── ImportReceipt.java
│   │   ├── DiscrepancyReport.java           # Biên bản chênh lệch
│   │   └── DiscrepancyReportDetail.java     # Chi tiết sản phẩm chênh lệch
│   ├── repository/         # Giao tiếp với database qua Spring Data JPA
│   └── service/            # Lớp chứa xử lý logic nghiệp vụ chính
│       └── impl/
│           └── ImportReceiptServiceImpl.java # Logic xử lý kiểm đếm, đối chiếu & tạo biên bản
└── src/main/resources/
    ├── application.yml     # Cấu hình chung của ứng dụng
    └── db/migration/       # Các file migration SQL của Flyway
```

---

## 💻 Hướng Dẫn Chạy Dự Án Nhanh

### 1. Chuẩn bị môi trường
* Đảm bảo đã cài đặt **Java 21** và **Maven**.
* Tạo database PostgreSQL và cấu hình chuỗi kết nối trong file `.env` hoặc `src/main/resources/application.yml` (hoặc thông qua profile `application-neon.yml`).

### 2. Chạy ứng dụng Spring Boot
Chạy lệnh sau tại thư mục gốc của dự án:

```bash
# Trên Windows:
.\mvnw.cmd spring-boot:run

# Trên macOS/Linux:
./mvnw spring-boot:run
```

Ứng dụng sẽ khởi chạy tại cổng mặc định `8080`: `http://localhost:8080`.

---

## 🧪 Lệnh Chạy Bộ Test Tự Động

Để chạy kiểm thử tự động cho toàn bộ ứng dụng hoặc các lớp dịch vụ chính của luồng Nhập kho:

* **Chạy toàn bộ kiểm thử (Test suite)**:
  ```bash
  ./mvnw test
  ```
  *(Hoặc sử dụng `.\mvnw.cmd test` trên Windows)*

* **Chạy kiểm thử riêng cho luồng kiểm hàng & lập biên bản chênh lệch**:
  ```bash
  ./mvnw test -Dtest=ImportReceiptDetailServiceTest,ImportReceiptInspectControllerTest,ImportReceiptDiscrepancyReportControllerTest
  ```
