package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftRequest;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportReceiptController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ImportReceiptDraftControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportReceiptService importReceiptService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void saveDraft_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void saveDraft_withEmployeeRole_shouldReturnResponse() throws Exception {
        when(importReceiptService.saveDraft(eq(123L), any(SaveImportReceiptDraftRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.code").value("PNK-001"))
                .andExpect(jsonPath("$.warehouseId").value(1))
                .andExpect(jsonPath("$.supplierId").value(10))
                .andExpect(jsonPath("$.createdById").value(5))
                .andExpect(jsonPath("$.status").value("NHAP"))
                .andExpect(jsonPath("$.totalAmount").value(1250000))
                .andExpect(jsonPath("$.detailCount").value(1))
                .andExpect(jsonPath("$.details[0].lineTotal").value(1250000))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void saveDraft_withAdminRole_shouldReturnResponse() throws Exception {
        when(importReceiptService.saveDraft(eq(123L), any(SaveImportReceiptDraftRequest.class))).thenReturn(response());

        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NHAP"));
    }

    @Test
    void saveDraft_withManagerRole_shouldReturn403() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .with(user("manager@example.com").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void saveDraft_withMissingWarehouseOrSupplier_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.warehouseId").exists())
                .andExpect(jsonPath("$.errors.supplierId").exists());
    }

    @Test
    void saveDraft_withInvalidDetail_shouldReturn400() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":1,\"supplierId\":10,\"items\":[{\"productId\":25,\"quantity\":0,\"unitPrice\":125000}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['items[0].quantity']").exists());
    }

    private String validBody() {
        return "{\"warehouseId\":1,\"supplierId\":10,\"note\":\"Phieu nhap du kien\"," +
                "\"items\":[{\"productId\":25,\"quantity\":10,\"unitPrice\":125000,\"note\":\"Lo 1\"}]}";
    }

    private ImportReceiptDraftResponse response() {
        return new ImportReceiptDraftResponse(
                123L,
                "PNK-001",
                1L,
                "Kho tong",
                10L,
                "Nha cung cap A",
                5L,
                "Nguyen Van A",
                "NHAP",
                new BigDecimal("1250000"),
                "Phieu nhap du kien",
                List.of(new ImportReceiptItemResponse(
                        1001L,
                        123L,
                        25L,
                        "SP-001",
                        "Ca phe rang xay",
                        10,
                        new BigDecimal("125000"),
                        new BigDecimal("1250000"),
                        "Lo 1"
                )),
                1,
                LocalDateTime.of(2026, 6, 18, 17, 0),
                1L
        );
    }
}
