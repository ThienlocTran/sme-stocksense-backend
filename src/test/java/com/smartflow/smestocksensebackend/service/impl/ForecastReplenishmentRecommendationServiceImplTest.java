package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.replenishment.HorizonDemand;
import com.smartflow.smestocksensebackend.dto.replenishment.WarehouseCapacityAvailability;
import com.smartflow.smestocksensebackend.entity.ForecastModelMetadata;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStockConfig;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.ForecastModelMetadataRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseStockConfigRepository;
import com.smartflow.smestocksensebackend.service.DailyForecastDemandService;
import com.smartflow.smestocksensebackend.service.EffectiveMinStockResolver;
import com.smartflow.smestocksensebackend.service.ReorderQuantityCalculator;
import com.smartflow.smestocksensebackend.service.ReplenishmentCapacityGuard;
import com.smartflow.smestocksensebackend.service.WarehouseCapacityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastReplenishmentRecommendationServiceImplTest {

    @Mock ProductRepository productRepository;
    @Mock WarehouseRepository warehouseRepository;
    @Mock InventoryLevelRepository inventoryLevelRepository;
    @Mock WarehouseStockConfigRepository warehouseStockConfigRepository;
    @Mock ForecastModelMetadataRepository forecastModelMetadataRepository;
    @Mock DailyForecastDemandService dailyForecastDemandService;
    @Mock WarehouseCapacityService warehouseCapacityService;

    EffectiveMinStockResolver effectiveMinStockResolver = new EffectiveMinStockResolver();
    ReorderQuantityCalculator reorderQuantityCalculator = new ReorderQuantityCalculator();
    ReplenishmentCapacityGuard replenishmentCapacityGuard = new ReplenishmentCapacityGuard();

    private ForecastReplenishmentRecommendationServiceImpl service() {
        return new ForecastReplenishmentRecommendationServiceImpl(productRepository, warehouseRepository,
                inventoryLevelRepository, warehouseStockConfigRepository, forecastModelMetadataRepository,
                effectiveMinStockResolver, dailyForecastDemandService, reorderQuantityCalculator,
                warehouseCapacityService, replenishmentCapacityGuard);
    }

    @Test
    void sevenDayRecommendationUsesForecastStockMinAndCapacity() {
        stubBase((short) 7, new BigDecimal("50"), 20, 10, 100);

        var result = service().getRecommendation(10L, 20L, (short) 7);

        assertEquals(new BigDecimal("50"), result.forecastDemand());
        assertEquals(20, result.currentStock());
        assertEquals(10, result.effectiveMinStock());
        assertEquals(40, result.rawSuggestedQty());
        assertEquals(40, result.suggestedQty());
        assertFalse(result.capacityLimited());
        assertEquals(30L, result.modelMetadataId());
        assertEquals(2, result.modelVersion());
    }

    @Test
    void fourteenDayRecommendationUsesRequestedHorizon() {
        stubBase((short) 14, new BigDecimal("80"), 20, 10, 100);

        var result = service().getRecommendation(10L, 20L, (short) 14);

        assertEquals((short) 14, result.horizonDays());
        assertEquals(70, result.rawSuggestedQty());
    }

    @Test
    void thirtyDayRecommendationUsesRequestedHorizon() {
        stubBase((short) 30, new BigDecimal("120"), 20, 10, 100);

        var result = service().getRecommendation(10L, 20L, (short) 30);

        assertEquals((short) 30, result.horizonDays());
        assertEquals(110, result.rawSuggestedQty());
    }

    @Test
    void recommendationCanBeScopedToSelectedSource() {
        Product product = product(10);
        Warehouse warehouse = warehouse();
        ForecastModelMetadata model = model(product, warehouse);
        InventoryLevel inventory = new InventoryLevel();
        inventory.setQuantity(20);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(warehouseStockConfigRepository.findByProductIdAndWarehouseId(10L, 20L)).thenReturn(Optional.empty());
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(10L, 20L)).thenReturn(Optional.of(inventory));
        when(forecastModelMetadataRepository.findFirstByProductIdAndWarehouseIdAndHistorySourceOrderByVersionDesc(
                10L, 20L, SalesHistorySource.SEED_DEMO))
                .thenReturn(Optional.of(model));
        when(dailyForecastDemandService.sumHorizonDemand(model, (short) 7, 10L, 20L))
                .thenReturn(new HorizonDemand(30L, 2, (short) 7, new BigDecimal("50")));
        when(warehouseCapacityService.getAvailability(20L, product.getUnitVolumeM3()))
                .thenReturn(new WarehouseCapacityAvailability(new BigDecimal("100.000"), new BigDecimal("20.000"),
                        new BigDecimal("80.000"), 100, null));

        var result = service().getRecommendation(10L, 20L, (short) 7, SalesHistorySource.SEED_DEMO);

        assertEquals(30L, result.modelMetadataId());
        verify(forecastModelMetadataRepository).findFirstByProductIdAndWarehouseIdAndHistorySourceOrderByVersionDesc(
                10L, 20L, SalesHistorySource.SEED_DEMO);
        verify(forecastModelMetadataRepository, never()).findFirstByProductIdAndWarehouseIdOrderByVersionDesc(10L, 20L);
    }

    @Test
    void currentStockScopedToRequestedWarehouseAndOverrideMinWins() {
        WarehouseStockConfig config = WarehouseStockConfig.builder().minStockOverride(25).build();
        stubBase((short) 7, new BigDecimal("50"), 30, 25, 100, config);

        var result = service().getRecommendation(10L, 20L, (short) 7);

        assertEquals(30, result.currentStock());
        assertEquals(25, result.effectiveMinStock());
        assertEquals(45, result.rawSuggestedQty());
        verify(inventoryLevelRepository).findByProductIdAndWarehouseId(10L, 20L);
    }

    @Test
    void capacityCappedRecommendationShowsShortfall() {
        stubBase((short) 7, new BigDecimal("500"), 20, 10, 320);

        var result = service().getRecommendation(10L, 20L, (short) 7);

        assertEquals(490, result.rawSuggestedQty());
        assertEquals(320, result.suggestedQty());
        assertTrue(result.capacityLimited());
        assertEquals(170, result.capacityShortfallQty());
    }

    @Test
    void unsupportedHorizonRejected() {
        assertThrows(BadRequestException.class, () -> service().getRecommendation(10L, 20L, (short) 21));
    }

    @Test
    void noInventoryMutationOrAiAssignmentCreation() throws Exception {
        stubBase((short) 7, new BigDecimal("50"), 20, 10, 100);

        service().getRecommendation(10L, 20L, (short) 7);

        verify(inventoryLevelRepository, never()).save(org.mockito.ArgumentMatchers.any());
        assertThrows(NoSuchFieldException.class,
                () -> ForecastReplenishmentRecommendationServiceImpl.class.getDeclaredField("aiPurchaseRequestRepository"));
        assertThrows(NoSuchFieldException.class,
                () -> ForecastReplenishmentRecommendationServiceImpl.class.getDeclaredField("importReceiptRepository"));
    }

    private void stubBase(Short horizon, BigDecimal demand, int currentStock, int defaultMinStock, int maxUnits) {
        stubBase(horizon, demand, currentStock, defaultMinStock, maxUnits, null);
    }

    private void stubBase(Short horizon, BigDecimal demand, int currentStock, int defaultMinStock, int maxUnits,
            WarehouseStockConfig config) {
        Product product = product(defaultMinStock);
        Warehouse warehouse = warehouse();
        ForecastModelMetadata model = model(product, warehouse);
        InventoryLevel inventory = new InventoryLevel();
        inventory.setQuantity(currentStock);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(20L)).thenReturn(Optional.of(warehouse));
        when(warehouseStockConfigRepository.findByProductIdAndWarehouseId(10L, 20L)).thenReturn(Optional.ofNullable(config));
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(10L, 20L)).thenReturn(Optional.of(inventory));
        when(forecastModelMetadataRepository.findFirstByProductIdAndWarehouseIdAndHistorySourceOrderByVersionDesc(
                10L, 20L, SalesHistorySource.EXTERNAL_STORE_ITEM))
                .thenReturn(Optional.of(model));
        when(dailyForecastDemandService.sumHorizonDemand(model, horizon, 10L, 20L))
                .thenReturn(new HorizonDemand(30L, 2, horizon, demand));
        when(warehouseCapacityService.getAvailability(20L, product.getUnitVolumeM3()))
                .thenReturn(new WarehouseCapacityAvailability(new BigDecimal("100.000"), new BigDecimal("20.000"),
                        new BigDecimal("80.000"), maxUnits, null));
    }

    private Product product(int defaultMinStock) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", 10L);
        product.setCode("SP001");
        product.setName("Laptop");
        product.setDefaultMinStock(defaultMinStock);
        product.setUnitVolumeM3(new BigDecimal("1.000"));
        return product;
    }

    private Warehouse warehouse() {
        Warehouse warehouse = new Warehouse();
        ReflectionTestUtils.setField(warehouse, "id", 20L);
        warehouse.setCode("K001");
        warehouse.setName("Kho chinh");
        return warehouse;
    }

    private ForecastModelMetadata model() {
        return model(product(10), warehouse());
    }

    private ForecastModelMetadata model(Product product, Warehouse warehouse) {
        ForecastModelMetadata model = new ForecastModelMetadata();
        ReflectionTestUtils.setField(model, "id", 30L);
        model.setVersion(2);
        model.setProduct(product);
        model.setWarehouse(warehouse);
        return model;
    }
}
