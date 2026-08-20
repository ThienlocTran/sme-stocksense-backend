package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.entity.SystemSetting;
import com.smartflow.smestocksensebackend.entity.SystemSettingHistory;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.repository.SystemSettingRepository;
import com.smartflow.smestocksensebackend.repository.SystemSettingHistoryRepository;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system-settings")
public class SystemSettingController {

    private static final String IMPORT_THRESHOLD_KEY = "IMPORT_RECEIPT_SECOND_APPROVAL_THRESHOLD";

    private final SystemSettingRepository systemSettingRepository;
    private final SystemSettingHistoryRepository historyRepository;

    public SystemSettingController(SystemSettingRepository systemSettingRepository, SystemSettingHistoryRepository historyRepository) {
        this.systemSettingRepository = systemSettingRepository;
        this.historyRepository = historyRepository;
    }

    @GetMapping("/import-receipt-threshold")
    public Map<String, Object> getImportReceiptThreshold() {
        SystemSetting setting = systemSettingRepository.findById(IMPORT_THRESHOLD_KEY)
                .or(() -> systemSettingRepository.findById("import_receipt_threshold"))
                .orElseThrow(() -> new NotFoundException("Cấu hình ngưỡng phê duyệt không tồn tại."));
        return Map.of(
                "key", setting.getKey(),
                "value", setting.getValue(),
                "description", setting.getDescription() != null ? setting.getDescription() : ""
        );
    }

    @PutMapping("/import-receipt-threshold")
    @Transactional
    public Map<String, Object> updateImportReceiptThreshold(@RequestBody Map<String, String> request) {
        Employee actor = currentAdmin();
        String rawValue = request.get("value");
        String reason = normalize(request.get("reason"));
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new BadRequestException("Giá trị ngưỡng không được để trống.");
        }
        if (reason == null) {
            throw new BadRequestException("Lý do thay đổi cấu hình không được để trống.");
        }
        try {
            BigDecimal numericValue = new BigDecimal(rawValue);
            if (numericValue.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BadRequestException("Giá trị ngưỡng phải lớn hơn 0.");
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException("Giá trị ngưỡng phải là số hợp lệ.");
        }

        SystemSetting setting = systemSettingRepository.findById(IMPORT_THRESHOLD_KEY)
                .orElseGet(() -> SystemSetting.builder()
                        .key(IMPORT_THRESHOLD_KEY)
                        .description("Ngưỡng phê duyệt cấp 2 cho phiếu nhập kho (VND)")
                        .build());
        String oldValue = setting.getValue();

        setting.setValue(rawValue.trim());
        systemSettingRepository.save(setting);

        SystemSettingHistory history = new SystemSettingHistory();
        history.setSettingKey(setting.getKey());
        history.setOldValue(oldValue);
        history.setNewValue(setting.getValue());
        history.setReason(reason);
        history.setChangedBy(actor);
        historyRepository.save(history);

        return Map.of(
                "key", setting.getKey(),
                "value", setting.getValue(),
                "description", setting.getDescription() != null ? setting.getDescription() : ""
        );
    }

    @GetMapping("/import-receipt-threshold/history")
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getImportReceiptThresholdHistory() {
        return historyRepository.findBySettingKeyOrderByChangedAtDesc(IMPORT_THRESHOLD_KEY).stream()
                .map(history -> Map.<String, Object>of(
                        "oldValue", history.getOldValue() != null ? history.getOldValue() : "",
                        "newValue", history.getNewValue(),
                        "reason", history.getReason(),
                        "changedById", history.getChangedBy().getId(),
                        "changedAt", history.getChangedAt()
                ))
                .toList();
    }

    private Employee currentAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Employee employee)) {
            throw new MissingRoleException("Không xác định được người dùng.");
        }
        RoleCode role = employee.getRole() != null ? employee.getRole().getCode() : null;
        if (role != RoleCode.ADMIN) {
            throw new MissingRoleException("Chỉ ADMIN được cập nhật cấu hình nghiệp vụ.");
        }
        return employee;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
