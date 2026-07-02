package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.inbound.ImportReceiptItemResponse;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.ImportReceiptService;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportReceiptController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ImportReceiptItemControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportReceiptService importReceiptService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void addItem_withoutToken_shouldReturn401() throws Exception {
        mockMvc.perform(post("/api/import-receipts/123/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void addItem_withEmployeeRole_shouldReturnCreatedResponse() throws Exception {
        when(importReceiptService.addItem(eq(123L), any(AddImportReceiptItemRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/import-receipts/123/items")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1001))
                .andExpect(jsonPath("$.receiptId").value(123))
                .andExpect(jsonPath("$.productId").value(25))
                .andExpect(jsonPath("$.productCode").value("SP-001"))
                .andExpect(jsonPath("$.productName").value("Ca phe rang xay"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.unitPrice").value(125000))
                .andExpect(jsonPath("$.lineTotal").value(1250000))
                .andExpect(jsonPath("$.note").value("Lo hang thang 6"));
    }

    @Test
    void addItem_withAdminRole_shouldReturnCreatedResponse() throws Exception {
        when(importReceiptService.addItem(eq(123L), any(AddImportReceiptItemRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/import-receipts/123/items")
                        .with(user("admin@example.com").roles("ADMIN"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.receiptId").value(123));
    }

    @Test
    void addItem_withManagerRole_shouldReturn403() throws Exception {
        mockMvc.perform(post("/api/import-receipts/123/items")
                        .with(user("manager@example.com").roles("MANAGER"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(validBody()))
                .andExpect(status().isForbidden());
    }

    @Test
    void addItem_withMissingProductId_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/import-receipts/123/items")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":10,\"unitPrice\":125000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.productId").exists());
    }

    @Test
    void addItem_withInvalidQuantity_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/import-receipts/123/items")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":25,\"quantity\":0,\"unitPrice\":125000}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.quantity").exists());
    }

    @Test
    void addItem_withInvalidUnitPrice_shouldReturn400() throws Exception {
        mockMvc.perform(post("/api/import-receipts/123/items")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":25,\"quantity\":10,\"unitPrice\":-1}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.unitPrice").exists());
    }

    private String validBody() {
        return "{\"productId\":25,\"quantity\":10,\"unitPrice\":125000,\"note\":\"Lo hang thang 6\"}";
    }

    private ImportReceiptItemResponse response() {
        return new ImportReceiptItemResponse(
                1001L,
                123L,
                25L,
                "SP-001",
                "Ca phe rang xay",
                10,
                new BigDecimal("125000"),
                new BigDecimal("1250000"),
                "Lo hang thang 6"
        );
    }
}
