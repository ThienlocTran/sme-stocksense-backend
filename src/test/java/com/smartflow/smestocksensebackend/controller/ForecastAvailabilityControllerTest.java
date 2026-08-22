package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.forecast.ForecastAvailabilityResponse;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.ForecastService;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ForecastAvailabilityController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ForecastAvailabilityControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ForecastService forecastService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    EmployeeRepository employeeRepository;

    @Test
    void getAvailability_shouldUseDefaultSource() throws Exception {
        when(forecastService.getAvailability(SalesHistorySource.EXTERNAL_STORE_ITEM)).thenReturn(response("EXTERNAL_STORE_ITEM"));

        mockMvc.perform(get("/api/forecasts/availability").with(user("u").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("EXTERNAL_STORE_ITEM"))
                .andExpect(jsonPath("$.combinations[0].productCode").value("SP001"))
                .andExpect(jsonPath("$.combinations[0].warehouseCode").value("K001"));
    }

    @Test
    void getAvailability_shouldUseExplicitSource() throws Exception {
        when(forecastService.getAvailability(SalesHistorySource.EXTERNAL_RETAIL)).thenReturn(response("EXTERNAL_RETAIL"));

        mockMvc.perform(get("/api/forecasts/availability")
                        .param("source", "EXTERNAL_RETAIL")
                        .with(user("u").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("EXTERNAL_RETAIL"));
    }

    private ForecastAvailabilityResponse response(String source) {
        return new ForecastAvailabilityResponse(source, List.of(new ForecastAvailabilityResponse.Combination(
                1L, "SP001", "Product SP001", 2L, "K001", "Warehouse K001",
                1826L, LocalDate.parse("2019-01-01"), LocalDate.parse("2023-12-31"))));
    }
}
