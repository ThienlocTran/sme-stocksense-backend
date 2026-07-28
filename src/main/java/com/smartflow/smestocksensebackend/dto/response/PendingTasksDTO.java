package com.smartflow.smestocksensebackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingTasksDTO {
    private long importReceipts;
    private long exportReceipts;
    private long inventoryAlerts;
}
