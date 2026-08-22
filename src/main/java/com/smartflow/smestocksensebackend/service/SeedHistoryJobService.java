package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.forecast.SeedHistoryJobResponse;
import com.smartflow.smestocksensebackend.entity.AiDataJob;
import com.smartflow.smestocksensebackend.entity.AiDataJobStatus;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.AiDataJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeedHistoryJobService {

    private final AiDataJobRepository aiDataJobRepository;
    private final SeedHistoryJobRunner seedHistoryJobRunner;
    private final TransactionTemplate transactionTemplate;

    public SeedHistoryJobResponse start() {
        StartResult result = transactionTemplate.execute(status -> {
            Optional<AiDataJob> active = findActive();
            if (active.isPresent()) {
                return new StartResult(toResponse(active.get()), false);
            }

            AiDataJob job = new AiDataJob();
            job.setJobId(UUID.randomUUID());
            job.setJobType(AiDataJob.TYPE_SEED_DEMO_HISTORY);
            job.setStatus(AiDataJobStatus.RUNNING);
            job.setStartedAt(LocalDateTime.now());
            try {
                aiDataJobRepository.saveAndFlush(job);
            } catch (DataIntegrityViolationException e) {
                return findActive()
                        .map(activeJob -> new StartResult(toResponse(activeJob), false))
                        .orElseThrow(() -> e);
            }
            return new StartResult(toResponse(job), true);
        });
        if (result.launch()) {
            seedHistoryJobRunner.run(result.response().jobId());
        }
        return result.response();
    }

    public SeedHistoryJobResponse get(UUID jobId) {
        return transactionTemplate.execute(status -> aiDataJobRepository.findByJobId(jobId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tác vụ sinh dữ liệu demo.")));
    }

    public Optional<SeedHistoryJobResponse> active() {
        return transactionTemplate.execute(status -> findActive().map(this::toResponse));
    }

    private Optional<AiDataJob> findActive() {
        return aiDataJobRepository.findFirstByJobTypeAndStatusOrderByStartedAtDesc(
                AiDataJob.TYPE_SEED_DEMO_HISTORY, AiDataJobStatus.RUNNING);
    }

    private SeedHistoryJobResponse toResponse(AiDataJob job) {
        return new SeedHistoryJobResponse(job.getJobId(), job.getStatus().name(), job.getStartedAt(),
                job.getCompletedAt(), job.getRowsInserted(), job.getSeriesSeeded(), job.getErrorMessage());
    }

    private record StartResult(SeedHistoryJobResponse response, boolean launch) {}
}
