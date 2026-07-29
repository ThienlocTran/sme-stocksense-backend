package com.smartflow.smestocksensebackend.event;

import com.smartflow.smestocksensebackend.service.InventoryAlertDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Lắng nghe sự kiện biến động tồn kho (T184).
 *
 * Thiết kế:
 * - @TransactionalEventListener(AFTER_COMMIT): Chỉ kích hoạt SAU KHI giao dịch kho commit thành công.
 *   Đảm bảo cảnh báo không được tạo nếu giao dịch kho bị rollback.
 * - @Async: Chạy ở thread nền, KHÔNG làm chậm luồng nhập/xuất kho của người dùng.
 * - Listener chỉ uỷ quyền cho Service, KHÔNG chứa business logic.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryAlertEventListener {

    private final InventoryAlertDetectionService inventoryAlertDetectionService;

    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onInventoryLevelChanged(InventoryLevelChangedEvent event) {
        // Ghi log để theo dõi khi nào listener được kích hoạt
        log.debug("[T184] Nhận event biến động tồn kho: SP [{}] Kho [{}] {} -> {}",
                event.productId(), event.warehouseId(), event.oldQuantity(), event.newQuantity());

        try {
            // Uỷ quyền toàn bộ business logic xuống Detection Service (Listener không chứa if/else)
            inventoryAlertDetectionService.processInventoryChange(event);
        } catch (RuntimeException ex) {
            // Log lỗi đầy đủ để debug mà không làm chết Async thread
            // Lỗi ở đây KHÔNG ảnh hưởng đến giao dịch kho đã commit (AFTER_COMMIT)
            log.error("[T184] Xử lý event biến động tồn kho thất bại. warehouseId={}, productId={}, oldQty={}, newQty={}",
                    event.warehouseId(), event.productId(), event.oldQuantity(), event.newQuantity(), ex);
        }
    }
}
