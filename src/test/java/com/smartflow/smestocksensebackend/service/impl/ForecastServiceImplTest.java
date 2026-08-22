package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.forecast.AiForecastClientRequest;
import com.smartflow.smestocksensebackend.dto.forecast.AiForecastClientResult;
import com.smartflow.smestocksensebackend.dto.forecast.DriftResponse;
import com.smartflow.smestocksensebackend.dto.forecast.ForecastResponse;
import com.smartflow.smestocksensebackend.dto.inventory.DailyQuantityProjection;
import com.smartflow.smestocksensebackend.entity.ForecastDatasetType;
import com.smartflow.smestocksensebackend.entity.ForecastMode;
import com.smartflow.smestocksensebackend.entity.ForecastModelMetadata;
import com.smartflow.smestocksensebackend.entity.ForecastResult;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.SalesHistory;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ForecastDriftLogRepository;
import com.smartflow.smestocksensebackend.repository.ForecastModelMetadataRepository;
import com.smartflow.smestocksensebackend.repository.ForecastResultRepository;
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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
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
                forecastModelMetadataRepository, forecastDriftLogRepository, aiServiceRestClient);
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
    void runForecast_shouldUseColdStartAverage_whenNotEnoughHistory() {
        List<SalesHistory> history = buildHistory(10, 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.THUC_TE)).thenReturn(history);
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
        List<SalesHistory> history = buildHistory(90, 5);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.THUC_TE)).thenReturn(history);
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
                new BigDecimal("8.5"), 72, 18,
                Map.of("7", new BigDecimal("6"), "14", new BigDecimal("6"), "30", new BigDecimal("6"))));

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
                new BigDecimal("5"), 70, 20,
                Map.of("7", new BigDecimal("5"), "14", new BigDecimal("5"), "30", new BigDecimal("5"))));

        service.runForecast(1L, 2L);

        ArgumentCaptor<SalesHistory> captor = ArgumentCaptor.forClass(SalesHistory.class);
        verify(salesHistoryRepository).save(captor.capture());
        SalesHistory saved = captor.getValue();
        assertEquals(realDate, saved.getNgay());
        assertEquals(99, saved.getQuantity());
        assertEquals(SalesHistorySource.THUC_TE, saved.getSource());
    }

    @Test
    void runForecast_shouldQueryOnlySelectedThucTeSource() {
        List<SalesHistory> history = buildHistory(90, 5, SalesHistorySource.THUC_TE);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.THUC_TE)).thenReturn(history);
        when(forecastResultRepository.findMaxVersion(1L, 2L)).thenReturn(null);
        when(productRepository.getReferenceById(1L)).thenReturn(product);
        when(warehouseRepository.getReferenceById(2L)).thenReturn(warehouse);
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(1L, 2L)).thenReturn(Optional.empty());
        stubAiForecast();

        service.runForecast(1L, 2L);

        verify(salesHistoryRepository).findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.THUC_TE);
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
        SalesHistory real = salesHistory(LocalDate.now().minusDays(1), 5, SalesHistorySource.THUC_TE);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.THUC_TE)).thenReturn(List.of(real));
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
    }

    @Test
    void runForecast_shouldNormalizeHistoryToContinuousDailySeries() {
        LocalDate start = LocalDate.now().minusDays(70);
        List<SalesHistory> history = new ArrayList<>();
        for (int i = 0; i <= 60; i++) {
            if (i == 2) {
                continue;
            }
            SalesHistory row = salesHistory(start.plusDays(i), i == 3 ? 0 : 5, SalesHistorySource.THUC_TE);
            row.setAverageSellingPrice(new BigDecimal("12000"));
            history.add(row);
        }
        history.add(salesHistory(start.plusDays(4), 2, SalesHistorySource.THUC_TE));
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(salesHistoryRepository.findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(1L, 2L,
                SalesHistorySource.THUC_TE)).thenReturn(history);
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
    void runForecast_shouldThrowNotFound_whenProductMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.runForecast(99L, 2L));
    }

    @Test
    void getLatestForecast_shouldThrowNotFound_whenNoForecastYet() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(forecastModelMetadataRepository.findFirstByProductIdAndWarehouseIdOrderByVersionDesc(1L, 2L))
                .thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getLatestForecast(1L, 2L));
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

    private void stubAiForecast() {
        stubAiForecast(null);
    }

    private void stubAiForecast(ArgumentCaptor<AiForecastClientRequest> requestCaptor) {
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
                new BigDecimal("5"), 70, 20,
                Map.of("7", new BigDecimal("5"), "14", new BigDecimal("5"), "30", new BigDecimal("5"))));
    }
}
