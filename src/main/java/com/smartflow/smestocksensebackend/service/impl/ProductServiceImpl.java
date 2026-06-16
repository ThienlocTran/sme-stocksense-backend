package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.product.ProductListItemResponse;
import com.smartflow.smestocksensebackend.dto.product.ProductPageResponse;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.service.ProductService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;

    /**
     * Lấy danh sách sản phẩm có hỗ trợ tìm kiếm theo tên/SKU và lọc theo trạng thái.
     * Sử dụng JOIN FETCH thông qua Specification để tránh N+1 query với category và partner.
     */
    @Override
    @Transactional(readOnly = true)
    public ProductPageResponse listProducts(String keyword, String status, Pageable pageable) {
        ProductStatus parsedStatus = parseStatus(status);
        String keywordLike = normalizeKeyword(keyword);

        return ProductPageResponse.from(
                productRepository
                        .findAll(buildSpecification(keywordLike, parsedStatus), pageable)
                        .map(ProductListItemResponse::from)
        );
    }

    private Specification<Product> buildSpecification(String keywordLike, ProductStatus status) {
        return (root, query, cb) -> {
            // Eager join để tránh N+1 khi map sang DTO
            if (query != null && !query.getResultType().equals(Long.class)) {
                root.fetch("category", jakarta.persistence.criteria.JoinType.LEFT);
                root.fetch("partner", jakarta.persistence.criteria.JoinType.LEFT);
            }

            List<Predicate> predicates = new ArrayList<>();

            if (keywordLike != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("name")), keywordLike),
                        cb.like(cb.lower(root.get("sku")), keywordLike)
                ));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    private ProductStatus parseStatus(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("status không hợp lệ. Chỉ nhận HOAT_DONG hoặc NGUNG_HOAT_DONG.");
        }
    }
}
