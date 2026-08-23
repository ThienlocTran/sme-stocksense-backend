package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.*;
import com.smartflow.smestocksensebackend.dto.replenishment.*;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.*;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import java.util.List;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReplenishmentSuggestionController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ReplenishmentSuggestionControllerTest {
    @Autowired MockMvc mockMvc;
    @MockitoBean ReplenishmentSuggestionService service;
    @MockitoBean ForecastReplenishmentRecommendationService recommendationService;
    @MockitoBean JwtService jwtService;
    @MockitoBean EmployeeRepository employeeRepository;

    @ParameterizedTest @ValueSource(strings={"ADMIN","MANAGER","EMPLOYEE"})
    void allApprovedRoles_shouldAccess(String role) throws Exception {
        when(service.listSuggestions(isNull(),isNull(),isNull(),any())).thenReturn(page());
        mockMvc.perform(get("/api/replenishment-suggestions").with(user("u").roles(role)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.content[0].suggestedQuantity").value(23));
    }
    @Test void anonymous_shouldBeUnauthorized() throws Exception { mockMvc.perform(get("/api/replenishment-suggestions")).andExpect(status().isUnauthorized()); }
    @Test void invalidPagination_shouldBeBadRequest() throws Exception { mockMvc.perform(get("/api/replenishment-suggestions?page=-1&size=0").with(user("u").roles("EMPLOYEE"))).andExpect(status().isBadRequest()); }

    @Test
    void recommendationEndpoint_shouldExposeRawAndCapacityFields() throws Exception {
        when(recommendationService.getRecommendation(44L, 11L, (short) 7, SalesHistorySource.EXTERNAL_STORE_ITEM)).thenReturn(
                new ForecastReplenishmentRecommendationResponse(44L, "SP001", "Laptop", 11L, "K001", "Kho",
                        (short) 7, java.math.BigDecimal.valueOf(50), 20, 10, 40, 30, true, 10, 30,
                        java.math.BigDecimal.valueOf(100), java.math.BigDecimal.valueOf(70),
                        java.math.BigDecimal.valueOf(30), 99L, 2, null));

        mockMvc.perform(get("/api/replenishment-suggestions/recommendation")
                        .param("productId", "44")
                        .param("warehouseId", "11")
                        .param("horizonDays", "7")
                        .param("source", "EXTERNAL_STORE_ITEM")
                        .with(user("u").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rawSuggestedQty").value(40))
                .andExpect(jsonPath("$.suggestedQty").value(30))
                .andExpect(jsonPath("$.capacityLimited").value(true))
                .andExpect(jsonPath("$.modelMetadataId").value(99));
    }

    private Page<ReplenishmentSuggestionResponse> page(){ return new PageImpl<>(List.of(new ReplenishmentSuggestionResponse(44L,"SP001","Laptop",11L,"K001","Kho",7,10,30,3,23,ReplenishmentReason.BELOW_MINIMUM,ReplenishmentPriority.HIGH,null)),PageRequest.of(0,20),1); }
}
