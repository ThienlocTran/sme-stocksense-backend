package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailItemResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailResponse;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExportReceiptController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class })
class ExportReceiptDetailControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @MockitoBean
        private ExportReceiptService exportReceiptService;

        @MockitoBean
        private JwtService jwtService;

        @MockitoBean
        private EmployeeRepository employeeRepository;

        @Test
        void getDetail_foundAndAuthorized_shouldReturn200() throws Exception {
                when(exportReceiptService.getDetail(100L)).thenReturn(detailResponse(100L, 12, false));

                mockMvc.perform(get("/api/export-receipts/100")
                                .with(user("manager@example.com").roles("MANAGER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.id").value(100))
                                .andExpect(jsonPath("$.code").value("XUAT-001"))
                                .andExpect(jsonPath("$.items[0].warning").value(false));
        }

        @Test
        void getDetail_notFound_shouldReturn404() throws Exception {
                when(exportReceiptService.getDetail(404L))
                                .thenThrow(new NotFoundException("Phieu xuat khong ton tai."));

                mockMvc.perform(get("/api/export-receipts/404")
                                .with(user("manager@example.com").roles("MANAGER")))
                                .andExpect(status().isNotFound());
        }

        @Test
        void getDetail_noPermission_shouldReturn403() throws Exception {
                when(exportReceiptService.getDetail(403L))
                                .thenThrow(new MissingRoleException("Khong co quyen xem phieu xuat."));

                mockMvc.perform(get("/api/export-receipts/403")
                                .with(user("employee@example.com").roles("EMPLOYEE")))
                                .andExpect(status().isForbidden());
        }

        @Test
        void getDetail_lowInventory_shouldReturnTrue() throws Exception {
                when(exportReceiptService.getDetail(200L)).thenReturn(detailResponse(200L, 6, true));

                mockMvc.perform(get("/api/export-receipts/200")
                                .with(user("manager@example.com").roles("MANAGER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.items[0].warning").value(true));
        }

        @Test
        void getDetail_highInventory_shouldReturnFalse() throws Exception {
                when(exportReceiptService.getDetail(201L)).thenReturn(detailResponse(201L, 20, false));

                mockMvc.perform(get("/api/export-receipts/201")
                                .with(user("manager@example.com").roles("MANAGER")))
                                .andExpect(status().isOk())
                                .andExpect(jsonPath("$.items[0].warning").value(false));
        }

        private ExportReceiptDetailResponse detailResponse(Long id, int currentInventory, boolean warning) {
                ExportReceiptDetailItemResponse item = new ExportReceiptDetailItemResponse(
                                1L,
                                10L,
                                "SP-001",
                                "Sản phẩm A",
                                "hộp",
                                5,
                                currentInventory,
                                warning);
                return new ExportReceiptDetailResponse(
                                id,
                                "XUAT-001",
                                "Nguyen Van A",
                                LocalDateTime.of(2026, 7, 5, 8, 0),
                                LocalDateTime.of(2026, 7, 5, 7, 0),
                                "Kho A",
                                "Ghi chú",
                                "CHO_DUYET_CAP_1",
                                "LEVEL_1",
                                null,
                                null,
                                null,
                                List.of(item));
        }
}
