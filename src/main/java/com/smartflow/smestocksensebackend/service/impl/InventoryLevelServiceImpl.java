package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryFilterRequest;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryListItemResponse;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryPageResponse;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.service.InventoryLevelService;
import com.smartflow.smestocksensebackend.specification.InventorySpecification;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InventoryLevelServiceImpl implements InventoryLevelService {

    private final InventoryLevelRepository inventoryLevelRepository;

    /**
     * Lọc tồn kho động + phân trang.
     * Specification dùng LEFT JOIN FETCH product/warehouse nên việc map sang DTO
     * (chạy trong cùng transaction readOnly) không phát sinh N+1 hay lazy-init lỗi.
     */
    @Override
    @Transactional(readOnly = true)
    public InventoryPageResponse getInventoryPage(InventoryFilterRequest filter, Pageable pageable) {
        Specification<InventoryLevel> specification = new InventorySpecification(filter).build();

        return InventoryPageResponse.from(
                inventoryLevelRepository
                        .findAll(specification, pageable)
                        .map(InventoryListItemResponse::from)
        );
    }
}
