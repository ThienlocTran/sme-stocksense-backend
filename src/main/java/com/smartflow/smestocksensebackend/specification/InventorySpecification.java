package com.smartflow.smestocksensebackend.specification;

import com.smartflow.smestocksensebackend.dto.inventory.InventoryFilterRequest;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Specification lọc {@link InventoryLevel} theo {@link InventoryFilterRequest}.
 * Kế thừa {@link BaseSpecification} để tái dùng helper build predicate động.
 *
 * keyword tìm theo mã / tên sản phẩm bằng LIKE %keyword% (không phân biệt hoa thường).
 * LEFT JOIN FETCH product + warehouse để tránh N+1 khi map sang DTO (bỏ qua ở query count).
 */
public class InventorySpecification extends BaseSpecification<InventoryLevel> {

    private final InventoryFilterRequest filter;

    public InventorySpecification(InventoryFilterRequest filter) {
        this.filter = filter;
    }

    @Override
    public Specification<InventoryLevel> build() {
        return (root, query, cb) -> {
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("product", JoinType.LEFT);
                root.fetch("warehouse", JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(equal(cb, root.get("warehouse").get("id"), filter.warehouseId()));
            predicates.add(equal(cb, root.get("product").get("category").get("id"), filter.categoryId()));
            predicates.add(keywordOnAnyField(cb, filter.keyword(),
                    root.get("product").get("code"), root.get("product").get("name")));
            predicates.add(greaterThanOrEqual(cb, root.get("quantity"), filter.minQuantity()));
            predicates.add(lessThanOrEqual(cb, root.get("quantity"), filter.maxQuantity()));

            return combineAnd(cb, root, predicates);
        };
    }
}
