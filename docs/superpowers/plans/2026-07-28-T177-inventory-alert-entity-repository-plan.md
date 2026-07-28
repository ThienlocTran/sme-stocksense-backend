# Task T177: Tạo Entity & Repository Cảnh Báo Tồn Kho Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Xây dựng mô hình lưu trữ dữ liệu (Database Schema, Enums, JPA Entity với Transition Guard và Repository) cho Hệ thống Cảnh báo Tồn kho đạt chuẩn 10/10 Balanced Architect Edition.

**Architecture:** Sử dụng kiến trúc Single Source of Truth, imbutable snapshot, Partial Unique Index tại tầng DB để chống race condition, `@Version` Optimistic Lock chống xung đột cập nhật đồng thời, và Transition Guard tại Entity (`canAcknowledge / canResolve`) theo phong cách Rich Domain Model để bảo vệ toàn vẹn luồng nghiệp vụ.

**Tech Stack:** Java 21, Spring Boot 4.0.6, Spring Data JPA / Hibernate, PostgreSQL / H2 Database, Flyway Migration, JUnit 5 & Mockito.

## Global Constraints

- Tuân thủ nguyên tắc `@ponytail`: Đơn giản, xúc tích, không over-engineering, loại bỏ các ràng buộc/test không phản ánh nghiệp vụ thực tế đồ án.
- Bắt buộc có chú thích (Note) tiếng Việt vào từng khối logic giải thích luồng xử lý theo chuẩn AGENTS.md.
- Sử dụng `ON DELETE RESTRICT` cho các khóa ngoại (`san_pham_id`, `kho_id`). Dùng `nguoi_xu_ly VARCHAR(100)` để hỗ trợ cả nhân viên kho và hệ thống tự động (`"SYSTEM"`, `"SCHEDULER"`).
- Bắt buộc chạy lệnh `mvnw clean compile` thành công (BUILD SUCCESS) và 100% Test Passed trước khi hoàn tất task.

---

## Task Structure

### Task 1: Flyway Migration Script (`V29`)
Khởi tạo bảng `canh_bao_ton_kho`, các chỉ mục hiệu năng và Partial Unique Index hỗ trợ chống tạo trùng lặp.

**Files:**
- Create: `src/main/resources/db/migration/V29__create_inventory_alert_table.sql`

**Interfaces:**
- Consumes: Bảng `san_pham`, `kho` hiện hữu trong DB.
- Produces: Bảng `canh_bao_ton_kho` cùng các Index `idx_unique_open_ack_alert`, `idx_canh_bao_kho_trang_thai_ngay`, v.v.

- [ ] **Step 1: Viết script DDL cho migration V29**
```sql
CREATE TABLE canh_bao_ton_kho (
    id BIGSERIAL PRIMARY KEY,
    san_pham_id BIGINT NOT NULL,
    kho_id BIGINT NOT NULL,
    so_luong_hien_tai INTEGER NOT NULL,
    ton_toi_thieu INTEGER,
    ton_toi_da INTEGER,
    muc_do VARCHAR(20) NOT NULL,
    trang_thai VARCHAR(20) NOT NULL,
    ghi_chu VARCHAR(500),
    nguoi_xu_ly VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    ngay_tao TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    ngay_cap_nhat TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    ngay_giai_quyet TIMESTAMP WITHOUT TIME ZONE,
    
    CONSTRAINT fk_canh_bao_san_pham FOREIGN KEY (san_pham_id) REFERENCES san_pham (id) ON DELETE RESTRICT,
    CONSTRAINT fk_canh_bao_kho FOREIGN KEY (kho_id) REFERENCES kho (id) ON DELETE RESTRICT
);

CREATE INDEX idx_canh_bao_sp_kho_trang_thai ON canh_bao_ton_kho (san_pham_id, kho_id, trang_thai);
CREATE INDEX idx_canh_bao_kho_trang_thai_ngay ON canh_bao_ton_kho (kho_id, trang_thai, ngay_tao DESC);
CREATE INDEX idx_canh_bao_muc_do ON canh_bao_ton_kho (muc_do);
```
- [ ] **Step 2: Chạy kiểm thử khởi động DB để xác nhận Flyway migration thành công**
Run: `.\mvnw.cmd test-compile`
Expected: BUILD SUCCESS không lỗi cú pháp SQL.

---

### Task 2: Enums Nghiệp vụ (`InventoryAlertStatus` & `InventoryAlertSeverity`)
Tạo 2 Enum định nghĩa vòng đời và mức độ cảnh báo theo đúng Spec chốt từ `/grill-me`.

**Files:**
- Create: `src/main/java/com/smartflow/smestocksensebackend/entity/InventoryAlertStatus.java`
- Create: `src/main/java/com/smartflow/smestocksensebackend/entity/InventoryAlertSeverity.java`

**Interfaces:**
- Produces: `InventoryAlertStatus` (`OPEN`, `ACKNOWLEDGED`, `RESOLVED`), `InventoryAlertSeverity` (`CRITICAL`, `WARNING`).

- [ ] **Step 1: Tạo InventoryAlertStatus.java**
- [ ] **Step 2: Tạo InventoryAlertSeverity.java**
- [ ] **Step 3: Biên dịch kiểm tra**
Run: `.\mvnw.cmd compile`
Expected: BUILD SUCCESS.

---

### Task 3: JPA Entity (`InventoryAlert`) & Unit Tests cho Transition Guard
Tạo Entity ánh xạ sang bảng `canh_bao_ton_kho` kèm cơ chế `@Version` và các phương thức Rich Domain Model với Transition Guard (`canAcknowledge`, `canResolve`, `acknowledge`, `resolve`).

**Files:**
- Create: `src/main/java/com/smartflow/smestocksensebackend/entity/InventoryAlert.java`
- Create: `src/test/java/com/smartflow/smestocksensebackend/entity/InventoryAlertTest.java`

**Interfaces:**
- Consumes: `Product`, `Warehouse`, `InventoryAlertStatus`, `InventoryAlertSeverity`.
- Produces: `InventoryAlert` entity.

- [ ] **Step 1: Viết failing unit test cho Transition Guard trong `InventoryAlertTest.java`**
Kiểm tra:
1. `canAcknowledge()` và `canResolve()` trả về đúng boolean.
2. `acknowledge(...)` thành công khi ở trạng thái `OPEN`.
3. `acknowledge(...)` ném `IllegalStateException` khi đang ở trạng thái `RESOLVED`.
4. `resolve("SYSTEM")` thành công và ghi nhận `handledBy = "SYSTEM"`.
5. `resolve(...)` idempotent (gọi nhiều lần không lỗi khi đã `RESOLVED`).
- [ ] **Step 2: Chạy test để xác nhận fail**
Run: `.\mvnw.cmd test "-Dtest=InventoryAlertTest"`
Expected: FAIL (Do class entity chưa được tạo/chưa có method).
- [ ] **Step 3: Triển khai tối giản Entity `InventoryAlert.java`**
Bổ sung `@Version`, `@ManyToOne(fetch = FetchType.LAZY)` và logic kiểm tra trạng thái trong `canAcknowledge`, `canResolve`, `acknowledge`, `resolve`.
- [ ] **Step 4: Chạy lại unit test để xác nhận pass**
Run: `.\mvnw.cmd test "-Dtest=InventoryAlertTest"`
Expected: PASS 100%.
- [ ] **Step 5: Commit (chờ tích hợp)**

---

### Task 4: JPA Repository (`InventoryAlertRepository`) & Integration Tests
Tạo Repository với các method phục vụ deduplication (`existsBy...` và `findFirstBy...`) và truy vấn Dashboard/List. Kiểm chứng tích hợp bằng Spring Boot Test với H2/JPA.

**Files:**
- Create: `src/main/java/com/smartflow/smestocksensebackend/repository/InventoryAlertRepository.java`
- Create: `src/test/java/com/smartflow/smestocksensebackend/repository/InventoryAlertRepositoryTest.java`

**Interfaces:**
- Consumes: `InventoryAlert` entity.
- Produces: `InventoryAlertRepository`.

- [ ] **Step 1: Viết failing integration test `InventoryAlertRepositoryTest.java`**
Bao phủ các test cases thực tế trong Ma trận kiểm thử:
1. `testSaveAndFindSnapshot`: Tạo mới và kiểm tra số lượng snapshot lưu trữ.
2. `testExistsBy_FoundAndEmpty`: Kiểm tra method `existsByProductIdAndWarehouseIdAndStatusIn` siêu nhanh.
3. `testFindFirst_Match`: Kiểm tra method `findFirstByProductIdAndWarehouseIdAndStatusIn`.
4. `testCountByWarehouseIdAndStatus`: Kiểm tra hàm đếm số lượng cảnh báo cho KPI Dashboard.
- [ ] **Step 2: Chạy test để xác nhận fail**
Run: `.\mvnw.cmd test "-Dtest=InventoryAlertRepositoryTest"`
Expected: FAIL.
- [ ] **Step 3: Triển khai `InventoryAlertRepository.java`**
Định nghĩa interface kế thừa `JpaRepository<InventoryAlert, Long>` và `JpaSpecificationExecutor<InventoryAlert>`, khai báo method `existsByProductIdAndWarehouseIdAndStatusIn`, `findFirstByProductIdAndWarehouseIdAndStatusIn`, `findByWarehouseIdAndStatusOrderByCreatedAtDesc`, `countByWarehouseIdAndStatus`.
- [ ] **Step 4: Chạy lại test để xác nhận pass**
Run: `.\mvnw.cmd test "-Dtest=InventoryAlertRepositoryTest"`
Expected: PASS 100%!
- [ ] **Step 5: Commit**

---

### Task 5: Documentation & Final Verification
Tổng kết tài liệu, chạy toàn bộ bộ kiểm thử của backend và cập nhật trạng thái tiến độ đồ án.

**Files:**
- Create: `docs/README_T177.md`
- Modify: `feature_list.json`
- Modify: `progress.md`

- [ ] **Step 1: Tạo tài liệu tóm tắt `docs/README_T177.md`**
Tóm tắt ngắn gọn bằng tiếng Việt: Chức năng, cấu trúc DB, Transition Guard (`canAcknowledge / canResolve`), DB deduplication (`existsBy...`), mini ERD. Không đưa JSON mẫu thừa thãi.
- [ ] **Step 2: Chạy kiểm tra tổng thể `mvnw clean compile` và `mvnw test`**
Run: `.\mvnw.cmd clean test`
Expected: BUILD SUCCESS (Toàn bộ test cũ + test mới T177 đều pass 100%).
- [ ] **Step 3: Cập nhật `feature_list.json` và `progress.md`**
Chuyển T177 từ `TODO` sang `DONE` và đánh dấu `[x]` vào báo cáo tiến độ.
