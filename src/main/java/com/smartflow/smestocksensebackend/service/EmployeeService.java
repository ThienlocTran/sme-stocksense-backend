package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.employee.CreateEmployeeRequest;
import com.smartflow.smestocksensebackend.dto.employee.EmployeeListItemResponse;
import com.smartflow.smestocksensebackend.dto.employee.EmployeePageResponse;
import com.smartflow.smestocksensebackend.dto.employee.ResetPasswordRequest;
import com.smartflow.smestocksensebackend.dto.employee.ResetPasswordResponse;
import com.smartflow.smestocksensebackend.dto.employee.UpdateEmployeeRequest;
import com.smartflow.smestocksensebackend.dto.employee.ProfileResponse;
import com.smartflow.smestocksensebackend.dto.employee.UpdateProfileRequest;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.FieldValidationException;
import com.smartflow.smestocksensebackend.exception.InvalidCredentialsException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.exception.UnsupportedMediaTypeException;
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
            Pattern.CASE_INSENSITIVE);
    private static final Pattern PHONE_PATTERN = Pattern.compile("^(\\+84|0)[3|5|7|8|9][0-9]{8}$");
    private static final String EMAIL_UNIQUE_CONSTRAINT = "nhan_vien_email_key";
    private static final String DUPLICATE_EMAIL_MESSAGE = "Email đã tồn tại.";
    private static final String DUPLICATE_PHONE_MESSAGE = "Số điện thoại đã tồn tại.";

    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final CloudinaryService cloudinaryService;

    @Transactional(readOnly = true)
    public ProfileResponse getMyProfile() {
        Long currentId = getCurrentEmployeeId();
        if (currentId == null) throw new InvalidCredentialsException();
        Employee employee = findEmployeeById(currentId);
        if (employee.getStatus() != EmployeeStatus.HOAT_DONG) throw new AccountInactiveException();
        return mapToProfileResponse(employee);
    }

    @Transactional
    public ProfileResponse updateMyProfile(UpdateProfileRequest request) {
        Long currentId = getCurrentEmployeeId();
        if (currentId == null) throw new InvalidCredentialsException();
        Employee employee = findEmployeeById(currentId);
        if (employee.getStatus() != EmployeeStatus.HOAT_DONG) throw new AccountInactiveException();

        String phone = normalizePhone(request.phone());
        if (phone != null) {
            if (!PHONE_PATTERN.matcher(phone).matches()) {
                throw new FieldValidationException(Map.of("phone", "Số điện thoại không hợp lệ"));
            }
            if (employeeRepository.existsByPhoneAndIdNot(phone, currentId)) {
                throw duplicatePhoneException();
            }
        }

        employee.setFullName(request.fullName().trim());
        employee.setPhone(phone);
        employee.setGender(request.gender());
        employee.setDateOfBirth(request.dateOfBirth());
        
        return mapToProfileResponse(employeeRepository.saveAndFlush(employee));
    }

    public ProfileResponse uploadMyAvatar(org.springframework.web.multipart.MultipartFile file) {
        Long currentId = getCurrentEmployeeId();
        if (currentId == null) throw new InvalidCredentialsException();
        Employee employee = findEmployeeById(currentId);
        if (employee.getStatus() != EmployeeStatus.HOAT_DONG) throw new AccountInactiveException();

        if (file.getSize() > 2 * 1024 * 1024) throw new BadRequestException("Kích thước file không được vượt quá 2MB.");
        
        try {
            java.awt.image.BufferedImage image = javax.imageio.ImageIO.read(new java.io.ByteArrayInputStream(file.getBytes()));
            if (image == null) {
                throw new UnsupportedMediaTypeException("File không phải là định dạng ảnh hợp lệ.");
            }
            if (image.getWidth() > 4000 || image.getHeight() > 4000) {
                throw new BadRequestException("Kích thước ảnh (pixel) quá lớn. Tối đa 4000x4000.");
            }
        } catch (BadRequestException | UnsupportedMediaTypeException e) {
            throw e;
        } catch (Exception e) {
            throw new UnsupportedMediaTypeException("File không phải là định dạng ảnh hợp lệ.");
        }

        Map<String, Object> result;
        try {
            result = cloudinaryService.uploadAvatar(file, employee.getId());
        } catch (java.io.IOException e) {
            throw new BadRequestException("Upload ảnh thất bại: " + e.getMessage());
        }

        String secureUrl = stringResult(result, "secure_url");
        String newPublicId = stringResult(result, "public_id");
        if (secureUrl == null || newPublicId == null) {
            throw new BadRequestException("Upload ảnh thất bại: Cloudinary không trả URL hợp lệ.");
        }
        String oldPublicId = employee.getAvatarPublicId();

        employee.setAvatarUrl(secureUrl);
        employee.setAvatarPublicId(newPublicId);

        try {
            employeeRepository.saveAndFlush(employee);
        } catch (Exception e) {
            cloudinaryService.deleteAvatarByPublicId(newPublicId);
            throw new BadRequestException("Lỗi lưu trữ DB, đã xóa ảnh vừa upload để rollback.");
        }

        if (oldPublicId != null) {
            cloudinaryService.deleteAvatarByPublicId(oldPublicId);
        }

        return mapToProfileResponse(employee);
    }

    private String normalizePhone(String phone) {
        if (phone == null || phone.isBlank()) return null;
        String normalized = phone.replaceAll("\\s+", "");
        if (normalized.startsWith("+84")) {
            normalized = "0" + normalized.substring(3);
        }
        return normalized;
    }

    private String stringResult(Map<String, Object> result, String key) {
        Object value = result.get(key);
        return value == null || value.toString().isBlank() ? null : value.toString();
    }

    private ProfileResponse mapToProfileResponse(Employee employee) {
        return new ProfileResponse(
                employee.getId(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getPhone(),
                employee.getRole() != null ? employee.getRole().getCode().name() : null,
                employee.getStatus() != null ? employee.getStatus().name() : null,
                employee.getAvatarUrl(),
                employee.getGender(),
                employee.getDateOfBirth()
        );
    }

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
        Long currentEmployeeId = getCurrentEmployeeId();
        boolean selfUpdate = currentEmployeeId != null && currentEmployeeId.equals(id);

        String email = normalizeEmail(request.email());
        String phone = normalizeOptional(request.phoneNumber());
        validateEmail(email);

        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw duplicateEmailException();
        }
        if (phone != null && employeeRepository.existsByPhoneAndIdNot(phone, id)) {
            throw duplicatePhoneException();
        }

        RoleCode roleCode = parseRequiredEnum(RoleCode.class, request.roleCode(), "roleCode");
        EmployeeStatus status = parseRequiredEnum(EmployeeStatus.class, request.status(), "status");
        if (selfUpdate && (employee.getRole() == null || employee.getRole().getCode() != roleCode
                || employee.getStatus() != status)) {
            throw new BadRequestException("Không thể tự thay đổi role hoặc trạng thái tài khoản.");
        }
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BadRequestException("roleCode khong hop le."));

        employee.setFullName(request.fullName().trim());
        employee.setEmail(email);
        employee.setPhone(phone);
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
        String phone = normalizeOptional(request.phoneNumber());
        validateEmail(email);

        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw duplicateEmailException();
        }
        if (phone != null && employeeRepository.existsByPhone(phone)) {
            throw duplicatePhoneException();
        }

        RoleCode roleCode = parseRequiredEnum(RoleCode.class, request.roleCode(), "roleCode");
        EmployeeStatus status = parseEnum(EmployeeStatus.class, request.status(), "status");
        Role role = roleRepository.findByCode(roleCode)
                .orElseThrow(() -> new BadRequestException("roleCode khong hop le."));

        Employee employee = new Employee();
        employee.setFullName(request.fullName().trim());
        employee.setEmail(email);
        employee.setPhone(phone);
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
            String roleCode) {
        validatePageRequest(page, size);

        EmployeeStatus parsedStatus = parseEnum(EmployeeStatus.class, status, "status");
        RoleCode parsedRoleCode = parseEnum(RoleCode.class, roleCode, "roleCode");
        String keywordLike = normalizeKeyword(keyword);

        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));

        return EmployeePageResponse.from(employeeRepository
                .findAll(buildSpecification(keywordLike, parsedStatus, parsedRoleCode), pageRequest)
                .map(EmployeeListItemResponse::from));
    }

    private Specification<Employee> buildSpecification(
            String keywordLike,
            EmployeeStatus status,
            RoleCode roleCode) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (keywordLike != null) {
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("fullName")), keywordLike),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("email")), keywordLike)));
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

    private FieldValidationException duplicatePhoneException() {
        return new FieldValidationException(Map.of("phoneNumber", DUPLICATE_PHONE_MESSAGE));
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
