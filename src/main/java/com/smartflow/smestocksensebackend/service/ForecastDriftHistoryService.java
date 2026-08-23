package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.forecast.DriftHistoryResponse;
import com.smartflow.smestocksensebackend.entity.ForecastDriftLog;
import com.smartflow.smestocksensebackend.repository.ForecastDriftLogRepository;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ForecastDriftHistoryService {

    private final ForecastDriftLogRepository forecastDriftLogRepository;

    @Transactional(readOnly = true)
    public Page<DriftHistoryResponse> list(String product, String warehouse, Boolean retrainNeeded,
            Boolean targetRetrainNeeded, LocalDateTime detectedFrom, LocalDateTime detectedTo, Pageable pageable) {
        return forecastDriftLogRepository.findAll(spec(product, warehouse, retrainNeeded, targetRetrainNeeded,
                        detectedFrom, detectedTo), pageable)
                .map(this::toResponse);
    }

    private Specification<ForecastDriftLog> spec(String product, String warehouse, Boolean retrainNeeded,
            Boolean targetRetrainNeeded, LocalDateTime detectedFrom, LocalDateTime detectedTo) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (product != null && !product.isBlank()) {
                Join<Object, Object> p = root.join("product");
                String value = "%" + product.toLowerCase().trim() + "%";
                var productPredicate = cb.or(
                        cb.like(cb.lower(p.get("code")), value),
                        cb.like(cb.lower(p.get("name")), value));
                try {
                    productPredicate = cb.or(productPredicate, cb.equal(p.get("id"), Long.valueOf(product.trim())));
                } catch (NumberFormatException ignored) {
                    // not an id filter
                }
                predicate = cb.and(predicate, productPredicate);
            }
            if (warehouse != null && !warehouse.isBlank()) {
                Join<Object, Object> w = root.join("warehouse");
                String value = "%" + warehouse.toLowerCase().trim() + "%";
                var warehousePredicate = cb.or(
                        cb.like(cb.lower(w.get("code")), value),
                        cb.like(cb.lower(w.get("name")), value));
                try {
                    warehousePredicate = cb.or(warehousePredicate, cb.equal(w.get("id"), Long.valueOf(warehouse.trim())));
                } catch (NumberFormatException ignored) {
                    // not an id filter
                }
                predicate = cb.and(predicate, warehousePredicate);
            }
            if (retrainNeeded != null) {
                predicate = cb.and(predicate, cb.equal(root.get("retrainNeeded"), retrainNeeded));
            }
            if (targetRetrainNeeded != null) {
                predicate = cb.and(predicate, cb.equal(root.get("targetRetrainNeeded"), targetRetrainNeeded));
            }
            if (detectedFrom != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("detectedAt"), detectedFrom));
            }
            if (detectedTo != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("detectedAt"), detectedTo));
            }
            return predicate;
        };
    }

    private DriftHistoryResponse toResponse(ForecastDriftLog row) {
        var metadata = row.getModelMetadata();
        return new DriftHistoryResponse(row.getId(), row.getDetectedAt(), row.getCheckedAt(),
                row.getProduct().getId(), row.getProduct().getCode(), row.getProduct().getName(),
                row.getWarehouse().getId(), row.getWarehouse().getCode(), row.getWarehouse().getName(),
                metadata == null ? null : metadata.getVersion(),
                row.getActualSmape(), row.getRollingSmape(), row.getThresholdSmape(),
                row.getRetrainNeeded(), row.getTargetRetrainNeeded(), row.getComparedDays());
    }
}
