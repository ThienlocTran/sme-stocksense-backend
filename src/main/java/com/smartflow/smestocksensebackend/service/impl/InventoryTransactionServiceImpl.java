package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.InventoryTransaction;
import com.smartflow.smestocksensebackend.entity.InventoryTransactionType;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.InventoryTransactionRepository;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryTransactionResponse;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.InventoryTransactionService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Lớp triển khai các phương thức nghiệp vụ ghi log giao dịch kho.
 * Kế thừa T73, track biến động kho phục vụ đối soát.
 */
@Service
@Slf4j
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
            String note) {
        validateInput(productId, warehouseId, transactionType, quantity, quantityBefore, quantityAfter);

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

    private void validateInput(Long productId, Long warehouseId, InventoryTransactionType transactionType,
            Integer quantity, Integer quantityBefore, Integer quantityAfter) {
        if (productId == null || productId <= 0) {
            throw new BadRequestException("ID sản phẩm không hợp lệ.");
        }
        if (warehouseId == null || warehouseId <= 0) {
            throw new BadRequestException("ID kho không hợp lệ.");
        }
        if (transactionType == null) {
            throw new BadRequestException("Loại giao dịch kho không được để trống.");
        }
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Số lượng giao dịch phải lớn hơn 0.");
        }
        if (quantityBefore == null || quantityAfter == null) {
            throw new BadRequestException("Số lượng trước/sau không được để trống.");
        }

        int expectedAfter = switch (transactionType) {
            case NHAP_KHO, NHAP_DAU_KY, DIEU_CHINH_TANG -> quantityBefore + quantity;
            case XUAT_KHO, DIEU_CHINH_GIAM -> quantityBefore - quantity;
        };

        if (quantityAfter != expectedAfter) {
            throw new BadRequestException("Số lượng sau không khớp với số lượng trước và số lượng thay đổi.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Page<InventoryTransactionResponse> searchTransactions(
            String keyword,
            Long productId,
            Long warehouseId,
            InventoryTransactionType transactionType,
            Long createdById,
            LocalDateTime from,
            LocalDateTime to,
            Pageable pageable) {
        Specification<InventoryTransaction> spec = buildSpecification(keyword, productId, warehouseId, transactionType,
                createdById, from, to);

        Page<InventoryTransaction> page = inventoryTransactionRepository.findAll(spec, pageable);
        return page.map(this::toResponse);
    }

    private Specification<InventoryTransaction> buildSpecification(
            String keyword,
            Long productId,
            Long warehouseId,
            InventoryTransactionType transactionType,
            Long createdById,
            LocalDateTime from,
            LocalDateTime to) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keyword != null && !keyword.isBlank()) {
                String likeKeyword = "%" + keyword.trim().toLowerCase() + "%";
                Join<Object, Object> productJoin = root.join("product", JoinType.LEFT);
                Join<Object, Object> createdByJoin = root.join("createdBy", JoinType.LEFT);

                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(productJoin.get("code")), likeKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(productJoin.get("name")), likeKeyword),
                        criteriaBuilder.like(criteriaBuilder.lower(createdByJoin.get("fullName")), likeKeyword)));
            }

            if (productId != null) {
                predicates.add(criteriaBuilder.equal(root.get("product").get("id"), productId));
            }

            if (warehouseId != null) {
                predicates.add(criteriaBuilder.equal(root.get("warehouse").get("id"), warehouseId));
            }

            if (transactionType != null) {
                predicates.add(criteriaBuilder.equal(root.get("transactionType"), transactionType));
            }

            if (createdById != null) {
                predicates.add(criteriaBuilder.equal(root.get("createdBy").get("id"), createdById));
            }

            if (from != null && to != null) {
                predicates.add(criteriaBuilder.between(root.get("createdAt"), from, to));
            } else if (from != null) {
                predicates.add(criteriaBuilder.greaterThanOrEqualTo(root.get("createdAt"), from));
            } else if (to != null) {
                predicates.add(criteriaBuilder.lessThanOrEqualTo(root.get("createdAt"), to));
            }

            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private InventoryTransactionResponse toResponse(InventoryTransaction t) {
        InventoryTransactionResponse r = new InventoryTransactionResponse();
        r.setId(t.getId());
        if (t.getProduct() != null) {
            r.setProductId(t.getProduct().getId());
            r.setProductCode(t.getProduct().getCode());
            r.setProductName(t.getProduct().getName());
        }
        if (t.getWarehouse() != null) {
            r.setWarehouseId(t.getWarehouse().getId());
            r.setWarehouseCode(t.getWarehouse().getCode());
            r.setWarehouseName(t.getWarehouse().getName());
        }
        r.setTransactionType(t.getTransactionType() != null ? t.getTransactionType().name() : null);
        r.setQuantity(t.getQuantity());
        r.setQuantityBefore(t.getQuantityBefore());
        r.setQuantityAfter(t.getQuantityAfter());
        r.setNote(t.getNote());
        if (t.getCreatedBy() != null) {
            r.setCreatedById(t.getCreatedBy().getId());
            r.setCreatedByName(t.getCreatedBy().getFullName());
        }
        r.setCreatedAt(t.getCreatedAt());
        r.setImportReceiptId(t.getImportReceipt() != null ? t.getImportReceipt().getId() : null);
        r.setExportReceiptId(t.getExportReceipt() != null ? t.getExportReceipt().getId() : null);
        return r;
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
