package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.ImportReceiptService;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportReceiptController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ImportReceiptSubmitControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportReceiptService importReceiptService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void submitForApproval_withoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/submit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void submitForApproval_employeeOwnerShouldReturnWaitingLevel1Response() throws Exception {
        when(importReceiptService.submitForApproval(eq(123L))).thenReturn(response());

        mockMvc.perform(put("/api/import-receipts/123/submit")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.code").value("PNK-001"))
                .andExpect(jsonPath("$.warehouseId").value(1))
                .andExpect(jsonPath("$.supplierId").value(10))
                .andExpect(jsonPath("$.createdById").value(5))
                .andExpect(jsonPath("$.submittedById").value(5))
                .andExpect(jsonPath("$.status").value("CHO_DUYET_CAP_1"))
                .andExpect(jsonPath("$.totalAmount").value(1250000))
                .andExpect(jsonPath("$.note").value("Phieu nhap du kien"))
                .andExpect(jsonPath("$.detailCount").value(1))
                .andExpect(jsonPath("$.details[0].receiptId").value(123))
                .andExpect(jsonPath("$.version").value(2));

        verify(importReceiptService).submitForApproval(123L);
    }

    @Test
    void submitForApproval_employeeNonOwnerShouldReturn403() throws Exception {
        when(importReceiptService.submitForApproval(eq(123L)))
                .thenThrow(new MissingRoleException("Khong co quyen sua phieu nhap cua nguoi khac."));

        mockMvc.perform(put("/api/import-receipts/123/submit")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitForApproval_adminShouldReturnWaitingLevel1Response() throws Exception {
        when(importReceiptService.submitForApproval(eq(123L))).thenReturn(response());

        mockMvc.perform(put("/api/import-receipts/123/submit")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHO_DUYET_CAP_1"));
    }

    @Test
    void submitForApproval_managerShouldReturn403() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/submit")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void submitForApproval_missingReceiptShouldReturn404() throws Exception {
        when(importReceiptService.submitForApproval(eq(404L)))
                .thenThrow(new NotFoundException("Phieu nhap khong ton tai."));

        mockMvc.perform(put("/api/import-receipts/404/submit")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Phieu nhap khong ton tai."));
    }

    @Test
    void submitForApproval_invalidStatusShouldReturn409() throws Exception {
        when(importReceiptService.submitForApproval(eq(123L)))
                .thenThrow(new ConflictException("Chi duoc gui duyet phieu nhap o trang thai NHAP."));

        mockMvc.perform(put("/api/import-receipts/123/submit")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Chi duoc gui duyet phieu nhap o trang thai NHAP."));
    }

    @Test
    void submitForApproval_emptyDetailsShouldReturn409() throws Exception {
        when(importReceiptService.submitForApproval(eq(123L)))
                .thenThrow(new ConflictException("Phieu nhap phai co it nhat mot san pham hop le de gui duyet."));

        mockMvc.perform(put("/api/import-receipts/123/submit")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Phieu nhap phai co it nhat mot san pham hop le de gui duyet."));
    }

    @Test
    void submitForApproval_errorResponseShouldNotExposeSqlConstraintOrStackTrace() throws Exception {
        when(importReceiptService.submitForApproval(eq(123L)))
                .thenThrow(new ConflictException("Chi duoc gui duyet phieu nhap o trang thai NHAP."));

        mockMvc.perform(put("/api/import-receipts/123/submit")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("constraint"))))
                .andExpect(content().string(not(containsString("SQL"))))
                .andExpect(content().string(not(containsString("Hibernate"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
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
                5L,
                "Nguyen Van A",
                LocalDateTime.of(2026, 6, 18, 17, 10),
                "CHO_DUYET_CAP_1",
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
                2L
        );
    }
}
