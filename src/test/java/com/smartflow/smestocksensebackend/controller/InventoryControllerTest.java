package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inventory.InventoryLevelResponse;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.InventoryService;
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

@WebMvcTest(controllers = InventoryController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void listInventory_authorizedShouldReturnStablePageResponse() throws Exception {
        when(inventoryService.listInventory(eq(null), eq(null), eq(null), eq(null), eq(null), eq(null),
                any(Pageable.class)))
                .thenReturn(page());

        mockMvc.perform(get("/api/inventory")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.number").doesNotExist());
    }

    @Test
    void listLowStockInventory_authorizedShouldReturnStablePageResponse() throws Exception {
        when(inventoryService.listInventory(eq(null), eq(null), eq(null), eq("LOW_STOCK"), eq(null), eq(null),
                any(Pageable.class)))
                .thenReturn(page());

        mockMvc.perform(get("/api/inventory/low-stock")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").exists())
                .andExpect(jsonPath("$.page").exists())
                .andExpect(jsonPath("$.size").exists())
                .andExpect(jsonPath("$.totalElements").exists())
                .andExpect(jsonPath("$.totalPages").exists())
                .andExpect(jsonPath("$.pageable").doesNotExist())
                .andExpect(jsonPath("$.number").doesNotExist());
    }

    @Test
    void listInventory_invalidStockStatusShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/inventory")
                        .param("status", "LOW")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listInventory_invalidWarehouseStatusShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/inventory")
                        .param("warehouseStatus", "ACTIVE")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isBadRequest());
    }

    @Test
    void listLowStockInventory_invalidProductStatusShouldReturn400() throws Exception {
        mockMvc.perform(get("/api/inventory/low-stock")
                        .param("productStatus", "INACTIVE")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isBadRequest());
    }

    private PageImpl<InventoryLevelResponse> page() {
        return new PageImpl<>(
                List.of(new InventoryLevelResponse(
                        1L,
                        2L,
                        "SP-001",
                        "Ca phe",
                        "893000000001",
                        3L,
                        "KHO-001",
                        "Kho tong",
                        5,
                        10,
                        100,
                        "HOAT_DONG",
                        "HOAT_DONG",
                        "LOW_STOCK",
                        LocalDateTime.of(2026, 6, 18, 9, 0)
                )),
                PageRequest.of(0, 20),
                1
        );
    }
}
