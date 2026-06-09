package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.auth.LoginRequest;
import com.smartflow.smestocksensebackend.dto.auth.LoginResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.InvalidCredentialsException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        String email = request.email().trim();
        Employee employee = employeeRepository.findByEmailIgnoreCase(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(request.password(), employee.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }

        if (employee.getStatus() != EmployeeStatus.HOAT_DONG) {
            throw new AccountInactiveException();
        }

        if (employee.getRole() == null || employee.getRole().getCode() == null) {
            throw new MissingRoleException();
        }

        return new LoginResponse(
                employee.getId(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getRole().getCode().name(),
                employee.getStatus().name()
        );
    }
}
