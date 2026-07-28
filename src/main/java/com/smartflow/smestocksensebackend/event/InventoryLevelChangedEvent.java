package com.smartflow.smestocksensebackend.event;

/**
 * Sự kiện biến động tồn kho (T184).
 * Được bắn ra sau khi InventoryLevel đã được cập nhật thành công trong DB.
 *
 * Event Contract:
 * - warehouseId / productId: định danh cặp tồn kho thay đổi.
 * - oldQuantity: số lượng tồn kho trước khi thay đổi.
 * - newQuantity: số lượng tồn kho sau khi thay đổi.
 * - minStock: ngưỡng tồn kho tối thiểu (snapshot tại thời điểm thay đổi).
 */
public record InventoryLevelChangedEvent(
        Long warehouseId,
        Long productId,
        Integer oldQuantity,
        Integer newQuantity,
        Integer minStock
) {}
