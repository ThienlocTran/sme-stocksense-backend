package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptSummaryResponse;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.ImportReceiptService;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportReceiptController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ImportReceiptListControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportReceiptService importReceiptService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void listMyReceipts_withoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/import-receipts/my"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listMyReceipts_employeeShouldReturnOwnReceipts() throws Exception {
        when(importReceiptService.listMyReceipts(eq(null), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(response("NHAP"));

        mockMvc.perform(get("/api/import-receipts/my")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(123))
                .andExpect(jsonPath("$.content[0].code").value("PNK-001"))
                .andExpect(jsonPath("$.content[0].warehouseId").value(1))
                .andExpect(jsonPath("$.content[0].warehouseName").value("Kho tong"))
                .andExpect(jsonPath("$.content[0].supplierId").value(10))
                .andExpect(jsonPath("$.content[0].supplierName").value("Nha cung cap A"))
                .andExpect(jsonPath("$.content[0].createdById").value(5))
                .andExpect(jsonPath("$.content[0].createdByName").value("Nguyen Van A"))
                .andExpect(jsonPath("$.content[0].status").value("NHAP"))
                .andExpect(jsonPath("$.content[0].totalAmount").value(1250000))
                .andExpect(jsonPath("$.content[0].note").value("Can xu ly"))
                .andExpect(jsonPath("$.content[0].version").value(1))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));
    }

    @Test
    void listMyReceipts_shouldPassPaginationAndStatusFilter() throws Exception {
        when(importReceiptService.listMyReceipts(eq("TU_CHOI"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(response("TU_CHOI"));

        mockMvc.perform(get("/api/import-receipts/my")
                        .param("page", "2")
                        .param("size", "5")
                        .param("status", "TU_CHOI")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("TU_CHOI"))
                .andExpect(jsonPath("$.content[0].rejectionReason").value("Sai don gia nhap."));

        verify(importReceiptService).listMyReceipts(eq("TU_CHOI"), org.mockito.ArgumentMatchers.any(Pageable.class));
    }

    @Test
    void listMyReceipts_adminShouldReturn200() throws Exception {
        when(importReceiptService.listMyReceipts(eq(null), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenReturn(response("HUY"));

        mockMvc.perform(get("/api/import-receipts/my")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].status").value("HUY"));
    }

    @Test
    void listMyReceipts_managerShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/import-receipts/my")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listMyReceipts_invalidStatusShouldReturn400() throws Exception {
        when(importReceiptService.listMyReceipts(eq("INVALID"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenThrow(new BadRequestException("status khong hop le."));

        mockMvc.perform(get("/api/import-receipts/my")
                        .param("status", "INVALID")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("status khong hop le."));
    }

    @Test
    void listMyReceipts_errorResponseShouldNotExposeSqlConstraintOrStackTrace() throws Exception {
        when(importReceiptService.listMyReceipts(eq("INVALID"), org.mockito.ArgumentMatchers.any(Pageable.class)))
                .thenThrow(new BadRequestException("status khong hop le."));

        mockMvc.perform(get("/api/import-receipts/my")
                        .param("status", "INVALID")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isBadRequest())
                .andExpect(content().string(not(containsString("constraint"))))
                .andExpect(content().string(not(containsString("SQL"))))
                .andExpect(content().string(not(containsString("Hibernate"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
    }

    private ImportReceiptPageResponse response(String status) {
        return new ImportReceiptPageResponse(
                List.of(new ImportReceiptSummaryResponse(
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
                        "Can xu ly",
                        "TU_CHOI".equals(status) ? "Sai don gia nhap." : null,
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
}
