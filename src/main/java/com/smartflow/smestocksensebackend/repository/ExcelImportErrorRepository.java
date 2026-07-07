package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ExcelImportError;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ExcelImportErrorRepository extends JpaRepository<ExcelImportError, Long> {

    List<ExcelImportError> findByExcelImportIdOrderByRowNumberAscIdAsc(Long excelImportId);

    Page<ExcelImportError> findByExcelImportId(Long excelImportId, Pageable pageable);

    void deleteByExcelImportId(Long excelImportId);
}
