package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelProjection;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelResponse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
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
            String stockStatus, String warehouseStatus, String productStatus, Pageable pageable) {
        if (warehouseId != null && !warehouseRepository.existsById(warehouseId)) {
            throw new com.smartflow.smestocksensebackend.exception.NotFoundException("Kho hàng không tồn tại.");
        }
        if (productId != null && !productRepository.existsById(productId)) {
            throw new com.smartflow.smestocksensebackend.exception.NotFoundException("Sản phẩm không tồn tại.");
        }

        String normalizedStockStatus;
        String normalizedWarehouseStatus;
        String normalizedProductStatus;

        try {
            normalizedStockStatus = normalizeStockStatus(stockStatus);
            normalizedWarehouseStatus = normalizeActiveStatus(warehouseStatus);
            normalizedProductStatus = normalizeActiveStatus(productStatus);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        String keywordParam = (keyword == null || keyword.isBlank()) ? null : "%" + keyword.trim() + "%";
        Page<InventoryLevelProjection> result = inventoryLevelRepository.findInventory(warehouseId, productId,
                keywordParam, normalizedStockStatus, normalizedWarehouseStatus, normalizedProductStatus, pageable);

        return result.map(this::mapProjectionToResponse);
    }

    private String normalizeStockStatus(String stockStatus) {
        if (stockStatus == null || stockStatus.isBlank()) {
            return null;
        }

        String normalized = stockStatus.trim().toUpperCase().replace('-', '_');
        return switch (normalized) {
            case "ZERO", "OUT_OF_STOCK", "OUT_OFSTOCK" -> "OUT_OF_STOCK";
            case "LOW", "LOW_STOCK" -> "LOW_STOCK";
            case "HIGH", "OVER_STOCK", "OVERSTOCK" -> "OVER_STOCK";
            case "NORMAL" -> "NORMAL";
            default -> throw new IllegalArgumentException("Trạng thái tồn kho không hợp lệ: " + stockStatus);
        };
    }

    private String normalizeActiveStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return switch (status.trim().toUpperCase()) {
            case "ACTIVE", "HOAT_DONG" -> "HOAT_DONG";
            case "INACTIVE", "NGUNG_HOAT_DONG" -> "NGUNG_HOAT_DONG";
            default -> throw new IllegalArgumentException("Trạng thái hoạt động không hợp lệ: " + status);
        };
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
                projection.getWarehouse(),
                projection.getCurrentQuantity(),
                projection.getMinStock(),
                projection.getMaxStock(),
                projection.getProductStatus(),
                projection.getWarehouseStatus(),
                projection.getStatus(),
                projection.getLastUpdatedAt());
    }
}
