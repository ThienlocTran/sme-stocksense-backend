package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.forecast.AiForecastClientRequest;
import com.smartflow.smestocksensebackend.dto.forecast.AiForecastClientResult;
import com.smartflow.smestocksensebackend.dto.forecast.DriftResponse;
import com.smartflow.smestocksensebackend.dto.forecast.ForecastAvailabilityResponse;
import com.smartflow.smestocksensebackend.dto.forecast.ForecastResponse;
import com.smartflow.smestocksensebackend.dto.inventory.DailyQuantityProjection;
import com.smartflow.smestocksensebackend.entity.DailyForecastResult;
import com.smartflow.smestocksensebackend.entity.ForecastDriftLog;
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
import com.smartflow.smestocksensebackend.repository.DailyForecastResultRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.InventoryTransactionRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.SalesHistoryRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.ForecastService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastServiceImpl implements ForecastService {

    private static final int MIN_HISTORY_DAYS = 60;
    private static final int REAL_SALES_LOOKBACK_DAYS = 730;
    private static final int DRIFT_WINDOW_DAYS = 30;
    private static final int DRIFT_MIN_OVERLAP_DAYS = 7;
    private static final BigDecimal DRIFT_THRESHOLD_SMAPE = new BigDecimal("20.0");
    private static final List<Integer> HORIZONS = List.of(7, 14, 30);

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryLevelRepository inventoryLevelRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final SalesHistoryRepository salesHistoryRepository;
    private final ForecastResultRepository forecastResultRepository;
    private final DailyForecastResultRepository dailyForecastResultRepository;
    private final ForecastModelMetadataRepository forecastModelMetadataRepository;
    private final ForecastDriftLogRepository forecastDriftLogRepository;
    private final RestClient aiServiceRestClient;

    @org.springframework.beans.factory.annotation.Autowired
    com.smartflow.smestocksensebackend.repository.WarehouseStockConfigRepository warehouseStockConfigRepository;

    @org.springframework.beans.factory.annotation.Autowired
    com.smartflow.smestocksensebackend.service.EffectiveMinStockResolver effectiveMinStockResolver;

    @org.springframework.beans.factory.annotation.Autowired
    com.smartflow.smestocksensebackend.service.WarehouseCapacityService warehouseCapacityService;

    @Override
    @Transactional
    public ForecastResponse runForecast(Long productId, Long warehouseId) {
        return runForecast(productId, warehouseId, SalesHistorySource.EXTERNAL_STORE_ITEM);
    }

    @Override
    @Transactional(readOnly = true)
    public ForecastAvailabilityResponse getAvailability(SalesHistorySource source) {
        SalesHistorySource effectiveSource = source == null ? SalesHistorySource.EXTERNAL_STORE_ITEM : source;
        List<ForecastAvailabilityResponse.Combination> combinations = salesHistoryRepository
                .findForecastAvailability(effectiveSource, MIN_HISTORY_DAYS)
                .stream()
                .map(row -> new ForecastAvailabilityResponse.Combination(
                        row.getProductId(), row.getProductCode(), row.getProductName(),
                        row.getWarehouseId(), row.getWarehouseCode(), row.getWarehouseName(),
                        row.getHistoryDays(), row.getHistoryStart(), row.getHistoryEnd()))
                .toList();
        return new ForecastAvailabilityResponse(effectiveSource.name(), combinations);
    }

    @Override
    @Transactional
    public ForecastResponse runForecast(Long productId, Long warehouseId, SalesHistorySource source) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm với id " + productId));
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy kho với id " + warehouseId));

        if (source == SalesHistorySource.THUC_TE) {
            syncRealSalesHistory(productId, warehouseId);
        }

        List<SalesHistory> history = salesHistoryRepository
                .findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(productId, warehouseId, source);
        history = normalizeDailySeries(history, source);
        int dataDays = history.size();

        BigDecimal smape;
        BigDecimal mae = null;
        BigDecimal rmse = null;
        Map<Integer, BigDecimal> forecastByHorizon;
        List<AiForecastClientResult.DailyPrediction> dailyPredictions = List.of();
        ForecastMode mode;

        if (dataDays < MIN_HISTORY_DAYS) {
            mode = ForecastMode.COLD_START_AVG;
            smape = BigDecimal.ZERO;
            forecastByHorizon = coldStartForecast(history);
        } else {
            mode = ForecastMode.XGBOOST;
            AiForecastClientResult result = callAiService(history);
            smape = result.smape() != null ? result.smape() : BigDecimal.ZERO;
            mae = result.mae();
            rmse = result.rmse();
            forecastByHorizon = new HashMap<>();
            for (Integer horizon : HORIZONS) {
                BigDecimal value = result.forecast() != null ? result.forecast().get(String.valueOf(horizon)) : null;
                forecastByHorizon.put(horizon, value != null ? value : BigDecimal.ZERO);
            }
            if (result.dailyPredictions() != null) {
                dailyPredictions = result.dailyPredictions();
            }
        }

        int version = firstNonNull(forecastResultRepository.findMaxVersion(productId, warehouseId), 0) + 1;
        LocalDate baseDate = history.isEmpty() ? LocalDate.now() : history.get(history.size() - 1).getNgay();

        ForecastModelMetadata metadata = new ForecastModelMetadata();
        metadata.setProduct(productRepository.getReferenceById(productId));
        metadata.setWarehouse(warehouseRepository.getReferenceById(warehouseId));
        metadata.setSmape(smape);
        metadata.setMae(mae);
        metadata.setRmse(rmse);
        metadata.setVersion(version);
        metadata.setDataDays(dataDays);
        metadata.setMode(mode);
        metadata.setDatasetType(datasetType(source, mode));
        metadata.setHistorySource(source);
        if (!history.isEmpty()) {
            metadata.setHistoryStartDate(history.get(0).getNgay());
            metadata.setHistoryEndDate(history.get(history.size() - 1).getNgay());
        }
        ForecastModelMetadata savedMetadata = forecastModelMetadataRepository.save(metadata);

        for (Integer horizon : HORIZONS) {
            ForecastResult forecastResult = new ForecastResult();
            forecastResult.setModelMetadata(savedMetadata);
            forecastResult.setProduct(productRepository.getReferenceById(productId));
            forecastResult.setWarehouse(warehouseRepository.getReferenceById(warehouseId));
            forecastResult.setForecastDate(baseDate.plusDays(horizon));
            forecastResult.setHorizonDays(horizon);
            forecastResult.setPredictedQuantity(forecastByHorizon.get(horizon));
            forecastResult.setVersion(version);
            forecastResult.setTargetHorizonDays(horizon.shortValue());
            forecastResult.setForecastBaseDate(baseDate);
            forecastResult.setAverageDailyDemand(forecastByHorizon.get(horizon));
            forecastResultRepository.save(forecastResult);
        }
        persistDailyForecasts(savedMetadata, dailyPredictions);

        int currentStock = currentStock(productId, warehouseId);
        Integer minStock = effectiveMinStockResolver
                .resolve(product, warehouseStockConfigRepository.findByProductIdAndWarehouseId(productId, warehouseId).orElse(null))
                .orElse(null);

        int reorder7 = computeReorderQty(minStock, currentStock, forecastByHorizon.get(7), 7);
        int reorder14 = computeReorderQty(minStock, currentStock, forecastByHorizon.get(14), 14);
        int reorder30 = computeReorderQty(minStock, currentStock, forecastByHorizon.get(30), 30);

        CapacityLimitInfo capInfo = calculateCapacityLimit(product.getUnitVolumeM3(), warehouseId, reorder7, reorder14, reorder30);

        return new ForecastResponse(savedMetadata.getId(), productId, warehouseId, version, mode.name(),
                savedMetadata.getDatasetType().name(), source.name(), smape, mae, rmse,
                forecastByHorizon.get(7), forecastByHorizon.get(14), forecastByHorizon.get(30),
                currentStock, minStock, reorder7, reorder14, reorder30,
                dataDays, LocalDateTime.now(),
                capInfo.allowed7d(), capInfo.allowed14d(), capInfo.allowed30d(),
                capInfo.limited7d(), capInfo.limited14d(), capInfo.limited30d(),
                capInfo.capacityStatus(),
                savedMetadata.getHistoryStartDate(), savedMetadata.getHistoryEndDate(),
                toHistoryPoints(history), toDailyForecastPoints(savedMetadata.getId()));
    }

    @Override
    @Transactional(readOnly = true)
    public ForecastResponse getLatestForecast(Long productId, Long warehouseId) {
        productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm với id " + productId));
        warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy kho với id " + warehouseId));

        ForecastModelMetadata metadata = forecastModelMetadataRepository
                .findFirstByProductIdAndWarehouseIdOrderByVersionDesc(productId, warehouseId)
                .orElseThrow(() -> new NotFoundException(
                        "Chưa có dự báo nào cho sản phẩm/kho này, hãy chạy dự báo trước."));

        List<ForecastResult> results = forecastResultRepository
                .findByProductIdAndWarehouseIdAndVersion(productId, warehouseId, metadata.getVersion());

        Map<Integer, BigDecimal> forecastByHorizon = new HashMap<>();
        for (ForecastResult result : results) {
            forecastByHorizon.put(result.getHorizonDays(), result.getPredictedQuantity());
        }

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm với id " + productId));
        int currentStock = currentStock(productId, warehouseId);
        Integer minStock = effectiveMinStockResolver
                .resolve(product, warehouseStockConfigRepository.findByProductIdAndWarehouseId(productId, warehouseId).orElse(null))
                .orElse(null);

        int reorder7 = computeReorderQty(minStock, currentStock, forecastByHorizon.get(7), 7);
        int reorder14 = computeReorderQty(minStock, currentStock, forecastByHorizon.get(14), 14);
        int reorder30 = computeReorderQty(minStock, currentStock, forecastByHorizon.get(30), 30);

        CapacityLimitInfo capInfo = calculateCapacityLimit(product.getUnitVolumeM3(), warehouseId, reorder7, reorder14, reorder30);

        return new ForecastResponse(metadata.getId(), productId, warehouseId, metadata.getVersion(),
                metadata.getMode().name(), metadata.getDatasetType().name(), sourceName(metadata),
                metadata.getSmape(), metadata.getMae(), metadata.getRmse(),
                forecastByHorizon.get(7), forecastByHorizon.get(14), forecastByHorizon.get(30),
                currentStock, minStock, reorder7, reorder14, reorder30,
                metadata.getDataDays(), metadata.getTrainedAt() != null ? metadata.getTrainedAt() : metadata.getCreatedAt(),
                capInfo.allowed7d(), capInfo.allowed14d(), capInfo.allowed30d(),
                capInfo.limited7d(), capInfo.limited14d(), capInfo.limited30d(),
                capInfo.capacityStatus(),
                metadata.getHistoryStartDate(), metadata.getHistoryEndDate(),
                toHistoryPoints(historyForMetadata(productId, warehouseId, metadata)),
                toDailyForecastPoints(metadata.getId()));
    }

    @Override
    @Transactional
    public DriftResponse checkDrift(Long productId, Long warehouseId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(DRIFT_WINDOW_DAYS);

        List<ForecastResult> forecastRows = forecastResultRepository
                .findByProductIdAndWarehouseIdAndHorizonDaysAndForecastDateBetweenOrderByForecastDateAsc(
                        productId, warehouseId, 7, start, end);
        if (forecastRows.isEmpty()) {
            return new DriftResponse(productId, warehouseId, "NO_FORECAST_DATA", null, DRIFT_THRESHOLD_SMAPE, false, 0);
        }

        List<DailyQuantityProjection> actualRows = inventoryTransactionRepository
                .sumDailyXuatKho(productId, warehouseId, start, end);
        if (actualRows.isEmpty()) {
            return new DriftResponse(productId, warehouseId, "NO_ACTUAL_DATA", null, DRIFT_THRESHOLD_SMAPE, false, 0);
        }

        Map<LocalDate, BigDecimal> forecastByDate = new HashMap<>();
        for (ForecastResult row : forecastRows) {
            forecastByDate.put(row.getForecastDate(), row.getPredictedQuantity());
        }
        Map<LocalDate, BigDecimal> actualByDate = new HashMap<>();
        for (DailyQuantityProjection row : actualRows) {
            actualByDate.put(row.getNgay(), BigDecimal.valueOf(row.getTongSoLuong()));
        }

        TreeSet<LocalDate> commonDates = new TreeSet<>(forecastByDate.keySet());
        commonDates.retainAll(actualByDate.keySet());

        if (commonDates.size() < DRIFT_MIN_OVERLAP_DAYS) {
            return new DriftResponse(productId, warehouseId, "INSUFFICIENT_OVERLAP", null, DRIFT_THRESHOLD_SMAPE,
                    false, commonDates.size());
        }

        double[] actual = new double[commonDates.size()];
        double[] predicted = new double[commonDates.size()];
        int i = 0;
        for (LocalDate date : commonDates) {
            actual[i] = actualByDate.get(date).doubleValue();
            predicted[i] = forecastByDate.get(date).doubleValue();
            i++;
        }
        BigDecimal rollingSmape = BigDecimal.valueOf(computeSmape(actual, predicted)).setScale(4, RoundingMode.HALF_UP);
        boolean drift = rollingSmape.compareTo(DRIFT_THRESHOLD_SMAPE) > 0;

        if (drift) {
            ForecastDriftLog log = new ForecastDriftLog();
            log.setProduct(productRepository.getReferenceById(productId));
            log.setWarehouse(warehouseRepository.getReferenceById(warehouseId));
            log.setActualSmape(rollingSmape);
            log.setRollingSmape(rollingSmape);
            log.setThresholdSmape(DRIFT_THRESHOLD_SMAPE);
            log.setRetrainNeeded(true);
            log.setTargetRetrainNeeded(true);
            log.setComparedDays(commonDates.size());
            forecastDriftLogRepository.save(log);
        }

        return new DriftResponse(productId, warehouseId, drift ? "DRIFT" : "OK", rollingSmape,
                DRIFT_THRESHOLD_SMAPE, drift, commonDates.size());
    }

    // --- Helpers ---

    /**
     * Đồng bộ giao dịch xuất kho THẬT (giao_dich_kho, loại XUAT_KHO) vào ai.lich_su_ban_hang
     * trước khi huấn luyện: ngày nào đã có giao dịch thật thì GHI ĐÈ lên dữ liệu seed giả của
     * ngày đó (đánh dấu nguon=THUC_TE), ngày nào chưa có giao dịch thật thì giữ nguyên seed.
     * Nhờ vậy "train lại" sau khi phát hiện drift mới thực sự phản ánh đúng nhu cầu hiện tại,
     * thay vì học lại y nguyên trên dữ liệu demo tĩnh.
     */
    private void syncRealSalesHistory(Long productId, Long warehouseId) {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(REAL_SALES_LOOKBACK_DAYS);
        List<DailyQuantityProjection> actualRows = inventoryTransactionRepository
                .sumDailyXuatKho(productId, warehouseId, start, end);
        if (actualRows.isEmpty()) {
            return;
        }

        Product productRef = productRepository.getReferenceById(productId);
        Warehouse warehouseRef = warehouseRepository.getReferenceById(warehouseId);
        for (DailyQuantityProjection row : actualRows) {
            SalesHistory entry = salesHistoryRepository
                    .findByProductIdAndWarehouseIdAndNgayAndSource(productId, warehouseId, row.getNgay(),
                            SalesHistorySource.THUC_TE)
                    .orElseGet(SalesHistory::new);
            entry.setProduct(productRef);
            entry.setWarehouse(warehouseRef);
            entry.setNgay(row.getNgay());
            entry.setQuantity(row.getTongSoLuong());
            entry.setSource(SalesHistorySource.THUC_TE);
            salesHistoryRepository.save(entry);
        }
        log.info("[ForecastService] Da dong bo {} ngay giao dich xuat kho thuc te vao lich su ban hang cho SP={} Kho={}",
                actualRows.size(), productId, warehouseId);
    }

    private AiForecastClientResult callAiService(List<SalesHistory> history) {
        List<AiForecastClientRequest.SalesPoint> points = new ArrayList<>(history.size());
        for (SalesHistory row : history) {
            points.add(new AiForecastClientRequest.SalesPoint(row.getNgay().toString(),
                    BigDecimal.valueOf(row.getQuantity()), row.getAverageSellingPrice()));
        }
        AiForecastClientRequest request = new AiForecastClientRequest(points, HORIZONS);
        return aiServiceRestClient.post()
                .uri("/forecast")
                .body(request)
                .retrieve()
                .body(AiForecastClientResult.class);
    }

    private void persistDailyForecasts(ForecastModelMetadata metadata,
            List<AiForecastClientResult.DailyPrediction> dailyPredictions) {
        for (AiForecastClientResult.DailyPrediction prediction : dailyPredictions) {
            DailyForecastResult row = new DailyForecastResult();
            row.setModelMetadata(metadata);
            row.setForecastDate(LocalDate.parse(prediction.date()));
            row.setPredictedQuantity(prediction.predictedQuantity());
            dailyForecastResultRepository.save(row);
        }
    }

    private List<SalesHistory> normalizeDailySeries(List<SalesHistory> history, SalesHistorySource source) {
        if (history.isEmpty()) {
            return history;
        }
        TreeMap<LocalDate, SalesHistory> byDate = new TreeMap<>();
        for (SalesHistory row : history) {
            byDate.merge(row.getNgay(), row, (left, right) -> {
                left.setQuantity(firstNonNull(left.getQuantity(), 0) + firstNonNull(right.getQuantity(), 0));
                if (left.getAverageSellingPrice() == null) {
                    left.setAverageSellingPrice(right.getAverageSellingPrice());
                }
                return left;
            });
        }

        LocalDate end = byDate.lastKey();
        List<SalesHistory> normalized = new ArrayList<>();
        for (LocalDate date = byDate.firstKey(); !date.isAfter(end); date = date.plusDays(1)) {
            SalesHistory row = byDate.get(date);
            if (row == null) {
                row = new SalesHistory();
                row.setNgay(date);
                row.setQuantity(0);
                row.setSource(source);
            }
            normalized.add(row);
        }
        return normalized;
    }

    private Map<Integer, BigDecimal> coldStartForecast(List<SalesHistory> history) {
        int window = Math.min(30, history.size());
        BigDecimal average;
        if (window == 0) {
            average = BigDecimal.ZERO;
        } else {
            long sum = 0;
            for (SalesHistory row : history.subList(history.size() - window, history.size())) {
                sum += row.getQuantity();
            }
            average = BigDecimal.valueOf(sum).divide(BigDecimal.valueOf(window), 2, RoundingMode.HALF_UP);
        }
        Map<Integer, BigDecimal> result = new HashMap<>();
        for (Integer horizon : HORIZONS) {
            result.put(horizon, average);
        }
        return result;
    }

    private int currentStock(Long productId, Long warehouseId) {
        return inventoryLevelRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .map(level -> level.getQuantity() != null ? level.getQuantity() : 0)
                .orElse(0);
    }

    /**
     * Số lượng cần nhập thêm NGAY BÂY GIỜ để tồn kho không tụt dưới ngưỡng tối thiểu
     * trong suốt {@code horizonDays} ngày tới, dựa trên tốc độ tiêu thụ trung bình/ngày
     * dự báo cho đúng khung thời gian đó (forecast7d cho 7 ngày, forecast14d cho 14 ngày, ...).
     */
    private int computeReorderQty(Integer minStock, int currentStock, BigDecimal avgDailyDemand, int horizonDays) {
        if (minStock == null || avgDailyDemand == null) {
            return 0;
        }
        double demandOverHorizon = avgDailyDemand.doubleValue() * horizonDays;
        double forecastedStock = Math.max(0.0, currentStock - demandOverHorizon);
        if (forecastedStock >= minStock) {
            return 0;
        }
        return (int) Math.round(Math.max(0.0, minStock + demandOverHorizon - currentStock));
    }


    private double computeSmape(double[] actual, double[] predicted) {
        double sum = 0;
        int count = 0;
        for (int i = 0; i < actual.length; i++) {
            if (actual[i] == 0 && predicted[i] == 0) {
                continue;
            }
            sum += 2 * Math.abs(actual[i] - predicted[i]) / (Math.abs(actual[i]) + Math.abs(predicted[i]));
            count++;
        }
        return count == 0 ? 0.0 : (sum / count) * 100;
    }

    private static int firstNonNull(Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    private List<ForecastResponse.DailyPoint> toHistoryPoints(List<SalesHistory> history) {
        return history.stream()
                .map(row -> new ForecastResponse.DailyPoint(row.getNgay(), BigDecimal.valueOf(row.getQuantity())))
                .toList();
    }

    private List<ForecastResponse.DailyPoint> toDailyForecastPoints(Long modelMetadataId) {
        return dailyForecastResultRepository.findByModelMetadataIdOrderByForecastDateAsc(modelMetadataId)
                .stream()
                .map(row -> new ForecastResponse.DailyPoint(row.getForecastDate(), row.getPredictedQuantity()))
                .toList();
    }

    private List<SalesHistory> historyForMetadata(Long productId, Long warehouseId, ForecastModelMetadata metadata) {
        SalesHistorySource source = sourceForMetadata(metadata);
        if (source == null) {
            return List.of();
        }
        return normalizeDailySeries(salesHistoryRepository
                .findByProductIdAndWarehouseIdAndSourceOrderByNgayAsc(productId, warehouseId, source), source);
    }

    private String sourceName(ForecastModelMetadata metadata) {
        SalesHistorySource source = sourceForMetadata(metadata);
        return source == null ? null : source.name();
    }

    private SalesHistorySource sourceForMetadata(ForecastModelMetadata metadata) {
        if (metadata.getHistorySource() != null) {
            return metadata.getHistorySource();
        }
        ForecastDatasetType datasetType = metadata.getDatasetType();
        if (datasetType == ForecastDatasetType.THUC_TE || datasetType == ForecastDatasetType.COLD_START) {
            return SalesHistorySource.THUC_TE;
        }
        return null;
    }

    private ForecastDatasetType datasetType(SalesHistorySource source, ForecastMode mode) {
        if (mode == ForecastMode.COLD_START_AVG) {
            return ForecastDatasetType.COLD_START;
        }
        if (source == SalesHistorySource.THUC_TE) {
            return ForecastDatasetType.THUC_TE;
        }
        if (source == SalesHistorySource.EXTERNAL_RETAIL
                || source == SalesHistorySource.EXTERNAL_M5
                || source == SalesHistorySource.EXTERNAL_STORE_ITEM) {
            return ForecastDatasetType.EXTERNAL;
        }
        return ForecastDatasetType.LEGACY_UNKNOWN;
    }

    private static record CapacityLimitInfo(
            int allowed7d, boolean limited7d,
            int allowed14d, boolean limited14d,
            int allowed30d, boolean limited30d,
            String capacityStatus) {}

    private CapacityLimitInfo calculateCapacityLimit(BigDecimal unitVolume, Long warehouseId, int req7d, int req14d, int req30d) {
        if (unitVolume == null) {
            return new CapacityLimitInfo(req7d, false, req14d, false, req30d, false, "UNKNOWN");
        }
        BigDecimal remaining = warehouseCapacityService.getRemainingCapacity(warehouseId);
        if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
            return new CapacityLimitInfo(0, req7d > 0, 0, req14d > 0, 0, req30d > 0, "LIMITED");
        }
        int maxFit = remaining.divide(unitVolume, 0, RoundingMode.DOWN).intValue();
        maxFit = Math.max(0, maxFit);

        boolean lim7 = req7d > maxFit;
        boolean lim14 = req14d > maxFit;
        boolean lim30 = req30d > maxFit;

        String status = (lim7 || lim14 || lim30) ? "LIMITED" : "OK";

        return new CapacityLimitInfo(
                lim7 ? maxFit : req7d, lim7,
                lim14 ? maxFit : req14d, lim14,
                lim30 ? maxFit : req30d, lim30,
                status
        );
    }
}
