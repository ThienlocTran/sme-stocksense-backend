package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelProjection;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelResponse;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryLevelRepository inventoryLevelRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryLevelResponse> listInventory(Long warehouseId, Long productId, String keyword,
            String stockStatus, Pageable pageable) {
        if (warehouseId != null && !warehouseRepository.existsById(warehouseId)) {
            throw new com.smartflow.smestocksensebackend.exception.NotFoundException("Kho hàng không tồn tại.");
        }
        if (productId != null && !productRepository.existsById(productId)) {
            throw new com.smartflow.smestocksensebackend.exception.NotFoundException("Sản phẩm không tồn tại.");
        }

        String keywordParam = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        Page<InventoryLevelProjection> result = inventoryLevelRepository.findInventory(warehouseId, productId,
                keywordParam, stockStatus, pageable);

        return result.map(this::mapProjectionToResponse);
    }

    private InventoryLevelResponse mapProjectionToResponse(InventoryLevelProjection projection) {
        return new InventoryLevelResponse(
                projection.getInventoryId(),
                projection.getProductId(),
                projection.getProductCode(),
                projection.getProductName(),
                projection.getBarcode(),
                projection.getWarehouseId(),
                projection.getWarehouseCode(),
                projection.getWarehouseName(),
                projection.getQuantity(),
                projection.getMinStock(),
                projection.getMaxStock(),
                projection.getProductStatus(),
                projection.getWarehouseStatus(),
                projection.getStockStatus(),
                projection.getLastUpdatedAt());
    }
}
