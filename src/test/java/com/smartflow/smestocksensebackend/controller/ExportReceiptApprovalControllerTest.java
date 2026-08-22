package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inbound.CancelReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.RejectExportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailResponse;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.ExportReceiptService;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExportReceiptController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class })
class ExportReceiptApprovalControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ExportReceiptService exportReceiptService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private EmployeeRepository employeeRepository;

        @Test
        void approve_withoutTokenShouldReturn401() throws Exception {
                mockMvc.perform(put("/api/export-receipts/100/approve"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void reject_managerShouldReturn200() throws Exception {
                when(exportReceiptService.reject(eq(100L), any(RejectExportReceiptRequest.class)))
                                .thenReturn(detailResponse("TU_CHOI"));

                mockMvc.perform(put("/api/export-receipts/100/reject")
                                .with(user("manager@example.com").roles("MANAGER"))
                                .contentType("application/json")
                                .content("{\"rejectReason\":\"Sai thông tin khách hàng\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("TU_CHOI"));
        }

        @Test
        void reject_withoutTokenShouldReturn401() throws Exception {
                mockMvc.perform(put("/api/export-receipts/100/reject")
                                .contentType("application/json")
                                .content("{\"rejectReason\":\"Sai thông tin khách hàng\"}"))
                                .andExpect(status().isUnauthorized());
        }

        @Test
        void reject_employeeShouldReturn403() throws Exception {
                when(exportReceiptService.reject(eq(100L), any(RejectExportReceiptRequest.class)))
                                .thenThrow(new MissingRoleException("Khong co quyen duyet phieu xuat."));

                mockMvc.perform(put("/api/export-receipts/100/reject")
                                .with(user("employee@example.com").roles("EMPLOYEE"))
                                .contentType("application/json")
                                .content("{\"rejectReason\":\"Sai thông tin khách hàng\"}"))
                                .andExpect(status().isForbidden());
        }

        @Test
        void reject_notFoundShouldReturn404() throws Exception {
                when(exportReceiptService.reject(eq(404L), any(RejectExportReceiptRequest.class)))
                                .thenThrow(new NotFoundException("Phieu xuat khong ton tai."));

                mockMvc.perform(put("/api/export-receipts/404/reject")
                                .with(user("manager@example.com").roles("MANAGER"))
                                .contentType("application/json")
                                .content("{\"rejectReason\":\"Sai thông tin khách hàng\"}"))
                                .andExpect(status().isNotFound());
        }

        @Test
        void reject_conflictShouldReturn409() throws Exception {
                when(exportReceiptService.reject(eq(100L), any(RejectExportReceiptRequest.class)))
                                .thenThrow(new ConflictException(
                                                "Chi duoc tu choi phieu xuat o trang thai cho duyet."));

                mockMvc.perform(put("/api/export-receipts/100/reject")
                                .with(user("manager@example.com").roles("MANAGER"))
                                .contentType("application/json")
                                .content("{\"rejectReason\":\"Sai thông tin khách hàng\"}"))
                                .andExpect(status().isConflict());
        }

        @Test
        void reject_blankReasonShouldReturn400() throws Exception {
                when(exportReceiptService.reject(eq(100L), any(RejectExportReceiptRequest.class)))
                                .thenThrow(new BadRequestException("Ly do tu choi khong duoc de trong."));

                mockMvc.perform(put("/api/export-receipts/100/reject")
                                .with(user("manager@example.com").roles("MANAGER"))
                                .contentType("application/json")
                                .content("{\"rejectReason\":\"   \"}"))
                                .andExpect(status().isBadRequest());
        }

        @Test
        void approve_managerShouldReturn200() throws Exception {
                when(exportReceiptService.approve(eq(100L))).thenReturn(detailResponse("DA_DUYET"));

                mockMvc.perform(put("/api/export-receipts/100/approve")
                                .with(user("manager@example.com").roles("MANAGER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("DA_DUYET"));
        }

        @Test
        void complete_employeeShouldReturn200() throws Exception {
                when(exportReceiptService.complete(eq(100L))).thenReturn(detailResponse("HOAN_THANH"));

                mockMvc.perform(put("/api/export-receipts/100/complete")
                                .with(user("employee@example.com").roles("EMPLOYEE")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("HOAN_THANH"));
        }

        @Test
        void approve_employeeShouldReturn403() throws Exception {
                when(exportReceiptService.approve(eq(100L)))
                                .thenThrow(new MissingRoleException("Khong co quyen duyet phieu xuat."));

                mockMvc.perform(put("/api/export-receipts/100/approve")
                                .with(user("employee@example.com").roles("EMPLOYEE")))
                                .andExpect(status().isForbidden());
        }

        @Test
        void approve_conflictShouldReturn409() throws Exception {
                when(exportReceiptService.approve(eq(100L)))
                                .thenThrow(new ConflictException(
                                                "Chi duoc duyet phieu xuat o trang thai CHO_DUYET."));

                mockMvc.perform(put("/api/export-receipts/100/approve")
                                .with(user("manager@example.com").roles("MANAGER")))
                                .andExpect(status().isConflict());
        }

        @Test
        void approve_notFoundShouldReturn404() throws Exception {
                when(exportReceiptService.approve(eq(404L)))
                                .thenThrow(new NotFoundException("Phieu xuat khong ton tai."));

                mockMvc.perform(put("/api/export-receipts/404/approve")
                                .with(user("manager@example.com").roles("MANAGER")))
                                .andExpect(status().isNotFound());
        }

        @Test
        void cancel_managerShouldReturn200() throws Exception {
                when(exportReceiptService.cancel(eq(100L), any(CancelReceiptRequest.class)))
                                .thenReturn(detailResponse("HUY"));

                mockMvc.perform(post("/api/export-receipts/100/cancel")
                                .with(user("manager@example.com").roles("MANAGER"))
                                .contentType("application/json")
                                .content("{\"reason\":\"Khach huy\"}"))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.status").value("HUY"));
        }

        @Test
        void cancel_employeeShouldReturn403() throws Exception {
                mockMvc.perform(post("/api/export-receipts/100/cancel")
                                .with(user("employee@example.com").roles("EMPLOYEE"))
                                .contentType("application/json")
                                .content("{\"reason\":\"Khach huy\"}"))
                                .andExpect(status().isForbidden());
        }

        private ExportReceiptDetailResponse detailResponse(String status) {
                return new ExportReceiptDetailResponse(
                                100L,
                                "XUAT-001",
                                "Nguyen Van A",
                                null,
                                null,
                                "Kho A",
                                "Ghi chú",
                                status,
                                "LEVEL_1",
                                null,
                                null,
                                null,
                                List.of());
        }
}
