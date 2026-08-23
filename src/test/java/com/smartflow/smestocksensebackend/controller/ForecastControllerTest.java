package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.forecast.SeedHistoryJobResponse;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.ForecastService;
import com.smartflow.smestocksensebackend.service.JwtService;
import com.smartflow.smestocksensebackend.service.SeedHistoryJobService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    SeedHistoryJobService seedHistoryJobService;

    @MockitoBean
    JwtService jwtService;

    @MockitoBean
    EmployeeRepository employeeRepository;

    @Test
    void seedHistory_shouldAllowManager() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(seedHistoryJobService.start()).thenReturn(job(jobId, "RUNNING"));

        mockMvc.perform(post("/api/forecast/seed-history")
                        .with(csrf())
                        .with(user("u").roles("MANAGER")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void seedHistory_shouldRejectEmployee() throws Exception {
        mockMvc.perform(post("/api/forecast/seed-history")
                        .with(csrf())
                        .with(user("u").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void startSeedHistoryJob_shouldReturnAccepted() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(seedHistoryJobService.start()).thenReturn(job(jobId, "RUNNING"));

        mockMvc.perform(post("/api/forecast/seed-history/jobs")
                        .with(csrf())
                        .with(user("u").roles("ADMIN")))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.status").value("RUNNING"));
    }

    @Test
    void getActiveSeedHistoryJob_shouldReturnCurrentJob() throws Exception {
        UUID jobId = UUID.randomUUID();
        when(seedHistoryJobService.active()).thenReturn(Optional.of(job(jobId, "RUNNING")));

        mockMvc.perform(get("/api/forecast/seed-history/jobs/active")
                        .with(user("u").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.jobId").value(jobId.toString()));
    }

    private SeedHistoryJobResponse job(UUID jobId, String status) {
        return new SeedHistoryJobResponse(jobId, status, LocalDateTime.parse("2026-08-22T10:00:00"),
                null, null, null, null);
    }
}
