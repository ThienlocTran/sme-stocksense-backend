package com.smartflow.smestocksensebackend.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * Kết quả trả về từ AI Forecast service. "train_size"/"test_size" là snake_case
 * bên Python (Pydantic) nên cần @JsonProperty để map đúng sang field Java.
 */
public record AiForecastClientResult(
        BigDecimal smape,
        BigDecimal mae,
        BigDecimal rmse,
        @JsonProperty("train_size") Integer trainSize,
        @JsonProperty("test_size") Integer testSize,
        Map<String, BigDecimal> forecast,
        @JsonProperty("daily_predictions") List<DailyPrediction> dailyPredictions) {

    public record DailyPrediction(
            String date,
            @JsonProperty("predicted_quantity") BigDecimal predictedQuantity) {
    }
}
