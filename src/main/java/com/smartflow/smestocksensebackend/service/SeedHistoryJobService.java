package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.forecast.SeedHistoryJobResponse;
import com.smartflow.smestocksensebackend.entity.AiDataJob;
import com.smartflow.smestocksensebackend.entity.AiDataJobStatus;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.AiDataJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SeedHistoryJobService {

    private final AiDataJobRepository aiDataJobRepository;
    private final SeedHistoryJobRunner seedHistoryJobRunner;

    @Transactional
    public SeedHistoryJobResponse start() {
        Optional<AiDataJob> active = findActive();
        if (active.isPresent()) {
            return toResponse(active.get());
        }

        AiDataJob job = new AiDataJob();
        job.setJobId(UUID.randomUUID());
        job.setJobType(AiDataJob.TYPE_SEED_DEMO_HISTORY);
        job.setStatus(AiDataJobStatus.RUNNING);
        job.setStartedAt(LocalDateTime.now());
        try {
            aiDataJobRepository.saveAndFlush(job);
        } catch (DataIntegrityViolationException e) {
            return findActive().map(this::toResponse).orElseThrow(() -> e);
        }
        seedHistoryJobRunner.run(job.getJobId());
        return toResponse(job);
    }

    @Transactional(readOnly = true)
    public SeedHistoryJobResponse get(UUID jobId) {
        return aiDataJobRepository.findByJobId(jobId)
                .map(this::toResponse)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy tác vụ sinh dữ liệu demo."));
    }

    @Transactional(readOnly = true)
    public Optional<SeedHistoryJobResponse> active() {
        return findActive().map(this::toResponse);
    }

    private Optional<AiDataJob> findActive() {
        return aiDataJobRepository.findFirstByJobTypeAndStatusOrderByStartedAtDesc(
                AiDataJob.TYPE_SEED_DEMO_HISTORY, AiDataJobStatus.RUNNING);
    }

    private SeedHistoryJobResponse toResponse(AiDataJob job) {
        return new SeedHistoryJobResponse(job.getJobId(), job.getStatus().name(), job.getStartedAt(),
                job.getCompletedAt(), job.getRowsInserted(), job.getSeriesSeeded(), job.getErrorMessage());
    }
}
