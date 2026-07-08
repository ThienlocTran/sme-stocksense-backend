package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ExportReceiptDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExportReceiptDetailRepository extends JpaRepository<ExportReceiptDetail, Long> {
    List<ExportReceiptDetail> findByExportReceiptIdOrderByIdAsc(Long exportReceiptId);
}
