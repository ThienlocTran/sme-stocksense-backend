# Spec: Tạo Service Phát Hiện Tồn Kho Thấp (Task T178) - 10/10 Balanced Architect Edition

**Ngày tạo:** 2026-07-28  
**Tác giả:** Antigravity (AI Assistant) & User (Senior Architect / Tech Lead)  
**Dự án:** SME StockSense Backend (Sprint 4 - Low Stock Alert System)  
**Trạng thái:** **APPROVED SPEC** (Đã chốt qua /grill-me theo chuẩn Balanced Architect & Ponytail)

---

## 1. Bối cảnh, Mục tiêu & Giá trị Nghiệp vụ (Background, Objective & Business Value)

Trong hệ thống chuỗi cung ứng, việc phát hiện kịp thời các mặt hàng sắp hết hoặc đã hết hàng là yếu tố sống còn để ngăn chặn đứt gãy kinh doanh. 
- **Task T176** đã chuẩn hóa các câu truy vấn SQL tìm ra mặt hàng tụt kho theo đúng thứ tự ưu tiên (`OUT_OF_STOCK` -> `LOW_STOCK`).
- **Task T177** đã thiết kế thực thể lưu trữ sự kiện (`InventoryAlert`) và hàm kiểm tra trùng lặp (`existsBy...`).
- **Task T178** là bước kết nối nghiệp vụ (Business Service Layer): **Xây dựng Service phát hiện tồn kho thấp (`InventoryAlertDetectionService`)**. Service này đóng vai trò như bộ quét (scanner), tự động kiểm tra số liệu tồn kho thực tế và sinh phiếu cảnh báo lưu vào CSDL để quản lý kho xử lý.

### Giá trị Nghiệp vụ & Kiến trúc Cân bằng (Pragmatic Architecture)
- **Tách biệt trách nhiệm (Ponytail Separation of Concerns):** Service ở T178 tập trung 100% vào việc **phát hiện rủi ro tụt kho và sinh phiếu mới (`OPEN`)**. Việc tự động giải quyết (`RESOLVED`) khi hàng về kho sẽ được để dành cho T183/T184 (gắn vào luồng duyệt phiếu nhập kho), tránh làm phình to logic và gây khó bảo trì.
- **Hỗ trợ đa luồng kích hoạt (Multi-trigger Resilience):** Cung cấp cả cơ chế **Quét chủ động theo lô (`scanAndCreateAlerts`)** phục vụ Cron Job định kỳ đêm hoặc nút bấm trên Dashboard, lẫn cơ chế **Kiểm tra điểm (`checkAndCreateAlert`)** gọi tức thời sau mỗi giao dịch xuất kho làm tụt định mức.
- **Minh bạch số liệu (Audit & Log Friendly):** Trả về DTO tổng kết kết quả quét (`AlertDetectionResultResponse`) với đầy đủ số liệu: tổng số mặt hàng đã quét, số phiếu mới được sinh ra, và số phiếu bỏ qua do đã tồn tại phiếu mở trước đó (Deduplication).

---

## 2. Các Quyết định Thiết kế Đã Chốt (Approved Design Decisions)

| Hạng mục | Quyết định thiết kế | Lý do kiến trúc & Thực tế đồ án |
| :--- | :--- | :--- |
| **Cơ chế kích hoạt** | **Quét chủ động (Batch/Scan) + Kiểm tra điểm (Spot Check)** | Đảm bảo hệ thống vừa quét tự động toàn kho định kỳ (bắt nhầm hơn bỏ sót), vừa phản ứng tức thời ngay khi nhân viên xuất kho làm tụt định mức. |
| **Phạm vi chức năng** | **Tập trung 100% vào phát hiện tụt kho (Single Responsibility)** | Không tự động chuyển `RESOLVED` phiếu cũ trong lần quét này. Giữ cho service nhẹ nhàng, chạy nhanh trên CI và không bị xung đột giao dịch với module Nhập kho. |
| **Dữ liệu trả về** | **DTO Tổng kết (`AlertDetectionResultResponse`)** | Giúp Controller và Job có dữ liệu rõ ràng để ghi log và trả phản hồi JSON cho Frontend (ví dụ: *"Đã quét 50 sản phẩm, tạo mới 2 phiếu cảnh báo, bỏ qua 1 phiếu cũ"*). |
| **Nguồn chân lý (SSOT)** | **Tái sử dụng SQL chuẩn hóa từ T176** | Sử dụng trực tiếp `InventoryLevelRepository.findInventory(..., "LOW_STOCK", ...)` để lấy danh sách tụt kho, bảo đảm 100% không lệch số liệu với màn hình danh sách kho. |

---

## 3. Giao ước Dữ liệu & Từ điển Nghiệp vụ (Data Contracts & Glossary)

### 3.1. DTO Kết quả Quét: `AlertDetectionResultResponse`
```java
public record AlertDetectionResultResponse(
    int totalScanned,          // Tổng số mặt hàng bị tụt kho được quét qua
    int newAlertsCreated,      // Số lượng phiếu cảnh báo OPEN mới được tạo thành công
    int existingAlertsSkipped, // Số lượng bỏ qua do đã có phiếu OPEN/ACKNOWLEDGED (Deduplication)
    LocalDateTime timestamp    // Thời điểm hoàn tất quá trình quét
) {}
```

### 3.2. Service Interface: `InventoryAlertDetectionService`
```java
public interface InventoryAlertDetectionService {
    /**
     * Quét toàn bộ sản phẩm tụt kho tại một kho (hoặc toàn hệ thống nếu warehouseId = null)
     * và tự động tạo phiếu cảnh báo mới cho những sản phẩm chưa có phiếu mở.
     */
    AlertDetectionResultResponse scanAndCreateAlerts(Long warehouseId);

    /**
     * Kiểm tra nhanh và tạo phiếu cảnh báo cho 1 sản phẩm tại 1 kho (Spot Check).
     * @return true nếu có phiếu mới được tạo, false nếu bình thường hoặc bị bỏ qua.
     */
    boolean checkAndCreateAlert(Long productId, Long warehouseId);
}
```

---

## 4. Phân tích Các Trường hợp Biên (Edge Cases Analysis)

1. **Trường hợp kho trống hoặc không có hàng tụt kho:**
   - Hàm `scanAndCreateAlerts` trả về `AlertDetectionResultResponse(0, 0, 0, now())`, không tạo bất kỳ giao dịch DB thừa thãi nào.
2. **Trường hợp sản phẩm có định mức `minStock = null` hoặc `minStock = 0`:**
   - Đã được bảo vệ tuyệt đối ở SQL tầng DB (T176): các sản phẩm này chỉ bị quét ra khi `so_luong <= 0` (`OUT_OF_STOCK`). Nếu số lượng > 0, hệ thống tự động bỏ qua.
3. **Trường hợp sản phẩm đã bị khóa/ngừng hoạt động (Inactive):**
   - Bộ lọc `findInventory` với tham số `productStatus = "HOAT_DONG"`, `warehouseStatus = "HOAT_DONG"` tự động loại bỏ các mặt hàng/kho ngừng hoạt động khỏi danh sách quét.
4. **Trường hợp trùng lặp (Deduplication Check):**
   - Trước khi gọi `save()`, Service kiểm tra qua `inventoryAlertRepository.existsByProductIdAndWarehouseIdAndStatusIn(..., List.of(OPEN, ACKNOWLEDGED))`. Nếu đã có phiếu đang xử lý, hệ thống bỏ qua và tăng biến đếm `existingAlertsSkipped`.
