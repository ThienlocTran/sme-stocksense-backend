package com.smartflow.smestocksensebackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardOverviewResponse {
    private OverviewMetricsDTO overview;
    private PendingTasksDTO pendingTasks;
}
