package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptHistoryResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
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
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportReceiptController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ImportReceiptDetailControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportReceiptService importReceiptService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void getDetail_employeeOwnerShouldReturn200() throws Exception {
        when(importReceiptService.getDetail(eq(123L))).thenReturn(response("NHAP"));

        mockMvc.perform(get("/api/import-receipts/123")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.code").value("PNK-001"))
                .andExpect(jsonPath("$.warehouseId").value(1))
                .andExpect(jsonPath("$.warehouseName").value("Kho tong"))
                .andExpect(jsonPath("$.supplierId").value(10))
                .andExpect(jsonPath("$.supplierName").value("Nha cung cap A"))
                .andExpect(jsonPath("$.status").value("NHAP"))
                .andExpect(jsonPath("$.totalAmount").value(1250000))
                .andExpect(jsonPath("$.details").isArray())
                .andExpect(jsonPath("$.details[0].productId").value(5))
                .andExpect(jsonPath("$.details[0].quantity").value(10))
                .andExpect(jsonPath("$.details[0].unitPrice").value(125000))
                .andExpect(jsonPath("$.detailCount").value(1));
    }

    @Test
    void getDetail_employeeNonOwnerShouldReturn403() throws Exception {
        when(importReceiptService.getDetail(eq(123L)))
                .thenThrow(new MissingRoleException("Khong co quyen xem phieu nhap cua nguoi khac."));

        mockMvc.perform(get("/api/import-receipts/123")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Khong co quyen xem phieu nhap cua nguoi khac."));
    }

    @Test
    void getDetail_adminShouldReturn200() throws Exception {
        when(importReceiptService.getDetail(eq(123L))).thenReturn(response("CHO_DUYET_CAP_1"));

        mockMvc.perform(get("/api/import-receipts/123")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.status").value("CHO_DUYET_CAP_1"));
    }

    @Test
    void getDetail_managerShouldReturn200() throws Exception {
        when(importReceiptService.getDetail(eq(123L))).thenReturn(response("HOAN_THANH"));

        mockMvc.perform(get("/api/import-receipts/123")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.status").value("HOAN_THANH"));
    }

    @Test
    void getHistory_managerShouldReturn200() throws Exception {
        when(importReceiptService.getHistory(eq(123L)))
                .thenReturn(List.of(new ImportReceiptHistoryResponse(
                        1L,
                        123L,
                        "Tran Thi Quan Ly",
                        "DUYET_CAP_2",
                        null,
                        LocalDateTime.of(2026, 6, 18, 10, 0)
                )));

        mockMvc.perform(get("/api/import-receipts/123/history")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].receiptId").value(123))
                .andExpect(jsonPath("$[0].action").value("DUYET_CAP_2"));
    }

    @Test
    void getHistory_employeeForbiddenWhenNotOwner() throws Exception {
        when(importReceiptService.getHistory(eq(123L)))
                .thenThrow(new MissingRoleException("Khong co quyen xem phieu nhap cua nguoi khac."));

        mockMvc.perform(get("/api/import-receipts/123/history")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void getDetail_missingReceiptShouldReturn404() throws Exception {
        when(importReceiptService.getDetail(eq(404L)))
                .thenThrow(new NotFoundException("Phieu nhap khong ton tai."));

        mockMvc.perform(get("/api/import-receipts/404")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Phieu nhap khong ton tai."));
    }

    @Test
    void getDetail_errorResponseShouldNotExposeSqlConstraintOrStackTrace() throws Exception {
        when(importReceiptService.getDetail(eq(404L)))
                .thenThrow(new NotFoundException("Phieu nhap khong ton tai."));

        mockMvc.perform(get("/api/import-receipts/404")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isNotFound())
                .andExpect(content().string(not(containsString("constraint"))))
                .andExpect(content().string(not(containsString("SQL"))))
                .andExpect(content().string(not(containsString("Hibernate"))))
                .andExpect(content().string(not(containsString("stackTrace"))));
    }

    @Test
    void getDetail_shouldIncludeAllHeaderFields() throws Exception {
        when(importReceiptService.getDetail(eq(123L))).thenReturn(response("TU_CHOI"));

        mockMvc.perform(get("/api/import-receipts/123")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.code").exists())
                .andExpect(jsonPath("$.warehouseId").exists())
                .andExpect(jsonPath("$.warehouseName").exists())
                .andExpect(jsonPath("$.supplierId").exists())
                .andExpect(jsonPath("$.supplierName").exists())
                .andExpect(jsonPath("$.createdById").exists())
                .andExpect(jsonPath("$.createdByName").exists())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.rejectionReason").value("Sai don gia nhap."))
                .andExpect(jsonPath("$.totalAmount").exists())
                .andExpect(jsonPath("$.details").exists())
                .andExpect(jsonPath("$.detailCount").exists())
                .andExpect(jsonPath("$.version").exists());
    }

    private ImportReceiptDraftResponse response(String status) {
        ImportReceiptItemResponse item = new ImportReceiptItemResponse(
                1L,
                123L,
                5L,
                "SP-001",
                "San pham A",
                10,
                new BigDecimal("125000"),
                new BigDecimal("1250000"),
                null
        );

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
                "Ghi chu test",
                "TU_CHOI".equals(status) ? "Sai don gia nhap." : null,
                List.of(item),
                1,
                LocalDateTime.of(2026, 6, 18, 10, 0),
                1L
        );
    }
}
