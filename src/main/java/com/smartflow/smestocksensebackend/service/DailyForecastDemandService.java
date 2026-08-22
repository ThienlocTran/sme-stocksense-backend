package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.replenishment.HorizonDemand;
import com.smartflow.smestocksensebackend.entity.DailyForecastResult;
import com.smartflow.smestocksensebackend.entity.ForecastModelMetadata;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.DailyForecastResultRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyForecastDemandService {

    private final DailyForecastResultRepository dailyForecastResultRepository;

    @Transactional(readOnly = true)
    public HorizonDemand sumHorizonDemand(ForecastModelMetadata modelMetadata, Short horizonDays,
            Long productId, Long warehouseId) {
        validate(modelMetadata, horizonDays, productId, warehouseId);

        List<DailyForecastResult> rows = dailyForecastResultRepository
                .findByModelMetadataIdOrderByForecastDateAsc(modelMetadata.getId());
        if (rows.size() < horizonDays) {
            throw new BadRequestException("Chưa đủ dữ liệu dự báo hằng ngày cho kỳ " + horizonDays + " ngày.");
        }

        LocalDate previous = null;
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = 0; i < horizonDays; i++) {
            DailyForecastResult row = rows.get(i);
            if (row.getForecastDate() == null) {
                throw new BadRequestException("Dữ liệu dự báo hằng ngày thiếu ngày dự báo.");
            }
            if (previous != null && !row.getForecastDate().equals(previous.plusDays(1))) {
                throw new BadRequestException("Dữ liệu dự báo hằng ngày không liên tục.");
            }
            sum = sum.add(row.getPredictedQuantity() == null ? BigDecimal.ZERO : row.getPredictedQuantity());
            previous = row.getForecastDate();
        }

        return new HorizonDemand(modelMetadata.getId(), modelMetadata.getVersion(), horizonDays, sum);
    }

    private void validate(ForecastModelMetadata modelMetadata, Short horizonDays, Long productId, Long warehouseId) {
        if (horizonDays == null || (horizonDays != 7 && horizonDays != 14 && horizonDays != 30)) {
            throw new BadRequestException("Kỳ dự báo chỉ hỗ trợ 7, 14 hoặc 30 ngày.");
        }
        if (modelMetadata == null || modelMetadata.getId() == null) {
            throw new BadRequestException("Thông tin mô hình dự báo không hợp lệ.");
        }
        Long modelProductId = modelMetadata.getProduct() != null ? modelMetadata.getProduct().getId() : null;
        Long modelWarehouseId = modelMetadata.getWarehouse() != null ? modelMetadata.getWarehouse().getId() : null;
        if (!productId.equals(modelProductId) || !warehouseId.equals(modelWarehouseId)) {
            throw new BadRequestException("Mô hình dự báo không khớp sản phẩm hoặc kho.");
        }
    }
}
