package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.ExcelImport;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ExcelImportRepository extends JpaRepository<ExcelImport, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from ExcelImport e where e.id = :id")
    Optional<ExcelImport> findByIdForUpdate(@Param("id") Long id);
}
