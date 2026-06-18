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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ImportReceiptController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ImportReceiptItemValidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ImportReceiptService importReceiptService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void addItem_withNullQuantity_shouldReturn400() throws Exception {
        assertInvalid("{\"productId\":25,\"unitPrice\":125000}", "quantity");
    }

    @Test
    void addItem_withZeroQuantity_shouldReturn400() throws Exception {
        assertInvalid("{\"productId\":25,\"quantity\":0,\"unitPrice\":125000}", "quantity");
    }

    @Test
    void addItem_withNegativeQuantity_shouldReturn400() throws Exception {
        assertInvalid("{\"productId\":25,\"quantity\":-1,\"unitPrice\":125000}", "quantity");
    }

    @Test
    void addItem_withNullUnitPrice_shouldReturn400() throws Exception {
        assertInvalid("{\"productId\":25,\"quantity\":10}", "unitPrice");
    }

    @Test
    void addItem_withNegativeUnitPrice_shouldReturn400() throws Exception {
        assertInvalid("{\"productId\":25,\"quantity\":10,\"unitPrice\":-1}", "unitPrice");
    }

    @Test
    void addItem_withTooLongNote_shouldReturn400() throws Exception {
        assertInvalid("{\"productId\":25,\"quantity\":10,\"unitPrice\":125000,\"note\":\""
                + "a".repeat(256)
                + "\"}", "note");
    }

    @Test
    void addItem_withValidBody_shouldCallService() throws Exception {
        when(importReceiptService.addItem(eq(123L), any(AddImportReceiptItemRequest.class))).thenReturn(response());

        mockMvc.perform(post("/api/import-receipts/123/items")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":25,\"quantity\":10,\"unitPrice\":125000,\"note\":\"Lo hang thang 6\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1001));

        verify(importReceiptService).addItem(eq(123L), any(AddImportReceiptItemRequest.class));
    }

    private void assertInvalid(String body, String field) throws Exception {
        mockMvc.perform(post("/api/import-receipts/123/items")
                        .with(user("employee@example.com").roles("EMPLOYEE"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors." + field).exists());

        verifyNoInteractions(importReceiptService);
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
