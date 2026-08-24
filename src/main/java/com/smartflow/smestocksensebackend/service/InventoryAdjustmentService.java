package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.inventoryadjustment.InventoryAdjustmentResponse;
import com.smartflow.smestocksensebackend.dto.inventoryadjustment.RejectInventoryAdjustmentRequest;

public interface InventoryAdjustmentService {
    InventoryAdjustmentResponse getOrCreateDraft(Long inventoryCountId);
    InventoryAdjustmentResponse getByInventoryCountId(Long inventoryCountId);
    InventoryAdjustmentResponse get(Long id);
    InventoryAdjustmentResponse submit(Long id);
    InventoryAdjustmentResponse approve(Long id);
    InventoryAdjustmentResponse reject(Long id, RejectInventoryAdjustmentRequest request);
}
