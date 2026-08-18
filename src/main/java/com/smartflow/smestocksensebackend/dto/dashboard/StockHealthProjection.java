package com.smartflow.smestocksensebackend.dto.dashboard;

public interface StockHealthProjection {
    Long getHealthy();

    Long getLowStock();

    Long getOutOfStock();
}
