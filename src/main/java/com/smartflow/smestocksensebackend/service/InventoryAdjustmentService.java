package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.inventoryadjustment.InventoryAdjustmentResponse;

public interface InventoryAdjustmentService {
    InventoryAdjustmentResponse getOrCreateDraft(Long inventoryCountId);
    InventoryAdjustmentResponse getByInventoryCountId(Long inventoryCountId);
    InventoryAdjustmentResponse get(Long id);
    InventoryAdjustmentResponse submit(Long id);
}
