package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.replenishment.CapacityGuardResult;
import com.smartflow.smestocksensebackend.dto.replenishment.ForecastReplenishmentRecommendationResponse;
import com.smartflow.smestocksensebackend.dto.replenishment.HorizonDemand;
import com.smartflow.smestocksensebackend.dto.replenishment.WarehouseCapacityAvailability;
import com.smartflow.smestocksensebackend.entity.ForecastModelMetadata;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStockConfig;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ForecastModelMetadataRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseStockConfigRepository;
import com.smartflow.smestocksensebackend.service.DailyForecastDemandService;
import com.smartflow.smestocksensebackend.service.EffectiveMinStockResolver;
import com.smartflow.smestocksensebackend.service.ForecastReplenishmentRecommendationService;
import com.smartflow.smestocksensebackend.service.ReorderQuantityCalculator;
import com.smartflow.smestocksensebackend.service.ReplenishmentCapacityGuard;
import com.smartflow.smestocksensebackend.service.WarehouseCapacityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ForecastReplenishmentRecommendationServiceImpl implements ForecastReplenishmentRecommendationService {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryLevelRepository inventoryLevelRepository;
    private final WarehouseStockConfigRepository warehouseStockConfigRepository;
    private final ForecastModelMetadataRepository forecastModelMetadataRepository;
    private final EffectiveMinStockResolver effectiveMinStockResolver;
    private final DailyForecastDemandService dailyForecastDemandService;
    private final ReorderQuantityCalculator reorderQuantityCalculator;
    private final WarehouseCapacityService warehouseCapacityService;
    private final ReplenishmentCapacityGuard replenishmentCapacityGuard;

    @Override
    @Transactional(readOnly = true)
    public ForecastReplenishmentRecommendationResponse getRecommendation(Long productId, Long warehouseId,
            Short horizonDays) {
        return getRecommendation(productId, warehouseId, horizonDays, SalesHistorySource.EXTERNAL_STORE_ITEM);
    }

    @Override
    @Transactional(readOnly = true)
    public ForecastReplenishmentRecommendationResponse getRecommendation(Long productId, Long warehouseId,
            Short horizonDays, SalesHistorySource source) {
        if (horizonDays == null || (horizonDays != 7 && horizonDays != 14 && horizonDays != 30)) {
            throw new BadRequestException("Kỳ dự báo chỉ hỗ trợ 7, 14 hoặc 30 ngày.");
        }
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại."));
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Kho hàng không tồn tại."));
        WarehouseStockConfig config = warehouseStockConfigRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .orElse(null);
        int effectiveMin = effectiveMinStockResolver.resolve(product, config)
                .orElseThrow(() -> new BadRequestException("Chưa cấu hình tồn tối thiểu hiệu lực."));
        int currentStock = inventoryLevelRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .map(InventoryLevel::getQuantity)
                .map(quantity -> Math.max(0, quantity))
                .orElse(0);
        SalesHistorySource effectiveSource = source == null ? SalesHistorySource.EXTERNAL_STORE_ITEM : source;
        ForecastModelMetadata modelMetadata = forecastModelMetadataRepository
                .findFirstByProductIdAndWarehouseIdAndHistorySourceOrderByVersionDesc(
                        productId, warehouseId, effectiveSource)
                .orElseThrow(() -> new BadRequestException("Chưa có mô hình dự báo cho sản phẩm, kho và nguồn dữ liệu."));
        HorizonDemand demand = dailyForecastDemandService
                .sumHorizonDemand(modelMetadata, horizonDays, productId, warehouseId);
        int rawSuggestedQty = reorderQuantityCalculator
                .rawSuggestedQuantity(currentStock, effectiveMin, demand.forecastDemand());
        WarehouseCapacityAvailability availability = warehouseCapacityService
                .getAvailability(warehouseId, product.getUnitVolumeM3());
        CapacityGuardResult guarded = replenishmentCapacityGuard.apply(rawSuggestedQty, availability);

        return new ForecastReplenishmentRecommendationResponse(
                product.getId(),
                product.getCode(),
                product.getName(),
                warehouse.getId(),
                warehouse.getCode(),
                warehouse.getName(),
                horizonDays,
                demand.forecastDemand(),
                currentStock,
                effectiveMin,
                guarded.rawSuggestedQty(),
                guarded.suggestedQty(),
                guarded.capacityLimited(),
                guarded.capacityShortfallQty(),
                guarded.maxAdditionalUnitsByCapacity(),
                guarded.warehouseCapacityM3(),
                guarded.warehouseOccupiedM3(),
                guarded.warehouseAvailableM3(),
                demand.modelMetadataId(),
                demand.modelVersion(),
                guarded.configurationWarning()
        );
    }
}
