package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.common.PageResponse;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelResponse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Set;

@RestController
@Validated
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private static final Set<String> ALLOWED_STOCK_STATUSES = Set.of(
            "OUT_OF_STOCK", "LOW_STOCK", "OVER_STOCK", "NORMAL");
    private static final Set<String> ALLOWED_ACTIVE_STATUSES = Set.of("HOAT_DONG", "NGUNG_HOAT_DONG");

    private final InventoryService inventoryService;

    /**
     * Lấy danh sách tồn kho với các bộ lọc linh hoạt.
     * 
     * Trạng thái tồn kho được tính toán theo logic:
     * - OUT_OF_STOCK: so_luong = 0
     * - LOW_STOCK: so_luong < effectiveMinStock
     * - OVER_STOCK: so_luong >= max_stock
     * - NORMAL: các trường hợp khác
     * 
     * @param warehouseId     ID của kho (optional)
     * @param productId       ID của sản phẩm (optional)
     * @param keyword         Từ khóa tìm kiếm (optional, tìm theo mã/tên sản phẩm,
     *                        mã/tên kho, mã vạch)
     * @param stockStatus     Trạng thái tồn: LOW_STOCK, OUT_OF_STOCK, NORMAL,
     *                        OVER_STOCK (optional)
     * @param warehouseStatus Trạng thái kho: HOAT_DONG, NGUNG_HOAT_DONG (optional)
     * @param productStatus   Trạng thái sản phẩm: HOAT_DONG, NGUNG_HOAT_DONG
     *                        (optional)
     * @param page            Số trang (0-indexed, default 0)
     * @param size            Số bản ghi trên một trang (1-100, default 20)
     * @return Danh sách tồn kho với phân trang
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public PageResponse<InventoryLevelResponse> listInventory(
            @RequestParam(required = false) @Positive Long warehouseId,
            @RequestParam(required = false) @Positive Long productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "status", required = false) String stockStatus,
            @RequestParam(required = false) String warehouseStatus,
            @RequestParam(required = false) String productStatus,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        validateOptionalValue(stockStatus, ALLOWED_STOCK_STATUSES, "Trạng thái tồn kho không hợp lệ");
        validateOptionalValue(warehouseStatus, ALLOWED_ACTIVE_STATUSES, "Trạng thái kho không hợp lệ");
        validateOptionalValue(productStatus, ALLOWED_ACTIVE_STATUSES, "Trạng thái sản phẩm không hợp lệ");
        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());
        return PageResponse.from(inventoryService.listInventory(warehouseId, productId, keyword, stockStatus,
                warehouseStatus, productStatus, pageable));
    }

    /**
     * Lấy danh sách sản phẩm có tồn kho dưới ngưỡng tối thiểu.
     * 
     * Điều kiện: current_quantity < effectiveMinStock
     * 
     * @param warehouseId     ID của kho (optional, dùng để lọc theo kho)
     * @param productId       ID của sản phẩm (optional, dùng để lọc theo sản phẩm)
     * @param keyword         Từ khóa tìm kiếm (optional, tìm theo mã/tên sản phẩm,
     *                        mã/tên kho, mã vạch)
     * @param warehouseStatus Trạng thái kho: HOAT_DONG, NGUNG_HOAT_DONG (optional)
     * @param productStatus   Trạng thái sản phẩm: HOAT_DONG, NGUNG_HOAT_DONG
     *                        (optional)
     * @param page            Số trang (0-indexed, default 0)
     * @param size            Số bản ghi trên một trang (1-100, default 20)
     * @return Danh sách sản phẩm tồn kho thấp với phân trang
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public PageResponse<InventoryLevelResponse> listLowStockInventory(
            @RequestParam(required = false) @Positive Long warehouseId,
            @RequestParam(required = false) @Positive Long productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String warehouseStatus,
            @RequestParam(required = false) String productStatus,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        // Kiểm tra hợp lệ các tham số lọc trạng thái kho và sản phẩm.
        validateOptionalValue(warehouseStatus, ALLOWED_ACTIVE_STATUSES, "Trạng thái kho không hợp lệ");
        validateOptionalValue(productStatus, ALLOWED_ACTIVE_STATUSES, "Trạng thái sản phẩm không hợp lệ");
        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());
        // Gọi service với tham số 'LOW_STOCK' để lấy toàn bộ mặt hàng thuộc diện cảnh
        // báo (bao gồm LOW_STOCK và OUT_OF_STOCK theo Rule 2 trong Spec).
        return PageResponse.from(inventoryService.listInventory(warehouseId, productId, keyword, "LOW_STOCK",
                warehouseStatus, productStatus, pageable));
    }

    private void validateOptionalValue(String value, Set<String> allowedValues, String messagePrefix) {
        if (value == null || value.isBlank()) {
            return;
        }
        String normalized = value.trim().toUpperCase();
        if (!allowedValues.contains(normalized)) {
            throw new BadRequestException(messagePrefix + ": " + value);
        }
    }
}
