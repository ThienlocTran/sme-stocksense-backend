package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.auth.ChangePasswordRequest;
import com.smartflow.smestocksensebackend.dto.auth.ChangePasswordResponse;
import com.smartflow.smestocksensebackend.dto.auth.LoginRequest;
import com.smartflow.smestocksensebackend.dto.auth.LoginResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.FieldValidationException;
import com.smartflow.smestocksensebackend.exception.InvalidCredentialsException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public ChangePasswordResponse changeOwnPassword(ChangePasswordRequest request) {
        Employee employee = getCurrentEmployee();
        if (employee.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        validatePasswordChange(request, employee);

        employee.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        employeeRepository.saveAndFlush(employee);

        return new ChangePasswordResponse("Đổi mật khẩu thành công.");
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim();

        Employee employee = employeeRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new InvalidCredentialsException());

        if (!passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (employee.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        if (employee.getRole() == null || employee.getRole().getCode() == null) {
            throw new MissingRoleException();
        }

        String accessToken = jwtService.generateAccessToken(employee);
        return new LoginResponse(
                accessToken, "Bearer", jwtService.getExpirationSeconds(),
                employee.getId(), employee.getFullName(), employee.getEmail(),
                employee.getRole().getCode().name(), employee.getStatus().name()
        );
    }

    private Employee getCurrentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Employee principal)) {
            throw new InvalidCredentialsException();
        }
        return employeeRepository.findById(principal.getId())
                .orElseThrow(() -> new NotFoundException("Nhân viên không tồn tại."));
    }

    private void validatePasswordChange(ChangePasswordRequest request, Employee employee) {
        Map<String, String> errors = new LinkedHashMap<>();
        boolean currentPasswordMatches = passwordEncoder.matches(request.currentPassword(), employee.getPasswordHash());

        if (!currentPasswordMatches) errors.put("currentPassword", "Mật khẩu hiện tại không đúng.");
        if (!request.newPassword().equals(request.confirmPassword())) errors.put("confirmPassword", "Xác nhận mật khẩu không khớp.");

        if (!errors.isEmpty()) throw new FieldValidationException(errors);
    }
}
