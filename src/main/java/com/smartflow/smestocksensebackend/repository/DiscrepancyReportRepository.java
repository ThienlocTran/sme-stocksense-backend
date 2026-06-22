package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.DiscrepancyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DiscrepancyReportRepository extends JpaRepository<DiscrepancyReport, Long> {
    Optional<DiscrepancyReport> findByImportReceiptId(Long phieuNhapId);
    boolean existsByImportReceiptId(Long phieuNhapId);
    boolean existsByCodeIgnoreCase(String code);
}
