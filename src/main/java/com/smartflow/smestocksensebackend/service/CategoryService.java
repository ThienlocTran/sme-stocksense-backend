package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.category.CategoryListItemResponse;
import com.smartflow.smestocksensebackend.dto.category.CategoryPageResponse;
import com.smartflow.smestocksensebackend.entity.Category;
import com.smartflow.smestocksensebackend.entity.CategoryStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.CategoryRepository;
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
public class CategoryService {

    private static final int MAX_PAGE_SIZE = 100;

    private final CategoryRepository categoryRepository;

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
