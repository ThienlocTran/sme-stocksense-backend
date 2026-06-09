package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.employee.CreateEmployeeRequest;
import com.smartflow.smestocksensebackend.dto.employee.EmployeeListItemResponse;
import com.smartflow.smestocksensebackend.dto.employee.EmployeePageResponse;
import com.smartflow.smestocksensebackend.dto.employee.ResetPasswordRequest;
import com.smartflow.smestocksensebackend.dto.employee.ResetPasswordResponse;
import com.smartflow.smestocksensebackend.dto.employee.UpdateEmployeeRequest;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.FieldValidationException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.repository.RoleRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmployeeService {

    private static final int MAX_PAGE_SIZE = 100;
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$",
            Pattern.CASE_INSENSITIVE
    );
    private static final String EMAIL_UNIQUE_CONSTRAINT = "nhan_vien_email_key";
    private static final String DUPLICATE_EMAIL_MESSAGE = "Email đã tồn tại.";

    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public ResetPasswordResponse resetEmployeePassword(Long id, ResetPasswordRequest request) {
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nhân viên không tồn tại."));

        employee.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        employeeRepository.saveAndFlush(employee);

        return new ResetPasswordResponse("Đặt lại mật khẩu thành công.");
    }

    @Transactional
    public EmployeeListItemResponse lockEmployee(Long id) {
        if (id.equals(getCurrentEmployeeId())) {
            throw new BadRequestException("Không thể khóa tài khoản đang đăng nhập.");
        }

        Employee employee = findEmployeeById(id);
        employee.setStatus(EmployeeStatus.TAM_KHOA);
        return EmployeeListItemResponse.from(employeeRepository.saveAndFlush(employee));
    }

    @Transactional
    public EmployeeListItemResponse unlockEmployee(Long id) {
        Employee employee = findEmployeeById(id);
        if (employee.getStatus() == EmployeeStatus.NGUNG_HOAT_DONG) {
            throw new BadRequestException("Không thể mở khóa nhân viên đã ngừng hoạt động.");
        }

        employee.setStatus(EmployeeStatus.HOAT_DONG);
        return EmployeeListItemResponse.from(employeeRepository.saveAndFlush(employee));
    }

    @Transactional
    public EmployeeListItemResponse updateEmployee(Long id, UpdateEmployeeRequest request) {
        Employee employee = findEmployeeById(id);

        String email = normalizeEmail(request.email());
        validateEmail(email);

        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw duplicateEmailException();
        }

        RoleCode roleCode = parseRequiredEnum(RoleCode.class, request.roleCode(), "roleCode");
        EmployeeStatus status = parseRequiredEnum(EmployeeStatus.class, request.status(), "status");
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BadRequestException("roleCode khong hop le."));

        employee.setFullName(request.fullName().trim());
        employee.setEmail(email);
        employee.setPhone(normalizeOptional(request.phoneNumber()));
        employee.setRole(role);
        employee.setStatus(status);

        try {
            return EmployeeListItemResponse.from(employeeRepository.saveAndFlush(employee));
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateEmailException(exception)) {
                throw duplicateEmailException();
            }
            throw exception;
        }
    }

    @Transactional
    public EmployeeListItemResponse createEmployee(CreateEmployeeRequest request) {
        String email = normalizeEmail(request.email());
        validateEmail(email);

        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw duplicateEmailException();
        }

        RoleCode roleCode = parseRequiredEnum(RoleCode.class, request.roleCode(), "roleCode");
        EmployeeStatus status = parseEnum(EmployeeStatus.class, request.status(), "status");
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BadRequestException("roleCode khong hop le."));

        Employee employee = new Employee();
        employee.setFullName(request.fullName().trim());
        employee.setEmail(email);
        employee.setPhone(normalizeOptional(request.phoneNumber()));
        employee.setPasswordHash(passwordEncoder.encode(request.password()));
        employee.setRole(role);
        employee.setStatus(status == null ? EmployeeStatus.HOAT_DONG : status);

        try {
            return EmployeeListItemResponse.from(employeeRepository.saveAndFlush(employee));
        } catch (DataIntegrityViolationException exception) {
            if (isDuplicateEmailException(exception)) {
                throw duplicateEmailException();
            }
            throw exception;
        }
    }

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

    private String normalizeEmail(String email) {
        if (email == null) {
            return null;
        }
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateEmail(String email) {
        if (email == null || email.isBlank()) {
            throw new FieldValidationException(Map.of("email", "Email không được để trống."));
        }
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new FieldValidationException(Map.of("email", "Email không hợp lệ."));
        }
    }

    private FieldValidationException duplicateEmailException() {
        return new FieldValidationException(Map.of("email", DUPLICATE_EMAIL_MESSAGE));
    }

    private boolean isDuplicateEmailException(DataIntegrityViolationException exception) {
        Throwable cause = exception.getMostSpecificCause();
        String message = cause == null ? exception.getMessage() : cause.getMessage();
        return message != null && message.contains(EMAIL_UNIQUE_CONSTRAINT);
    }

    private Employee findEmployeeById(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Nhân viên không tồn tại."));
    }

    private Long getCurrentEmployeeId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Employee employee)) {
            return null;
        }
        return employee.getId();
    }

    private String normalizeOptional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private <T extends Enum<T>> T parseRequiredEnum(Class<T> enumType, String value, String fieldName) {
        T parsed = parseEnum(enumType, value, fieldName);
        if (parsed == null) {
            throw new BadRequestException(fieldName + " khong duoc de trong.");
        }
        return parsed;
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
