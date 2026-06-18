package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ImportReceiptDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ImportReceiptDetailRepository extends JpaRepository<ImportReceiptDetail, Long> {

    List<ImportReceiptDetail> findByDocumentId(Long documentId);

    boolean existsByDocumentIdAndProductId(Long documentId, Long productId);

    boolean existsByDocumentIdAndProductIdAndIdNot(Long documentId, Long productId, Long id);
}
