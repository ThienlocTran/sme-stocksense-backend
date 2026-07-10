package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ExportReceipt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExportReceiptRepository extends JpaRepository<ExportReceipt, Long> {
}
