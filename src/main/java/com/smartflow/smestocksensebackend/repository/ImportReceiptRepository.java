package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ImportReceipt;
import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ImportReceiptRepository extends JpaRepository<ImportReceipt, Long>, JpaSpecificationExecutor<ImportReceipt> {

    Optional<ImportReceipt> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCase(String code);

    long countByStatus(ImportReceiptStatus status);
}
