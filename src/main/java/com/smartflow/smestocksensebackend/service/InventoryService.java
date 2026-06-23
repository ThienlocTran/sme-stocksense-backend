package com.smartflow.smestocksensebackend.service;

public interface InventoryService {

    /**
     * Core service tăng số lượng tồn kho cho một sản phẩm tại một kho hàng cụ thể.
     * Nếu sản phẩm chưa có tồn kho tại kho này, một bản ghi mới sẽ được tạo (Insert).
     * Nếu đã tồn tại, số lượng sẽ được cộng dồn (Update).
     * Bắt buộc dùng kèm @Transactional ở lớp gọi ngoài cùng.
     *
     * @param productId ID sản phẩm
     * @param warehouseId ID kho hàng
     * @param quantity Số lượng thực nhận cần cộng thêm vào tồn kho
     * @throws com.smartflow.smestocksensebackend.exception.NotFoundException nếu không tìm thấy sản phẩm hoặc kho hàng
     */
    void increaseInventory(Long productId, Long warehouseId, Integer quantity);



    Page<InventoryLevelResponse> listInventory(Long warehouseId, Long productId, String keyword, String stockStatus,
            String warehouseStatus, String productStatus, Pageable pageable);

}
