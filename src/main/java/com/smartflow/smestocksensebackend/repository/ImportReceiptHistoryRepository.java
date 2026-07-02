package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ImportReceiptHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ImportReceiptHistoryRepository extends JpaRepository<ImportReceiptHistory, Long> {
    List<ImportReceiptHistory> findByDocumentIdOrderByCreatedAtDesc(Long documentId);
}
