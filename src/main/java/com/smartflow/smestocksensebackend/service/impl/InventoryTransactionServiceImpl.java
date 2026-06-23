package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.InventoryTransaction;
import com.smartflow.smestocksensebackend.entity.InventoryTransactionType;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.InventoryTransactionRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp triển khai các phương thức nghiệp vụ ghi log giao dịch kho.
 * Kế thừa T73, track biến động kho phục vụ đối soát.
 */
@Service
@RequiredArgsConstructor
public class InventoryTransactionServiceImpl implements InventoryTransactionService {

    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional
    public InventoryTransaction recordTransaction(
            Long productId,
            Long warehouseId,
            InventoryTransactionType transactionType,
            Integer quantity,
            Integer quantityBefore,
            Integer quantityAfter,
            ImportReceipt importReceipt,
            String note
    ) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại để ghi nhận giao dịch kho."));

        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Kho hàng không tồn tại để ghi nhận giao dịch kho."));

        InventoryTransaction transaction = new InventoryTransaction();
        transaction.setProduct(product);
        transaction.setWarehouse(warehouse);
        transaction.setTransactionType(transactionType);
        transaction.setQuantity(quantity);
        transaction.setQuantityBefore(quantityBefore);
        transaction.setQuantityAfter(quantityAfter);
        transaction.setImportReceipt(importReceipt);
        transaction.setNote(note);
        transaction.setCreatedBy(currentEmployee());

        return inventoryTransactionRepository.saveAndFlush(transaction);
    }

    private Employee currentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()) {
            Object principal = authentication.getPrincipal();
            if (principal instanceof Employee) {
                return (Employee) principal;
            }
        }
        return null;
    }
}
