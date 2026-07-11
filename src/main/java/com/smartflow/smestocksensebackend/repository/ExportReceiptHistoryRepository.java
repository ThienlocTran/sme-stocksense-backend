package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ExportReceiptHistory;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ExportReceiptHistoryRepository extends JpaRepository<ExportReceiptHistory, Long> {
    @EntityGraph(attributePaths = "actor")
    List<ExportReceiptHistory> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
}
