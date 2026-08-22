package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.forecast.SeedHistoryResponse;
import com.smartflow.smestocksensebackend.entity.AiDataJob;
import com.smartflow.smestocksensebackend.entity.AiDataJobStatus;
import com.smartflow.smestocksensebackend.repository.AiDataJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeedHistoryJobRunner {

    private final ForecastService forecastService;
    private final AiDataJobRepository aiDataJobRepository;
    private final TransactionTemplate transactionTemplate;

    @Async
    public void run(UUID jobId) {
        try {
            SeedHistoryResponse result = forecastService.seedDemoHistory();
            complete(jobId, result);
        } catch (Exception e) {
            fail(jobId, e);
        }
    }

    void complete(UUID jobId, SeedHistoryResponse result) {
        transactionTemplate.executeWithoutResult(status -> {
            AiDataJob job = aiDataJobRepository.findByJobId(jobId).orElseThrow();
            job.setStatus(AiDataJobStatus.COMPLETED);
            job.setCompletedAt(LocalDateTime.now());
            job.setRowsInserted(result.rowsInserted());
            job.setSeriesSeeded(result.seriesSeeded());
            job.setErrorMessage(null);
            aiDataJobRepository.saveAndFlush(job);
        });
    }

    void fail(UUID jobId, Exception e) {
        transactionTemplate.executeWithoutResult(status -> {
            AiDataJob job = aiDataJobRepository.findByJobId(jobId).orElseThrow();
            job.setStatus(AiDataJobStatus.FAILED);
            job.setCompletedAt(LocalDateTime.now());
            job.setErrorMessage(safeMessage(e));
            aiDataJobRepository.saveAndFlush(job);
        });
    }

    private String safeMessage(Exception e) {
        String message = e.getMessage();
        return message == null || message.isBlank() ? e.getClass().getSimpleName() : message;
    }
}
