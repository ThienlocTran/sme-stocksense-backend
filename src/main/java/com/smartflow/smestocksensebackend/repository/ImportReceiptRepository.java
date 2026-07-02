package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Collection;
import java.util.Optional;

public interface ImportReceiptRepository extends JpaRepository<ImportReceipt, Long>, JpaSpecificationExecutor<ImportReceipt> {

    Optional<ImportReceipt> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    long countByStatus(ImportReceiptStatus status);

    @EntityGraph(attributePaths = {"warehouse", "supplier", "createdBy"})
    Page<ImportReceipt> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"warehouse", "supplier", "createdBy"})
    Page<ImportReceipt> findByCreatedById(Long createdById, Pageable pageable);

    @EntityGraph(attributePaths = {"warehouse", "supplier", "createdBy"})
    Page<ImportReceipt> findByCreatedByIdAndStatus(Long createdById, ImportReceiptStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"warehouse", "supplier", "createdBy"})
    Page<ImportReceipt> findByStatus(ImportReceiptStatus status, Pageable pageable);

    @EntityGraph(attributePaths = {"warehouse", "supplier", "createdBy"})
    Page<ImportReceipt> findByStatusIn(Collection<ImportReceiptStatus> statuses, Pageable pageable);
}
