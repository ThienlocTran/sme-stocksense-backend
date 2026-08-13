package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.inventorycount.InventoryCountResponse;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.InventoryCountService;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = InventoryCountController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class InventoryCountControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean InventoryCountService inventoryCountService;
    @MockitoBean JwtService jwtService;
    @MockitoBean EmployeeRepository employeeRepository;

    @Test void employeeCannotCreateFinalizeOrCancel() throws Exception {
        mockMvc.perform(post("/api/inventory-counts").with(user("e").roles("EMPLOYEE")).contentType("application/json").content("{\"warehouseId\":2}")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/inventory-counts/4/finalize").with(user("e").roles("EMPLOYEE")).contentType("application/json").content("{\"version\":0}")).andExpect(status().isForbidden());
        mockMvc.perform(post("/api/inventory-counts/4/cancel").with(user("e").roles("EMPLOYEE")).contentType("application/json").content("{\"reason\":\"Sai\",\"version\":0}")).andExpect(status().isForbidden());
    }

    @Test void managerCanCreateFinalizeAndCancel() throws Exception {
        when(inventoryCountService.create(any())).thenReturn(response("DANG_KIEM_KE"));
        when(inventoryCountService.finalizeCount(eq(4L), any())).thenReturn(response("DA_CHOT"));
        when(inventoryCountService.cancel(eq(4L), any())).thenReturn(response("DA_HUY"));
        mockMvc.perform(post("/api/inventory-counts").with(user("m").roles("MANAGER")).contentType("application/json").content("{\"warehouseId\":2}")).andExpect(status().isCreated());
        mockMvc.perform(post("/api/inventory-counts/4/finalize").with(user("m").roles("MANAGER")).contentType("application/json").content("{\"version\":0}")).andExpect(status().isOk());
        mockMvc.perform(post("/api/inventory-counts/4/cancel").with(user("m").roles("MANAGER")).contentType("application/json").content("{\"reason\":\"Sai\",\"version\":0}")).andExpect(status().isOk());
    }

    @Test void employeeCanListDetailAndUpdateActual() throws Exception {
        when(inventoryCountService.list(eq(null), eq(null), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(response("DANG_KIEM_KE"))));
        when(inventoryCountService.get(4L)).thenReturn(response("DANG_KIEM_KE"));
        when(inventoryCountService.recordActual(eq(4L), eq(5L), any())).thenReturn(response("DANG_KIEM_KE"));
        mockMvc.perform(get("/api/inventory-counts").with(user("e").roles("EMPLOYEE"))).andExpect(status().isOk());
        mockMvc.perform(get("/api/inventory-counts/4").with(user("e").roles("EMPLOYEE"))).andExpect(status().isOk());
        mockMvc.perform(put("/api/inventory-counts/4/details/5").with(user("e").roles("EMPLOYEE")).contentType("application/json").content("{\"actualQuantity\":7,\"version\":0}")).andExpect(status().isOk());
    }

    private InventoryCountResponse response(String status) {
        return new InventoryCountResponse(4L, "KK-1", 2L, "Kho A", status, null, 1L, "Toan", null, null, null, null, 0L, List.of());
    }
}
