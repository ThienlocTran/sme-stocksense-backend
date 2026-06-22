package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.DiscrepancyReportDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository cung cấp các phương thức truy vấn dữ liệu cho thực thể DiscrepancyReportDetail (Chi tiết biên bản chênh lệch).
 */
public interface DiscrepancyReportDetailRepository extends JpaRepository<DiscrepancyReportDetail, Long> {
    
    /**
     * Tìm danh sách các chi tiết biên bản chênh lệch theo ID của biên bản chênh lệch cha.
     *
     * @param reportId ID của biên bản chênh lệch cha
     * @return Danh sách chi tiết biên bản chênh lệch
     */
    List<DiscrepancyReportDetail> findByReportId(Long reportId);
}
