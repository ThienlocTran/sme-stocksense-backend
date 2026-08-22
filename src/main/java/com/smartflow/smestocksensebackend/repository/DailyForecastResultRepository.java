package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.DailyForecastResult;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface DailyForecastResultRepository extends JpaRepository<DailyForecastResult, Long> {

    List<DailyForecastResult> findByModelMetadataIdOrderByForecastDateAsc(Long modelMetadataId);

    List<DailyForecastResult> findByModelMetadataIdAndForecastDateBetweenOrderByForecastDateAsc(
            Long modelMetadataId, LocalDate start, LocalDate end);
}
