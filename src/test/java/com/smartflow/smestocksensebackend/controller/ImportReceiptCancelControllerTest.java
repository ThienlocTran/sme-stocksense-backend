package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inbound.CancelReceiptRequest;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportReceiptController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ImportReceiptCancelControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportReceiptService importReceiptService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void cancelDraft_withoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/cancel"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void cancelDraft_employeeOwnerShouldReturnCancelledResponse() throws Exception {
        when(importReceiptService.cancelDraft(eq(123L))).thenReturn(response());

        mockMvc.perform(put("/api/import-receipts/123/cancel")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.code").value("PNK-001"))
                .andExpect(jsonPath("$.warehouseId").value(1))
                .andExpect(jsonPath("$.supplierId").value(10))
                .andExpect(jsonPath("$.createdById").value(5))
                .andExpect(jsonPath("$.status").value("HUY"))
                .andExpect(jsonPath("$.totalAmount").value(1250000))
                .andExpect(jsonPath("$.note").value("Phieu nhap du kien"))
                .andExpect(jsonPath("$.detailCount").value(1))
                .andExpect(jsonPath("$.details[0].receiptId").value(123))
                .andExpect(jsonPath("$.version").value(2));
    }

    @Test
    void cancelDraft_employeeNonOwnerShouldReturn403() throws Exception {
        when(importReceiptService.cancelDraft(eq(123L)))
                .thenThrow(new MissingRoleException("Khong co quyen sua phieu nhap cua nguoi khac."));

        mockMvc.perform(put("/api/import-receipts/123/cancel")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelDraft_adminShouldReturnCancelledResponse() throws Exception {
        when(importReceiptService.cancelDraft(eq(123L))).thenReturn(response());

        mockMvc.perform(put("/api/import-receipts/123/cancel")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HUY"));
    }

    @Test
    void cancelDraft_managerShouldReturn403() throws Exception {
        mockMvc.perform(put("/api/import-receipts/123/cancel")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void cancelDraft_missingReceiptShouldReturn404() throws Exception {
        when(importReceiptService.cancelDraft(eq(404L)))
                .thenThrow(new NotFoundException("Phieu nhap khong ton tai."));

        mockMvc.perform(put("/api/import-receipts/404/cancel")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Phieu nhap khong ton tai."));
    }

    @Test
    void cancelDraft_nonDraftStatusShouldReturn409() throws Exception {
        when(importReceiptService.cancelDraft(eq(123L)))
                .thenThrow(new ConflictException("Chi duoc huy phieu nhap o trang thai NHAP."));

        mockMvc.perform(put("/api/import-receipts/123/cancel")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Chi duoc huy phieu nhap o trang thai NHAP."));
    }

    @Test
    void cancelDraft_errorResponseShouldNotExposeSqlConstraintOrStackTrace() throws Exception {
        when(importReceiptService.cancelDraft(eq(123L)))
                .thenThrow(new ConflictException("Chi duoc huy phieu nhap o trang thai NHAP."));

        mockMvc.perform(put("/api/import-receipts/123/cancel")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isConflict())
                .andExpect(content().string(not(containsString("constraint"))))
                .andExpect(content().string(not(containsString("SQL"))))
                .andExpect(content().string(not(containsString("Hibernate"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
    }

    @Test
    void cancelMidState_managerShouldReturnCancelledResponse() throws Exception {
        when(importReceiptService.cancel(eq(123L), any(CancelReceiptRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/import-receipts/123/cancel")
                        .with(user("manager@example.com").roles("MANAGER"))
                        .contentType("application/json")
                        .content("{\"reason\":\"NCC huy\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("HUY"));
    }

    @Test
    void cancelMidState_employeeShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/import-receipts/123/cancel")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType("application/json")
                        .content("{\"reason\":\"NCC huy\"}"))
                .andExpect(status().isForbidden());
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
                "HUY",
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
