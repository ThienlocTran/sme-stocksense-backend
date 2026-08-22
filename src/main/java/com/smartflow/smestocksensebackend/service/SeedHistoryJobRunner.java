package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.forecast.SeedHistoryResponse;
import com.smartflow.smestocksensebackend.entity.AiDataJob;
import com.smartflow.smestocksensebackend.entity.AiDataJobStatus;
import com.smartflow.smestocksensebackend.repository.AiDataJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeedHistoryJobRunner {

    private final ForecastService forecastService;
    private final AiDataJobRepository aiDataJobRepository;

    @Async
    public void run(UUID jobId) {
        try {
            SeedHistoryResponse result = forecastService.seedDemoHistory();
            complete(jobId, result);
        } catch (Exception e) {
            fail(jobId, e);
        }
    }

    @Transactional
    void complete(UUID jobId, SeedHistoryResponse result) {
        AiDataJob job = aiDataJobRepository.findByJobId(jobId).orElseThrow();
        job.setStatus(AiDataJobStatus.COMPLETED);
        job.setCompletedAt(LocalDateTime.now());
        job.setRowsInserted(result.rowsInserted());
        job.setSeriesSeeded(result.seriesSeeded());
    }

    @Transactional
    void fail(UUID jobId, Exception e) {
        AiDataJob job = aiDataJobRepository.findByJobId(jobId).orElseThrow();
        job.setStatus(AiDataJobStatus.FAILED);
        job.setCompletedAt(LocalDateTime.now());
        job.setErrorMessage(e.getMessage());
    }
}
