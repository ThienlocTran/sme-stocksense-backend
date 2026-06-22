package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.entity.InventoryTransaction;
import com.smartflow.smestocksensebackend.entity.InventoryTransactionType;

public interface InventoryTransactionService {

    InventoryTransaction applyMovement(Long productId, Long warehouseId, InventoryTransactionType type,
                                       int delta, Long referenceId, Long actorId, String note);
}