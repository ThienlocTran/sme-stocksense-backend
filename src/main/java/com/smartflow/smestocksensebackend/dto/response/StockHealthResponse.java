package com.smartflow.smestocksensebackend.dto.response;

public record StockHealthResponse(
        Long healthy,
        Long lowStock,
        Long outOfStock
) {
}
