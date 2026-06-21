package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ImportReceiptDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ImportReceiptDetailRepository extends JpaRepository<ImportReceiptDetail, Long> {

        List<ImportReceiptDetail> findByDocumentId(Long documentId);

        @Modifying(flushAutomatically = true)
        @Query("delete from ImportReceiptDetail detail where detail.document.id = :documentId")
        void deleteByDocumentId(@Param("documentId") Long documentId);

        List<ImportReceiptDetail> findByDocumentIdOrderByIdAsc(Long documentId);

        boolean existsByDocumentIdAndProductId(Long documentId, Long productId);

        boolean existsByDocumentIdAndProductIdAndIdNot(Long documentId, Long productId, Long id);

        @Query(value = "SELECT COALESCE(SUM(thanh_tien), 0) "
                        + "FROM chi_tiet_phieu_nhap "
                        + "WHERE phieu_nhap_id = :receiptId", nativeQuery = true)
        BigDecimal sumLineTotalByReceiptId(@Param("receiptId") Long receiptId);
}
