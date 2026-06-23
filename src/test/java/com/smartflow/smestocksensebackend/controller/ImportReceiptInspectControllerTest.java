package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptDraftResponse;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.inbound.InspectImportReceiptRequest;
import com.smartflow.smestocksensebackend.dto.inbound.InspectImportReceiptItemRequest;
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
class ImportReceiptInspectControllerTest {

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
    void inspect_withoutTokenShouldReturn401() throws Exception {
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(
                List.of(new InspectImportReceiptItemRequest(1L, 10, "Binh thuong", null))
        );

        mockMvc.perform(put("/api/import-receipts/123/inspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void inspect_employeeShouldReturnSuccess() throws Exception {
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(
                List.of(new InspectImportReceiptItemRequest(1L, 10, "Binh thuong", null))
        );
        when(importReceiptService.inspectReceipt(eq(123L), any(InspectImportReceiptRequest.class)))
                .thenReturn(response());

        mockMvc.perform(put("/api/import-receipts/123/inspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(jsonPath("$.status").value("CHO_KIEM_HANG"));
    }

    @Test
    void inspect_adminShouldReturnSuccess() throws Exception {
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(
                List.of(new InspectImportReceiptItemRequest(1L, 10, "Binh thuong", null))
        );
        when(importReceiptService.inspectReceipt(eq(123L), any(InspectImportReceiptRequest.class)))
                .thenReturn(response());

        mockMvc.perform(put("/api/import-receipts/123/inspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CHO_KIEM_HANG"));
    }

    @Test
    void inspect_managerShouldReturn403() throws Exception {
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(
                List.of(new InspectImportReceiptItemRequest(1L, 10, "Binh thuong", null))
        );

        mockMvc.perform(put("/api/import-receipts/123/inspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void inspect_invalidRequestBodyShouldReturn400() throws Exception {
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(List.of()); // Rỗng

        mockMvc.perform(put("/api/import-receipts/123/inspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void inspect_notFoundExceptionShouldReturn404() throws Exception {
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(
                List.of(new InspectImportReceiptItemRequest(1L, 10, "Binh thuong", null))
        );
        when(importReceiptService.inspectReceipt(eq(404L), any(InspectImportReceiptRequest.class)))
                .thenThrow(new NotFoundException("Phieu nhap khong ton tai."));

        mockMvc.perform(put("/api/import-receipts/404/inspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Phieu nhap khong ton tai."));
    }

    @Test
    void inspect_conflictExceptionShouldReturn409() throws Exception {
        InspectImportReceiptRequest request = new InspectImportReceiptRequest(
                List.of(new InspectImportReceiptItemRequest(1L, 10, "Binh thuong", null))
        );
        when(importReceiptService.inspectReceipt(eq(123L), any(InspectImportReceiptRequest.class)))
                .thenThrow(new ConflictException("Phieu nhap da duoc cap nhat boi request khac."));

        mockMvc.perform(put("/api/import-receipts/123/inspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Phieu nhap da duoc cap nhat boi request khac."));
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
                "CHO_KIEM_HANG",
                new BigDecimal("1250000"),
                "Phieu nhap kiem hang",
                List.of(new ImportReceiptItemResponse(
                        1001L,
                        123L,
                        1L,
                        "SP-001",
                        "Ca phe",
                        10,
                        10,
                        new BigDecimal("125000"),
                        new BigDecimal("1250000"),
                        "Lo 1",
                        "Binh thuong",
                        null,
                        "KHOP"
                )),
                1,
                LocalDateTime.of(2026, 6, 18, 17, 0),
                2L
        );
    }
}
