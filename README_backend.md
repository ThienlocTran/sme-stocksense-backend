# SME StockSense Backend

Backend Spring Boot cho hệ thống **SME StockSense** - hệ thống quản lý tồn kho tích hợp dự báo nhu cầu cho doanh nghiệp vừa và nhỏ.

> Trạng thái hiện tại: repository này đang ở giai đoạn khởi tạo source base. Dự án đã có cấu trúc Spring Boot cơ bản, dependency chính và cấu hình server tối thiểu, nhưng chưa có API nghiệp vụ, schema database hay tích hợp AI service.

## Trạng thái hiện tại

Đã có:

- Spring Boot application entry point.
- Maven Wrapper.
- Cấu hình `application.yml` tối thiểu.
- Dependency nền tảng cho Web MVC, JPA, Flyway, Security, Validation, PostgreSQL và Lombok.
- Cấu trúc package cơ bản cho `config`, `controller`, `dto`, `entity`, `exception`, `repository`, `service`.
- Thư mục `src/main/resources/db/migration/` để đặt migration Flyway sau này.

Chưa có:

- Health check endpoint.
- API xác thực, sản phẩm, kho, tồn kho, phiếu nhập/xuất, import Excel, forecast hoặc alert.
- Entity, repository, service và controller nghiệp vụ.
- File migration SQL.
- File cấu hình theo profile như `application-local.yml` hoặc `application-neon.yml.example`.
- `docker-compose.yml` cho PostgreSQL local.
- Kết nối thực tế đến AI service.

## Công nghệ

| Thành phần | Công nghệ |
| --- | --- |
| Ngôn ngữ | Java 21 |
| Framework | Spring Boot 4.0.6 |
| API | Spring Web MVC |
| Database access | Spring Data JPA |
| Bảo mật | Spring Security |
| Database dự kiến | PostgreSQL |
| Migration dự kiến | Flyway |
| Build tool | Maven / Maven Wrapper |
| Tiện ích | Lombok, Validation |

## Cấu trúc hiện tại

```text
sme-stocksense-backend/
├── src/main/java/com/smartflow/smestocksensebackend/
│   ├── config/
│   ├── controller/
│   ├── dto/
│   │   ├── request/
│   │   └── response/
│   ├── entity/
│   ├── exception/
│   ├── repository/
│   ├── service/
│   │   └── impl/
│   └── SmeStocksenseBackendApplication.java
├── src/main/resources/
│   ├── application.yml
│   └── db/migration/
├── src/test/
├── pom.xml
├── mvnw
├── mvnw.cmd
└── README_backend.md
```

## Yêu cầu cài đặt

- Java 21.
- Maven hoặc Maven Wrapper.
- PostgreSQL 15+ sẽ cần ở các giai đoạn sau, khi bắt đầu thêm JPA entity và Flyway migration.
- IntelliJ IDEA hoặc IDE hỗ trợ Spring Boot.

## Cấu hình hiện tại

File cấu hình chính:

```text
src/main/resources/application.yml
```

Nội dung hiện tại:

```yaml
spring:
  application:
    name: sme-stocksense-backend

server:
  port: 8080
```

Hiện tại repository chưa cấu hình datasource. Khi thêm database, nên tạo profile riêng, ví dụ:

- `application-local.yml` cho PostgreSQL local.
- `application-neon.yml.example` làm file mẫu cho môi trường demo dùng Neon.
- `application-neon.yml` là file thật chứa credential và không commit lên Git.

## Chạy ứng dụng

Trên Windows:

```bash
mvnw.cmd spring-boot:run
```

Trên macOS/Linux:

```bash
./mvnw spring-boot:run
```

Ứng dụng mặc định chạy tại:

```text
http://localhost:8080
```

Lưu ý: hiện chưa có `/api/health`, nên truy cập endpoint này sẽ chưa có kết quả đúng cho đến khi health controller được thêm.

## Kiểm thử

Chạy test:

```bash
mvnw.cmd test
```

Hoặc trên macOS/Linux:

```bash
./mvnw test
```

Repository hiện chỉ có test khởi tạo context mặc định. Khi thêm datasource, security hoặc controller, cần bổ sung test tương ứng để tránh lỗi context load.

## Kiến trúc mục tiêu

```text
sme-stocksense-frontend   Vue 3
          |
          | REST API
          v
sme-stocksense-backend    Spring Boot + JPA + Flyway
          |
          | JDBC
          v
Database                  PostgreSQL / Neon / Docker local

sme-stocksense-backend
          |
          | REST API
          v
sme-stocksense-ai         FastAPI + forecasting models
```

Backend sẽ là trung tâm xử lý nghiệp vụ. AI service chỉ phụ trách dự báo, sau đó backend lưu kết quả và cung cấp dữ liệu cho frontend.

## Module nghiệp vụ dự kiến

Các module dưới đây là định hướng triển khai, chưa phải chức năng đã hoàn thiện trong repository hiện tại.

### 1. Xác thực và phân quyền

- Đăng nhập, đăng xuất, lấy thông tin người dùng hiện tại.
- Quản lý nhân viên, vai trò và quyền.
- Cấu hình Spring Security phù hợp cho REST API.

### 2. Dữ liệu nền

- Quản lý danh mục.
- Quản lý đối tác.
- Quản lý kho.
- Quản lý sản phẩm.

### 3. Tồn kho

- Theo dõi tồn kho hiện tại theo sản phẩm và kho.
- Ghi lịch sử giao dịch kho.
- Lọc giao dịch theo sản phẩm, kho và thời gian.

### 4. Phiếu nhập và phiếu xuất

- Tạo phiếu nhập kho và phiếu xuất kho.
- Gửi duyệt, duyệt, từ chối.
- Hoàn thành phiếu và cập nhật tồn kho.

### 5. Import Excel

- Upload Excel.
- Kiểm tra lỗi dữ liệu.
- Chuẩn hóa dữ liệu.
- Xem trước dữ liệu import.
- Xác nhận import chính thức.

### 6. Dự báo AI và cảnh báo tồn kho

- Gửi dữ liệu lịch sử xuất kho sang AI service.
- Nhận kết quả dự báo, sai số và mô hình được chọn.
- Lưu kết quả forecast.
- Tạo cảnh báo tồn kho và đề xuất tái đặt hàng.

## API dự kiến

Các endpoint dưới đây là roadmap API, chưa phải API đang chạy.

| Method | Endpoint | Mô tả |
| --- | --- | --- |
| GET | `/api/health` | Kiểm tra backend |
| POST | `/api/auth/login` | Đăng nhập |
| POST | `/api/auth/logout` | Đăng xuất |
| GET | `/api/auth/me` | Lấy thông tin người dùng hiện tại |
| GET | `/api/employees` | Lấy danh sách nhân viên |
| GET | `/api/roles` | Lấy danh sách vai trò |
| GET | `/api/permissions` | Lấy danh sách quyền |
| GET | `/api/categories` | Lấy danh sách danh mục |
| GET | `/api/partners` | Lấy danh sách đối tác |
| GET | `/api/warehouses` | Lấy danh sách kho |
| GET | `/api/products` | Lấy danh sách sản phẩm |
| POST | `/api/products` | Tạo sản phẩm |
| PUT | `/api/products/{id}` | Cập nhật sản phẩm |
| DELETE | `/api/products/{id}` | Xóa hoặc ngưng hoạt động sản phẩm |
| GET | `/api/inventory` | Lấy tồn kho hiện tại |
| GET | `/api/inventory/transactions` | Lấy lịch sử giao dịch kho |
| POST | `/api/stock-in` | Tạo phiếu nhập kho |
| PATCH | `/api/stock-in/{id}/submit` | Gửi duyệt phiếu nhập |
| PATCH | `/api/stock-in/{id}/approve` | Duyệt phiếu nhập |
| PATCH | `/api/stock-in/{id}/reject` | Từ chối phiếu nhập |
| PATCH | `/api/stock-in/{id}/complete` | Hoàn thành phiếu nhập |
| POST | `/api/stock-out` | Tạo phiếu xuất kho |
| PATCH | `/api/stock-out/{id}/submit` | Gửi duyệt phiếu xuất |
| PATCH | `/api/stock-out/{id}/approve` | Duyệt phiếu xuất |
| PATCH | `/api/stock-out/{id}/reject` | Từ chối phiếu xuất |
| PATCH | `/api/stock-out/{id}/complete` | Hoàn thành phiếu xuất |
| POST | `/api/import-excel` | Upload Excel |
| POST | `/api/import-excel/{id}/confirm` | Xác nhận import |
| POST | `/api/forecast/run` | Gọi AI service chạy dự báo |
| GET | `/api/forecast/results` | Lấy kết quả dự báo |
| GET | `/api/alerts` | Lấy cảnh báo tồn kho |
| PATCH | `/api/alerts/{id}/resolve` | Đánh dấu cảnh báo đã xử lý |

## Flyway migration dự kiến

Thư mục migration:

```text
src/main/resources/db/migration/
```

Quy ước đặt tên đề xuất:

```text
V1__init_schema.sql
V2__seed_roles_permissions.sql
V3__create_inventory_tables.sql
V4__create_stock_documents.sql
V5__create_import_excel_tables.sql
V6__create_forecast_alert_tables.sql
```

Lưu ý:

- Không sửa migration cũ sau khi đã chạy trên database dùng chung.
- Nếu cần thay đổi schema, tạo file version mới.
- Khi schema đã ổn định, nên dùng `spring.jpa.hibernate.ddl-auto=validate`.

## Docker PostgreSQL dự kiến

Repository hiện chưa có `docker-compose.yml`. Khi bổ sung PostgreSQL local, có thể thêm service tên `postgres` để chạy:

```bash
docker compose up postgres -d
```

Dừng database:

```bash
docker compose down
```

Xóa luôn volume database local:

```bash
docker compose down -v
```

## Quy ước Git

Nhánh chính đề xuất:

```text
main      : phiên bản ổn định
develop   : nhánh tích hợp tính năng
feature/* : nhánh phát triển từng tính năng
```

Ví dụ tạo nhánh chức năng:

```bash
git checkout develop
git checkout -b feature/product-crud
```

Quy ước commit gợi ý:

```text
chore: initialize Spring Boot backend project
docs: update backend setup guide
chore: configure PostgreSQL and Flyway
feat: add health check endpoint
feat: add product CRUD API
fix: correct inventory quantity update
refactor: split stock document service
```

## Bảo mật

Không commit các file chứa thông tin nhạy cảm:

```text
.env
.env.*
application-neon.yml
application-prod.yml
```

Chỉ commit file mẫu, ví dụ:

```text
application-neon.yml.example
```

Nếu lỡ commit mật khẩu hoặc connection string thật, cần đổi mật khẩu ngay và cân nhắc xóa secret khỏi Git history.

## Roadmap triển khai

### Giai đoạn 1: Source base

- Hoàn thiện cấu trúc package.
- Thêm health check API.
- Thêm common response model.
- Thêm global exception handler.
- Cấu hình CORS cho frontend.

### Giai đoạn 2: Database foundation

- Thêm `application-local.yml.example`.
- Thêm `docker-compose.yml` cho PostgreSQL local.
- Tạo migration Flyway đầu tiên.
- Cấu hình JPA validate schema.

### Giai đoạn 3: Dữ liệu nền

- Quản lý danh mục.
- Quản lý đối tác.
- Quản lý kho.
- Quản lý sản phẩm.

### Giai đoạn 4: Tồn kho và chứng từ

- Theo dõi tồn kho hiện tại.
- Ghi lịch sử giao dịch kho.
- Tạo và xử lý phiếu nhập kho.
- Tạo và xử lý phiếu xuất kho.

### Giai đoạn 5: Import Excel

- Upload file.
- Validate dữ liệu.
- Preview dữ liệu.
- Xác nhận import.

### Giai đoạn 6: AI forecast và cảnh báo

- Thiết kế contract với AI service.
- Gọi forecast API.
- Lưu kết quả forecast.
- Tạo cảnh báo tồn kho.
- Đánh dấu cảnh báo đã xử lý.

## Tác giả / Nhóm phát triển

- Nhóm: SmartFlow
- Sản phẩm: SME StockSense
- Đề tài: Hệ thống dự báo tồn kho thông minh cho SME
