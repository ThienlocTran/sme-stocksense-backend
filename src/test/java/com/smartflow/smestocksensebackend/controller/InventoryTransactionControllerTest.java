package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryTransactionResponse;
import com.smartflow.smestocksensebackend.entity.InventoryTransactionType;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.InventoryTransactionService;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
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

@WebMvcTest(controllers = InventoryTransactionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class InventoryTransactionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryTransactionService inventoryTransactionService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void searchTransactions_authorizedShouldReturnStablePageResponse() throws Exception {
        when(inventoryTransactionService.searchTransactions(eq(null), eq(null), eq(null),
                eq((InventoryTransactionType) null), eq(null), eq(null), eq(null), any(Pageable.class)))
                .thenReturn(page());

        mockMvc.perform(get("/api/inventory/transactions")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.number").doesNotExist());
    }

    private PageImpl<InventoryTransactionResponse> page() {
        InventoryTransactionResponse response = new InventoryTransactionResponse();
        response.setId(1L);
        response.setProductId(2L);
        response.setProductCode("SP-001");
        response.setProductName("Ca phe");
        response.setWarehouseId(3L);
        response.setWarehouseCode("KHO-001");
        response.setWarehouseName("Kho tong");
        response.setTransactionType("IMPORT");
        response.setQuantity(5);
        response.setQuantityBefore(0);
        response.setQuantityAfter(5);
        response.setCreatedAt(LocalDateTime.of(2026, 6, 18, 9, 0));
        response.setCreatedById(4L);
        response.setCreatedByName("Nguyen Van A");

        return new PageImpl<>(List.of(response), PageRequest.of(0, 20), 1);
    }
}
