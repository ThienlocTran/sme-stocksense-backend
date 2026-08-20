package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.entity.SystemSetting;
import com.smartflow.smestocksensebackend.repository.SystemSettingRepository;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/system-settings")
public class SystemSettingController {

    private final SystemSettingRepository systemSettingRepository;

    public SystemSettingController(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    @GetMapping("/import-receipt-threshold")
    public Map<String, Object> getImportReceiptThreshold() {
        SystemSetting setting = systemSettingRepository.findById("import_receipt_threshold")
                .orElseThrow(() -> new NotFoundException("Cấu hình ngưỡng phê duyệt không tồn tại."));
        return Map.of(
                "key", setting.getKey(),
                "value", setting.getValue(),
                "description", setting.getDescription() != null ? setting.getDescription() : ""
        );
    }

    @PutMapping("/import-receipt-threshold")
    public Map<String, Object> updateImportReceiptThreshold(@RequestBody Map<String, String> request) {
        String rawValue = request.get("value");
        if (rawValue == null || rawValue.trim().isEmpty()) {
            throw new BadRequestException("Giá trị ngưỡng không được để trống.");
        }
        try {
            BigDecimal numericValue = new BigDecimal(rawValue);
            if (numericValue.compareTo(BigDecimal.ZERO) < 0) {
                throw new BadRequestException("Giá trị ngưỡng không được nhỏ hơn 0.");
            }
        } catch (NumberFormatException e) {
            throw new BadRequestException("Giá trị ngưỡng phải là số hợp lệ.");
        }

        SystemSetting setting = systemSettingRepository.findById("import_receipt_threshold")
                .orElseGet(() -> SystemSetting.builder()
                        .key("import_receipt_threshold")
                        .description("Ngưỡng phê duyệt cấp 2 cho phiếu nhập kho (VND)")
                        .build());

        setting.setValue(rawValue.trim());
        systemSettingRepository.save(setting);

        return Map.of(
                "key", setting.getKey(),
                "value", setting.getValue(),
                "description", setting.getDescription() != null ? setting.getDescription() : ""
        );
    }
}
