# T185 Design Spec: Test Backend Cảnh Báo Tồn Kho (Low Stock Alert)

## 1. Mục tiêu
Bổ sung đầy đủ Unit Test cho các Service/Component của module Low Stock Alert (T176–T184) chưa có coverage, đảm bảo toàn bộ business logic có test bảo vệ trước khi kết thúc Sprint 4.

## 2. Phạm vi (Scope)

### Đã có test (KHÔNG viết lại)
| Task | Test file | Coverage |
|------|-----------|----------|
| T180 | `AlertSeverityCalculatorTest.java` | WARNING, CRITICAL, escalation, no de-escalation |
| T183 | `InventoryAlertActionServiceImplTest.java` | ACK success, idempotent, reject RESOLVED, not found |
| T184 | `InventoryAlertDetectionServiceImplT184Test.java` | create, update, auto-resolve, no-op |
| T184 | `InventoryAlertEventListenerTest.java` | delegate, exception isolation |

### Cần viết mới (T185)
| Service/Component | File test mới | Kịch bản cần cover |
|---|---|---|
| `InventoryAlertDetectionServiceImpl` (Batch/Spot) | `InventoryAlertDetectionServiceImplTest.java` | `scanAndCreateAlerts` (empty, found), `checkAndCreateAlert` (low stock, normal stock) |
| `InventoryAlertQueryServiceImpl` | `InventoryAlertQueryServiceImplTest.java` | list with filter (warehouse, severity, status), pagination, empty result |

## 3. Acceptance Criteria

1. **AlertSeverityCalculator**: Không bổ sung test mới. Giữ nguyên coverage đã có từ T180.
2. **DetectionService (Batch)**: `scanAndCreateAlerts(null)` khi không có sản phẩm low stock → `newAlertsCreated = 0`. `scanAndCreateAlerts(warehouseId)` khi có 2 sản phẩm low stock → `newAlertsCreated = 2`.
3. **DetectionService (Spot)**: `checkAndCreateAlert` khi tồn kho bình thường → `false`. Khi tồn kho thiếu → `true`.
4. **QueryService**: Gọi với filter có kết quả → trả về đúng DTO đã map. Gọi với filter không có kết quả → trả về `Page<InventoryAlertResponse>` rỗng (`isEmpty() = true`, `totalElements = 0`).

## 4. Chiến lược Test
- **Tool**: JUnit 5 + Mockito (`@ExtendWith(MockitoExtension.class)`).
- **Tổ chức**: Mỗi class 1 file test riêng biệt.
- **Pattern**: AAA (Arrange / Act / Assert).
- **Giữ nguyên**: Toàn bộ test từ T180, T183, T184 — KHÔNG refactor.
- **Controller**: Không viết Controller test trong T185.

## 5. Coding Plan
- **Bước 1**: Viết `InventoryAlertDetectionServiceImplTest.java` cho luồng Batch (`scanAndCreateAlerts`) và Spot Check (`checkAndCreateAlert`) — 4 test cases.
- **Bước 2**: Viết `InventoryAlertQueryServiceImplTest.java` cho luồng list + filter — 3 test cases.
- **Bước 3**: Chạy `mvnw clean test` để verify toàn bộ test suite pass.
