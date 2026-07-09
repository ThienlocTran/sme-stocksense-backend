package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ExportReceipt;
import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.Optional;

public interface ExportReceiptRepository extends JpaRepository<ExportReceipt, Long> {

    @EntityGraph(attributePaths = { "warehouse", "createdBy" })
    Page<ExportReceipt> findByStatus(ExportReceiptStatus status, Pageable pageable);

    @EntityGraph(attributePaths = { "warehouse", "createdBy" })
    Page<ExportReceipt> findByStatusIn(Collection<ExportReceiptStatus> statuses, Pageable pageable);

    @EntityGraph(attributePaths = { "warehouse", "createdBy" })
    Optional<ExportReceipt> findById(Long id);
}
