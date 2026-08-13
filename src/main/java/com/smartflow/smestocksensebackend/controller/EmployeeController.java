package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.employee.CreateEmployeeRequest;
import com.smartflow.smestocksensebackend.dto.employee.EmployeeListItemResponse;
import com.smartflow.smestocksensebackend.dto.employee.EmployeePageResponse;
import com.smartflow.smestocksensebackend.dto.employee.ResetPasswordRequest;
import com.smartflow.smestocksensebackend.dto.employee.ResetPasswordResponse;
import com.smartflow.smestocksensebackend.dto.employee.UpdateEmployeeRequest;
import com.smartflow.smestocksensebackend.dto.employee.ProfileResponse;
import com.smartflow.smestocksensebackend.dto.employee.UpdateProfileRequest;
import com.smartflow.smestocksensebackend.service.EmployeeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;

    @PostMapping
    public ResponseEntity<EmployeeListItemResponse> createEmployee(
            @Valid @RequestBody CreateEmployeeRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(employeeService.createEmployee(request));
    }

    @PutMapping("/{id}")
    public EmployeeListItemResponse updateEmployee(
            @PathVariable Long id,
            @Valid @RequestBody UpdateEmployeeRequest request) {
        return employeeService.updateEmployee(id, request);
    }

    @PatchMapping("/{id}/reset-password")
    public ResetPasswordResponse resetEmployeePassword(
            @PathVariable Long id,
            @Valid @RequestBody ResetPasswordRequest request) {
        return employeeService.resetEmployeePassword(id, request);
    }

    @PatchMapping("/{id}/lock")
    public EmployeeListItemResponse lockEmployee(@PathVariable Long id) {
        return employeeService.lockEmployee(id);
    }

    @PatchMapping("/{id}/unlock")
    public EmployeeListItemResponse unlockEmployee(@PathVariable Long id) {
        return employeeService.unlockEmployee(id);
    }

    @GetMapping
    public EmployeePageResponse listEmployees(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String roleCode) {
        return employeeService.listEmployees(page, size, keyword, status, roleCode);
    }

    @GetMapping("/profile/me")
    public ProfileResponse getMyProfile() {
        return employeeService.getMyProfile();
    }

    @PutMapping("/profile/me")
    public ProfileResponse updateMyProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return employeeService.updateMyProfile(request);
    }

    @PostMapping("/profile/me/avatar")
    public ProfileResponse uploadMyAvatar(@RequestParam("file") MultipartFile file) {
        return employeeService.uploadMyAvatar(file);
    }
}
