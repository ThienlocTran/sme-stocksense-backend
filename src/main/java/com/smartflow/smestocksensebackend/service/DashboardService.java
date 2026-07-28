package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.dto.response.DashboardOverviewResponse;

public interface DashboardService {
    DashboardOverviewResponse getOverview(Employee employee);
}
