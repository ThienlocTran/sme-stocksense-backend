package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.forecast.DriftHistoryResponse;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.ForecastDriftHistoryService;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiDriftHistoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class AiDriftHistoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ForecastDriftHistoryService forecastDriftHistoryService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    EmployeeRepository employeeRepository;

    @Test
    void list_shouldReturnFilteredPageForManager() throws Exception {
        LocalDateTime detectedFrom = LocalDateTime.parse("2026-08-22T00:00:00");
        LocalDateTime detectedTo = LocalDateTime.parse("2026-08-22T23:59:59");
        when(forecastDriftHistoryService.list(eq("SP001"), eq("K001"), eq(true), eq(false),
                eq(detectedFrom), eq(detectedTo),
                eq(PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "detectedAt")))))
                .thenReturn(new PageImpl<>(List.of(new DriftHistoryResponse(
                        5L, detectedFrom, detectedFrom.plusMinutes(1),
                        1L, "SP001", "Product", 2L, "K001", "Warehouse", 3,
                        new BigDecimal("31.25"), new BigDecimal("22.10"), new BigDecimal("20.00"),
                        true, false, 14)), PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/ai/drift-history")
                        .param("product", "SP001")
                        .param("warehouse", "K001")
                        .param("retrainNeeded", "true")
                        .param("targetRetrainNeeded", "false")
                        .param("detectedFrom", "2026-08-22T00:00:00")
                        .param("detectedTo", "2026-08-22T23:59:59")
                        .param("size", "10")
                        .with(user("u").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].productCode").value("SP001"))
                .andExpect(jsonPath("$.content[0].modelVersion").value(3))
                .andExpect(jsonPath("$.content[0].retrainNeeded").value(true))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void list_shouldRejectEmployee() throws Exception {
        mockMvc.perform(get("/api/ai/drift-history")
                        .with(user("u").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }
}
