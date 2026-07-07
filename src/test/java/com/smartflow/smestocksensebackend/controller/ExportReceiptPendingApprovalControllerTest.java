package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptPageResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptSummaryResponse;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.ExportReceiptService;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExportReceiptController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class })
class ExportReceiptPendingApprovalControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ExportReceiptService exportReceiptService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private EmployeeRepository employeeRepository;

        @Test
        void pendingApproval_managerShouldReturn200() throws Exception {
                when(exportReceiptService.listPendingApproval(eq(null), any(Pageable.class)))
                                .thenReturn(pageResponse("CHO_DUYET_CAP_1"));

                mockMvc.perform(get("/api/export-receipts/pending-approval")
                                .with(user("manager@example.com").roles("MANAGER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.content[0].code").value("XUAT-001"))
                                .andExpect(jsonPath("$.content[0].approvalLevel").value("LEVEL_1"))
                                .andExpect(jsonPath("$.totalElements").value(1));
        }

        @Test
        void pendingApproval_unauthenticatedShouldReturn401() throws Exception {
                mockMvc.perform(get("/api/export-receipts/pending-approval"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void pendingApproval_employeeShouldReturn403() throws Exception {
                mockMvc.perform(get("/api/export-receipts/pending-approval")
                                .with(user("employee@example.com").roles("EMPLOYEE")))
                                .andExpect(status().isForbidden());
        }

        @Test
        void pendingApproval_negativePageShouldReturn400() throws Exception {
                mockMvc.perform(get("/api/export-receipts/pending-approval")
                                .param("page", "-1")
                                .with(user("manager@example.com").roles("MANAGER")))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void pendingApproval_sizeAbove100ShouldReturn400() throws Exception {
                mockMvc.perform(get("/api/export-receipts/pending-approval")
                                .param("size", "101")
                                .with(user("manager@example.com").roles("MANAGER")))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void pendingApproval_invalidStatusShouldReturn400() throws Exception {
                when(exportReceiptService.listPendingApproval(eq("NHAP"), any(Pageable.class)))
                                .thenThrow(new BadRequestException(
                                                "Chi duoc loc theo trang thai cho duyet (CHO_DUYET_CAP_1 hoac CHO_DUYET_CAP_2)."));

                mockMvc.perform(get("/api/export-receipts/pending-approval")
                                .param("status", "NHAP")
                                .with(user("manager@example.com").roles("MANAGER")))
                                .andExpect(status().isBadRequest());
        }

        private ExportReceiptPageResponse pageResponse(String status) {
                ExportReceiptSummaryResponse summary = new ExportReceiptSummaryResponse(
                                100L,
                                "XUAT-001",
                                10L,
                                "Kho A",
                                20L,
                                "Nguyen Van A",
                                status,
                                "CHO_DUYET_CAP_1".equals(status) ? "LEVEL_1" : "LEVEL_2",
                                LocalDateTime.of(2026, 7, 5, 8, 0),
                                LocalDateTime.of(2026, 7, 5, 7, 0));
                return new ExportReceiptPageResponse(List.of(summary), 0, 10, 1, 1);
        }
}
