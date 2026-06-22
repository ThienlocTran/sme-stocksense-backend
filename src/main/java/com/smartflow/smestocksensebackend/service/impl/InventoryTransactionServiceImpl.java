package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.*;
import com.smartflow.smestocksensebackend.exception.BadRequestException; // Sửa import này nếu project dùng tên khác
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.InventoryTransactionRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryTransactionServiceImpl implements InventoryTransactionService {

    private final InventoryLevelRepository inventoryLevelRepository;
    private final InventoryTransactionRepository inventoryTransactionRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public InventoryTransaction applyMovement(Long productId, Long warehouseId, InventoryTransactionType type,
                                              int delta, Long referenceId, Long actorId, String note) {

        log.info("Bắt đầu ghi nhận giao dịch kho: type={}, productId={}, warehouseId={}, delta={}", type, productId, warehouseId, delta);

        // 1. Khóa dòng tồn kho hiện tại (Dùng Pessimistic Lock + Version)
        InventoryLevel inventory = inventoryLevelRepository
                .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                .orElse(null);

        int quantityBefore = 0;
        int quantityAfter = 0;

        if (inventory == null) {
            // Trường hợp: CHƯA TỪNG CÓ TỒN KHO NÀY
            if (delta < 0) {
                throw new BadRequestException("Không thể xuất kho: Sản phẩm chưa từng được nhập vào kho này.");
            }
            quantityAfter = delta;

            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy sản phẩm có ID: " + productId));
            Warehouse warehouse = warehouseRepository.findById(warehouseId)
                    .orElseThrow(() -> new BadRequestException("Không tìm thấy kho có ID: " + warehouseId));

            inventory = InventoryLevel.builder()
                    .product(product)
                    .warehouse(warehouse)
                    .quantity(quantityAfter)
                    .version(0L) // Khởi tạo version đầu tiên
                    .build();
        } else {
            // Trường hợp: ĐÃ CÓ SẴN TỒN KHO
            quantityBefore = inventory.getQuantity();
            quantityAfter = quantityBefore + delta;

            if (quantityAfter < 0) {
                throw new BadRequestException("Không đủ số lượng tồn kho. Hiện tại: " + quantityBefore + ", Yêu cầu trừ: " + Math.abs(delta));
            }
            inventory.setQuantity(quantityAfter);
        }

        // 2. Lưu tồn kho
        inventoryLevelRepository.save(inventory);

// 3. Ghi sổ lịch sử giao dịch

        // Tạo các "vỏ rỗng" chứa ID để Hibernate tự hiểu khóa ngoại mà không cần chọc xuống DB (tối ưu hiệu năng)
        ImportReceipt importReceiptRef = null;
        if (type == InventoryTransactionType.NHAP_KHO && referenceId != null) {
            importReceiptRef = new ImportReceipt();
            importReceiptRef.setId(referenceId);
        }

        Employee createdByRef = null;
        if (actorId != null) {
            createdByRef = new Employee();
            createdByRef.setId(actorId);
        }

        InventoryTransaction transaction = InventoryTransaction.builder()
                .product(inventory.getProduct())
                .warehouse(inventory.getWarehouse())
                .transactionType(type)
                .quantity(delta) // Thêm trường quantity (so_luong) mà Kiro đẻ ra
                .quantityBefore(quantityBefore)
                .quantityAfter(quantityAfter)
                .importReceipt(importReceiptRef)
                .exportReceiptId(type == InventoryTransactionType.XUAT_KHO ? referenceId : null)
                .importBatchId(type == InventoryTransactionType.NHAP_DAU_KY ? referenceId : null) // Đổi thành importBatchId
                .createdBy(createdByRef)
                .createdAt(LocalDateTime.now())
                .note(note)
                .build();

        return inventoryTransactionRepository.save(transaction);
    }
}