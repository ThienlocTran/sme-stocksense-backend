package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventory.AlertDetectionResultResponse;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelProjection;
import com.smartflow.smestocksensebackend.entity.*;
import com.smartflow.smestocksensebackend.event.InventoryLevelChangedEvent;
import com.smartflow.smestocksensebackend.repository.InventoryAlertRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.service.AlertSeverityCalculator;
import com.smartflow.smestocksensebackend.service.InventoryAlertDetectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Triển khai logic phát hiện và chống tạo trùng phiếu cảnh báo tồn kho thấp:
 * - Tận dụng tối đa SQL Single Source of Truth (SSOT) từ T176 thông qua
 * InventoryLevelRepository.
 * - Loại bỏ hoàn toàn phụ thuộc vào ProductRepository và WarehouseRepository do
 * projection đã chứa đủ thông tin (Ponytail).
 * - Phòng thủ 2 lớp (Application Check + DB Partial Unique Index V30) chống
 * Race Condition và spam cảnh báo.
 * - Tuân thủ Separation of Concerns: T179 chỉ làm Deduplication & Quantity
 * Update khi tụt sâu hơn (newQty < oldQty),
 * tuyệt đối KHÔNG lấn sân sang logic leo thang Severity của T180 hay Resolve
 * của T183/T184.
 */
@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class InventoryAlertDetectionServiceImpl implements InventoryAlertDetectionService {

    private final InventoryLevelRepository inventoryLevelRepository;
    private final InventoryAlertRepository inventoryAlertRepository;
    private final AlertSeverityCalculator alertSeverityCalculator;

    private static final String STATUS_HOAT_DONG = "HOAT_DONG";
    private static final String STOCK_STATUS_LOW_STOCK = "LOW_STOCK";


    private static final List<InventoryAlertStatus> ACTIVE_STATUSES = List.of(
            InventoryAlertStatus.OPEN,
            InventoryAlertStatus.ACKNOWLEDGED);

    // T184: Người xử lý hệ thống khi Auto-Resolve (không phải user thủ công)
    private static final String SYSTEM_USER = "System";

    private enum DeduplicationAction {
        CREATED, UPDATED, UNCHANGED, RACE_IGNORED
    }

    @Override
    public AlertDetectionResultResponse scanAndCreateAlerts(Long warehouseId) {
        // 1. Quét danh sách mặt hàng đang tụt kho hoặc hết hàng (LOW_STOCK bao gồm cả
        // OUT_OF_STOCK trong SSOT T176)
        Page<InventoryLevelProjection> lowStockItems = inventoryLevelRepository.findInventory(
                warehouseId, null, null, STOCK_STATUS_LOW_STOCK, STATUS_HOAT_DONG, STATUS_HOAT_DONG,
                Pageable.unpaged());

        List<InventoryLevelProjection> items = lowStockItems.getContent();
        if (items.isEmpty()) {
            log.info("Quét tồn kho kho [{}]: Không phát hiện mặt hàng nào dưới định mức tối thiểu.", warehouseId);
            return AlertDetectionResultResponse.empty();
        }

        int totalScanned = items.size();
        int newAlertsCreated = 0;
        int existingAlertsUpdated = 0;
        int existingAlertsUnchanged = 0;
        int raceConditionIgnored = 0;

        // 2. Duyệt qua từng mặt hàng theo luồng tuần tự đơn giản (Ponytail: không
        // Stream phức tạp, không Async)
        for (InventoryLevelProjection stock : items) {
            DeduplicationAction action = processAlertForStock(stock);
            switch (action) {
                case CREATED -> newAlertsCreated++;
                case UPDATED -> existingAlertsUpdated++;
                case UNCHANGED -> existingAlertsUnchanged++;
                case RACE_IGNORED -> raceConditionIgnored++;
            }
        }

        log.info("Hoàn tất quét tồn kho [{}]: Tổng quét={}, Tạo mới={}, Cập nhật={}, Giữ nguyên={}, Bỏ qua Race={}",
                warehouseId, totalScanned, newAlertsCreated, existingAlertsUpdated, existingAlertsUnchanged,
                raceConditionIgnored);

        return AlertDetectionResultResponse.of(totalScanned, newAlertsCreated, existingAlertsUpdated,
                existingAlertsUnchanged, raceConditionIgnored);
    }

    @Override
    public boolean checkAndCreateAlert(Long productId, Long warehouseId) {
        // 1. Kiểm tra trạng thái tồn kho của riêng mặt hàng này theo SSOT T176
        Page<InventoryLevelProjection> lowStockItems = inventoryLevelRepository.findInventory(
                warehouseId, productId, null, STOCK_STATUS_LOW_STOCK, STATUS_HOAT_DONG, STATUS_HOAT_DONG,
                Pageable.unpaged());

        // 2. Nếu không có trong danh sách LOW_STOCK -> Tồn kho bình thường hoặc ngừng
        // hoạt động -> Trả về false
        if (lowStockItems.isEmpty() || lowStockItems.getContent().isEmpty()) {
            return false;
        }

        InventoryLevelProjection stock = lowStockItems.getContent().get(0);

        // 3. Xử lý deduplication & leo thang số lượng qua helper method
        DeduplicationAction action = processAlertForStock(stock);
        if (action == DeduplicationAction.CREATED || action == DeduplicationAction.UPDATED) {
            log.info("Spot Check: Đã xử lý [{}] phiếu cảnh báo cho Sản phẩm [{}] tại Kho [{}]", action, productId,
                    warehouseId);
            return true;
        }
        return false;
    }

    /**
     * Kiểm tra phiếu cũ theo danh sách trạng thái ACTIVE (OPEN, ACKNOWLEDGED).
     * - Nếu chưa có -> Tạo mới (CREATED) hoặc bắt ngoại lệ từ DB Unique Index
     * (RACE_IGNORED).
     * - Nếu có rồi -> Chỉ cập nhật số lượng khi tụt sâu hơn (UPDATED), nếu giữ
     * nguyên/phục hồi thì bỏ qua (UNCHANGED).
     */
    private DeduplicationAction processAlertForStock(InventoryLevelProjection stock) {
        Optional<InventoryAlert> existingOpt = inventoryAlertRepository.findFirstByProductIdAndWarehouseIdAndStatusIn(
                stock.getProductId(), stock.getWarehouseId(), ACTIVE_STATUSES);

        if (existingOpt.isPresent()) {
            InventoryAlert existingAlert = existingOpt.get();
            int newQty = stock.getCurrentQuantity() != null ? stock.getCurrentQuantity() : 0;
            int oldQty = existingAlert.getCurrentQuantity() != null ? existingAlert.getCurrentQuantity() : 0;

            // Kiểm tra xem phiếu có cần nâng cấp từ WARNING lên CRITICAL do tồn kho cạn
            // kiệt hay không
            boolean escalated = alertSeverityCalculator.evaluateAndApplyEscalation(existingAlert, newQty,
                    stock.getStatus());

            // Note: [T179/T180 - Khối 2: Cập nhật DB khi tụt sâu hoặc leo thang]
            // Chỉ lưu DB khi số lượng tụt sâu hơn trước (newQty < oldQty) hoặc khi xảy ra
            // leo thang (escalated == true).
            // Không lấn sân sang logic Resolve của T183/T184.
            if (newQty < oldQty || escalated) {
                try {
                    if (newQty < oldQty) {
                        existingAlert.setCurrentQuantity(newQty);
                    }
                    inventoryAlertRepository.save(existingAlert);
                    return DeduplicationAction.UPDATED;
                } catch (ObjectOptimisticLockingFailureException | DataIntegrityViolationException e) {
                    log.warn("Race condition hoặc Optimistic lock khi cập nhật phiếu [{}]: {}", existingAlert.getId(),
                            e.getMessage());
                    return DeduplicationAction.RACE_IGNORED;
                }
            } else {
                // Note: [T179/T180 - Khối 3: Bỏ qua khi không thay đổi]
                // Tồn kho giữ nguyên hoặc phục hồi nhẹ -> Bỏ qua không đổi
                return DeduplicationAction.UNCHANGED;
            }
        } else {
            // Khởi tạo và tạo phiếu mới, bắt ngoại lệ từ DB Partial Unique Index (V30).
            try {
                createAndSaveAlert(stock);
                return DeduplicationAction.CREATED;
            } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
                log.warn("Race condition bị cản lại bởi DB unique index cho SP [{}] tại Kho [{}]: {}",
                        stock.getProductId(), stock.getWarehouseId(), e.getMessage());
                return DeduplicationAction.RACE_IGNORED;
            }
        }
    }

    /**
     * Helper method tạo và lưu bản ghi InventoryAlert vào DB từ dữ liệu projection.
     * Sử dụng proxy object cho Product và Warehouse để tránh trụy vấn DB (Ponytail
     * zero N+1).
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
        // Note: [T180 - Khối 4: Tính toán severity ban đầu qua Component chuyên biệt]
        InventoryAlertSeverity severity = alertSeverityCalculator.calculate(currentQty, stock.getStatus());

        InventoryAlert alert = InventoryAlert.builder()
                .product(productProxy)
                .warehouse(warehouseProxy)
                .currentQuantity(currentQty)
                .minStock(stock.getMinStock())
                .maxStock(stock.getMaxStock())
                .severity(severity)
                .status(InventoryAlertStatus.OPEN)
                .handledBy(null) // Phiếu mới sinh ra chưa có nhân viên xử lý
                .note("Tự động sinh bởi hệ thống phát hiện tồn kho thấp (T178/T179)")
                .build();

        inventoryAlertRepository.save(alert);
    }

    // --- T184: Xử lý biến động tồn kho từ Event ---

    @Override
    @Transactional
    public void processInventoryChange(InventoryLevelChangedEvent event) {
        int newQty = event.newQuantity() != null ? event.newQuantity() : 0;
        int minStock = event.minStock() != null ? event.minStock() : 0;

        if (newQty <= minStock) {
            // Khối 1: Tồn kho vẫn ở mức thiếu hụt -> Tạo mới hoặc cập nhật cảnh báo
            handleLowStockFromEvent(event, newQty);
        } else {
            // Khối 2: Tồn kho đã về mức an toàn -> Auto-Resolve nếu có cảnh báo đang mở
            autoResolveAlertFromEvent(event);
        }
    }

    /**
     * [T184 - Khối 1] Tồn kho thiếu hụt: Kiểm tra cảnh báo hiện tại và tạo/cập nhật.
     * Deduplication: nếu đã có cảnh báo OPEN/ACKNOWLEDGED thì cập nhật currentQuantity, không tạo thêm.
     */
    private void handleLowStockFromEvent(InventoryLevelChangedEvent event, int newQty) {
        Optional<InventoryAlert> existingOpt = inventoryAlertRepository
                .findFirstByProductIdAndWarehouseIdAndStatusIn(
                        event.productId(), event.warehouseId(), ACTIVE_STATUSES);

        if (existingOpt.isPresent()) {
            // Đã có cảnh báo đang mở -> Chỉ cập nhật số lượng hiện tại (không tạo thêm, chống duplicate)
            InventoryAlert existing = existingOpt.get();
            existing.setCurrentQuantity(newQty);
            // Hibernate dirty checking tự flush khi commit, không cần gọi save()
            log.info("[T184] Cập nhật số lượng cảnh báo [{}] -> {}", existing.getId(), newQty);
        } else {
            // Chưa có cảnh báo nào -> Tạo mới
            try {
                createAlertFromEvent(event, newQty);
                log.info("[T184] Tạo mới cảnh báo cho SP [{}] tại Kho [{}]",
                        event.productId(), event.warehouseId());
            } catch (DataIntegrityViolationException | ObjectOptimisticLockingFailureException e) {
                // DB Unique Index (T179) đã chặn race condition
                log.warn("[T184] Race condition bị chặn bởi DB constraint: SP [{}] Kho [{}]",
                        event.productId(), event.warehouseId());
            }
        }
    }

    /**
     * [T184 - Khối 2] Tồn kho đã đủ hàng (newQuantity > minStock) -> Auto-Resolve.
     * Chỉ đồng cảnh báo nếu hiện có cảnh báo OPEN hoặc ACKNOWLEDGED.
     * Ghi handledBy = "System" để phân biệt với user xử lý thủ công.
     */
    private void autoResolveAlertFromEvent(InventoryLevelChangedEvent event) {
        Optional<InventoryAlert> existingOpt = inventoryAlertRepository
                .findFirstByProductIdAndWarehouseIdAndStatusIn(
                        event.productId(), event.warehouseId(), ACTIVE_STATUSES);

        if (existingOpt.isEmpty()) {
            // Không có cảnh báo đang mở -> Bỏ qua (no-op)
            return;
        }

        // Gọi resolve() tại Domain method của Entity (Idempotent, bảo vệ state machine)
        InventoryAlert alert = existingOpt.get();
        alert.resolve(SYSTEM_USER);
        // Hibernate dirty checking tự flush, không cần gọi save()
        log.info("[T184] Auto-Resolve cảnh báo [{}] cho SP [{}] tại Kho [{}]",
                alert.getId(), event.productId(), event.warehouseId());
    }

    /**
     * Tạo mới cảnh báo từ event (dùng proxy entity để tránh N+1 query).
     */
    private void createAlertFromEvent(InventoryLevelChangedEvent event, int newQty) {
        Product productProxy = new Product();
        productProxy.setId(event.productId());

        Warehouse warehouseProxy = new Warehouse();
        warehouseProxy.setId(event.warehouseId());

        // Tính severity dựa trên tỷ lệ tồn kho hiện tại / minStock
        // Dùng status string tương đương như SSOT batch scan
        String stockStatus = newQty <= 0 ? "OUT_OF_STOCK" : "LOW_STOCK";
        InventoryAlertSeverity severity = alertSeverityCalculator.calculate(newQty, stockStatus);

        InventoryAlert alert = InventoryAlert.builder()
                .product(productProxy)
                .warehouse(warehouseProxy)
                .currentQuantity(newQty)
                .minStock(event.minStock())
                .severity(severity)
                .status(InventoryAlertStatus.OPEN)
                .handledBy(null)
                .note("Tự động sinh từ biến động tồn kho (T184)")
                .build();

        inventoryAlertRepository.save(alert);
    }
}
