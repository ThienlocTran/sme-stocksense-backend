package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.category.CreateCategoryRequest;
import com.smartflow.smestocksensebackend.dto.category.CategoryListItemResponse;
import com.smartflow.smestocksensebackend.dto.category.CategoryPageResponse;
import com.smartflow.smestocksensebackend.entity.Category;
import com.smartflow.smestocksensebackend.entity.CategoryStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.FieldValidationException;
import com.smartflow.smestocksensebackend.repository.CategoryRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final String CATEGORY_CODE_UNIQUE_CONSTRAINT = "danh_muc_ma_danh_muc_key";

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryListItemResponse createCategory(CreateCategoryRequest request) {
        String code = normalizeCode(request.code());
        String name = request.name().trim();
        CategoryStatus status = parseEnum(CategoryStatus.class, request.status(), "status");

        validateUniqueCategory(code, name);

        Category category = new Category();
        category.setCode(code);
        category.setName(name);
        category.setDescription(normalizeOptional(request.description()));
        category.setStatus(status == null ? CategoryStatus.HOAT_DONG : status);

        try {
            return CategoryListItemResponse.from(categoryRepository.saveAndFlush(category));
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateCodeException(exception)) {
                throw duplicateCodeException();
            }
            throw exception;
        }
    }

    @Transactional(readOnly = true)
    public CategoryPageResponse listCategories(
            int page,
            int size,
            String keyword,
            String status
    ) {
        validatePageRequest(page, size);

        CategoryStatus parsedStatus = parseEnum(CategoryStatus.class, status, "status");
        String keywordLike = normalizeKeyword(keyword);
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        return CategoryPageResponse.from(categoryRepository
                .findAll(buildSpecification(keywordLike, parsedStatus), pageRequest)
                .map(CategoryListItemResponse::from));
    }

    private Specification<Category> buildSpecification(String keywordLike, CategoryStatus status) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keywordLike != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("code")), keywordLike),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keywordLike)
                ));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("page phai lon hon hoac bang 0.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size phai nam trong khoang 1 den 100.");
        }
    }

    private String normalizeKeyword(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return null;
        }
        return "%" + keyword.trim().toLowerCase() + "%";
    }

    private void validateUniqueCategory(String code, String name) {
        Map<String, String> errors = new LinkedHashMap<>();

        if (categoryRepository.existsByNormalizedCode(code)) {
            errors.put("code", "Mã danh mục đã tồn tại.");
        }
        if (categoryRepository.existsByNormalizedName(name)) {
            errors.put("name", "Tên danh mục đã tồn tại.");
        }

        if (!errors.isEmpty()) {
            throw new FieldValidationException(errors);
        }
    }

    private FieldValidationException duplicateCodeException() {
        return new FieldValidationException(Map.of("code", "Mã danh mục đã tồn tại."));
    }

    private boolean isDuplicateCodeException(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String message = cause == null ? exception.getMessage() : cause.getMessage();
        return message != null && message.contains(CATEGORY_CODE_UNIQUE_CONSTRAINT);
    }

    private String normalizeCode(String code) {
        return code.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(fieldName + " khong hop le.");
        }
    }
}
