package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelResponse;
import com.smartflow.smestocksensebackend.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Validated
@RequestMapping("/api/inventory")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;

    /**
     * Lấy danh sách tồn kho với các bộ lọc linh hoạt.
     * 
     * Trạng thái tồn kho được tính toán theo logic:
     * - OUT_OF_STOCK: so_luong = 0
     * - LOW_STOCK: so_luong <= min_stock
     * - OVER_STOCK: so_luong >= max_stock
     * - NORMAL: các trường hợp khác
     * 
     * @param warehouseId ID của kho (optional)
     * @param productId ID của sản phẩm (optional)
     * @param keyword Từ khóa tìm kiếm (optional, tìm theo mã/tên sản phẩm, mã/tên kho, mã vạch)
     * @param stockStatus Trạng thái tồn: LOW_STOCK, OUT_OF_STOCK, NORMAL, OVER_STOCK (optional)
     * @param warehouseStatus Trạng thái kho: HOAT_DONG, NGUNG_HOAT_DONG (optional)
     * @param productStatus Trạng thái sản phẩm: HOAT_DONG, NGUNG_HOAT_DONG (optional)
     * @param page Số trang (0-indexed, default 0)
     * @param size Số bản ghi trên một trang (1-100, default 20)
     * @return Danh sách tồn kho với phân trang
     */
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public Page<InventoryLevelResponse> listInventory(
            @RequestParam(required = false) @Positive Long warehouseId,
            @RequestParam(required = false) @Positive Long productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(name = "status", required = false) String stockStatus,
            @RequestParam(required = false) String warehouseStatus,
            @RequestParam(required = false) String productStatus,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());
        return inventoryService.listInventory(warehouseId, productId, keyword, stockStatus,
                warehouseStatus, productStatus, pageable);
    }

    /**
     * Lấy danh sách sản phẩm có tồn kho dưới ngưỡng tối thiểu.
     * 
     * Điều kiện: current_quantity <= min_stock
     * 
     * @param warehouseId ID của kho (optional, dùng để lọc theo kho)
     * @param productId ID của sản phẩm (optional, dùng để lọc theo sản phẩm)
     * @param keyword Từ khóa tìm kiếm (optional, tìm theo mã/tên sản phẩm, mã/tên kho, mã vạch)
     * @param warehouseStatus Trạng thái kho: HOAT_DONG, NGUNG_HOAT_DONG (optional)
     * @param productStatus Trạng thái sản phẩm: HOAT_DONG, NGUNG_HOAT_DONG (optional)
     * @param page Số trang (0-indexed, default 0)
     * @param size Số bản ghi trên một trang (1-100, default 20)
     * @return Danh sách sản phẩm tồn kho thấp với phân trang
     */
    @GetMapping("/low-stock")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public Page<InventoryLevelResponse> listLowStockInventory(
            @RequestParam(required = false) @Positive Long warehouseId,
            @RequestParam(required = false) @Positive Long productId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String warehouseStatus,
            @RequestParam(required = false) String productStatus,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.unsorted());
        return inventoryService.listInventory(warehouseId, productId, keyword, "LOW_STOCK",
                warehouseStatus, productStatus, pageable);
    }
}
