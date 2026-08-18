package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.response.InventoryMovementPointResponse;
import com.smartflow.smestocksensebackend.dto.response.StockHealthResponse;
import com.smartflow.smestocksensebackend.dto.response.WarehouseDistributionResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.dto.response.DashboardOverviewResponse;

import java.time.LocalDate;
import java.util.List;

public interface DashboardService {
    DashboardOverviewResponse getOverview(Employee employee);

    List<InventoryMovementPointResponse> getInventoryMovement(Employee employee, LocalDate from, LocalDate to,
            Long warehouseId);

    StockHealthResponse getStockHealth(Employee employee);

    List<WarehouseDistributionResponse> getWarehouseDistribution(Employee employee);
}
