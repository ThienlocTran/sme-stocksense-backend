package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ExportReceiptDetail;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExportReceiptDetailRepository extends JpaRepository<ExportReceiptDetail, Long> {
    @EntityGraph(attributePaths = "product")
    List<ExportReceiptDetail> findByExportReceiptIdOrderByIdAsc(Long exportReceiptId);

    List<ExportReceiptDetail> findByExportReceiptId(Long exportReceiptId);

    void deleteByExportReceiptId(Long exportReceiptId);
}
