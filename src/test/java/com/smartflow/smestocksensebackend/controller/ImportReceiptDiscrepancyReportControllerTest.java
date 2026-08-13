package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inbound.CreateDiscrepancyReportItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.CreateDiscrepancyReportRequest;
import com.smartflow.smestocksensebackend.dto.inbound.DiscrepancyReportDetailResponse;
import com.smartflow.smestocksensebackend.dto.inbound.DiscrepancyReportResponse;
import com.smartflow.smestocksensebackend.dto.inbound.RejectImportReceiptRequest;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.ImportReceiptService;
import com.smartflow.smestocksensebackend.service.JwtService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportReceiptController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ImportReceiptDiscrepancyReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .findAndRegisterModules();

    @MockitoBean
    private ImportReceiptService importReceiptService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void createDiscrepancyReport_withoutTokenShouldReturn401() throws Exception {
        CreateDiscrepancyReportRequest request = new CreateDiscrepancyReportRequest(
                "Biên bản lệch",
                List.of(new CreateDiscrepancyReportItemRequest(1L, "Thiếu hàng", "Chờ NCC giao bù"))
        );

        mockMvc.perform(post("/api/import-receipts/123/discrepancy-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void createDiscrepancyReport_employeeShouldReturnCreated() throws Exception {
        CreateDiscrepancyReportRequest request = new CreateDiscrepancyReportRequest(
                "Biên bản lệch",
                List.of(new CreateDiscrepancyReportItemRequest(1L, "Thiếu hàng", "Chờ NCC giao bù"))
        );
        when(importReceiptService.createDiscrepancyReport(eq(123L), any(CreateDiscrepancyReportRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/api/import-receipts/123/discrepancy-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.receiptId").value(123))
                .andExpect(jsonPath("$.code").value("BBCL-123"))
                .andExpect(jsonPath("$.status").value("CHO_DUYET"))
                .andExpect(jsonPath("$.detailCount").value(1));
    }

    @Test
    void createDiscrepancyReport_adminShouldReturnCreated() throws Exception {
        CreateDiscrepancyReportRequest request = new CreateDiscrepancyReportRequest(
                "Biên bản lệch",
                List.of(new CreateDiscrepancyReportItemRequest(1L, "Thiếu hàng", "Chờ NCC giao bù"))
        );
        when(importReceiptService.createDiscrepancyReport(eq(123L), any(CreateDiscrepancyReportRequest.class)))
                .thenReturn(response());

        mockMvc.perform(post("/api/import-receipts/123/discrepancy-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.receiptId").value(123));
    }

    @Test
    void createDiscrepancyReport_managerShouldReturn403() throws Exception {
        CreateDiscrepancyReportRequest request = new CreateDiscrepancyReportRequest(
                "Biên bản lệch",
                List.of(new CreateDiscrepancyReportItemRequest(1L, "Thiếu hàng", "Chờ NCC giao bù"))
        );

        mockMvc.perform(post("/api/import-receipts/123/discrepancy-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void createDiscrepancyReport_invalidRequestBodyShouldReturn400() throws Exception {
        // Items rỗng
        CreateDiscrepancyReportRequest request = new CreateDiscrepancyReportRequest("Biên bản lệch", List.of());

        mockMvc.perform(post("/api/import-receipts/123/discrepancy-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createDiscrepancyReport_notFoundExceptionShouldReturn404() throws Exception {
        CreateDiscrepancyReportRequest request = new CreateDiscrepancyReportRequest(
                "Biên bản lệch",
                List.of(new CreateDiscrepancyReportItemRequest(1L, "Thiếu hàng", "Chờ NCC giao bù"))
        );
        when(importReceiptService.createDiscrepancyReport(eq(404L), any(CreateDiscrepancyReportRequest.class)))
                .thenThrow(new NotFoundException("Phiếu nhập không tồn tại."));

        mockMvc.perform(post("/api/import-receipts/404/discrepancy-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Phiếu nhập không tồn tại."));
    }

    @Test
    void createDiscrepancyReport_conflictExceptionShouldReturn409() throws Exception {
        CreateDiscrepancyReportRequest request = new CreateDiscrepancyReportRequest(
                "Biên bản lệch",
                List.of(new CreateDiscrepancyReportItemRequest(1L, "Thiếu hàng", "Chờ NCC giao bù"))
        );
        when(importReceiptService.createDiscrepancyReport(eq(123L), any(CreateDiscrepancyReportRequest.class)))
                .thenThrow(new ConflictException("Phiếu nhập đã có biên bản chênh lệch."));

        mockMvc.perform(post("/api/import-receipts/123/discrepancy-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Phiếu nhập đã có biên bản chênh lệch."));
    }

    @Test
    void createDiscrepancyReport_badRequestExceptionShouldReturn400() throws Exception {
        CreateDiscrepancyReportRequest request = new CreateDiscrepancyReportRequest(
                "Biên bản lệch",
                List.of(new CreateDiscrepancyReportItemRequest(1L, "Thiếu hàng", "Chờ NCC giao bù"))
        );
        when(importReceiptService.createDiscrepancyReport(eq(123L), any(CreateDiscrepancyReportRequest.class)))
                .thenThrow(new BadRequestException("Không có dòng sản phẩm nào bị chênh lệch."));

        mockMvc.perform(post("/api/import-receipts/123/discrepancy-report")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Không có dòng sản phẩm nào bị chênh lệch."));
    }

    @Test
    void approveDiscrepancyReport_managerShouldReturnOk() throws Exception {
        DiscrepancyReportResponse approved = response();
        approved.setStatus("DA_DUYET");
        when(importReceiptService.approveDiscrepancyReport(123L, 1L)).thenReturn(approved);

        mockMvc.perform(post("/api/import-receipts/123/discrepancy-reports/1/approve")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DA_DUYET"));
    }

    @Test
    void approveDiscrepancyReport_employeeShouldReturn403() throws Exception {
        mockMvc.perform(post("/api/import-receipts/123/discrepancy-reports/1/approve")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void rejectDiscrepancyReport_blankReasonShouldReturn400() throws Exception {
        RejectImportReceiptRequest request = new RejectImportReceiptRequest(" ");

        mockMvc.perform(post("/api/import-receipts/123/discrepancy-reports/1/reject")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isBadRequest());
    }

    private DiscrepancyReportResponse response() {
        return new DiscrepancyReportResponse(
                1L,
                123L,
                "PNK-001",
                "BBCL-123",
                "CHO_DUYET",
                LocalDateTime.of(2026, 6, 22, 10, 0),
                2L,
                "Nguyen Van Employee",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                "Biên bản lệch",
                List.of(new DiscrepancyReportDetailResponse(
                        1L,
                        1L,
                        "SP-001",
                        "Ca phe",
                        10,
                        8,
                        -2,
                        "Thiếu hàng",
                        "Chờ NCC giao bù"
                )),
                1,
                LocalDateTime.of(2026, 6, 22, 10, 0),
                LocalDateTime.of(2026, 6, 22, 10, 0),
                1L
        );
    }
}
