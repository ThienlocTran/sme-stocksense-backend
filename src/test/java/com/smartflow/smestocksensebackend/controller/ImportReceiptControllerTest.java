package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inbound.CreateImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptArrivalRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.ImportReceiptService;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportReceiptController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ImportReceiptControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportReceiptService importReceiptService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void createDraft_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/import-receipts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":1,\"supplierId\":10}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createDraft_withEmployeeRole_shouldReturnCreatedResponse() throws Exception {
        when(importReceiptService.createDraft(any(CreateImportReceiptRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/import-receipts")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":1,\"supplierId\":10,\"note\":\"Phieu du kien\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.code").value("PNK-20260618-ABC123DEF456"))
                .andExpect(jsonPath("$.warehouseId").value(1))
                .andExpect(jsonPath("$.supplierId").value(10))
                .andExpect(jsonPath("$.createdById").value(5))
                .andExpect(jsonPath("$.status").value("NHAP"))
                .andExpect(jsonPath("$.totalAmount").value(0))
                .andExpect(jsonPath("$.version").value(0));
    }

    @Test
    void createDraft_withManagerRole_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/import-receipts")
                        .with(user("manager@example.com").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":1,\"supplierId\":10}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void createDraft_withMissingIds_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/import-receipts")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.warehouseId").exists())
                .andExpect(jsonPath("$.errors.supplierId").exists());
    }

    @Test
    void recordArrival_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/arrival")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualArrivalDate\":\"2026-06-22T10:00:00\"}"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void recordArrival_withEmployeeRole_shouldReturnOkResponse() throws Exception {
        java.time.LocalDateTime arrivalTime = java.time.LocalDateTime.of(2026, 6, 22, 10, 0);
        ImportReceiptDraftResponse response = new ImportReceiptDraftResponse(
                123L,
                "PNK-20260618-ABC123DEF456",
                1L,
                "Kho tong",
                10L,
                "Nha cung cap A",
                5L,
                "Nguyen Van A",
                null,
                null,
                null,
                arrivalTime,
                "CHO_KIEM_HANG",
                BigDecimal.ZERO,
                "Phieu du kien",
                List.of(),
                0,
                java.time.LocalDateTime.now(),
                1L
        );

        when(importReceiptService.recordArrival(eq(123L), any(ImportReceiptArrivalRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/import-receipts/123/arrival")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualArrivalDate\":\"2026-06-22T10:00:00\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.status").value("CHO_KIEM_HANG"))
                .andExpect(jsonPath("$.actualArrivalDate").value("2026-06-22T10:00:00"))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void recordArrival_withManagerRole_shouldReturn403() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/arrival")
                        .with(user("manager@example.com").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"actualArrivalDate\":\"2026-06-22T10:00:00\"}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void recordArrival_withMissingArrivalDate_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/arrival")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.actualArrivalDate").exists());
    }

    private ImportReceiptResponse response() {
        return new ImportReceiptResponse(
                123L,
                "PNK-20260618-ABC123DEF456",
                1L,
                "Kho tong",
                10L,
                "Nha cung cap A",
                5L,
                "Nguyen Van A",
                "NHAP",
                BigDecimal.ZERO,
                "Phieu du kien",
                LocalDateTime.of(2026, 6, 18, 15, 0),
                null,
                0L
        );
    }

}
