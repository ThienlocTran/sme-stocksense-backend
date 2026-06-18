package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.auth.ChangePasswordRequest;
import com.smartflow.smestocksensebackend.dto.auth.LoginRequest;
import com.smartflow.smestocksensebackend.dto.auth.LoginResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.FieldValidationException;
import com.smartflow.smestocksensebackend.exception.InvalidCredentialsException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void login_withValidCredentials_shouldReturnTokenAndRole() {
        Employee employee = activeEmployee(RoleCode.ADMIN);
        Mockito.when(employeeRepository.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(employee));
        Mockito.when(passwordEncoder.matches("secret123", employee.getPasswordHash())).thenReturn(true);
        Mockito.when(jwtService.generateAccessToken(employee)).thenReturn("jwt-token");
        Mockito.when(jwtService.getExpirationSeconds()).thenReturn(3600L);

        LoginResponse response = authService.login(new LoginRequest("admin@example.com", "secret123"));

        assertEquals("jwt-token", response.accessToken());
        assertEquals("Bearer", response.tokenType());
        assertEquals("ADMIN", response.role());
        assertEquals("HOAT_DONG", response.status());
    }

    @Test
    void login_withWrongPassword_shouldThrowInvalidCredentialsException() {
        Employee employee = activeEmployee(RoleCode.EMPLOYEE);
        Mockito.when(employeeRepository.findByEmailIgnoreCase("user@example.com")).thenReturn(Optional.of(employee));
        Mockito.when(passwordEncoder.matches("wrong", employee.getPasswordHash())).thenReturn(false);

        assertThrows(InvalidCredentialsException.class, () ->
                authService.login(new LoginRequest("user@example.com", "wrong"))
        );
    }

    @Test
    void login_withLockedAccount_shouldThrowAccountInactiveException() {
        Employee employee = activeEmployee(RoleCode.EMPLOYEE);
        employee.setStatus(EmployeeStatus.TAM_KHOA);
        Mockito.when(employeeRepository.findByEmailIgnoreCase("locked@example.com")).thenReturn(Optional.of(employee));
        Mockito.when(passwordEncoder.matches("secret123", employee.getPasswordHash())).thenReturn(true);

        assertThrows(AccountInactiveException.class, () ->
                authService.login(new LoginRequest("locked@example.com", "secret123"))
        );
    }

    @Test
    void changeOwnPassword_withValidPayload_shouldEncodeAndSavePassword() {
        Employee employee = activeEmployee(RoleCode.MANAGER);
        authenticateAs(employee);
        Mockito.when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        Mockito.when(passwordEncoder.matches("oldpass123", employee.getPasswordHash())).thenReturn(true);
        Mockito.when(passwordEncoder.matches("newpass123", employee.getPasswordHash())).thenReturn(false);
        Mockito.when(passwordEncoder.encode("newpass123")).thenReturn("encoded-newpass123");

        authService.changeOwnPassword(new ChangePasswordRequest("oldpass123", "newpass123", "newpass123"));

        assertEquals("encoded-newpass123", employee.getPasswordHash());
        Mockito.verify(employeeRepository).saveAndFlush(employee);
    }

    @Test
    void changeOwnPassword_withWrongCurrentPassword_shouldReturnFieldError() {
        Employee employee = activeEmployee(RoleCode.MANAGER);
        authenticateAs(employee);
        Mockito.when(employeeRepository.findById(employee.getId())).thenReturn(Optional.of(employee));
        Mockito.when(passwordEncoder.matches("badpass", employee.getPasswordHash())).thenReturn(false);

        FieldValidationException exception = assertThrows(FieldValidationException.class, () ->
                authService.changeOwnPassword(new ChangePasswordRequest("badpass", "newpass123", "newpass123"))
        );

        assertEquals("Mật khẩu hiện tại không đúng.", exception.getErrors().get("currentPassword"));
    }

    private void authenticateAs(Employee employee) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(employee, null)
        );
    }

    private Employee activeEmployee(RoleCode roleCode) {
        Role role = new Role();
        role.setId(1L);
        role.setCode(roleCode);
        role.setName(roleCode.name());

        Employee employee = new Employee();
        employee.setId(1L);
        employee.setFullName("Test User");
        employee.setEmail("admin@example.com");
        employee.setPasswordHash("encoded-oldpass123");
        employee.setStatus(EmployeeStatus.HOAT_DONG);
        employee.setRole(role);
        return employee;
    }
}
