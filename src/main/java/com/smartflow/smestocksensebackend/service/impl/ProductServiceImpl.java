package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.product.ProductCreateRequest;
import com.smartflow.smestocksensebackend.dto.product.ProductListItemResponse;
import com.smartflow.smestocksensebackend.dto.product.ProductPageResponse;
import com.smartflow.smestocksensebackend.dto.product.ProductUpdateRequest;
import com.smartflow.smestocksensebackend.dto.product.UpdateProductStatusRequest;
import com.smartflow.smestocksensebackend.entity.Category;
import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.FieldValidationException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.CategoryRepository;
import com.smartflow.smestocksensebackend.repository.PartnerRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.service.ProductService;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final PartnerRepository partnerRepository;

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

    @Override
    @Transactional
    public ProductListItemResponse createProduct(ProductCreateRequest request) {
        // excludeId = null → check toàn bảng
        validateUniqueFields(request.code(), request.sku(), request.barcode(), null);

        Product product = new Product();
        product.setCode(request.code().trim().toUpperCase());
        product.setName(request.name().trim());
        product.setSku(normalizeOptional(request.sku()));
        product.setBarcode(normalizeOptional(request.barcode()));
        product.setUnit(request.unit().trim());
        product.setPrice(request.price());
        product.setCategory(resolveCategory(request.categoryId()));
        product.setPartner(resolvePartner(request.partnerId()));
        product.setStatus(ProductStatus.HOAT_DONG);

        return ProductListItemResponse.from(productRepository.saveAndFlush(product));
    }

    @Override
    @Transactional
    public ProductListItemResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại."));

        // excludeId = id → bỏ qua chính record này khi check trùng
        validateUniqueFields(request.code(), request.sku(), request.barcode(), id);

        product.setCode(request.code().trim().toUpperCase());
        product.setName(request.name().trim());
        product.setSku(normalizeOptional(request.sku()));
        product.setBarcode(normalizeOptional(request.barcode()));
        product.setUnit(request.unit().trim());
        product.setPrice(request.price());
        product.setCategory(resolveCategory(request.categoryId()));
        product.setPartner(resolvePartner(request.partnerId()));
        product.setStatus(parseStatusStrict(request.status()));

        return ProductListItemResponse.from(productRepository.saveAndFlush(product));
    }

    @Override
    @Transactional
    public ProductListItemResponse updateStatus(Long id, UpdateProductStatusRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại."));

        product.setStatus(resolveProductStatus(request.trangThai()));

        return ProductListItemResponse.from(productRepository.saveAndFlush(product));
    }

    // -------------------------------------------------------------------------
    // Unique validation — dùng chung cho CREATE (excludeId=null) và UPDATE
    // -------------------------------------------------------------------------

    /**
     * Collect tất cả lỗi unique trước khi ném một lần duy nhất.
     *
     * @param excludeId null khi CREATE, product.id khi UPDATE
     */
    private void validateUniqueFields(String code, String sku, String barcode, Long excludeId) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (code != null && !code.isBlank()) {
            boolean duplicate = excludeId == null
                    ? productRepository.existsByCodeIgnoreCase(code.trim())
                    : productRepository.existsByCodeIgnoreCaseAndIdNot(code.trim(), excludeId);
            if (duplicate) {
                errors.put("code", "Mã sản phẩm đã tồn tại.");
            }
        }

        if (sku != null && !sku.isBlank()) {
            boolean duplicate = excludeId == null
                    ? productRepository.existsBySkuIgnoreCase(sku.trim())
                    : productRepository.existsBySkuIgnoreCaseAndIdNot(sku.trim(), excludeId);
            if (duplicate) {
                errors.put("sku", "SKU đã tồn tại.");
            }
        }

        if (barcode != null && !barcode.isBlank()) {
            boolean duplicate = excludeId == null
                    ? productRepository.existsByBarcodeIgnoreCase(barcode.trim())
                    : productRepository.existsByBarcodeIgnoreCaseAndIdNot(barcode.trim(), excludeId);
            if (duplicate) {
                errors.put("barcode", "Mã vạch đã tồn tại.");
            }
        }

        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) return null;
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Danh mục không tồn tại."));
    }

    private Partner resolvePartner(Long partnerId) {
        if (partnerId == null) return null;
        return partnerRepository.findById(partnerId)
                .orElseThrow(() -> new NotFoundException("Đối tác không tồn tại."));
    }

    private ProductStatus resolveProductStatus(String value) {
        return switch (value.trim().toUpperCase()) {
            case "ACTIVE", "HOAT_DONG" -> ProductStatus.HOAT_DONG;
            case "INACTIVE", "NGUNG_HOAT_DONG" -> ProductStatus.NGUNG_HOAT_DONG;
            default -> throw new BadRequestException(
                    "trangThai không hợp lệ. Chỉ nhận ACTIVE hoặc INACTIVE.");
        };
    }

    private ProductStatus parseStatus(String status) {
        if (status == null || status.isBlank()) return null;
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("status không hợp lệ. Chỉ nhận HOAT_DONG hoặc NGUNG_HOAT_DONG.");
        }
    }

    private ProductStatus parseStatusStrict(String status) {
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("status không hợp lệ. Chỉ nhận HOAT_DONG hoặc NGUNG_HOAT_DONG.");
        }
    }

    private Specification<Product> buildSpecification(String keywordLike, ProductStatus status) {
        return (root, query, cb) -> {
            if (query != null && !query.getResultType().equals(Long.class)) {
                root.fetch("category", JoinType.LEFT);
                root.fetch("partner", JoinType.LEFT);
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

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) return null;
        return value.trim();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) return null;
        return "%" + keyword.trim().toLowerCase() + "%";
    }
}
