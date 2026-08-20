package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.entity.WarehouseStockConfig;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseStockConfigRepository;
import com.smartflow.smestocksensebackend.service.EffectiveMinStockResolver;
import com.smartflow.smestocksensebackend.service.InventoryAlertDetectionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/warehouse-stock-configs")
@RequiredArgsConstructor
public class WarehouseStockConfigController {

    private final WarehouseStockConfigRepository warehouseStockConfigRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final EffectiveMinStockResolver effectiveMinStockResolver;
    private final InventoryAlertDetectionService inventoryAlertDetectionService;

    public record SaveConfigRequest(
            @NotNull(message = "productId không được để trống.")
            Long productId,
            @NotNull(message = "warehouseId không được để trống.")
            Long warehouseId,
            @Min(value = 0, message = "minStockOverride phải lớn hơn hoặc bằng 0.")
            Integer minStockOverride
    ) {}

    public record ConfigResponse(
            Long productId,
            Long warehouseId,
            Integer defaultMinStock,
            Integer minStockOverride,
            Integer effectiveMinStock,
            boolean usesDefault
    ) {}

    @RequestMapping(method = {RequestMethod.POST, RequestMethod.PUT, RequestMethod.PATCH})
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ConfigResponse saveConfig(@RequestBody @Valid SaveConfigRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại."));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new NotFoundException("Kho hàng không tồn tại."));
        if (product.getStatus() != ProductStatus.HOAT_DONG) {
            throw new BadRequestException("Sản phẩm không hoạt động.");
        }
        if (warehouse.getStatus() != WarehouseStatus.HOAT_DONG) {
            throw new BadRequestException("Kho hàng không hoạt động.");
        }
        WarehouseStockConfig config = warehouseStockConfigRepository
                .findByProductIdAndWarehouseId(request.productId(), request.warehouseId())
                .orElseGet(() -> {
                    return WarehouseStockConfig.builder()
                            .product(product)
                            .warehouse(warehouse)
                            .build();
                });
        config.setProduct(product);
        config.setWarehouse(warehouse);
        config.setMinStockOverride(request.minStockOverride());
        WarehouseStockConfig saved = warehouseStockConfigRepository.save(config);
        inventoryAlertDetectionService.reevaluate(request.productId(), request.warehouseId());
        return toResponse(saved);
    }

    private ConfigResponse toResponse(WarehouseStockConfig config) {
        Integer effective = effectiveMinStockResolver.resolve(config.getProduct(), config).orElse(null);
        return new ConfigResponse(
                config.getProduct().getId(),
                config.getWarehouse().getId(),
                config.getProduct().getDefaultMinStock(),
                config.getMinStockOverride(),
                effective,
                config.getMinStockOverride() == null
        );
    }
}
