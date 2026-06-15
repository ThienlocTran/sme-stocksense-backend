package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.product.ProductListItemResponse;
import com.smartflow.smestocksensebackend.dto.product.ProductPageResponse;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.service.ProductService;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private static final int MAX_PAGE_SIZE = 100;

    private final ProductRepository productRepository;

    @Override
    @Transactional(readOnly = true)
    public ProductPageResponse listProducts(int page, int size, String keyword, Long categoryId, String status) {
        validatePageRequest(page, size);

        Boolean parsedActive = parseActive(status);
        String keywordLike = normalizeKeyword(keyword);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.asc("code"))
        );

        return ProductPageResponse.from(
                productRepository
                        .findAll(buildSpecification(keywordLike, categoryId, parsedActive), pageRequest)
                        .map(ProductListItemResponse::from)
        );
    }

    private Specification<Product> buildSpecification(String keywordLike, Long categoryId, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Tìm tương đối theo mã nội bộ (code), SKU hoặc tên sản phẩm
            if (keywordLike != null) {
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("code")), keywordLike),
                        cb.like(cb.lower(root.get("sku")), keywordLike),
                        cb.like(cb.lower(root.get("name")), keywordLike)
                ));
            }

            // Lọc chính xác theo danh mục
            if (categoryId != null) {
                predicates.add(cb.equal(root.get("category").get("id"), categoryId));
            }

            // Lọc theo trạng thái hoạt động
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }

            return predicates.isEmpty()
                    ? cb.conjunction()
                    : cb.and(predicates.toArray(new Predicate[0]));
        };
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("page phải lớn hơn hoặc bằng 0.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size phải nằm trong khoảng 1 đến 100.");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    /**
     * Chuyển tham số status từ frontend sang boolean active.
     * "true" / "active" / "1"  → true  (đang hoạt động)
     * "false" / "inactive" / "0" → false (tạm ngưng)
     * null / blank              → null  (không lọc)
     */
    private Boolean parseActive(String status) {
        if (status == null || status.isBlank()) {
            return null;
        }
        return switch (status.trim().toLowerCase()) {
            case "true", "active", "1", "dang_ban" -> true;
            case "false", "inactive", "0", "tam_ngung" -> false;
            default -> throw new BadRequestException("status không hợp lệ. Giá trị hợp lệ: true, false.");
        };
    }
}
