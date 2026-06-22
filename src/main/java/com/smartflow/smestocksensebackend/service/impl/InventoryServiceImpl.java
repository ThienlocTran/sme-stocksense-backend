package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Lớp triển khai các phương thức nghiệp vụ của InventoryService.
 * Note: Core service thao tác DB tăng tồn kho. Bắt buộc dùng kèm @Transactional ở lớp gọi ngoài cùng.
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryLevelRepository inventoryLevelRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    /**
     * Tăng số lượng tồn kho cho một sản phẩm tại một kho hàng cụ thể.
     * Logic nghiệp vụ:
     * 0. Validate đầu vào: productId, warehouseId, quantity phải > 0.
     * 1. Xác thực xem sản phẩm và kho hàng có tồn tại trong hệ thống hay không.
     * 2. Nếu không tìm thấy -> Ném NotFoundException để kích hoạt rollback transaction.
     * 3. Tìm kiếm bản ghi tồn kho hiện tại (InventoryLevel) với Pessimistic Lock.
     * 4. Nếu chưa tồn tại -> Tạo mới bản ghi với số lượng = quantity (Insert).
     *    - Bắt DataIntegrityViolationException phòng race condition: query lại rồi cộng dồn.
     * 5. Nếu đã tồn tại -> Cộng dồn quantity vào số lượng hiện tại (Update).
     */
    @Override
    @Transactional
    public void increaseInventory(Long productId, Long warehouseId, Integer quantity) {
        // 0. Validate đầu vào
        if (productId == null || productId <= 0 || warehouseId == null || warehouseId <= 0 || quantity == null || quantity <= 0) {
            throw new IllegalArgumentException("productId, warehouseId, quantity phải > 0");
        }

        // 1. Kiểm tra sự tồn tại của sản phẩm
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại."));

        // 2. Kiểm tra sự tồn tại của kho hàng
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Kho hàng không tồn tại."));

        // 3. Tìm bản ghi tồn kho hiện tại với Pessimistic Write Lock
        Optional<InventoryLevel> inventoryLevelOpt = inventoryLevelRepository.findByProductIdAndWarehouseIdForUpdate(productId, warehouseId);

        if (inventoryLevelOpt.isEmpty()) {
            // 4. Nếu chưa có bản ghi tồn kho -> Tạo mới (Insert)
            InventoryLevel newInventory = new InventoryLevel();
            newInventory.setProduct(product);
            newInventory.setWarehouse(warehouse);
            newInventory.setQuantity(quantity);
            try {
                inventoryLevelRepository.saveAndFlush(newInventory);
            } catch (DataIntegrityViolationException e) {
                // Race condition: bản ghi đã được tạo bởi transaction khác -> query lại và cộng dồn
                InventoryLevel locked = inventoryLevelRepository.findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                        .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại."));
                locked.setQuantity(locked.getQuantity() + quantity);
                inventoryLevelRepository.saveAndFlush(locked);
            }
        } else {
            // 5. Nếu đã tồn tại -> Cập nhật cộng dồn (Update)
            InventoryLevel existingInventory = inventoryLevelOpt.get();
            existingInventory.setQuantity(existingInventory.getQuantity() + quantity);
            inventoryLevelRepository.saveAndFlush(existingInventory);
        }
    }
}
