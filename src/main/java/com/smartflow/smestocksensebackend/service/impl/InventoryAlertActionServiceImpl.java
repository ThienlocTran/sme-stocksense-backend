package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.mapper.InventoryAlertMapper;
import com.smartflow.smestocksensebackend.dto.response.InventoryAlertResponse;
import com.smartflow.smestocksensebackend.entity.InventoryAlert;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import com.smartflow.smestocksensebackend.exception.InvalidAlertStateException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.InventoryAlertRepository;
import com.smartflow.smestocksensebackend.service.InventoryAlertActionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryAlertActionServiceImpl implements InventoryAlertActionService {

    private final InventoryAlertRepository inventoryAlertRepository;

    @Override
    @Transactional
    public InventoryAlertResponse acknowledgeAlert(Long id) {
        // 1. Truy xuất Entity
        InventoryAlert alert = inventoryAlertRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy cảnh báo với ID: " + id));

        // 2. Kiểm tra State Transition
        if (alert.getStatus() == InventoryAlertStatus.RESOLVED) {
            throw new InvalidAlertStateException("Không thể tiếp nhận cảnh báo đã giải quyết.");
        }

        if (alert.getStatus() == InventoryAlertStatus.ACKNOWLEDGED) {
            // Idempotent: Bỏ qua cập nhật, không gọi save. handledBy giữ nguyên.
            log.debug("Cảnh báo {} đã ở trạng thái ACKNOWLEDGED, bỏ qua cập nhật.", id);
            return InventoryAlertMapper.toResponse(alert);
        }

        // 3. Xử lý trạng thái OPEN -> ACKNOWLEDGED
        String username = getCurrentUsername();
        
        alert.setStatus(InventoryAlertStatus.ACKNOWLEDGED);
        alert.setHandledBy(username);
        // Không cần gọi save() - Hibernate dirty checking tự flush khi commit @Transactional

        log.info("User {} đã tiếp nhận cảnh báo {}.", username, id);
        
        return InventoryAlertMapper.toResponse(alert);
    }
    
    private String getCurrentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // Nếu JWT bắt buộc thì authentication không bao giờ null ở đây
        // Nếu null nghĩa là lỗi Security Config, không nên che giấu bằng fallback
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getName())) {
            throw new IllegalStateException("Authentication context không hợp lệ. Yêu cầu JWT hợp lệ.");
        }
        return authentication.getName();
    }
}
