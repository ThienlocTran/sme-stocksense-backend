package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ExportReceiptItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ExportReceiptItemRepository extends JpaRepository<ExportReceiptItem, Long> {

    /**
     * Kiểm tra xem sản phẩm đã tồn tại trong phiếu xuất chưa.
     * Unique constraint: (phieu_xuat_id, san_pham_id).
     */
    boolean existsByExportReceiptIdAndProductId(Long exportReceiptId, Long productId);

    List<ExportReceiptItem> findByExportReceiptIdOrderByIdAsc(Long exportReceiptId);

    Optional<ExportReceiptItem> findByIdAndExportReceiptId(Long id, Long exportReceiptId);
}
