package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.response.InventoryAlertResponse;
import com.smartflow.smestocksensebackend.entity.InventoryAlert;
import com.smartflow.smestocksensebackend.entity.InventoryAlertSeverity;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import com.smartflow.smestocksensebackend.repository.InventoryAlertRepository;
import com.smartflow.smestocksensebackend.service.InventoryAlertQueryService;
import com.smartflow.smestocksensebackend.specification.InventoryAlertSpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryAlertQueryServiceImpl implements InventoryAlertQueryService {

    private final InventoryAlertRepository inventoryAlertRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryAlertResponse> getAlerts(
            Long warehouseId,
            Long productId,
            InventoryAlertSeverity severity,
            List<InventoryAlertStatus> statuses,
            Pageable pageable) {

        // Xử lý logic default status
        List<InventoryAlertStatus> finalStatuses = statuses;
        if (finalStatuses == null || finalStatuses.isEmpty()) {
            finalStatuses = List.of(InventoryAlertStatus.OPEN, InventoryAlertStatus.ACKNOWLEDGED);
        }

        // Tạo specification
        Specification<InventoryAlert> spec = Specification.where(InventoryAlertSpecification.hasStatusIn(finalStatuses))
                .and(InventoryAlertSpecification.hasWarehouseId(warehouseId))
                .and(InventoryAlertSpecification.hasProductId(productId))
                .and(InventoryAlertSpecification.hasSeverity(severity));

        // Xử lý sorting mặc định theo business priority nếu client không truyền sort
        Pageable finalPageable = pageable;
        if (pageable.getSort().isUnsorted()) {
            spec = spec.and(InventoryAlertSpecification.sortByBusinessPriorityAndDate());
            finalPageable = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        }

        // Truy vấn DB với @EntityGraph
        Page<InventoryAlert> alertPage = inventoryAlertRepository.findAll(spec, finalPageable);

        // Map sang DTO
        return alertPage.map(this::mapToResponse);
    }

    private InventoryAlertResponse mapToResponse(InventoryAlert entity) {
        var product = entity.getProduct();
        var warehouse = entity.getWarehouse();

        return InventoryAlertResponse.builder()
                .id(entity.getId())
                .productId(product != null ? product.getId() : null)
                .productCode(product != null ? product.getCode() : null)
                .productName(product != null ? product.getName() : null)
                .warehouseId(warehouse != null ? warehouse.getId() : null)
                .warehouseCode(warehouse != null ? warehouse.getCode() : null)
                .warehouseName(warehouse != null ? warehouse.getName() : null)
                .currentQuantity(entity.getCurrentQuantity())
                .minStock(entity.getMinStock())
                .severity(entity.getSeverity())
                .status(entity.getStatus())
                .note(entity.getNote())
                .handledBy(entity.getHandledBy())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
