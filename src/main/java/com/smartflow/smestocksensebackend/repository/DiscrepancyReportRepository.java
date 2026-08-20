package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.DiscrepancyReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repository cung cấp các phương thức truy vấn dữ liệu cho thực thể DiscrepancyReport (Biên bản chênh lệch).
 */
public interface DiscrepancyReportRepository extends JpaRepository<DiscrepancyReport, Long> {
    
    /**
     * Tìm biên bản chênh lệch dựa trên ID của phiếu nhập kho liên kết.
     *
     * @param phieuNhapId ID của phiếu nhập kho
     * @return Optional chứa biên bản chênh lệch nếu tìm thấy
     */
    Optional<DiscrepancyReport> findByImportReceiptId(Long phieuNhapId);
    
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"importReceipt", "createdBy", "approvedBy", "rejectedBy"})
    Optional<DiscrepancyReport> findWithAllAssociationsById(Long id);
    
    /**
     * Kiểm tra sự tồn tại của biên bản chênh lệch dựa trên ID của phiếu nhập kho.
     *
     * @param phieuNhapId ID của phiếu nhập kho
     * @return true nếu biên bản đã tồn tại, ngược lại trả về false
     */
    boolean existsByImportReceiptId(Long phieuNhapId);
    
    /**
     * Kiểm tra sự tồn tại của biên bản chênh lệch dựa trên mã biên bản (không phân biệt chữ hoa chữ thường).
     *
     * @param code Mã biên bản chênh lệch cần kiểm tra
     * @return true nếu mã biên bản đã tồn tại, ngược lại trả về false
     */
    boolean existsByCodeIgnoreCase(String code);
}
