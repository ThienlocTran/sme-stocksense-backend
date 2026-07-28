package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.InventoryService;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.InventoryTransactionType;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelProjection;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelResponse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.service.InventoryTransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Lớp triển khai các phương thức nghiệp vụ của InventoryService.
 * Note: Core service thao tác DB tăng tồn kho. Bắt buộc dùng kèm @Transactional
 * ở lớp gọi ngoài cùng.
 */
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryLevelRepository inventoryLevelRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryTransactionService inventoryTransactionService;

    /**
     * Tăng số lượng tồn kho cho một sản phẩm tại một kho hàng cụ thể (Không log
     * transaction).
     */
    @Override
    @Transactional
    public void increaseInventory(Long productId, Long warehouseId, Integer quantity) {
        increaseInventory(productId, warehouseId, quantity, null);
    }

    /**
     * Tăng số lượng tồn kho cho một sản phẩm tại một kho hàng cụ thể và ghi nhận
     * lịch sử giao dịch (T103).
     * Logic nghiệp vụ:
     * 0. Validate đầu vào: productId, warehouseId, quantity phải > 0.
     * 1. Xác thực xem sản phẩm và kho hàng có tồn tại trong hệ thống hay không.
     * 2. Nếu không tìm thấy -> Ném NotFoundException để kích hoạt rollback
     * transaction.
     * 3. Tìm kiếm bản ghi tồn kho hiện tại (InventoryLevel) với Pessimistic Lock.
     * 4. Nếu chưa tồn tại -> Tạo mới bản ghi với số lượng = quantity (Insert).
     * - Bắt DataIntegrityViolationException phòng race condition: query lại rồi
     * cộng dồn.
     * 5. Nếu đã tồn tại -> Cộng dồn quantity vào số lượng hiện tại (Update).
     * 6. Ghi log giao dịch kho loại NHAP_KHO liên kết phiếu nhập nếu có.
     */
    @Override
    @Transactional
    public void increaseInventory(Long productId, Long warehouseId, Integer quantity, ImportReceipt importReceipt) {
        // 0. Validate đầu vào
        if (productId == null || productId <= 0 || warehouseId == null || warehouseId <= 0 || quantity == null
                || quantity <= 0) {
            throw new BadRequestException("productId, warehouseId, quantity phải > 0");
        }

        // 0.1 Kiểm tra trạng thái phiếu nhập: chỉ cho phép cập nhật tồn kho khi phiếu
        // đã HOAN_THANH
        if (importReceipt != null && !ImportReceiptStatus.HOAN_THANH.equals(importReceipt.getStatus())) {
            throw new ConflictException("Chỉ cập nhật tồn kho khi phiếu nhập đã COMPLETED.");
        }

        // 1. Kiểm tra sự tồn tại của sản phẩm
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại."));

        // 2. Kiểm tra sự tồn tại của kho hàng
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new NotFoundException("Kho hàng không tồn tại."));

        // 3. Tìm bản ghi tồn kho hiện tại với Pessimistic Write Lock
        Optional<InventoryLevel> inventoryLevelOpt = inventoryLevelRepository
                .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId);

        int quantityBefore = 0;
        int quantityAfter = 0;

        if (inventoryLevelOpt.isEmpty()) {
            // 4. Nếu chưa có bản ghi tồn kho -> Tạo mới (Insert)
            quantityBefore = 0;
            quantityAfter = quantity;
            InventoryLevel newInventory = new InventoryLevel();
            newInventory.setProduct(product);
            newInventory.setWarehouse(warehouse);
            newInventory.setQuantity(quantity);
            try {
                inventoryLevelRepository.saveAndFlush(newInventory);
            } catch (DataIntegrityViolationException e) {
                // Race condition: bản ghi đã được tạo bởi transaction khác -> query lại và cộng
                // dồn
                InventoryLevel locked = inventoryLevelRepository
                        .findByProductIdAndWarehouseIdForUpdate(productId, warehouseId)
                        .orElseThrow(() -> e);
                quantityBefore = locked.getQuantity();
                quantityAfter = safeAddQuantity(quantityBefore, quantity);
                locked.setQuantity(quantityAfter);
                inventoryLevelRepository.saveAndFlush(locked);
            }
        } else {
            // 5. Nếu đã tồn tại -> Cập nhật cộng dồn (Update)
            InventoryLevel existingInventory = inventoryLevelOpt.get();
            quantityBefore = existingInventory.getQuantity();
            quantityAfter = safeAddQuantity(quantityBefore, quantity);
            existingInventory.setQuantity(quantityAfter);
            inventoryLevelRepository.saveAndFlush(existingInventory);
        }

        // 6. Ghi log giao dịch kho loại NHAP_KHO nếu có phiếu nhập
        if (importReceipt != null) {
            // Kế thừa T73, track biến động kho phục vụ đối soát
            inventoryTransactionService.recordTransaction(
                    productId,
                    warehouseId,
                    InventoryTransactionType.NHAP_KHO,
                    quantity,
                    quantityBefore,
                    quantityAfter,
                    importReceipt,
                    "Kế thừa T73, track biến động kho phục vụ đối soát");
        }
    }

    /**
     * Cộng số lượng tồn kho an toàn, tránh lỗi tràn số nguyên (Integer overflow).
     *
     * @param current Số lượng tồn hiện tại
     * @param delta   Số lượng cần cộng thêm
     * @return Tổng số lượng sau khi cộng
     * @throws IllegalArgumentException nếu tổng vượt quá Integer.MAX_VALUE
     */
    private int safeAddQuantity(int current, int delta) {
        try {
            return Math.addExact(current, delta);
        } catch (ArithmeticException ex) {
            throw new BadRequestException("Tong so luong ton kho vuot gioi han cho phep.");
        }
    }

    /**
     * Lấy danh sách tồn kho với các bộ lọc chi tiết.
     * 
     * Hỗ trợ lọc theo:
     * - Kho hàng (warehouseId)
     * - Sản phẩm (productId)
     * - Từ khóa tìm kiếm (keyword: mã/tên sản phẩm, mã/tên kho, mã vạch)
     * - Trạng thái tồn kho (stockStatus)
     * - Trạng thái kho (warehouseStatus)
     * - Trạng thái sản phẩm (productStatus)
     * 
     * @param warehouseId     ID kho (null để không lọc)
     * @param productId       ID sản phẩm (null để không lọc)
     * @param keyword         từ khóa tìm kiếm (null để không lọc)
     * @param stockStatus     trạng thái tồn: LOW_STOCK, OUT_OF_STOCK, NORMAL,
     *                        OVER_STOCK (null để không lọc)
     * @param warehouseStatus trạng thái kho: HOAT_DONG, NGUNG_HOAT_DONG (null để
     *                        không lọc)
     * @param productStatus   trạng thái sản phẩm: HOAT_DONG, NGUNG_HOAT_DONG (null
     *                        để không lọc)
     * @param pageable        thông tin phân trang và sắp xếp
     * @return danh sách tồn kho phân trang
     * @throws NotFoundException   nếu warehouseId hoặc productId không tồn tại
     * @throws BadRequestException nếu stockStatus, warehouseStatus hoặc
     *                             productStatus không hợp lệ
     */
    @Override
    @Transactional(readOnly = true)
    public Page<InventoryLevelResponse> listInventory(Long warehouseId, Long productId, String keyword,
            String stockStatus, String warehouseStatus, String productStatus, Pageable pageable) {
        // Note: [T176 - Khối 1] Kiểm tra tính hợp lệ của kho hàng và sản phẩm (nhanh
        // chóng fail fast nếu ID không tồn tại).
        if (warehouseId != null && !warehouseRepository.existsById(warehouseId)) {
            throw new com.smartflow.smestocksensebackend.exception.NotFoundException("Kho hàng không tồn tại.");
        }
        if (productId != null && !productRepository.existsById(productId)) {
            throw new com.smartflow.smestocksensebackend.exception.NotFoundException("Sản phẩm không tồn tại.");
        }

        String normalizedStockStatus;
        String normalizedWarehouseStatus;
        String normalizedProductStatus;

        // Chuẩn hóa các tham số lọc trạng thái đầu vào, đảm bảo nhất quán trước khi đẩy
        // xuống DB.
        try {
            normalizedStockStatus = normalizeStockStatus(stockStatus);
            normalizedWarehouseStatus = normalizeActiveStatus(warehouseStatus);
            normalizedProductStatus = normalizeActiveStatus(productStatus);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException(e.getMessage());
        }

        String keywordParam = (keyword == null || keyword.isBlank()) ? null : "%" + keyword.trim() + "%";

        // Truy vấn trực tiếp từ Repository SQL (Single Source of Truth cho việc phân
        // loại trạng thái).
        Page<InventoryLevelProjection> result = inventoryLevelRepository.findInventory(warehouseId, productId,
                keywordParam, normalizedStockStatus, normalizedWarehouseStatus, normalizedProductStatus, pageable);

        return result.map(this::mapProjectionToResponse);
    }

    /**
     * Chuẩn hóa trạng thái tồn kho từ input người dùng.
     * 
     * Chấp nhận các giá trị:
     * - ZERO, OUT_OF_STOCK, OUT_OFSTOCK → OUT_OF_STOCK
     * - LOW, LOW_STOCK → LOW_STOCK
     * - HIGH, OVER_STOCK, OVERSTOCK → OVER_STOCK
     * - NORMAL → NORMAL
     * 
     * @param stockStatus giá trị input (không phân biệt chữ hoa/thường, hỗ trợ '-'
     *                    và '_')
     * @return giá trị chuẩn hóa hoặc null nếu input null/blank
     * @throws IllegalArgumentException nếu giá trị không hợp lệ
     */
    private String normalizeStockStatus(String stockStatus) {
        if (stockStatus == null || stockStatus.isBlank()) {
            return null;
        }

        // Note: [T176 - Khối 4] Chuyển đổi các biến thể status input về chuỗi chuẩn hóa
        // tương ứng với các case trong SQL Repository.
        String normalized = stockStatus.trim().toUpperCase().replace('-', '_');
        return switch (normalized) {
            case "ZERO", "OUT_OF_STOCK", "OUT_OFSTOCK" -> "OUT_OF_STOCK";
            case "LOW", "LOW_STOCK" -> "LOW_STOCK";
            case "HIGH", "OVER_STOCK", "OVERSTOCK" -> "OVER_STOCK";
            case "NORMAL" -> "NORMAL";
            default -> throw new IllegalArgumentException("Trạng thái tồn kho không hợp lệ: " + stockStatus);
        };
    }

    /**
     * Chuẩn hóa trạng thái hoạt động (kho/sản phẩm) từ input người dùng.
     * 
     * Chấp nhận các giá trị:
     * - ACTIVE, HOAT_DONG → HOAT_DONG
     * - INACTIVE, NGUNG_HOAT_DONG → NGUNG_HOAT_DONG
     * 
     * @param status giá trị input (không phân biệt chữ hoa/thường)
     * @return giá trị chuẩn hóa hoặc null nếu input null/blank
     * @throws IllegalArgumentException nếu giá trị không hợp lệ
     */
    private String normalizeActiveStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }

        return switch (status.trim().toUpperCase()) {
            case "ACTIVE", "HOAT_DONG" -> "HOAT_DONG";
            case "INACTIVE", "NGUNG_HOAT_DONG" -> "NGUNG_HOAT_DONG";
            default -> throw new IllegalArgumentException("Trạng thái hoạt động không hợp lệ: " + status);
        };
    }

    /**
     * Ánh xạ từ InventoryLevelProjection (native query result) sang
     * InventoryLevelResponse DTO.
     * 
     * @param projection kết quả từ native query
     * @return DTO response để trả về client
     */
    private InventoryLevelResponse mapProjectionToResponse(InventoryLevelProjection projection) {
        return new InventoryLevelResponse(
                projection.getInventoryId(),
                projection.getProductId(),
                projection.getProductCode(),
                projection.getProductName(),
                projection.getBarcode(),
                projection.getWarehouseId(),
                projection.getWarehouseCode(),
                projection.getWarehouse(),
                projection.getCurrentQuantity(),
                projection.getMinStock(),
                projection.getMaxStock(),
                projection.getProductStatus(),
                projection.getWarehouseStatus(),
                projection.getStatus(),
                projection.getLastUpdatedAt());
    }
}
