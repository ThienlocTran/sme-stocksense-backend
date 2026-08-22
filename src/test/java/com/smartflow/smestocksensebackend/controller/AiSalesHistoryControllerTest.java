package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.forecast.SalesHistoryReadResponse;
import com.smartflow.smestocksensebackend.dto.forecast.SalesHistorySummaryResponse;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.ForecastHistoryReadService;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AiSalesHistoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class AiSalesHistoryControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ForecastHistoryReadService forecastHistoryReadService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    EmployeeRepository employeeRepository;

    @Test
    void list_shouldReturnFilteredPageForManager() throws Exception {
        when(forecastHistoryReadService.list(
                eq(SalesHistorySource.SEED_DEMO), eq("SP001"), eq("K001"),
                eq(LocalDate.parse("2026-02-24")), eq(LocalDate.parse("2026-08-22")),
                eq(PageRequest.of(0, 10, org.springframework.data.domain.Sort.by(
                        org.springframework.data.domain.Sort.Direction.DESC, "ngay")))))
                .thenReturn(new PageImpl<>(List.of(new SalesHistoryReadResponse(
                        LocalDate.parse("2026-08-22"), 1L, "SP001", "Product",
                        2L, "K001", "Warehouse", 12, new BigDecimal("10000"), "SEED_DEMO")),
                        PageRequest.of(0, 10), 1));

        mockMvc.perform(get("/api/ai/sales-history")
                        .param("source", "SEED_DEMO")
                        .param("product", "SP001")
                        .param("warehouse", "K001")
                        .param("dateFrom", "2026-02-24")
                        .param("dateTo", "2026-08-22")
                        .param("size", "10")
                        .with(user("u").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].source").value("SEED_DEMO"))
                .andExpect(jsonPath("$.content[0].productCode").value("SP001"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void summary_shouldRejectEmployee() throws Exception {
        mockMvc.perform(get("/api/ai/sales-history/summary")
                        .with(user("u").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void summary_shouldReturnSourceCounts() throws Exception {
        when(forecastHistoryReadService.summary(SalesHistorySource.SEED_DEMO, null, null, null, null))
                .thenReturn(new SalesHistorySummaryResponse("SEED_DEMO", 180, 1, 1, 1,
                        LocalDate.parse("2026-02-24"), LocalDate.parse("2026-08-22")));

        mockMvc.perform(get("/api/ai/sales-history/summary")
                        .param("source", "SEED_DEMO")
                        .with(user("u").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("SEED_DEMO"))
                .andExpect(jsonPath("$.rowCount").value(180));
    }
}
