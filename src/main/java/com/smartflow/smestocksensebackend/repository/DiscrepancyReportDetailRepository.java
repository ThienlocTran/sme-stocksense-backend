package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.DiscrepancyReportDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiscrepancyReportDetailRepository extends JpaRepository<DiscrepancyReportDetail, Long> {
    List<DiscrepancyReportDetail> findByReportId(Long reportId);
}
