package com.smartflow.smestocksensebackend.dto.forecast;

import java.math.BigDecimal;
import java.util.List;

/**
 * Payload gửi sang AI Forecast service (Python/FastAPI, stateless).
 * Tên field khớp trực tiếp với Pydantic model bên Python (không cần snake_case mapping).
 */
public record AiForecastClientRequest(List<SalesPoint> history, List<Integer> horizons) {

    public record SalesPoint(String date, BigDecimal quantity, BigDecimal price) {
    }
}
