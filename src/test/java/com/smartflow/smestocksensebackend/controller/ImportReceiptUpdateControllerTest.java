package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.inbound.SaveImportReceiptDraftRequest;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
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
class ImportReceiptUpdateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportReceiptService importReceiptService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void update_withoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void update_employeeOwnerDraftShouldReturnResponse() throws Exception {
        when(importReceiptService.saveDraft(eq(123L), any(SaveImportReceiptDraftRequest.class))).thenReturn(response("NHAP"));

        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.code").value("PNK-001"))
                .andExpect(jsonPath("$.status").value("NHAP"))
                .andExpect(jsonPath("$.totalAmount").value(1250000))
                .andExpect(jsonPath("$.detailCount").value(1))
                .andExpect(jsonPath("$.details[0].lineTotal").value(1250000))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    void update_employeeOwnerRejectedShouldReturnRejectedStatus() throws Exception {
        when(importReceiptService.saveDraft(eq(123L), any(SaveImportReceiptDraftRequest.class))).thenReturn(response("TU_CHOI"));

        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TU_CHOI"));
    }

    @Test
    void update_employeeNonOwnerShouldReturn403() throws Exception {
        when(importReceiptService.saveDraft(eq(123L), any(SaveImportReceiptDraftRequest.class)))
                .thenThrow(new MissingRoleException("Khong co quyen sua phieu nhap cua nguoi khac."));

        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_adminShouldReturnResponse() throws Exception {
        when(importReceiptService.saveDraft(eq(123L), any(SaveImportReceiptDraftRequest.class))).thenReturn(response("TU_CHOI"));

        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TU_CHOI"));
    }

    @Test
    void update_managerShouldReturn403() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .with(user("manager@example.com").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void update_missingHeaderIdsShouldReturn400() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.warehouseId").exists())
                .andExpect(jsonPath("$.errors.supplierId").exists());
    }

    @Test
    void update_invalidDetailShouldReturn400() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"warehouseId\":1,\"supplierId\":10,\"items\":[{\"productId\":25,\"quantity\":0,\"unitPrice\":125000}]}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors['items[0].quantity']").exists());
    }

    @Test
    void update_nonEditableStatusShouldReturn409() throws Exception {
        when(importReceiptService.saveDraft(eq(123L), any(SaveImportReceiptDraftRequest.class)))
                .thenThrow(new ConflictException("Chi duoc luu phieu nhap o trang thai NHAP hoac TU_CHOI."));

        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Chi duoc luu phieu nhap o trang thai NHAP hoac TU_CHOI."));
    }

    @Test
    void update_domainBadRequestShouldNotExposeSql() throws Exception {
        when(importReceiptService.saveDraft(eq(123L), any(SaveImportReceiptDraftRequest.class)))
                .thenThrow(new BadRequestException("San pham khong hoat dong."));

        mockMvc.perform(put("/api/import-receipts/123/draft")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("San pham khong hoat dong."));
    }

    private String validBody() {
        return "{\"warehouseId\":1,\"supplierId\":10,\"note\":\"Phieu nhap du kien\"," +
                "\"items\":[{\"productId\":25,\"quantity\":10,\"unitPrice\":125000,\"note\":\"Lo 1\"}]}";
    }

    private ImportReceiptDraftResponse response(String status) {
        return new ImportReceiptDraftResponse(
                123L,
                "PNK-001",
                1L,
                "Kho tong",
                10L,
                "Nha cung cap A",
                5L,
                "Nguyen Van A",
                status,
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
