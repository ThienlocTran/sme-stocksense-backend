# Task T178: Tạo Service Phát Hiện Tồn Kho Thấp Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Xây dựng Service phát hiện tồn kho thấp (`InventoryAlertDetectionService`) hỗ trợ cả quét chủ động (Scan/Batch) và kiểm tra điểm (Spot Check), đạt chuẩn 10/10 Balanced Architect Edition.

**Architecture:** Sử dụng kiến trúc Separation of Concerns (chỉ tập trung phát hiện tụt kho và sinh phiếu mới `OPEN`, để dành auto-resolve cho T183/T184). Tận dụng SQL chuẩn hóa từ T176 làm Single Source of Truth và kiểm tra deduplication từ T177 trước khi lưu.

**Tech Stack:** Java 21, Spring Boot 4.0.6, Spring Data JPA / Hibernate, JUnit 5 & Mockito.

## Global Constraints

- Tuân thủ nguyên tắc `@ponytail`: Đơn giản, xúc tích, không over-engineering, loại bỏ các ràng buộc/test không phản ánh nghiệp vụ thực tế đồ án.
- Bắt buộc có chú thích (Note) tiếng Việt vào từng khối logic giải thích luồng xử lý theo chuẩn AGENTS.md.
- Bắt buộc chạy lệnh `mvnw clean compile` thành công (BUILD SUCCESS) và 100% Test Passed trước khi hoàn tất task.

---

## Task Structure

### Task 1: Tạo DTO Phản hồi Kết quả Quét (`AlertDetectionResultResponse`)
Tạo record DTO chứa thông số tổng kết sau mỗi lần quét (tổng số quét, tạo mới, bỏ qua, timestamp).

**Files:**
- Create: `src/main/resources/` (không có)
- Create: `src/main/java/com/smartflow/smestocksensebackend/dto/inventory/AlertDetectionResultResponse.java`

**Interfaces:**
- Consumes: `java.time.LocalDateTime`.
- Produces: Record `AlertDetectionResultResponse` phục vụ trả về cho Controller/Job và ghi log.

- [x] Tạo file `AlertDetectionResultResponse.java` với các thuộc tính `totalScanned`, `newAlertsCreated`, `existingAlertsSkipped`, `timestamp`.

---

### Task 2: Tạo Interface & Implementation cho Detection Service
Xây dựng interface `InventoryAlertDetectionService` và lớp thực hiện `InventoryAlertDetectionServiceImpl`.

**Files:**
- Create: `src/main/java/com/smartflow/smestocksensebackend/service/InventoryAlertDetectionService.java`
- Create: `src/main/java/com/smartflow/smestocksensebackend/service/impl/InventoryAlertDetectionServiceImpl.java`

**Interfaces:**
- Consumes: `InventoryLevelRepository`, `InventoryAlertRepository`.
- Produces: `scanAndCreateAlerts(Long warehouseId)` và `checkAndCreateAlert(Long productId, Long warehouseId)`.

- [x] Tạo interface `InventoryAlertDetectionService`.
- [x] Tạo implementation `InventoryAlertDetectionServiceImpl` với annotation `@Service`, `@RequiredArgsConstructor`, `@Transactional`.
- [x] Bổ sung các chú thích tiếng Việt rõ ràng giải thích luồng quét, kiểm tra deduplication và sinh phiếu mới.

---

### Task 3: Kiểm thử tự động với Mockito (`InventoryAlertDetectionServiceTest`)
Viết kiểm thử hợp đồng và luồng xử lý bằng Mockito để xác minh độ chính xác 100% trên CI/Maven mà không cần DB thực.

**Files:**
- Create: `src/test/java/com/smartflow/smestocksensebackend/service/impl/InventoryAlertDetectionServiceTest.java`

**Interfaces:**
- Consumes: `InventoryAlertDetectionServiceImpl`, Mock của `InventoryLevelRepository` và `InventoryAlertRepository`.

- [x] Viết unit test `testScanAndCreateAlerts_WithNewAndExistingAlerts` xác minh đúng số phiếu tạo mới và số phiếu bỏ qua do trùng lặp.
- [x] Viết unit test `testScan_NoLowStock_ReturnEmptyResult` xác minh trả về (0, 0, 0) khi không có hàng tụt kho.
- [x] Viết unit test `testCheckAndCreateAlert_LowStockAndNormal` xác minh tạo phiếu khi tụt kho và từ chối khi bình thường.
- [x] Chạy lệnh `mvnw test -Dtest=InventoryAlertDetectionServiceTest` xác nhận **BUILD SUCCESS**.

---

### Task 4: Tài Liệu Tổng Hợp & Cập Nhật Tiến Độ
Hoàn thiện tài liệu tổng kết Task T178 và cập nhật trạng thái hệ thống.

**Files:**
- Create: `docs/README_T178.md`
- Modify: `feature_list.json`
- Modify: `progress.md`

- [x] Viết tài liệu `docs/README_T178.md` tóm tắt API/Service, logic nghiệp vụ chính và DTO bằng tiếng Việt.
- [x] Cập nhật trạng thái `DONE` cho T178 trong `feature_list.json` và `progress.md`.
- [x] Chạy lại toàn bộ bộ test liên quan để verify lần cuối trước khi báo cáo.
