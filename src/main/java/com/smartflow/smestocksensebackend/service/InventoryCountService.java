package com.smartflow.smestocksensebackend.service;
import com.smartflow.smestocksensebackend.dto.inventorycount.*;
import org.springframework.data.domain.*;
public interface InventoryCountService {
    InventoryCountResponse create(InventoryCountRequests.Create request);
    Page<InventoryCountResponse> list(Long warehouseId, String status, Pageable pageable);
    InventoryCountResponse get(Long id);
    InventoryCountResponse recordActual(Long id, Long detailId, InventoryCountRequests.RecordActual request);
    InventoryCountResponse finalizeCount(Long id, InventoryCountRequests.Finalize request);
    InventoryCountResponse cancel(Long id, InventoryCountRequests.Cancel request);
}
