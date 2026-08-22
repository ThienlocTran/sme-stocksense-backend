package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.forecast.SalesHistoryReadResponse;
import com.smartflow.smestocksensebackend.dto.forecast.SalesHistorySummaryResponse;
import com.smartflow.smestocksensebackend.entity.SalesHistory;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import com.smartflow.smestocksensebackend.repository.SalesHistoryRepository;
import jakarta.persistence.criteria.Join;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class ForecastHistoryReadService {

    private final SalesHistoryRepository salesHistoryRepository;

    @Transactional(readOnly = true)
    public Page<SalesHistoryReadResponse> list(SalesHistorySource source, String product, String warehouse,
            LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        return salesHistoryRepository.findAll(spec(source, product, warehouse, dateFrom, dateTo), pageable)
                .map(this::toResponse);
    }

    @Transactional(readOnly = true)
    public SalesHistorySummaryResponse summary(SalesHistorySource source, String product, String warehouse,
            LocalDate dateFrom, LocalDate dateTo) {
        SalesHistoryRepository.SalesHistorySummaryRow row = salesHistoryRepository.summarize(
                source == null ? null : source.name(),
                like(product), exact(product),
                like(warehouse), exact(warehouse),
                dateFrom, dateTo);
        return new SalesHistorySummaryResponse(source == null ? null : source.name(),
                nz(row.getRowCount()), nz(row.getDistinctProducts()), nz(row.getDistinctWarehouses()),
                nz(row.getDistinctCombinations()), row.getMinDate(), row.getMaxDate());
    }

    private Specification<SalesHistory> spec(SalesHistorySource source, String product, String warehouse,
            LocalDate dateFrom, LocalDate dateTo) {
        return (root, query, cb) -> {
            var predicate = cb.conjunction();
            if (source != null) {
                predicate = cb.and(predicate, cb.equal(root.get("source"), source));
            }
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
            if (dateFrom != null) {
                predicate = cb.and(predicate, cb.greaterThanOrEqualTo(root.get("ngay"), dateFrom));
            }
            if (dateTo != null) {
                predicate = cb.and(predicate, cb.lessThanOrEqualTo(root.get("ngay"), dateTo));
            }
            return predicate;
        };
    }

    private SalesHistoryReadResponse toResponse(SalesHistory row) {
        return new SalesHistoryReadResponse(row.getNgay(),
                row.getProduct().getId(), row.getProduct().getCode(), row.getProduct().getName(),
                row.getWarehouse().getId(), row.getWarehouse().getCode(), row.getWarehouse().getName(),
                row.getQuantity(), row.getAverageSellingPrice(), row.getSource().name());
    }

    private String like(String value) {
        return value == null || value.isBlank() ? null : "%" + value.toLowerCase().trim() + "%";
    }

    private String exact(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private long nz(Long value) {
        return value == null ? 0L : value;
    }
}
