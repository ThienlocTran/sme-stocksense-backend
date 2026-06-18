package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.employee.CreateEmployeeRequest;
import com.smartflow.smestocksensebackend.dto.employee.EmployeePageResponse;
import com.smartflow.smestocksensebackend.dto.employee.ResetPasswordRequest;
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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class EmployeeServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private EmployeeService employeeService;

    @Test
    void listEmployees_shouldReturnPagedEmployees() {
        Employee employee = employee(1L, "Admin", "admin@example.com", "0900000001", RoleCode.ADMIN, EmployeeStatus.HOAT_DONG);
        Mockito.when(employeeRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(employee)));

        EmployeePageResponse response = employeeService.listEmployees(0, 10, "admin", "HOAT_DONG", "ADMIN");

        assertEquals(1, response.totalElements());
        assertEquals("ADMIN", response.content().get(0).roleCode());
        assertEquals("HOAT_DONG", response.content().get(0).status());
    }

    @Test
    void createEmployee_withValidPayload_shouldCreateEmployee() {
        Role role = role(RoleCode.EMPLOYEE);
        Mockito.when(employeeRepository.existsByEmailIgnoreCase("new@example.com")).thenReturn(false);
        Mockito.when(employeeRepository.existsByPhone("0900000002")).thenReturn(false);
        Mockito.when(roleRepository.findByCode(RoleCode.EMPLOYEE)).thenReturn(Optional.of(role));
        Mockito.when(passwordEncoder.encode("password123")).thenReturn("encoded-password123");
        Mockito.when(employeeRepository.saveAndFlush(any(Employee.class))).thenAnswer(invocation -> {
            Employee employee = invocation.getArgument(0);
            employee.setId(2L);
            return employee;
        });

        var response = employeeService.createEmployee(new CreateEmployeeRequest(
                "New Employee",
                "new@example.com",
                "0900000002",
                "password123",
                "EMPLOYEE",
                "HOAT_DONG"
        ));

        assertEquals(2L, response.id());
        assertEquals("new@example.com", response.email());
        assertEquals("0900000002", response.phoneNumber());
        assertEquals("EMPLOYEE", response.roleCode());
    }

    @Test
    void createEmployee_withDuplicateEmail_shouldReturnEmailError() {
        Mockito.when(employeeRepository.existsByEmailIgnoreCase("dup@example.com")).thenReturn(true);

        FieldValidationException exception = assertThrows(FieldValidationException.class, () ->
                employeeService.createEmployee(new CreateEmployeeRequest(
                        "Duplicate Email", "dup@example.com", "0900000003", "password123", "EMPLOYEE", "HOAT_DONG"
                ))
        );

        assertEquals("Email đã tồn tại.", exception.getErrors().get("email"));
    }

    @Test
    void createEmployee_withDuplicatePhone_shouldReturnPhoneError() {
        Mockito.when(employeeRepository.existsByEmailIgnoreCase("phone@example.com")).thenReturn(false);
        Mockito.when(employeeRepository.existsByPhone("0900000004")).thenReturn(true);

        FieldValidationException exception = assertThrows(FieldValidationException.class, () ->
                employeeService.createEmployee(new CreateEmployeeRequest(
                        "Duplicate Phone", "phone@example.com", "0900000004", "password123", "EMPLOYEE", "HOAT_DONG"
                ))
        );

        assertEquals("Số điện thoại đã tồn tại.", exception.getErrors().get("phoneNumber"));
    }

    @Test
    void updateEmployee_withValidPayload_shouldUpdateEmployee() {
        Employee existing = employee(3L, "Old Name", "old@example.com", "0900000005", RoleCode.EMPLOYEE, EmployeeStatus.HOAT_DONG);
        Role managerRole = role(RoleCode.MANAGER);
        Mockito.when(employeeRepository.findById(3L)).thenReturn(Optional.of(existing));
        Mockito.when(employeeRepository.existsByEmailIgnoreCaseAndIdNot("manager@example.com", 3L)).thenReturn(false);
        Mockito.when(employeeRepository.existsByPhoneAndIdNot("0900000006", 3L)).thenReturn(false);
        Mockito.when(roleRepository.findByCode(RoleCode.MANAGER)).thenReturn(Optional.of(managerRole));
        Mockito.when(employeeRepository.saveAndFlush(existing)).thenReturn(existing);

        var response = employeeService.updateEmployee(3L, new UpdateEmployeeRequest(
                "Manager", "manager@example.com", "0900000006", "MANAGER", "TAM_KHOA"
        ));

        assertEquals("Manager", response.fullName());
        assertEquals("manager@example.com", response.email());
        assertEquals("0900000006", response.phoneNumber());
        assertEquals("MANAGER", response.roleCode());
        assertEquals("TAM_KHOA", response.status());
    }

    @Test
    void lockEmployee_shouldSetStatusToLocked() {
        Employee existing = employee(4L, "Staff", "staff@example.com", "0900000007", RoleCode.EMPLOYEE, EmployeeStatus.HOAT_DONG);
        Mockito.when(employeeRepository.findById(4L)).thenReturn(Optional.of(existing));
        Mockito.when(employeeRepository.saveAndFlush(existing)).thenReturn(existing);

        var response = employeeService.lockEmployee(4L);

        assertEquals("TAM_KHOA", response.status());
    }

    @Test
    void unlockEmployee_withStoppedEmployee_shouldThrowBadRequestException() {
        Employee existing = employee(5L, "Stopped", "stopped@example.com", "0900000008", RoleCode.EMPLOYEE, EmployeeStatus.NGUNG_HOAT_DONG);
        Mockito.when(employeeRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class, () -> employeeService.unlockEmployee(5L));
    }

    @Test
    void resetEmployeePassword_shouldEncodeAndSaveNewPassword() {
        Employee existing = employee(6L, "Reset", "reset@example.com", "0900000009", RoleCode.EMPLOYEE, EmployeeStatus.HOAT_DONG);
        Mockito.when(employeeRepository.findById(6L)).thenReturn(Optional.of(existing));
        Mockito.when(passwordEncoder.encode("newpass123")).thenReturn("encoded-newpass123");

        employeeService.resetEmployeePassword(6L, new ResetPasswordRequest("newpass123"));

        assertEquals("encoded-newpass123", existing.getPasswordHash());
        Mockito.verify(employeeRepository).saveAndFlush(existing);
    }

    @Test
    void updateEmployee_withMissingEmployee_shouldThrowNotFoundException() {
        Mockito.when(employeeRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () ->
                employeeService.updateEmployee(404L, new UpdateEmployeeRequest(
                        "Missing", "missing@example.com", null, "EMPLOYEE", "HOAT_DONG"
                ))
        );
    }

    private Employee employee(Long id, String name, String email, String phone, RoleCode roleCode, EmployeeStatus status) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setFullName(name);
        employee.setEmail(email);
        employee.setPhone(phone);
        employee.setPasswordHash("encoded-password");
        employee.setRole(role(roleCode));
        employee.setStatus(status);
        return employee;
    }

    private Role role(RoleCode roleCode) {
        Role role = new Role();
        role.setId((long) roleCode.ordinal() + 1);
        role.setCode(roleCode);
        role.setName(roleCode.name());
        return role;
    }
}
