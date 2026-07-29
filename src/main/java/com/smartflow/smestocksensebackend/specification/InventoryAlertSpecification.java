package com.smartflow.smestocksensebackend.specification;

import com.smartflow.smestocksensebackend.entity.InventoryAlert;
import com.smartflow.smestocksensebackend.entity.InventoryAlertSeverity;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;

public final class InventoryAlertSpecification {

    private InventoryAlertSpecification() {}

    public static Specification<InventoryAlert> hasWarehouseId(Long warehouseId) {
        return (root, query, cb) -> warehouseId == null ? cb.conjunction() : cb.equal(root.get("warehouse").get("id"), warehouseId);
    }

    public static Specification<InventoryAlert> hasProductId(Long productId) {
        return (root, query, cb) -> productId == null ? cb.conjunction() : cb.equal(root.get("product").get("id"), productId);
    }

    public static Specification<InventoryAlert> hasSeverity(InventoryAlertSeverity severity) {
        return (root, query, cb) -> severity == null ? cb.conjunction() : cb.equal(root.get("severity"), severity);
    }

    public static Specification<InventoryAlert> hasStatusIn(List<InventoryAlertStatus> statuses) {
        return (root, query, cb) -> (statuses == null || statuses.isEmpty()) ? cb.conjunction() : root.get("status").in(statuses);
    }

    public static Specification<InventoryAlert> sortByBusinessPriorityAndDate() {
        return (root, query, cb) -> {
            if (query.getResultType() != Long.class && query.getResultType() != long.class) {
                query.orderBy(
                    cb.asc(
                        cb.selectCase()
                            .when(cb.equal(root.get("severity"), InventoryAlertSeverity.CRITICAL), 0)
                            .when(cb.equal(root.get("severity"), InventoryAlertSeverity.WARNING), 1)
                            .otherwise(2)
                    ),
                    cb.desc(root.get("createdAt"))
                );
            }
            return cb.conjunction();
        };
    }
}
