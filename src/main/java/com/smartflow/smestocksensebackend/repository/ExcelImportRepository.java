package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ExcelImport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExcelImportRepository extends JpaRepository<ExcelImport, Long> {
}
