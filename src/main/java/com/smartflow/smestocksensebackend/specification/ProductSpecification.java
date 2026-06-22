package com.smartflow.smestocksensebackend.specification;

import com.smartflow.smestocksensebackend.dto.product.ProductSearchCriteria;
import com.smartflow.smestocksensebackend.entity.Product;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

/**
 * Specification lọc {@link Product} dựa trên {@link ProductSearchCriteria}.
 * Kế thừa {@link BaseSpecification} để dùng lại helper build predicate động.
 *
 * Toàn bộ logic lọc nằm ở đây — Controller chỉ gom params, Service chỉ gọi build().
 */
public class ProductSpecification extends BaseSpecification<Product> {

    private final ProductSearchCriteria criteria;

    public ProductSpecification(ProductSearchCriteria criteria) {
        this.criteria = criteria;
    }

    @Override
    public Specification<Product> build() {
        return (root, query, cb) -> {
            // LEFT JOIN FETCH để tránh N+1 khi map sang DTO (bỏ qua ở query count).
            if (query != null && Long.class != query.getResultType()) {
                root.fetch("category", JoinType.LEFT);
                root.fetch("partner", JoinType.LEFT);
            }

            // ArrayList cho phép phần tử null; combineAnd sẽ lọc bỏ điều kiện rỗng.
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(keywordOnAnyField(cb, criteria.keyword(),
                    root.get("name"), root.get("sku"), root.get("code")));
            predicates.add(equal(cb, root.get("category").get("id"), criteria.categoryId()));
            predicates.add(equal(cb, root.get("status"), criteria.status()));

            return combineAnd(cb, root, predicates);
        };
    }
}
