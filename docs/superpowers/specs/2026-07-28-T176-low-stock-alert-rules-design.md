# Spec: Chuẩn Hóa Rule Cảnh Báo Tồn Kho Thấp (Task T176) - 10/10 Enterprise Edition

**Ngày tạo:** 2026-07-28  
**Tác giả:** Antigravity (AI Assistant) & User  
**Dự án:** SME StockSense Backend (Sprint 4 - Low Stock Alert)  
**Trạng thái:** Đã thẩm định theo chuẩn Enterprise (BA/PO/PM/Architecture/QA) - Chờ duyệt Spec

---

## 1. Bối cảnh, Mục tiêu & Giá trị Nghiệp vụ (Background, Objective & Business Value)
Trong Sprint 4 (Xây dựng hệ thống cảnh báo tồn kho thấp - Low Stock Alert), **Task T176** đóng vai trò nền tảng định nghĩa chuẩn xác toàn bộ tiêu chuẩn, công thức phân loại, độ ưu tiên và các điều kiện biên cho tồn kho.

### Giá trị Nghiệp vụ (Business Value - PO Perspective)
- **Giảm thiểu tối đa nguy cơ đứt gãy chuỗi cung ứng (Reduce Stockouts):** Phát hiện sớm các mặt hàng chạm ngưỡng tối thiểu hoặc đã hết hàng để kích hoạt quy trình mua sắm bổ sung (Replenishment).
- **Tối ưu hóa vốn lưu động (Optimize Working Capital):** Tránh nhập thừa thãi hàng hóa không cần thiết, giúp quản lý kho tập trung đúng vào nhóm mặt hàng có rủi ro cao.
- **Tăng năng suất vận hành kho (Operational Efficiency):** Loại bỏ tiếng ồn cảnh báo từ các mặt hàng/kho ngưng hoạt động, cung cấp cho quản lý kho một màn hình theo dõi duy nhất, chính xác 100%.

---

## 2. Từ điển Nghiệp vụ (Business Glossary)

| Thuật ngữ | Định nghĩa nghiệp vụ trong SME StockSense |
| :--- | :--- |
| **Inventory Level** | Số lượng tồn kho thực tế (`so_luong`) của **một sản phẩm tại một kho cụ thể** (cặp `Product + Warehouse`), không phải tổng tồn trên toàn hệ thống. |
| **Physical vs Reserved Stock** | Trong phạm vi đồ án SME StockSense hiện tại, số lượng tính toán cảnh báo dựa trên **Physical Stock** (`so_luong` hiện có trong bảng `ton_kho`). |
| **Out Of Stock (OOS)** | Tình trạng hết hàng hoàn toàn (`so_luong <= 0`, bao gồm cả tồn kho bị âm do chênh lệch kiểm kê/lỗi dữ liệu). Mức độ nghiêm trọng: **Critical**. |
| **Low Stock (LOW)** | Tình trạng sắp hết hàng, cần lên kế hoạch nhập bổ sung. Xảy ra khi tồn kho nằm trong ngưỡng cảnh báo (`0 < so_luong <= minStock`). Mức độ nghiêm trọng: **Warning**. |
| **Over Stock (OVER)** | Tình trạng vượt định mức lưu trữ tối đa (`so_luong > maxStock`). Dùng để phân loại báo cáo, **không thuộc phạm vi cảnh báo Sprint 4**. |
| **minStock = NULL vs 0** | • `NULL`: **Not Configured** (Chưa thiết lập ngưỡng tối thiểu).<br>• `0`: **Disabled** (Chủ động tắt cảnh báo sắp hết hàng).<br>👉 Cả hai trường hợp đều **KHÔNG** kích hoạt cảnh báo `LOW_STOCK` (chỉ báo `OUT_OF_STOCK` khi `<= 0`). |
| **RBAC (Access Rights)** | Tái sử dụng hệ thống phân quyền hiện tại: Các role **ADMIN, MANAGER, EMPLOYEE** đều có quyền xem danh sách cảnh báo tồn kho (dựa trên `@PreAuthorize` hiện có). |

---

## 3. User Story & Acceptance Criteria (Given / When / Then)

### User Story
> **As a** Warehouse Manager / Inventory Controller  
> **I want to** view a consolidated list of low-stock and out-of-stock items filtered by active products and warehouses  
> **So that** I can prioritize timely replenishment orders and prevent stockouts without being distracted by false alarms from inactive items.

### Acceptance Criteria

* **AC 1: Phát hiện đúng mặt hàng Sắp hết hàng (Low Stock - Warning)**
  * **Given** sản phẩm A và kho HCM đều đang `HOAT_DONG`, có `minStock = 10`, `so_luong = 5`
  * **When** hệ thống đánh giá trạng thái tồn kho (hoặc gọi `GET /api/inventory/low-stock`)
  * **Then** sản phẩm A tại kho HCM được phân loại là `LOW_STOCK`, có severity là `Warning`, và xuất hiện trong danh sách cảnh báo.

* **AC 2: Phát hiện mặt hàng Hết hàng và Tồn kho âm (Out of Stock - Critical)**
  * **Given** sản phẩm B tại kho HN có `so_luong = 0` (hoặc `-2` do chênh lệch kiểm kê)
  * **When** hệ thống đánh giá trạng thái tồn kho
  * **Then** sản phẩm B được phân loại là `OUT_OF_STOCK` với severity `Critical` và nằm trong danh sách cảnh báo tồn kho thấp.

* **AC 3: Bỏ qua mặt hàng chưa cấu hình ngưỡng (`minStock = NULL` hoặc `0`)**
  * **Given** sản phẩm C có `minStock = 0` (hoặc `NULL`), `so_luong = 1`
  * **When** hệ thống đánh giá trạng thái tồn kho
  * **Then** sản phẩm C được phân loại là `NORMAL` (không phải `LOW_STOCK`) và KHÔNG xuất hiện trong danh sách cảnh báo.

* **AC 4: Loại bỏ rác cảnh báo từ Sản phẩm/Kho ngưng hoạt động hoặc chưa có dữ liệu tồn**
  * **Given** sản phẩm D có `so_luong = 0`, `minStock = 10` nhưng trạng thái kho hoặc sản phẩm là `NGUNG_HOAT_DONG` (hoặc sản phẩm chưa từng có bản ghi trong bảng `ton_kho`)
  * **When** gọi API lấy danh sách cảnh báo tồn kho (`GET /api/inventory/low-stock`)
  * **Then** sản phẩm D KHÔNG được trả về trong danh sách cảnh báo.

---

## 4. Quy tắc Nghiệp vụ & Bảng Quyết định (Decision Table)

### Rule 1: Thứ tự Đánh giá Trạng thái Tồn kho (Evaluation Order per Product + Warehouse)
Đánh giá theo thứ tự ưu tiên tuyệt đối từ trên xuống dưới cho từng cặp `(san_pham_id, kho_id)`:
```text
[1] OUT_OF_STOCK  (qty <= 0)                      --> Severity: CRITICAL
       ↓ (Nếu qty > 0)
[2] LOW_STOCK     (min IS NOT NULL AND min > 0 AND qty <= min) --> Severity: WARNING
       ↓ (Nếu qty > min)
[3] OVER_STOCK    (max IS NOT NULL AND max > 0 AND qty > max)  --> Severity: NONE (Out of alert scope)
       ↓
[4] NORMAL        (Các trường hợp còn lại)         --> Severity: NONE
```

### Rule 2: Bảng Quyết định (Decision Table - BA/Architecture Perspective)
Bảng dưới đây tổng hợp toàn bộ tổ hợp điều kiện để QA và Dev đối chiếu 1-1:

| Qty (`so_luong`) | `minStock` | `maxStock` | Trạng thái (`status`) | Mức độ (`severity`) | Có hiển thị trên `/low-stock`? | Ghi chú nghiệp vụ |
| :---: | :---: | :---: | :---: | :---: | :---: | :--- |
| `< 0` (âm) | Bất kỳ | Bất kỳ | **`OUT_OF_STOCK`** | **Critical** | **Có** | Lỗi kiểm kê / xuất vượt tồn |
| `0` | Bất kỳ | Bất kỳ | **`OUT_OF_STOCK`** | **Critical** | **Có** | Hết hàng hoàn toàn |
| `1` | `0` hoặc `NULL` | Bất kỳ | **`NORMAL`** | None | **Không** | Tắt/Chưa cấu hình minStock -> Bỏ qua |
| `10` | `10` | `100` | **`LOW_STOCK`** | **Warning** | **Có** | Chạm đúng ngưỡng minStock |
| `11` | `10` | `100` | **`NORMAL`** | None | **Không** | Nằm trong ngưỡng an toàn |
| `100` | `10` | `100` | **`NORMAL`** | None | **Không** | Đạt đúng định mức tối đa (chưa tràn) |
| `101` | `10` | `100` | **`OVER_STOCK`** | None | **Không** | Vượt định mức tối đa |

---

## 5. Phạm vi Tác động Kỹ thuật & Kiến trúc (Technical & Architecture Scope)

### 1. Single Source of Truth (SSOT) & DRY Architecture
- **Single Source of Truth:** Logic phân loại trạng thái tồn kho (`status`) được xác định **duy nhất tại tầng Database SQL (`InventoryLevelRepository`)**. Không tính toán lại hay ghi đè logic ở tầng Java Service/Controller để đảm bảo nhất quán 100%.
- **DRY (Don't Repeat Yourself) Strategy:** Để tránh vi phạm lỗi lặp lại code (`smell`) khi cùng một biểu thức `CASE WHEN` bị copy ở nhiều query (`findInventory`, `countQuery`, `replenishment`), tại T176 chúng ta chuẩn hóa biểu thức SQL thành một constant/nguyên tắc cố định. Trong tương lai (doanh nghiệp mở rộng), có thể chuyển thành **PostgreSQL SQL View (`inventory_status_view`)** hoặc **PostgreSQL Function (`calculate_inventory_status()`)**.

### 2. Chuẩn hóa Câu SQL trong `InventoryLevelRepository.java`
Sử dụng cú pháp tường minh với điều kiện rõ ràng:
```sql
CASE 
  WHEN t.so_luong <= 0 THEN 'OUT_OF_STOCK'
  WHEN sp.ton_toi_thieu IS NOT NULL AND sp.ton_toi_thieu > 0 AND t.so_luong <= sp.ton_toi_thieu THEN 'LOW_STOCK'
  WHEN sp.ton_toi_da IS NOT NULL AND sp.ton_toi_da > 0 AND t.so_luong > sp.ton_toi_da THEN 'OVER_STOCK'
  ELSE 'NORMAL' 
END AS "status"
```

### 3. Ma trận Kiểm thử Sát ngưỡng (Boundary Testing Matrix - QA/Dev)

| Test Case ID | `so_luong` | `minStock` | `maxStock` | Trạng thái mong đợi | Mục tiêu kiểm thử |
| :--- | :---: | :---: | :---: | :---: | :--- |
| **TC-01 (Negative)** | `-5` | `10` | `100` | `OUT_OF_STOCK` | Kiểm tra tồn kho âm -> Critical |
| **TC-02 (Zero OOS)** | `0` | `0` / `NULL` | `100` | `OUT_OF_STOCK` | Kiểm tra tồn = 0 khi không set min |
| **TC-03 (Disabled)** | `1` | `0` / `NULL` | `100` | `NORMAL` | Kiểm tra min=0/NULL không trigger LOW |
| **TC-04 (Equality 1)** | `1` | `1` | `10` | `LOW_STOCK` | Kiểm tra biên minStock = 1, qty = 1 |
| **TC-05 (Exact min)**| `10` | `10` | `100` | `LOW_STOCK` | Kiểm tra chạm đúng ngưỡng min |
| **TC-06 (Above min)**| `11` | `10` | `100` | `NORMAL` | Kiểm tra vừa vượt min 1 đơn vị |
| **TC-07 (Exact max)**| `100` | `10` | `100` | `NORMAL` | Kiểm tra chạm đúng ngưỡng max |
| **TC-08 (Over max)** | `101` | `10` | `100` | `OVER_STOCK` | Kiểm tra vượt max 1 đơn vị (`>`) |
| **TC-09 (Max Int)**  | `2147483647`| `10` | `100` | `OVER_STOCK` | Kiểm tra giá trị integer lớn nhất |
| **TC-10 (Inactive)** | `5` | `10` | `100` | *Excluded* | Kho/SP `NGUNG_HOAT_DONG` -> Bỏ qua |

---

## 6. Ngoài phạm vi (Out of Scope)
Các tính năng sau đây được xác định rõ là **KHÔNG** thuộc phạm vi của Task T176 và Sprint 4 hiện tại để kiểm soát chặt chẽ scope:
- **Push Notification / Email Alert**: Gửi email hay thông báo thời gian thực khi tụt kho.
- **Background Scheduler / Cron Jobs**: Tự động quét định kỳ qua job ngầm.
- **Demand Forecasting**: Dự báo nhu cầu nhập hàng dựa trên tốc độ tiêu thụ (Velocity).
- **Auto Purchase Order**: Tự động sinh phiếu đặt hàng nhà cung cấp khi chạm ngưỡng.

---

## 7. Quản lý Rủi ro, Ước tính Effort & Kế hoạch Rollback (PM Perspective)

### 1. Ma trận Rủi ro (Risk Matrix)

| Rủi ro (Risk Description) | Xác suất (Probability) | Tác động (Impact) | Giải pháp Giảm thiểu (Mitigation Strategy) |
| :--- | :---: | :---: | :--- |
| **Ảnh hưởng hiển thị Dashboard cũ:** Sửa `CASE WHEN` có thể làm lệch báo cáo tồn kho tổng quan. | Thấp (Low) | Trung bình (Medium) | • Giữ nguyên logic phổ thông, chỉ chuẩn hóa các nhánh biên.<br>• Chạy full test suite (`mvnw test`) trên toàn bộ module liên quan. |
| **Hiểu nhầm giữa `>` và `>=` cho maxStock:** Dev có thể nhầm lẫn với rule cũ. | Thấp (Low) | Thấp (Low) | • Document rõ ràng Decision Table.<br>• Thêm test case TC-07 & TC-08 khóa cứng contract. |

### 2. Ước tính Effort (Timeline & Effort Estimate)
- **Repository / SQL Refactoring:** 0.5 day
- **Service Logic & Verification:** 0.5 day
- **Unit Testing (Boundary Matrix):** 0.5 day
- **Code Review & Integration:** 0.5 day  
👉 **Tổng Effort ước tính:** **2.0 days** (rất an toàn cho Sprint 4).

### 3. Kế hoạch Rollback (Rollback Plan)
- **No DB Migration:** Task này hoàn toàn không thay đổi Cấu trúc Database hay API Contract.
- **Instant Rollback:** Nếu phát hiện sự cố trên staging/production, chỉ cần chạy lệnh `git revert <commit-hash>` và re-deploy ứng dụng mà không gây tác dụng phụ tới dữ liệu.

---

## 8. Tác động Hệ thống (System Impact)

- **Backward Compatibility (Tính tương thích ngược):** 100% tương thích. Frontend (`DATN_FE`) không cần sửa đổi bất kỳ dòng code nào. Cấu trúc JSON Response (`status`, `currentQuantity`, `minStock`) được bảo toàn trọn vẹn.
- **Performance Impact:** Zero Overhead. Tận dụng tối đa index hiện có trên `san_pham_id` và `kho_id`, không tạo thêm N+1 query hay full table scan.

---

## 9. Nguyên tắc Thiết kế (Design Principles - Architecture)
1. **Single Source of Truth (SSOT):** SQL Repository là nơi duy nhất phân loại trạng thái.
2. **Strict Evaluation Order:** Ưu tiên từ Critical (`OUT_OF_STOCK`) -> Warning (`LOW_STOCK`) -> Info (`OVER_STOCK`) -> Normal.
3. **No Duplicate Logic:** Khóa cứng công thức chuẩn, tạo tiền đề cho SQL View/Function trong tương lai.
4. **Stateless & Backward Compatible:** Đảm bảo API không phụ thuộc trạng thái phiên, tương thích hoàn toàn với client hiện hữu.

---

## 10. Tiêu chí Hoàn thành (Definition of Done - DoD)
- [x] Spec 10/10 Enterprise Edition được thẩm định và phê duyệt bởi User.
- [ ] Tạo file Kế hoạch Triển khai (Implementation Plan via `@writing-plans`) và được User duyệt.
- [ ] Refactor SQL query trong `InventoryLevelRepository` theo đúng Rule 1 & Rule 2.
- [ ] Bổ sung đầy đủ 10 Test Cases trong ma trận kiểm thử vào Unit Test.
- [ ] Chạy lệnh `mvnw clean compile` và `mvnw test` đạt 100% BUILD SUCCESS.
- [ ] Viết tài liệu tổng kết API `docs/README_T176.md` theo quy định của AGENTS.md.
