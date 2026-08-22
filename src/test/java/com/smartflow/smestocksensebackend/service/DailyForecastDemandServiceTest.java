package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.entity.DailyForecastResult;
import com.smartflow.smestocksensebackend.entity.ForecastModelMetadata;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.DailyForecastResultRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DailyForecastDemandServiceTest {

    @Mock DailyForecastResultRepository repository;

    @Test
    void sevenDayDemandSumsFirstSevenPredictionsByDate() {
        DailyForecastDemandService service = new DailyForecastDemandService(repository);
        ForecastModelMetadata model = model(30L, 2, 10L, 20L);
        when(repository.findByModelMetadataIdOrderByForecastDateAsc(30L)).thenReturn(rows(30));

        var result = service.sumHorizonDemand(model, (short) 7, 10L, 20L);

        assertEquals(new BigDecimal("28"), result.forecastDemand());
        assertEquals(30L, result.modelMetadataId());
        assertEquals(2, result.modelVersion());
    }

    @Test
    void fourteenDayDemandSumsFirstFourteenPredictions() {
        DailyForecastDemandService service = new DailyForecastDemandService(repository);
        ForecastModelMetadata model = model(30L, 2, 10L, 20L);
        when(repository.findByModelMetadataIdOrderByForecastDateAsc(30L)).thenReturn(rows(30));

        assertEquals(new BigDecimal("105"),
                service.sumHorizonDemand(model, (short) 14, 10L, 20L).forecastDemand());
    }

    @Test
    void thirtyDayDemandSumsFirstThirtyPredictions() {
        DailyForecastDemandService service = new DailyForecastDemandService(repository);
        ForecastModelMetadata model = model(30L, 2, 10L, 20L);
        when(repository.findByModelMetadataIdOrderByForecastDateAsc(30L)).thenReturn(rows(30));

        assertEquals(new BigDecimal("465"),
                service.sumHorizonDemand(model, (short) 30, 10L, 20L).forecastDemand());
    }

    @Test
    void wrongModelVersionExcludedByUsingExactModelId() {
        DailyForecastDemandService service = new DailyForecastDemandService(repository);
        ForecastModelMetadata model = model(30L, 2, 10L, 20L);
        when(repository.findByModelMetadataIdOrderByForecastDateAsc(30L)).thenReturn(rows(7));

        service.sumHorizonDemand(model, (short) 7, 10L, 20L);

        verify(repository).findByModelMetadataIdOrderByForecastDateAsc(30L);
    }

    @Test
    void wrongProductWarehouseRejected() {
        DailyForecastDemandService service = new DailyForecastDemandService(repository);

        assertThrows(BadRequestException.class,
                () -> service.sumHorizonDemand(model(30L, 2, 99L, 20L), (short) 7, 10L, 20L));
    }

    @Test
    void incompleteDailyRowsHandledExplicitly() {
        DailyForecastDemandService service = new DailyForecastDemandService(repository);
        ForecastModelMetadata model = model(30L, 2, 10L, 20L);
        when(repository.findByModelMetadataIdOrderByForecastDateAsc(30L)).thenReturn(rows(29));

        assertThrows(BadRequestException.class,
                () -> service.sumHorizonDemand(model, (short) 30, 10L, 20L));
    }

    @Test
    void gapInForecastDatesHandledExplicitly() {
        DailyForecastDemandService service = new DailyForecastDemandService(repository);
        ForecastModelMetadata model = model(30L, 2, 10L, 20L);
        List<DailyForecastResult> rows = rows(7);
        rows.get(3).setForecastDate(LocalDate.of(2026, 1, 10));
        when(repository.findByModelMetadataIdOrderByForecastDateAsc(30L)).thenReturn(rows);

        assertThrows(BadRequestException.class,
                () -> service.sumHorizonDemand(model, (short) 7, 10L, 20L));
    }

    @Test
    void noAverageTimesHorizonFallback() {
        DailyForecastDemandService service = new DailyForecastDemandService(repository);
        ForecastModelMetadata model = model(30L, 2, 10L, 20L);
        when(repository.findByModelMetadataIdOrderByForecastDateAsc(30L)).thenReturn(rows(6));

        assertThrows(BadRequestException.class,
                () -> service.sumHorizonDemand(model, (short) 7, 10L, 20L));
    }

    private List<DailyForecastResult> rows(int count) {
        return IntStream.rangeClosed(1, count)
                .mapToObj(day -> row(LocalDate.of(2026, 1, day), BigDecimal.valueOf(day)))
                .toList();
    }

    private DailyForecastResult row(LocalDate date, BigDecimal quantity) {
        DailyForecastResult result = new DailyForecastResult();
        result.setForecastDate(date);
        result.setPredictedQuantity(quantity);
        return result;
    }

    private ForecastModelMetadata model(Long id, Integer version, Long productId, Long warehouseId) {
        Product product = new Product();
        ReflectionTestUtils.setField(product, "id", productId);
        Warehouse warehouse = new Warehouse();
        ReflectionTestUtils.setField(warehouse, "id", warehouseId);
        ForecastModelMetadata model = new ForecastModelMetadata();
        ReflectionTestUtils.setField(model, "id", id);
        model.setVersion(version);
        model.setProduct(product);
        model.setWarehouse(warehouse);
        return model;
    }
}
