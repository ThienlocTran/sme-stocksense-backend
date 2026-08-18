package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.forecast.AiForecastClientRequest;
import com.smartflow.smestocksensebackend.dto.forecast.AiForecastClientResult;
import com.smartflow.smestocksensebackend.dto.forecast.DriftResponse;
import com.smartflow.smestocksensebackend.dto.forecast.ForecastResponse;
import com.smartflow.smestocksensebackend.dto.forecast.SeedHistoryResponse;
import com.smartflow.smestocksensebackend.dto.inventory.DailyQuantityProjection;
import com.smartflow.smestocksensebackend.entity.ForecastDriftLog;
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
import java.util.Random;
import java.util.TreeSet;

@Service
@RequiredArgsConstructor
@Slf4j
public class ForecastServiceImpl implements ForecastService {

    private static final int MIN_HISTORY_DAYS = 60;
    private static final int SEED_HISTORY_DAYS = 180;
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
    private final ForecastModelMetadataRepository forecastModelMetadataRepository;
    private final ForecastDriftLogRepository forecastDriftLogRepository;
    private final RestClient aiServiceRestClient;

    @Override
    @Transactional
    public ForecastResponse runForecast(Long productId, Long warehouseId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy sản phẩm với id " + productId));
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy kho với id " + warehouseId));

        syncRealSalesHistory(productId, warehouseId);

        List<SalesHistory> history = salesHistoryRepository
                .findByProductIdAndWarehouseIdOrderByNgayAsc(productId, warehouseId);
        int dataDays = history.size();

        BigDecimal smape;
        Map<Integer, BigDecimal> forecastByHorizon;
        ForecastMode mode;

        if (dataDays < MIN_HISTORY_DAYS) {
            mode = ForecastMode.COLD_START_AVG;
            smape = BigDecimal.ZERO;
            forecastByHorizon = coldStartForecast(history);
        } else {
            mode = ForecastMode.XGBOOST;
            AiForecastClientResult result = callAiService(history, product.getPrice());
            smape = result.smape() != null ? result.smape() : BigDecimal.ZERO;
            forecastByHorizon = new HashMap<>();
            for (Integer horizon : HORIZONS) {
                BigDecimal value = result.forecast() != null ? result.forecast().get(String.valueOf(horizon)) : null;
                forecastByHorizon.put(horizon, value != null ? value : BigDecimal.ZERO);
            }
        }

        int version = firstNonNull(forecastResultRepository.findMaxVersion(productId, warehouseId), 0) + 1;
        LocalDate baseDate = history.isEmpty() ? LocalDate.now() : history.get(history.size() - 1).getNgay();

        ForecastModelMetadata metadata = new ForecastModelMetadata();
        metadata.setProduct(productRepository.getReferenceById(productId));
        metadata.setWarehouse(warehouseRepository.getReferenceById(warehouseId));
        metadata.setSmape(smape);
        metadata.setVersion(version);
        metadata.setDataDays(dataDays);
        metadata.setMode(mode);
        forecastModelMetadataRepository.save(metadata);

        for (Integer horizon : HORIZONS) {
            ForecastResult forecastResult = new ForecastResult();
            forecastResult.setProduct(productRepository.getReferenceById(productId));
            forecastResult.setWarehouse(warehouseRepository.getReferenceById(warehouseId));
            forecastResult.setForecastDate(baseDate.plusDays(horizon));
            forecastResult.setHorizonDays(horizon);
            forecastResult.setPredictedQuantity(forecastByHorizon.get(horizon));
            forecastResult.setVersion(version);
            forecastResultRepository.save(forecastResult);
        }

        int currentStock = currentStock(productId, warehouseId);
        int minStock = product.getMinStock() != null ? product.getMinStock() : 0;

        return new ForecastResponse(productId, warehouseId, version, mode.name(), smape,
                forecastByHorizon.get(7), forecastByHorizon.get(14), forecastByHorizon.get(30),
                currentStock, minStock,
                computeReorderQty(minStock, currentStock, forecastByHorizon.get(7), 7),
                computeReorderQty(minStock, currentStock, forecastByHorizon.get(14), 14),
                computeReorderQty(minStock, currentStock, forecastByHorizon.get(30), 30),
                dataDays, LocalDateTime.now());
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

        Product product = productRepository.getReferenceById(productId);
        int currentStock = currentStock(productId, warehouseId);
        int minStock = product.getMinStock() != null ? product.getMinStock() : 0;

        return new ForecastResponse(productId, warehouseId, metadata.getVersion(), metadata.getMode().name(),
                metadata.getSmape(), forecastByHorizon.get(7), forecastByHorizon.get(14), forecastByHorizon.get(30),
                currentStock, minStock,
                computeReorderQty(minStock, currentStock, forecastByHorizon.get(7), 7),
                computeReorderQty(minStock, currentStock, forecastByHorizon.get(14), 14),
                computeReorderQty(minStock, currentStock, forecastByHorizon.get(30), 30),
                metadata.getDataDays(), metadata.getCreatedAt());
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
            log.setThresholdSmape(DRIFT_THRESHOLD_SMAPE);
            log.setRetrainNeeded(true);
            forecastDriftLogRepository.save(log);
        }

        return new DriftResponse(productId, warehouseId, drift ? "DRIFT" : "OK", rollingSmape,
                DRIFT_THRESHOLD_SMAPE, drift, commonDates.size());
    }

    @Override
    @Transactional
    public SeedHistoryResponse seedHistory() {
        List<InventoryLevel> levels = inventoryLevelRepository.findAll();
        int productsSeeded = 0;
        int rowsInserted = 0;

        for (InventoryLevel level : levels) {
            Long productId = level.getProduct().getId();
            Long warehouseId = level.getWarehouse().getId();
            if (salesHistoryRepository.countByProductIdAndWarehouseId(productId, warehouseId) >= MIN_HISTORY_DAYS) {
                continue;
            }
            List<SalesHistory> rows = generateSyntheticHistory(productId, warehouseId);
            salesHistoryRepository.saveAll(rows);
            productsSeeded++;
            rowsInserted += rows.size();
        }
        log.info("[ForecastService] Seed du lieu demo: {} san pham/kho, {} dong lich su", productsSeeded, rowsInserted);
        return new SeedHistoryResponse(productsSeeded, rowsInserted);
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
                    .findByProductIdAndWarehouseIdAndNgay(productId, warehouseId, row.getNgay())
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

    private AiForecastClientResult callAiService(List<SalesHistory> history, BigDecimal price) {
        BigDecimal effectivePrice = price != null ? price : BigDecimal.ZERO;
        List<AiForecastClientRequest.SalesPoint> points = new ArrayList<>(history.size());
        for (SalesHistory row : history) {
            points.add(new AiForecastClientRequest.SalesPoint(row.getNgay().toString(),
                    BigDecimal.valueOf(row.getQuantity()), effectivePrice));
        }
        AiForecastClientRequest request = new AiForecastClientRequest(points, HORIZONS);
        return aiServiceRestClient.post()
                .uri("/forecast")
                .body(request)
                .retrieve()
                .body(AiForecastClientResult.class);
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
    private int computeReorderQty(int minStock, int currentStock, BigDecimal avgDailyDemand, int horizonDays) {
        if (avgDailyDemand == null) {
            return 0;
        }
        double demandOverHorizon = avgDailyDemand.doubleValue() * horizonDays;
        double forecastedStock = Math.max(0.0, currentStock - demandOverHorizon);
        if (forecastedStock >= minStock) {
            return 0;
        }
        return (int) Math.round(Math.max(0.0, minStock + demandOverHorizon - currentStock));
    }

    private List<SalesHistory> generateSyntheticHistory(Long productId, Long warehouseId) {
        Random random = new Random(productId * 31L + warehouseId);
        double base = 5 + random.nextInt(20);
        double trendPerDay = (random.nextDouble() - 0.3) * 0.05;
        LocalDate start = LocalDate.now().minusDays(SEED_HISTORY_DAYS);

        List<SalesHistory> rows = new ArrayList<>(SEED_HISTORY_DAYS);
        for (int i = 0; i < SEED_HISTORY_DAYS; i++) {
            LocalDate date = start.plusDays(i);
            double weekly = 1 + 0.3 * Math.sin(2 * Math.PI * date.getDayOfWeek().getValue() / 7.0);
            double noise = (random.nextDouble() - 0.5) * base * 0.4;
            double value = (base + trendPerDay * i) * weekly + noise;
            int quantity = (int) Math.max(0, Math.round(value));

            SalesHistory row = new SalesHistory();
            row.setProduct(productRepository.getReferenceById(productId));
            row.setWarehouse(warehouseRepository.getReferenceById(warehouseId));
            row.setNgay(date);
            row.setQuantity(quantity);
            row.setSource(SalesHistorySource.SEED);
            rows.add(row);
        }
        return rows;
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
}
