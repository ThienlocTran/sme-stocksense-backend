package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.forecast.SeedHistoryResponse;
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

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ForecastController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ForecastControllerTest {

    @Autowired
    MockMvc mockMvc;

    @MockitoBean
    ForecastService forecastService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    EmployeeRepository employeeRepository;

    @Test
    void seedHistory_shouldAllowManager() throws Exception {
        when(forecastService.seedDemoHistory()).thenReturn(new SeedHistoryResponse(
                "SEED_DEMO", 1, 180, LocalDate.parse("2026-02-24"), LocalDate.parse("2026-08-22")));

        mockMvc.perform(post("/api/forecast/seed-history")
                        .with(csrf())
                        .with(user("u").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.source").value("SEED_DEMO"))
                .andExpect(jsonPath("$.seriesSeeded").value(1))
                .andExpect(jsonPath("$.rowsInserted").value(180));
    }

    @Test
    void seedHistory_shouldRejectEmployee() throws Exception {
        mockMvc.perform(post("/api/forecast/seed-history")
                        .with(csrf())
                        .with(user("u").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }
}
