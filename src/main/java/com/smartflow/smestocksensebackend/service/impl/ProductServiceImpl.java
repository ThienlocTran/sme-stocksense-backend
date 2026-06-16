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

    /**
     * Lấy danh sách sản phẩm có hỗ trợ tìm kiếm theo tên/SKU và lọc theo trạng thái.
     * Dùng LEFT JOIN FETCH trong Specification để tránh N+1 với category và partner.
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

    /**
     * Tạo mới sản phẩm:
     * - Validate trùng code, SKU, barcode.
     * - Resolve Category và Partner theo ID (nullable).
     * - Set mặc định status = HOAT_DONG.
     */
    @Override
    @Transactional
    public ProductListItemResponse createProduct(ProductCreateRequest request) {
        validateUniqueFields(request);

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

    /**
     * Cập nhật sản phẩm theo id:
     * - Ném NotFoundException nếu không tìm thấy.
     * - Validate unique code/sku/barcode, loại trừ chính id đang cập nhật.
     * - Resolve lại Category và Partner.
     * - Cho phép cập nhật status.
     */
    @Override
    @Transactional
    public ProductListItemResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại."));

        validateUniqueFieldsForUpdate(request, id);

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

    // -------------------------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------------------------

    private void validateUniqueFields(ProductCreateRequest request) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (productRepository.existsByCodeIgnoreCase(request.code().trim())) {
            errors.put("code", "Mã sản phẩm đã tồn tại.");
        }
        if (request.sku() != null && !request.sku().isBlank()
                && productRepository.existsBySkuIgnoreCase(request.sku().trim())) {
            errors.put("sku", "SKU đã tồn tại.");
        }
        if (request.barcode() != null && !request.barcode().isBlank()
                && productRepository.existsByBarcodeIgnoreCase(request.barcode().trim())) {
            errors.put("barcode", "Mã vạch đã tồn tại.");
        }

        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
    }

    private void validateUniqueFieldsForUpdate(ProductUpdateRequest request, Long excludedId) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (productRepository.existsByCodeIgnoreCaseAndIdNot(request.code().trim(), excludedId)) {
            errors.put("code", "Mã sản phẩm đã tồn tại.");
        }
        if (request.sku() != null && !request.sku().isBlank()
                && productRepository.existsBySkuIgnoreCaseAndIdNot(request.sku().trim(), excludedId)) {
            errors.put("sku", "SKU đã tồn tại.");
        }
        if (request.barcode() != null && !request.barcode().isBlank()
                && productRepository.existsByBarcodeIgnoreCaseAndIdNot(request.barcode().trim(), excludedId)) {
            errors.put("barcode", "Mã vạch đã tồn tại.");
        }

        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
    }

    private Category resolveCategory(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new NotFoundException("Danh mục không tồn tại."));
    }

    private Partner resolvePartner(Long partnerId) {
        if (partnerId == null) {
            return null;
        }
        return partnerRepository.findById(partnerId)
                .orElseThrow(() -> new NotFoundException("Đối tác không tồn tại."));
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
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    /** Dùng cho list/filter — trả về null nếu không truyền status. */
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

    /** Dùng cho update — bắt buộc phải có giá trị hợp lệ. */
    private ProductStatus parseStatusStrict(String status) {
        try {
            return ProductStatus.valueOf(status.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("status không hợp lệ. Chỉ nhận HOAT_DONG hoặc NGUNG_HOAT_DONG.");
        }
    }

    /**
     * Soft delete / đổi trạng thái sản phẩm.
     * Nhận ACTIVE → HOAT_DONG, INACTIVE → NGUNG_HOAT_DONG.
     * Cũng chấp nhận trực tiếp HOAT_DONG / NGUNG_HOAT_DONG.
     */
    @Override
    @Transactional
    public ProductListItemResponse updateStatus(Long id, UpdateProductStatusRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại."));

        product.setStatus(resolveProductStatus(request.trangThai()));

        return ProductListItemResponse.from(productRepository.saveAndFlush(product));
    }

    private ProductStatus resolveProductStatus(String value) {
        return switch (value.trim().toUpperCase()) {
            case "ACTIVE", "HOAT_DONG" -> ProductStatus.HOAT_DONG;
            case "INACTIVE", "NGUNG_HOAT_DONG" -> ProductStatus.NGUNG_HOAT_DONG;
            default -> throw new BadRequestException(
                    "trangThai không hợp lệ. Chỉ nhận ACTIVE hoặc INACTIVE.");
        };
    }
}
