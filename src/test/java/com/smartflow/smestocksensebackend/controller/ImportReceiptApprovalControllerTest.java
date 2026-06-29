package com.smartflow.smestocksensebackend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptSummaryResponse;
import com.smartflow.smestocksensebackend.dto.inbound.RejectImportReceiptRequest;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.ImportReceiptService;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportReceiptController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ImportReceiptApprovalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @MockitoBean
    private ImportReceiptService importReceiptService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    // ===================== T91 - pending-approval =====================

    @Test
    void pendingApproval_withoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/import-receipts/pending-approval"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void pendingApproval_managerShouldReturn200() throws Exception {
        when(importReceiptService.listPendingApproval(eq(null), any(Pageable.class)))
                .thenReturn(pageResponse("CHO_DUYET_CAP_1"));

        mockMvc.perform(get("/api/import-receipts/pending-approval")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(100))
                .andExpect(jsonPath("$.content[0].status").value("CHO_DUYET_CAP_1"))
                .andExpect(jsonPath("$.totalElements").value(1));
    }

    @Test
    void pendingApproval_adminShouldReturn200() throws Exception {
        when(importReceiptService.listPendingApproval(eq("CHO_DUYET_CAP_2"), any(Pageable.class)))
                .thenReturn(pageResponse("CHO_DUYET_CAP_2"));

        mockMvc.perform(get("/api/import-receipts/pending-approval")
                        .param("status", "CHO_DUYET_CAP_2")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("CHO_DUYET_CAP_2"));
    }

    @Test
    void pendingApproval_employeeShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/import-receipts/pending-approval")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void pendingApproval_invalidStatusShouldReturn400() throws Exception {
        when(importReceiptService.listPendingApproval(eq("NHAP"), any(Pageable.class)))
                .thenThrow(new BadRequestException("Chi duoc loc theo trang thai cho duyet (CHO_DUYET_CAP_1 hoac CHO_DUYET_CAP_2)."));

        mockMvc.perform(get("/api/import-receipts/pending-approval")
                        .param("status", "NHAP")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isBadRequest());
    }

    // ===================== T92 - approval-detail =====================

    @Test
    void approvalDetail_withoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/import-receipts/100/approval-detail"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approvalDetail_managerShouldReturn200() throws Exception {
        when(importReceiptService.getApprovalDetail(eq(100L))).thenReturn(draftResponse("CHO_DUYET_CAP_1"));

        mockMvc.perform(get("/api/import-receipts/100/approval-detail")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.status").value("CHO_DUYET_CAP_1"))
                .andExpect(jsonPath("$.details[0].productId").value(1));
    }

    @Test
    void approvalDetail_employeeShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/import-receipts/100/approval-detail")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void approvalDetail_notFoundShouldReturn404() throws Exception {
        when(importReceiptService.getApprovalDetail(eq(404L)))
                .thenThrow(new NotFoundException("Phieu nhap khong ton tai."));

        mockMvc.perform(get("/api/import-receipts/404/approval-detail")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Phieu nhap khong ton tai."));
    }

    // ===================== T93 - approve =====================

    @Test
    void approve_withoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(put("/api/import-receipts/100/approve"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void approve_managerShouldReturn200() throws Exception {
        when(importReceiptService.approve(eq(100L))).thenReturn(draftResponse("CHO_DUYET_CAP_2"));

        mockMvc.perform(put("/api/import-receipts/100/approve")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHO_DUYET_CAP_2"));
    }

    @Test
    void approve_adminShouldReturn200() throws Exception {
        when(importReceiptService.approve(eq(100L))).thenReturn(draftResponse("CHO_HANG_VE"));

        mockMvc.perform(put("/api/import-receipts/100/approve")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHO_HANG_VE"));
    }

    @Test
    void approve_employeeShouldReturn403() throws Exception {
        mockMvc.perform(put("/api/import-receipts/100/approve")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void approve_conflictShouldReturn409() throws Exception {
        when(importReceiptService.approve(eq(100L)))
                .thenThrow(new ConflictException("Chi duoc duyet phieu nhap o trang thai cho duyet (CHO_DUYET_CAP_1 hoac CHO_DUYET_CAP_2)."));

        mockMvc.perform(put("/api/import-receipts/100/approve")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isConflict());
    }

    @Test
    void approve_notFoundShouldReturn404() throws Exception {
        when(importReceiptService.approve(eq(404L)))
                .thenThrow(new NotFoundException("Phieu nhap khong ton tai."));

        mockMvc.perform(put("/api/import-receipts/404/approve")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isNotFound());
    }

    // ===================== T94 - reject =====================

    @Test
    void reject_withoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(put("/api/import-receipts/100/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectImportReceiptRequest("Ly do."))))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void reject_managerShouldReturn200() throws Exception {
        when(importReceiptService.reject(eq(100L), any(RejectImportReceiptRequest.class)))
                .thenReturn(draftResponse("TU_CHOI"));

        mockMvc.perform(put("/api/import-receipts/100/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectImportReceiptRequest("Sai don gia.")))
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("TU_CHOI"));
    }

    @Test
    void reject_employeeShouldReturn403() throws Exception {
        mockMvc.perform(put("/api/import-receipts/100/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectImportReceiptRequest("Ly do.")))
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void reject_blankReasonShouldReturn400() throws Exception {
        mockMvc.perform(put("/api/import-receipts/100/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectImportReceiptRequest("   ")))
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void reject_conflictShouldReturn409() throws Exception {
        when(importReceiptService.reject(eq(100L), any(RejectImportReceiptRequest.class)))
                .thenThrow(new ConflictException("Chi duoc tu choi phieu nhap o trang thai cho duyet (CHO_DUYET_CAP_1 hoac CHO_DUYET_CAP_2)."));

        mockMvc.perform(put("/api/import-receipts/100/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RejectImportReceiptRequest("Ly do.")))
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isConflict());
    }

    // ===================== Helpers =====================

    private ImportReceiptPageResponse pageResponse(String status) {
        return new ImportReceiptPageResponse(
                List.of(new ImportReceiptSummaryResponse(
                        100L,
                        "PNK-001",
                        1L,
                        "Kho tong",
                        10L,
                        "Nha cung cap A",
                        5L,
                        "Nguyen Van A",
                        status,
                        new BigDecimal("1250000"),
                        "Ghi chu",
                        null,
                        LocalDateTime.of(2026, 6, 18, 9, 0),
                        LocalDateTime.of(2026, 6, 18, 10, 0),
                        LocalDateTime.of(2026, 6, 18, 11, 0),
                        null,
                        1L
                )),
                0,
                10,
                1,
                1
        );
    }

    private ImportReceiptDraftResponse draftResponse(String status) {
        return new ImportReceiptDraftResponse(
                100L,
                "PNK-001",
                1L,
                "Kho tong",
                10L,
                "Nha cung cap A",
                5L,
                "Nguyen Van A",
                status,
                new BigDecimal("1250000"),
                "Ghi chu",
                List.of(new com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse(
                        1001L,
                        100L,
                        1L,
                        "SP-001",
                        "Ca phe",
                        10,
                        null,
                        new BigDecimal("125000"),
                        new BigDecimal("1250000"),
                        "Lo 1",
                        null,
                        null,
                        null
                )),
                1,
                LocalDateTime.of(2026, 6, 18, 17, 0),
                2L
        );
    }
}
