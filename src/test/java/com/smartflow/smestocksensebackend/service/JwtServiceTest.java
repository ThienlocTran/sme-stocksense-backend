package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.config.JwtProperties;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret("test-secret-key-for-jwt-service-32-bytes-minimum");
        properties.setExpirationSeconds(3600L);

        jwtService = new JwtService(properties);
        jwtService.init();
    }

    @Test
    void generateAccessToken_shouldIncludeEmployeeIdAndRoleClaims() {
        String token = jwtService.generateAccessToken(employee(RoleCode.MANAGER));

        assertTrue(jwtService.isTokenValid(token));
        assertEquals("manager@example.com", jwtService.extractSubject(token).orElseThrow());
        assertEquals(7L, jwtService.extractEmployeeId(token).orElseThrow());
        assertEquals("MANAGER", jwtService.extractRole(token).orElseThrow());
        assertEquals(3600L, jwtService.getExpirationSeconds());
    }

    @Test
    void isTokenValid_withInvalidToken_shouldReturnFalse() {
        assertFalse(jwtService.isTokenValid("not-a-jwt"));
        assertTrue(jwtService.extractSubject("not-a-jwt").isEmpty());
    }

    private Employee employee(RoleCode roleCode) {
        Role role = new Role();
        role.setId(2L);
        role.setCode(roleCode);
        role.setName(roleCode.name());

        Employee employee = new Employee();
        employee.setId(7L);
        employee.setEmail("manager@example.com");
        employee.setRole(role);
        return employee;
    }
}
