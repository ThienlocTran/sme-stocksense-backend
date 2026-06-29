package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ExcelImportError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExcelImportErrorRepository extends JpaRepository<ExcelImportError, Long> {

    List<ExcelImportError> findByExcelImportIdOrderByRowNumberAscIdAsc(Long excelImportId);

    void deleteByExcelImportId(Long excelImportId);
}
