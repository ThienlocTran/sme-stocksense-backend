package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.replenishment.ReplenishmentSuggestionResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ReplenishmentSuggestionService {
    Page<ReplenishmentSuggestionResponse> listSuggestions(Long warehouseId, Long productId, String keyword, Pageable pageable);
}
