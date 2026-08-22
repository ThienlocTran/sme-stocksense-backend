package com.smartflow.smestocksensebackend.service.impl;

import com.fasterxml.jackson.databind.json.JsonMapper;
import com.smartflow.smestocksensebackend.dto.forecast.AiForecastClientRequest;
import com.smartflow.smestocksensebackend.dto.forecast.AiForecastClientResult;
import com.smartflow.smestocksensebackend.dto.forecast.DriftResponse;
import com.smartflow.smestocksensebackend.dto.forecast.ForecastAvailabilityResponse;
import com.smartflow.smestocksensebackend.dto.forecast.ForecastResponse;
import com.smartflow.smestocksensebackend.dto.inventory.DailyQuantityProjection;
import com.smartflow.smestocksensebackend.entity.DailyForecastResult;
import com.smartflow.smestocksensebackend.entity.ForecastDatasetType;
import com.smartflow.smestocksensebackend.entity.ForecastMode;
import com.smartflow.smestocksensebackend.entity.ForecastModelMetadata;
import com.smartflow.smestocksensebackend.entity.ForecastResult;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.SalesHistory;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ForecastDriftLogRepository;
import com.smartflow.smestocksensebackend.repository.ForecastModelMetadataRepository;
import com.smartflow.smestocksensebackend.repository.ForecastResultRepository;
import com.smartflow.smestocksensebackend.repository.DailyForecastResultRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.InventoryTransactionRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.SalesHistoryRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ForecastServiceImplTest {

    @Mock
    ProductRepository productRepository;
    @Mock
    WarehouseRepository warehouseRepository;
    @Mock
    InventoryLevelRepository inventoryLevelRepository;
    @Mock
    InventoryTransactionRepository inventoryTransactionRepository;
    @Mock
    SalesHistoryRepository salesHistoryRepository;
    @Mock
    ForecastResultRepository forecastResultRepository;
    @Mock
    DailyForecastResultRepository dailyForecastResultRepository;
    @Mock
    ForecastModelMetadataRepository forecastModelMetadataRepository;
    @Mock
    ForecastDriftLogRepository forecastDriftLogRepository;
    @Mock
    RestClient aiServiceRestClient;
    @Mock
    com.smartflow.smestocksensebackend.repository.WarehouseStockConfigRepository warehouseStockConfigRepository;
    @Mock
    com.smartflow.smestocksensebackend.service.WarehouseCapacityService warehouseCapacityService;
    com.smartflow.smestocksensebackend.service.EffectiveMinStockResolver effectiveMinStockResolver;

    ForecastServiceImpl service;

    Product product;
    Warehouse warehouse;

    @BeforeEach
    void setUp() {
        service = new ForecastServiceImpl(productRepository, warehouseRepository, inventoryLevelRepository,
                inventoryTransactionRepository, salesHistoryRepository, forecastResultRepository,
                dailyForecastResultRepository, forecastModelMetadataRepository, forecastDriftLogRepository,
                aiServiceRestClient);
        service.warehouseStockConfigRepository = warehouseStockConfigRepository;
        service.warehouseCapacityService = warehouseCapacityService;
        service.effectiveMinStockResolver = effectiveMinStockResolver = new com.smartflow.smestocksensebackend.service.EffectiveMinStockResolver();

        product = new Product();
        product.setId(1L);
        product.setPrice(new BigDecimal("10000"));
        product.setDefaultMinStock(10);

        warehouse = new Warehouse();
        warehouse.setId(2L);

        com.smartflow.smestocksensebackend.entity.WarehouseStockConfig config = com.smartflow.smestocksensebackend.entity.WarehouseStockConfig.builder()
                .product(product)
                .warehouse(warehouse)
                .minStockOverride(50)
                .build();
        org.mockito.Mockito.lenient().when(warehouseStockConfigRepository.findByProductIdAndWarehouseId(1L, 2L))
                .thenReturn(Optional.of(config));
        org.mockito.Mockito.lenient().when(warehouseCapacityService.getRemainingCapacity(2L))
                .thenReturn(new BigDecimal("1000.000"));
        AtomicLong metadataIds = new AtomicLong(10);
        org.mockito.Mockito.lenient().when(forecastModelMetadataRepository.save(any(ForecastModelMetadata.class)))
                .thenAnswer(invocation -> {
                    ForecastModelMetadata metadata = invocation.getArgument(0);
                    metadata.setId(metadataIds.getAndIncrement());
                    return metadata;
                });
    }

    private List<SalesHistory> buildHistory(int days, int dailyQuantity) {
        return buildHistory(days, dailyQuantity, SalesHistorySource.THUC_TE);
    }

    private List<SalesHistory> buildHistory(int days, int dailyQuantity, SalesHistorySource source) {
        List<SalesHistory> rows = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(days);
        for (int i = 0; i < days; i++) {
            SalesHistory row = new SalesHistory();
            row.setProduct(product);
            row.setWarehouse(warehouse);
            row.setNgay(start.plusDays(i));
            row.setQuantity(dailyQuantity);
            row.setSource(source);
            rows.add(row);
        }
        return rows;
    }

    @Test
    void getAvailability_shouldDefaultToExternalStoreItemSource() {
        LocalDate start = LocalDate.parse("2019-01-01");
        when(salesHistoryRepository.findForecastAvailability(SalesHistorySource.EXTERNAL_STORE_ITEM, 60))
                .thenReturn(List.of(row(1L, "SP001", 2L, "K001", 1826L, start, LocalDate.parse("2023-12-31"))));

        ForecastAvailabilityResponse response = service.getAvailability(null);

        assertEquals("EXTERNAL_STORE_ITEM", response.source());
        assertEquals(1, response.combinations().size());
        assertEquals("SP001", response.combinations().get(0).productCode());
        assertEquals("K001", response.combinations().get(0).warehouseCode());
        assertEquals(1826L, response.combinations().get(0).historyDays());
    }

    @Test
    void getAvailability_shouldUseExplicitSource() {
        when(salesHistoryRepository.findForecastAvailability(SalesHistorySource.EXTERNAL_RETAIL, 60))
                .thenReturn(List.of(row(98L, "SP098", 2L, "K002", 362L,
                        LocalDate.parse("2022-01-01"), LocalDate.parse("2022-12-28"))));

        ForecastAvailabilityResponse response = service.getAvailability(SalesHistorySource.EXTERNAL_RETAIL);

        assertEquals("EXTERNAL_RETAIL", response.source());
        assertEquals("SP098", response.combinations().get(0).productCode());
        assertEquals("K002", response.combinations().get(0).warehouseCode());
    }

    @Test
    @SuppressWarnings("unchecked")
    void seedDemoHistory_shouldIgnoreExternalHistoryCount() {
        when(inventoryLevelRepository.findSeedDemoSeriesTargets()).thenReturn(List.of(seedTarget()));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceAndNgayBetweenOrderByNgayAsc(
                eq(1L), eq(2L), eq(SalesHistorySource.SEED_DEMO), any(), any()))
                .thenReturn(List.of());
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);

        com.smartflow.smestocksensebackend.dto.forecast.SeedHistoryResponse response = service.seedDemoHistory();

        ArgumentCaptor<List<SalesHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(salesHistoryRepository).saveAll(captor.capture());
        List<SalesHistory> rows = captor.getValue();
        assertEquals("SEED_DEMO", response.source());
        assertEquals(1, response.seriesSeeded());
        assertEquals(180, response.rowsInserted());
        assertEquals(180, rows.size());
        assertEquals(SalesHistorySource.SEED_DEMO, rows.get(0).getSource());
        assertEquals(product.getPrice(), rows.get(0).getAverageSellingPrice());
        assertEquals(LocalDate.now().minusDays(179), rows.get(0).getNgay());
        assertEquals(LocalDate.now(), rows.get(179).getNgay());
    }

    @Test
    @SuppressWarnings("unchecked")
    void seedDemoHistory_shouldFillOnlyMissingSeedDemoDates() {
        when(inventoryLevelRepository.findSeedDemoSeriesTargets()).thenReturn(List.of(seedTarget()));
        SalesHistory existing = salesHistory(LocalDate.now().minusDays(179), 9, SalesHistorySource.SEED_DEMO);
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceAndNgayBetweenOrderByNgayAsc(
                eq(1L), eq(2L), eq(SalesHistorySource.SEED_DEMO), any(), any()))
                .thenReturn(List.of(existing));
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);

        com.smartflow.smestocksensebackend.dto.forecast.SeedHistoryResponse response = service.seedDemoHistory();

        ArgumentCaptor<List<SalesHistory>> captor = ArgumentCaptor.forClass(List.class);
        verify(salesHistoryRepository).saveAll(captor.capture());
        assertEquals(179, captor.getValue().size());
        assertEquals(179, response.rowsInserted());
    }

    @Test
    void seedDemoHistory_shouldRejectConcurrentRun() {
        when(inventoryLevelRepository.findSeedDemoSeriesTargets()).thenAnswer(invocation -> {
            assertThrows(ConflictException.class, () -> service.seedDemoHistory());
            return List.of();
        });

        service.seedDemoHistory();
    }

    @Test
    void seedDemoHistory_shouldUseShortTransactionPerSeries() {
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        service.transactionTemplate = transactionTemplate;
        when(transactionTemplate.execute(any())).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        });
        when(inventoryLevelRepository.findSeedDemoSeriesTargets()).thenReturn(List.of(seedTarget(), seedTarget(3L, 4L)));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceAndNgayBetweenOrderByNgayAsc(
                any(), any(), eq(SalesHistorySource.SEED_DEMO), any(), any()))
                .thenReturn(List.of());
        when(productRepository.getReferenceById(any())).thenReturn(product);
        when(warehouseRepository.getReferenceById(any())).thenReturn(warehouse);

        service.seedDemoHistory();

        verify(transactionTemplate, times(3)).execute(any());
        verify(salesHistoryRepository, times(2)).saveAll(any());
    }

    @Test
    void runForecast_shouldUseColdStartAverage_whenNotEnoughHistory() {
        List<SalesHistory> history = buildHistory(10, 5, SalesHistorySource.EXTERNAL_STORE_ITEM);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.EXTERNAL_STORE_ITEM)).thenReturn(history);
        when(forecastResultRepository.findMaxVersion(1L, 2L)).thenReturn(null);
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());

        ForecastResponse response = service.runForecast(1L, 2L);

        assertEquals("COLD_START_AVG", response.mode());
        assertEquals(BigDecimal.ZERO, response.smape());
        assertEquals(new BigDecimal("5.00"), response.forecast7d());
        assertEquals(1, response.version());
    }

    @Test
    void runForecast_shouldCallAiServiceAndComputeRecommendedOrderQty_whenEnoughHistory() {
        List<SalesHistory> history = buildHistory(90, 5, SalesHistorySource.EXTERNAL_STORE_ITEM);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.EXTERNAL_STORE_ITEM)).thenReturn(history);
        when(forecastResultRepository.findMaxVersion(1L, 2L)).thenReturn(3);
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);

        InventoryLevel level = new InventoryLevel();
        level.setQuantity(20);
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.of(level));

        RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(aiServiceRestClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.body(any(AiForecastClientRequest.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(AiForecastClientResult.class))).thenReturn(new AiForecastClientResult(
                new BigDecimal("8.5"), new BigDecimal("1.25"), new BigDecimal("2.50"), 72, 18,
                Map.of("7", new BigDecimal("6"), "14", new BigDecimal("6"), "30", new BigDecimal("6")),
                List.of()));

        ForecastResponse response = service.runForecast(1L, 2L);

        assertEquals("XGBOOST", response.mode());
        assertEquals(new BigDecimal("8.5"), response.smape());
        assertEquals(4, response.version());
        assertEquals(20, response.currentStock());
        assertEquals(50, response.minStock());
        // rate 6/ngay cho ca 3 moc -> forecastedStock luon < minStock(50), reorder = minStock + demand - currentStock
        assertEquals(72, response.reorderQty7d());   // 50 + 6*7  - 20
        assertEquals(114, response.reorderQty14d());  // 50 + 6*14 - 20
        assertEquals(210, response.reorderQty30d());  // 50 + 6*30 - 20
    }

    @Test
    void runForecast_shouldMergeRealTransactionsIntoSalesHistory_beforeTraining() {
        List<SalesHistory> history = buildHistory(90, 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.THUC_TE)).thenReturn(history);
        when(forecastResultRepository.findMaxVersion(1L, 2L)).thenReturn(null);
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());

        LocalDate realDate = LocalDate.now().minusDays(3);
        when(inventoryTransactionRepository.sumDailyXuatKho(eq(1L), eq(2L), any(), any()))
                .thenReturn(List.of(dailyQuantity(realDate, 99)));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndNgayAndSource(1L, 2L, realDate,
                SalesHistorySource.THUC_TE))
                .thenReturn(Optional.empty());

        RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(aiServiceRestClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(anyString())).thenReturn(bodySpec);
        when(bodySpec.body(any(AiForecastClientRequest.class))).thenReturn(bodySpec);
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(AiForecastClientResult.class))).thenReturn(new AiForecastClientResult(
                new BigDecimal("5"), new BigDecimal("1.25"), new BigDecimal("2.50"), 70, 20,
                Map.of("7", new BigDecimal("5"), "14", new BigDecimal("5"), "30", new BigDecimal("5")),
                List.of()));

        service.runForecast(1L, 2L, SalesHistorySource.THUC_TE);

        ArgumentCaptor<SalesHistory> captor = ArgumentCaptor.forClass(SalesHistory.class);
        verify(salesHistoryRepository).save(captor.capture());
        SalesHistory saved = captor.getValue();
        assertEquals(realDate, saved.getNgay());
        assertEquals(99, saved.getQuantity());
        assertEquals(SalesHistorySource.THUC_TE, saved.getSource());
    }

    @Test
    void runForecast_shouldDefaultToExternalStoreItemSource() {
        List<SalesHistory> history = buildHistory(90, 5, SalesHistorySource.EXTERNAL_STORE_ITEM);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.EXTERNAL_STORE_ITEM)).thenReturn(history);
        when(forecastResultRepository.findMaxVersion(1L, 2L)).thenReturn(null);
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());
        stubAiForecast();

        service.runForecast(1L, 2L);

        verify(salesHistoryRepository).findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.EXTERNAL_STORE_ITEM);
    }

    @Test
    void runForecast_shouldQueryOnlySelectedExternalRetailSource() {
        List<SalesHistory> history = buildHistory(90, 7, SalesHistorySource.EXTERNAL_RETAIL);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.EXTERNAL_RETAIL)).thenReturn(history);
        when(forecastResultRepository.findMaxVersion(1L, 2L)).thenReturn(null);
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());
        stubAiForecast();

        service.runForecast(1L, 2L, SalesHistorySource.EXTERNAL_RETAIL);

        verify(salesHistoryRepository).findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.EXTERNAL_RETAIL);
    }

    @Test
    void runForecast_shouldNotDoubleDemandForSameCalendarDateAcrossSources() {
        SalesHistory real = salesHistory(LocalDate.now().minusDays(1), 5, SalesHistorySource.EXTERNAL_STORE_ITEM);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.EXTERNAL_STORE_ITEM)).thenReturn(List.of(real));
        when(forecastResultRepository.findMaxVersion(1L, 2L)).thenReturn(null);
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());

        ForecastResponse response = service.runForecast(1L, 2L);

        assertEquals(new BigDecimal("5.00"), response.forecast7d());
    }

    @Test
    void runForecast_shouldStoreDatasetTypeFromSelectedSource() {
        List<SalesHistory> history = buildHistory(90, 7, SalesHistorySource.EXTERNAL_RETAIL);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.EXTERNAL_RETAIL)).thenReturn(history);
        when(forecastResultRepository.findMaxVersion(1L, 2L)).thenReturn(null);
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());
        stubAiForecast();

        service.runForecast(1L, 2L, SalesHistorySource.EXTERNAL_RETAIL);

        ArgumentCaptor<ForecastModelMetadata> captor = ArgumentCaptor.forClass(ForecastModelMetadata.class);
        verify(forecastModelMetadataRepository).save(captor.capture());
        assertEquals(ForecastDatasetType.EXTERNAL, captor.getValue().getDatasetType());
        assertEquals(SalesHistorySource.EXTERNAL_RETAIL, captor.getValue().getHistorySource());
    }

    @Test
    void runForecast_shouldStoreExactExternalStoreItemSource() {
        List<SalesHistory> history = buildHistory(90, 5, SalesHistorySource.EXTERNAL_STORE_ITEM);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.EXTERNAL_STORE_ITEM)).thenReturn(history);
        when(forecastResultRepository.findMaxVersion(1L, 2L)).thenReturn(null);
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());
        stubAiForecast();

        service.runForecast(1L, 2L);

        ArgumentCaptor<ForecastModelMetadata> captor = ArgumentCaptor.forClass(ForecastModelMetadata.class);
        verify(forecastModelMetadataRepository).save(captor.capture());
        assertEquals(ForecastDatasetType.EXTERNAL, captor.getValue().getDatasetType());
        assertEquals(SalesHistorySource.EXTERNAL_STORE_ITEM, captor.getValue().getHistorySource());
    }

    @Test
    void runForecast_shouldNormalizeHistoryToContinuousDailySeries() {
        LocalDate start = LocalDate.now().minusDays(70);
        List<SalesHistory> history = new ArrayList<>();
        for (int i = 0; i <= 60; i++) {
            if (i == 2) {
                continue;
            }
            SalesHistory row = salesHistory(start.plusDays(i), i == 3 ? 0 : 5, SalesHistorySource.EXTERNAL_STORE_ITEM);
            row.setAverageSellingPrice(new BigDecimal("12000"));
            history.add(row);
        }
        history.add(salesHistory(start.plusDays(4), 2, SalesHistorySource.EXTERNAL_STORE_ITEM));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.EXTERNAL_STORE_ITEM)).thenReturn(history);
        when(forecastResultRepository.findMaxVersion(1L, 2L)).thenReturn(null);
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());
        ArgumentCaptor<AiForecastClientRequest> requestCaptor = ArgumentCaptor.forClass(AiForecastClientRequest.class);
        stubAiForecast(requestCaptor);

        service.runForecast(1L, 2L);

        List<AiForecastClientRequest.SalesPoint> points = requestCaptor.getValue().history();
        assertEquals(61, points.size());
        for (int i = 0; i < points.size(); i++) {
            assertEquals(start.plusDays(i).toString(), points.get(i).date());
        }
        assertEquals(BigDecimal.ZERO, points.get(2).quantity());
        assertNull(points.get(2).price());
        assertEquals(BigDecimal.ZERO, points.get(3).quantity());
        assertEquals(new BigDecimal("7"), points.get(4).quantity());
    }

    @Test
    void runForecast_shouldUseOnlyActualHistoricalPricesInForecastInput() {
        List<SalesHistory> history = buildHistory(90, 5, SalesHistorySource.EXTERNAL_STORE_ITEM);
        history.get(0).setAverageSellingPrice(new BigDecimal("12345"));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.EXTERNAL_STORE_ITEM)).thenReturn(history);
        when(forecastResultRepository.findMaxVersion(1L, 2L)).thenReturn(null);
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());
        ArgumentCaptor<AiForecastClientRequest> requestCaptor = ArgumentCaptor.forClass(AiForecastClientRequest.class);
        stubAiForecast(requestCaptor);

        service.runForecast(1L, 2L);

        List<AiForecastClientRequest.SalesPoint> points = requestCaptor.getValue().history();
        assertEquals(new BigDecimal("12345"), points.get(0).price());
        assertNull(points.get(1).price());
    }

    @Test
    void aiForecastClientResult_shouldDeserializeDailyPredictions() throws Exception {
        String json = """
                {
                  "smape": 8.5,
                  "mae": 1.25,
                  "rmse": 2.5,
                  "train_size": 72,
                  "test_size": 18,
                  "forecast": {"7": 6.1, "14": 6.2, "30": 6.3},
                  "daily_predictions": [
                    {"date": "2026-03-12", "predicted_quantity": 4.25}
                  ]
                }
                """;

        AiForecastClientResult result = JsonMapper.builder()
                .build()
                .readValue(json, AiForecastClientResult.class);

        assertEquals("2026-03-12", result.dailyPredictions().get(0).date());
        assertEquals(new BigDecimal("4.25"), result.dailyPredictions().get(0).predictedQuantity());
        assertEquals(new BigDecimal("1.25"), result.mae());
        assertEquals(new BigDecimal("2.5"), result.rmse());
    }

    @Test
    void runForecast_shouldPersistRealDailyPredictionsFromPythonResponse() {
        List<SalesHistory> history = buildHistory(90, 5, SalesHistorySource.EXTERNAL_STORE_ITEM);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.EXTERNAL_STORE_ITEM)).thenReturn(history);
        when(forecastResultRepository.findMaxVersion(1L, 2L)).thenReturn(null, 1);
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());
        stubAiForecast(null, dailyPredictions(LocalDate.parse("2026-03-12"), 30));

        service.runForecast(1L, 2L);
        service.runForecast(1L, 2L);

        ArgumentCaptor<DailyForecastResult> dailyCaptor = ArgumentCaptor.forClass(DailyForecastResult.class);
        verify(dailyForecastResultRepository, org.mockito.Mockito.times(60)).save(dailyCaptor.capture());
        List<DailyForecastResult> rows = dailyCaptor.getAllValues();
        assertEquals(30, rows.subList(0, 30).size());
        assertEquals(LocalDate.parse("2026-03-12"), rows.get(0).getForecastDate());
        assertEquals(LocalDate.parse("2026-04-10"), rows.get(29).getForecastDate());
        assertEquals(new BigDecimal("1"), rows.get(0).getPredictedQuantity());
        assertEquals(new BigDecimal("30"), rows.get(29).getPredictedQuantity());
        assertEquals(10L, rows.get(0).getModelMetadata().getId());
        assertEquals(11L, rows.get(30).getModelMetadata().getId());

        ArgumentCaptor<ForecastResult> summaryCaptor = ArgumentCaptor.forClass(ForecastResult.class);
        verify(forecastResultRepository, org.mockito.Mockito.times(6)).save(summaryCaptor.capture());
        assertEquals(3, summaryCaptor.getAllValues().stream()
                .filter(row -> row.getModelMetadata().getId().equals(10L))
                .count());
    }

    @Test
    void runForecast_shouldPersistEvaluationMetrics() {
        List<SalesHistory> history = buildHistory(90, 5, SalesHistorySource.EXTERNAL_STORE_ITEM);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.EXTERNAL_STORE_ITEM)).thenReturn(history);
        when(forecastResultRepository.findMaxVersion(1L, 2L)).thenReturn(null);
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());
        stubAiForecast(null, List.of(), new BigDecimal("3.25"), new BigDecimal("4.50"));

        service.runForecast(1L, 2L);

        ArgumentCaptor<ForecastModelMetadata> captor = ArgumentCaptor.forClass(ForecastModelMetadata.class);
        verify(forecastModelMetadataRepository).save(captor.capture());
        assertEquals(new BigDecimal("3.25"), captor.getValue().getMae());
        assertEquals(new BigDecimal("4.50"), captor.getValue().getRmse());
    }

    @Test
    void forecastModelMetadata_shouldAllowNullableLegacyMetrics() {
        ForecastModelMetadata metadata = new ForecastModelMetadata();

        assertNull(metadata.getMae());
        assertNull(metadata.getRmse());
    }

    @Test
    void runForecast_shouldThrowNotFound_whenProductMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.runForecast(99L, 2L));
    }

    @Test
    void getLatestForecast_shouldThrowNotFound_whenNoForecastYet() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(forecastModelMetadataRepository.findFirstByProductIdAndWarehouseIdAndHistorySourceOrderByVersionDesc(
                1L, 2L, SalesHistorySource.EXTERNAL_STORE_ITEM))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getLatestForecast(1L, 2L));
    }

    @Test
    void getLatestForecast_shouldReturnExactExternalStoreItemSource() {
        ForecastModelMetadata metadata = metadata(ForecastDatasetType.EXTERNAL, SalesHistorySource.EXTERNAL_STORE_ITEM);
        stubLatestForecast(metadata, SalesHistorySource.EXTERNAL_STORE_ITEM);

        ForecastResponse response = service.getLatestForecast(1L, 2L);

        assertEquals("EXTERNAL", response.datasetType());
        assertEquals("EXTERNAL_STORE_ITEM", response.source());
    }

    @Test
    void getLatestForecast_shouldReturnExactExternalRetailSource() {
        ForecastModelMetadata metadata = metadata(ForecastDatasetType.EXTERNAL, SalesHistorySource.EXTERNAL_RETAIL);
        stubLatestForecast(metadata, SalesHistorySource.EXTERNAL_RETAIL);

        ForecastResponse response = service.getLatestForecast(1L, 2L, SalesHistorySource.EXTERNAL_RETAIL);

        assertEquals("EXTERNAL", response.datasetType());
        assertEquals("EXTERNAL_RETAIL", response.source());
    }

    @Test
    void getLatestForecast_shouldNotInventSourceForLegacyExternalMetadata() {
        ForecastModelMetadata metadata = metadata(ForecastDatasetType.EXTERNAL, null);
        stubLatestForecast(metadata, SalesHistorySource.EXTERNAL_STORE_ITEM);

        ForecastResponse response = service.getLatestForecast(1L, 2L);

        assertEquals("EXTERNAL", response.datasetType());
        assertNull(response.source());
    }

    @Test
    void getLatestForecast_shouldUseExplicitSeedDemoSource() {
        ForecastModelMetadata metadata = metadata(ForecastDatasetType.LEGACY_UNKNOWN, SalesHistorySource.SEED_DEMO);
        stubLatestForecast(metadata, SalesHistorySource.SEED_DEMO);

        ForecastResponse response = service.getLatestForecast(1L, 2L, SalesHistorySource.SEED_DEMO);

        assertEquals("SEED_DEMO", response.source());
        verify(forecastModelMetadataRepository).findFirstByProductIdAndWarehouseIdAndHistorySourceOrderByVersionDesc(
                1L, 2L, SalesHistorySource.SEED_DEMO);
    }

    @Test
    void checkDrift_shouldReturnNoForecastData_whenNoSavedForecast() {
        when(forecastResultRepository.findByProductIdAndWarehouseIdAndHorizonDaysAndForecastDateBetweenOrderByForecastDateAsc(
                eq(1L), eq(2L), eq(7), any(), any())).thenReturn(List.of());

        DriftResponse response = service.checkDrift(1L, 2L);

        assertEquals("NO_FORECAST_DATA", response.status());
    }

    @Test
    void checkDrift_shouldFlagDrift_whenActualDeviatesFromForecast() {
        LocalDate today = LocalDate.now();
        List<ForecastResult> forecastRows = new ArrayList<>();
        List<DailyQuantityProjection> actualRows = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            LocalDate date = today.minusDays(10 - i);
            ForecastResult fr = new ForecastResult();
            fr.setForecastDate(date);
            fr.setHorizonDays(7);
            fr.setPredictedQuantity(new BigDecimal("5"));
            forecastRows.add(fr);
            actualRows.add(dailyQuantity(date, 50)); // hugely different from forecast -> high sMAPE
        }
        when(forecastResultRepository.findByProductIdAndWarehouseIdAndHorizonDaysAndForecastDateBetweenOrderByForecastDateAsc(
                eq(1L), eq(2L), eq(7), any(), any())).thenReturn(forecastRows);
        when(inventoryTransactionRepository.sumDailyXuatKho(eq(1L), eq(2L), any(), any())).thenReturn(actualRows);
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);

        DriftResponse response = service.checkDrift(1L, 2L);

        assertEquals("DRIFT", response.status());
        assertEquals(true, response.retrainNeeded());
    }

    private DailyQuantityProjection dailyQuantity(LocalDate date, int total) {
        return new DailyQuantityProjection() {
            @Override
            public LocalDate getNgay() {
                return date;
            }

            @Override
            public Integer getTongSoLuong() {
                return total;
            }
        };
    }

    private SalesHistory salesHistory(LocalDate date, int quantity, SalesHistorySource source) {
        SalesHistory row = new SalesHistory();
        row.setProduct(product);
        row.setWarehouse(warehouse);
        row.setNgay(date);
        row.setQuantity(quantity);
        row.setSource(source);
        return row;
    }

    private InventoryLevelRepository.SeedDemoSeriesTarget seedTarget() {
        return seedTarget(1L, 2L);
    }

    private InventoryLevelRepository.SeedDemoSeriesTarget seedTarget(Long productId, Long warehouseId) {
        return new InventoryLevelRepository.SeedDemoSeriesTarget() {
            @Override
            public Long getProductId() {
                return productId;
            }

            @Override
            public Long getWarehouseId() {
                return warehouseId;
            }

            @Override
            public BigDecimal getPrice() {
                return product.getPrice();
            }
        };
    }

    private ForecastModelMetadata metadata(ForecastDatasetType datasetType, SalesHistorySource source) {
        ForecastModelMetadata metadata = new ForecastModelMetadata();
        metadata.setId(10L);
        metadata.setVersion(1);
        metadata.setMode(ForecastMode.XGBOOST);
        metadata.setDatasetType(datasetType);
        metadata.setHistorySource(source);
        metadata.setSmape(new BigDecimal("5"));
        metadata.setDataDays(90);
        return metadata;
    }

    private void stubLatestForecast(ForecastModelMetadata metadata, SalesHistorySource source) {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(forecastModelMetadataRepository.findFirstByProductIdAndWarehouseIdAndHistorySourceOrderByVersionDesc(
                1L, 2L, source))
                .thenReturn(Optional.of(metadata));
        when(forecastResultRepository.findByProductIdAndWarehouseIdAndVersion(1L, 2L, 1))
                .thenReturn(List.of(forecastResult(7), forecastResult(14), forecastResult(30)));
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());
        when(dailyForecastResultRepository.findByModelMetadataIdOrderByForecastDateAsc(10L)).thenReturn(List.of());
        if (metadata.getHistorySource() != null) {
            when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L, source))
                    .thenReturn(buildHistory(90, 5, source));
        }
    }

    private ForecastResult forecastResult(int horizon) {
        ForecastResult result = new ForecastResult();
        result.setHorizonDays(horizon);
        result.setPredictedQuantity(new BigDecimal("5"));
        return result;
    }

    private SalesHistoryRepository.ForecastAvailabilityRow row(Long productId, String productCode, Long warehouseId,
            String warehouseCode, Long historyDays, LocalDate historyStart, LocalDate historyEnd) {
        return new AvailabilityRow(productId, productCode, "Product " + productCode, warehouseId, warehouseCode,
                "Warehouse " + warehouseCode, historyDays, historyStart, historyEnd);
    }

    private record AvailabilityRow(
            Long productId,
            String productCode,
            String productName,
            Long warehouseId,
            String warehouseCode,
            String warehouseName,
            Long historyDays,
            LocalDate historyStart,
            LocalDate historyEnd) implements SalesHistoryRepository.ForecastAvailabilityRow {

        @Override
        public Long getProductId() {
            return productId;
        }

        @Override
        public String getProductCode() {
            return productCode;
        }

        @Override
        public String getProductName() {
            return productName;
        }

        @Override
        public Long getWarehouseId() {
            return warehouseId;
        }

        @Override
        public String getWarehouseCode() {
            return warehouseCode;
        }

        @Override
        public String getWarehouseName() {
            return warehouseName;
        }

        @Override
        public Long getHistoryDays() {
            return historyDays;
        }

        @Override
        public LocalDate getHistoryStart() {
            return historyStart;
        }

        @Override
        public LocalDate getHistoryEnd() {
            return historyEnd;
        }
    }

    private void stubAiForecast() {
        stubAiForecast(null);
    }

    private void stubAiForecast(ArgumentCaptor<AiForecastClientRequest> requestCaptor) {
        stubAiForecast(requestCaptor, List.of());
    }

    private void stubAiForecast(ArgumentCaptor<AiForecastClientRequest> requestCaptor,
            List<AiForecastClientResult.DailyPrediction> dailyPredictions) {
        stubAiForecast(requestCaptor, dailyPredictions, new BigDecimal("1.25"), new BigDecimal("2.50"));
    }

    private void stubAiForecast(ArgumentCaptor<AiForecastClientRequest> requestCaptor,
            List<AiForecastClientResult.DailyPrediction> dailyPredictions, BigDecimal mae, BigDecimal rmse) {
        RestClient.RequestBodyUriSpec bodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.RequestBodySpec bodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);
        when(aiServiceRestClient.post()).thenReturn(bodyUriSpec);
        when(bodyUriSpec.uri(anyString())).thenReturn(bodySpec);
        if (requestCaptor == null) {
            when(bodySpec.body(any(AiForecastClientRequest.class))).thenReturn(bodySpec);
        } else {
            when(bodySpec.body(requestCaptor.capture())).thenReturn(bodySpec);
        }
        when(bodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(eq(AiForecastClientResult.class))).thenReturn(new AiForecastClientResult(
                new BigDecimal("5"), mae, rmse, 70, 20,
                Map.of("7", new BigDecimal("5"), "14", new BigDecimal("5"), "30", new BigDecimal("5")),
                dailyPredictions));
    }

    private List<AiForecastClientResult.DailyPrediction> dailyPredictions(LocalDate start, int days) {
        List<AiForecastClientResult.DailyPrediction> predictions = new ArrayList<>();
        for (int i = 0; i < days; i++) {
            predictions.add(new AiForecastClientResult.DailyPrediction(start.plusDays(i).toString(),
                    BigDecimal.valueOf(i + 1)));
        }
        return predictions;
    }
}
