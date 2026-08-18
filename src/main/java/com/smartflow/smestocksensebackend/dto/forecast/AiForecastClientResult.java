package com.smartflow.smestocksensebackend.dto.forecast;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.Map;

/**
 * Kết quả trả về từ AI Forecast service. "train_size"/"test_size" là snake_case
 * bên Python (Pydantic) nên cần @JsonProperty để map đúng sang field Java.
 */
public record AiForecastClientResult(
        BigDecimal smape,
        @JsonProperty("train_size") Integer trainSize,
        @JsonProperty("test_size") Integer testSize,
        Map<String, BigDecimal> forecast) {
}
