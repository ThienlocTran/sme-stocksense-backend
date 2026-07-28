package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventory.AlertDetectionResultResponse;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelProjection;
import com.smartflow.smestocksensebackend.entity.*;
import com.smartflow.smestocksensebackend.repository.InventoryAlertRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.service.InventoryAlertDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Note: [T178 - Service Impl] Triển khai logic phát hiện tồn kho thấp theo chuẩn Ponytail:
 * - Tận dụng tối đa SQL Single Source of Truth (SSOT) từ T176 thông qua InventoryLevelRepository.
 * - Loại bỏ hoàn toàn phụ thuộc vào ProductRepository và WarehouseRepository do projection đã chứa đủ thông tin.
 * - Chống tạo trùng phiếu cảnh báo đang mở (Deduplication) từ T177 trước khi lưu DB.
 * - Tuân thủ Separation of Concerns: Chỉ CREATE phiếu mới, KHÔNG update/resolve phiếu cũ (để dành cho T183/T184).
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InventoryAlertDetectionServiceImpl implements InventoryAlertDetectionService {

    private final InventoryLevelRepository inventoryLevelRepository;
    private final InventoryAlertRepository inventoryAlertRepository;

    private static final String STATUS_HOAT_DONG = "HOAT_DONG";
    private static final String STOCK_STATUS_LOW_STOCK = "LOW_STOCK";
    private static final String STOCK_STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";

    private static final List<InventoryAlertStatus> ACTIVE_STATUSES = List.of(
            InventoryAlertStatus.OPEN,
            InventoryAlertStatus.ACKNOWLEDGED
    );

    @Override
    public AlertDetectionResultResponse scanAndCreateAlerts(Long warehouseId) {
        // 1. Quét danh sách mặt hàng đang tụt kho hoặc hết hàng (LOW_STOCK bao gồm cả OUT_OF_STOCK trong SSOT T176)
        Page<InventoryLevelProjection> lowStockItems = inventoryLevelRepository.findInventory(
                warehouseId, null, null, STOCK_STATUS_LOW_STOCK, STATUS_HOAT_DONG, STATUS_HOAT_DONG, Pageable.unpaged()
        );

        List<InventoryLevelProjection> items = lowStockItems.getContent();
        if (items.isEmpty()) {
            log.info("Quét tồn kho kho [{}]: Không phát hiện mặt hàng nào dưới định mức tối thiểu.", warehouseId);
            return AlertDetectionResultResponse.empty();
        }

        int totalScanned = items.size();
        int newAlertsCreated = 0;
        int existingAlertsSkipped = 0;

        // 2. Duyệt qua từng mặt hàng theo luồng tuần tự đơn giản (Ponytail: không Stream phức tạp, không Async)
        for (InventoryLevelProjection stock : items) {
            boolean exists = inventoryAlertRepository.existsByProductIdAndWarehouseIdAndStatusIn(
                    stock.getProductId(), stock.getWarehouseId(), ACTIVE_STATUSES
            );

            if (exists) {
                // Đã có phiếu OPEN hoặc ACKNOWLEDGED -> Bỏ qua không tạo trùng (Deduplication)
                existingAlertsSkipped++;
            } else {
                // Chưa có phiếu đang xử lý -> Khởi tạo và lưu phiếu cảnh báo mới
                createAndSaveAlert(stock);
                newAlertsCreated++;
            }
        }

        log.info("Hoàn tất quét tồn kho [{}]: Tổng quét={}, Tạo mới={}, Bỏ qua={}",
                warehouseId, totalScanned, newAlertsCreated, existingAlertsSkipped);

        return AlertDetectionResultResponse.of(totalScanned, newAlertsCreated, existingAlertsSkipped);
    }

    @Override
    public boolean checkAndCreateAlert(Long productId, Long warehouseId) {
        // 1. Kiểm tra trạng thái tồn kho của riêng mặt hàng này theo SSOT T176
        Page<InventoryLevelProjection> lowStockItems = inventoryLevelRepository.findInventory(
                warehouseId, productId, null, STOCK_STATUS_LOW_STOCK, STATUS_HOAT_DONG, STATUS_HOAT_DONG, Pageable.unpaged()
        );

        // 2. Nếu không có trong danh sách LOW_STOCK -> Tồn kho bình thường hoặc ngừng hoạt động -> Trả về false
        if (lowStockItems.isEmpty() || lowStockItems.getContent().isEmpty()) {
            return false;
        }

        InventoryLevelProjection stock = lowStockItems.getContent().get(0);

        // 3. Kiểm tra deduplication: Nếu đã tồn tại phiếu OPEN hoặc ACKNOWLEDGED -> Trả về false (bỏ qua)
        boolean exists = inventoryAlertRepository.existsByProductIdAndWarehouseIdAndStatusIn(
                productId, warehouseId, ACTIVE_STATUSES
        );

        if (exists) {
            return false;
        }

        // 4. Khởi tạo và tạo phiếu cảnh báo mới
        createAndSaveAlert(stock);
        log.info("Spot Check: Đã tạo phiếu cảnh báo tồn kho cho Sản phẩm [{}] tại Kho [{}]", productId, warehouseId);
        return true;
    }

    /**
     * Helper method tạo và lưu bản ghi InventoryAlert vào DB từ dữ liệu projection.
     * Sử dụng proxy object cho Product và Warehouse để tránh truy vấn DB (Ponytail zero N+1).
     */
    private void createAndSaveAlert(InventoryLevelProjection stock) {
        Product productProxy = new Product();
        productProxy.setId(stock.getProductId());
        productProxy.setCode(stock.getProductCode());
        productProxy.setName(stock.getProductName());

        Warehouse warehouseProxy = new Warehouse();
        warehouseProxy.setId(stock.getWarehouseId());
        warehouseProxy.setCode(stock.getWarehouseCode());
        warehouseProxy.setName(stock.getWarehouse());

        int currentQty = stock.getCurrentQuantity() != null ? stock.getCurrentQuantity() : 0;
        InventoryAlertSeverity severity = (currentQty <= 0 || STOCK_STATUS_OUT_OF_STOCK.equals(stock.getStatus()))
                ? InventoryAlertSeverity.CRITICAL
                : InventoryAlertSeverity.WARNING;

        InventoryAlert alert = InventoryAlert.builder()
                .product(productProxy)
                .warehouse(warehouseProxy)
                .currentQuantity(currentQty)
                .minStock(stock.getMinStock())
                .maxStock(stock.getMaxStock())
                .severity(severity)
                .status(InventoryAlertStatus.OPEN)
                .handledBy(null) // Phiếu mới sinh ra chưa có nhân viên xử lý
                .note("Tự động sinh bởi hệ thống phát hiện tồn kho thấp (T178)")
                .build();

        inventoryAlertRepository.save(alert);
    }
}
