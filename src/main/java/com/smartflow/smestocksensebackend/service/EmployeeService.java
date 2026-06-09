package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.employee.EmployeeListItemResponse;
import com.smartflow.smestocksensebackend.dto.employee.EmployeePageResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
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
public class EmployeeService {

    private static final int MAX_PAGE_SIZE = 100;

    private final EmployeeRepository employeeRepository;

    @Transactional(readOnly = true)
    public EmployeePageResponse listEmployees(
            int page,
            int size,
            String keyword,
            String status,
            String roleCode
    ) {
        validatePageRequest(page, size);

        EmployeeStatus parsedStatus = parseEnum(EmployeeStatus.class, status, "status");
        RoleCode parsedRoleCode = parseEnum(RoleCode.class, roleCode, "roleCode");
        String keywordLike = normalizeKeyword(keyword);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );

        return EmployeePageResponse.from(employeeRepository
                .findAll(buildSpecification(keywordLike, parsedStatus, parsedRoleCode), pageRequest)
                .map(EmployeeListItemResponse::from));
    }

    private Specification<Employee> buildSpecification(
            String keywordLike,
            EmployeeStatus status,
            RoleCode roleCode
    ) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keywordLike != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), keywordLike),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), keywordLike)
                ));
            }

            if (status != null) {
                predicates.add(criteriaBuilder.equal(root.get("status"), status));
            }

            if (roleCode != null) {
                predicates.add(criteriaBuilder.equal(root.join("role").get("code"), roleCode));
            }

            return predicates.isEmpty()
                    ? criteriaBuilder.conjunction()
                    : criteriaBuilder.and(predicates.toArray(Predicate[]::new));
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

    private <T extends Enum<T>> T parseEnum(Class<T> enumType, String value, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }

        try {
            return Enum.valueOf(enumType, value.trim().toUpperCase());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException(fieldName + " không hợp lệ.");
        }
    }
}
