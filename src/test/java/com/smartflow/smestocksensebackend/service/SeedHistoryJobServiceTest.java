package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.forecast.SeedHistoryJobResponse;
import com.smartflow.smestocksensebackend.dto.forecast.SeedHistoryResponse;
import com.smartflow.smestocksensebackend.entity.AiDataJob;
import com.smartflow.smestocksensebackend.entity.AiDataJobStatus;
import com.smartflow.smestocksensebackend.repository.AiDataJobRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SeedHistoryJobServiceTest {

    @Mock
    AiDataJobRepository aiDataJobRepository;
    @Mock
    SeedHistoryJobRunner seedHistoryJobRunner;
    @Mock
    ForecastService forecastService;
    @Mock
    TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        lenient().doAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(null);
        }).when(transactionTemplate).execute(any());
        lenient().doAnswer(invocation -> {
            Consumer<TransactionStatus> callback = invocation.getArgument(0);
            callback.accept(null);
            return null;
        }).when(transactionTemplate).executeWithoutResult(any());
    }

    @Test
    void start_shouldCreateRunningJobThenLaunchAfterTransactionBlock() {
        when(aiDataJobRepository.findFirstByJobTypeAndStatusOrderByStartedAtDesc(
                AiDataJob.TYPE_SEED_DEMO_HISTORY, AiDataJobStatus.RUNNING))
                .thenReturn(Optional.empty());
        when(aiDataJobRepository.saveAndFlush(any(AiDataJob.class))).thenAnswer(invocation -> invocation.getArgument(0));
        SeedHistoryJobService service = new SeedHistoryJobService(aiDataJobRepository, seedHistoryJobRunner,
                transactionTemplate);

        SeedHistoryJobResponse response = service.start();

        assertEquals("RUNNING", response.status());
        assertNotNull(response.jobId());
        InOrder inOrder = inOrder(transactionTemplate, seedHistoryJobRunner);
        inOrder.verify(transactionTemplate).execute(any());
        inOrder.verify(seedHistoryJobRunner).run(response.jobId());
    }

    @Test
    void start_shouldReturnActiveJobWithoutLaunchingAnotherWorker() {
        AiDataJob active = runningJob(UUID.randomUUID());
        when(aiDataJobRepository.findFirstByJobTypeAndStatusOrderByStartedAtDesc(
                AiDataJob.TYPE_SEED_DEMO_HISTORY, AiDataJobStatus.RUNNING))
                .thenReturn(Optional.of(active));
        SeedHistoryJobService service = new SeedHistoryJobService(aiDataJobRepository, seedHistoryJobRunner,
                transactionTemplate);

        SeedHistoryJobResponse response = service.start();

        assertEquals(active.getJobId(), response.jobId());
        verify(seedHistoryJobRunner, never()).run(any());
    }

    @Test
    void complete_shouldPersistTerminalSuccess() {
        UUID jobId = UUID.randomUUID();
        AiDataJob job = runningJob(jobId);
        when(aiDataJobRepository.findByJobId(jobId)).thenReturn(Optional.of(job));
        SeedHistoryJobRunner runner = new SeedHistoryJobRunner(forecastService, aiDataJobRepository,
                transactionTemplate);

        runner.complete(jobId, new SeedHistoryResponse("SEED_DEMO", 2, 360,
                LocalDate.parse("2026-02-24"), LocalDate.parse("2026-08-22")));

        assertEquals(AiDataJobStatus.COMPLETED, job.getStatus());
        assertEquals(360, job.getRowsInserted());
        assertEquals(2, job.getSeriesSeeded());
        assertNotNull(job.getCompletedAt());
        verify(aiDataJobRepository).saveAndFlush(job);
    }

    @Test
    void fail_shouldPersistTerminalFailure() {
        UUID jobId = UUID.randomUUID();
        AiDataJob job = runningJob(jobId);
        when(aiDataJobRepository.findByJobId(jobId)).thenReturn(Optional.of(job));
        SeedHistoryJobRunner runner = new SeedHistoryJobRunner(forecastService, aiDataJobRepository,
                transactionTemplate);

        runner.fail(jobId, new IllegalStateException("boom"));

        assertEquals(AiDataJobStatus.FAILED, job.getStatus());
        assertEquals("boom", job.getErrorMessage());
        assertNotNull(job.getCompletedAt());
        verify(aiDataJobRepository).saveAndFlush(job);
    }

    private AiDataJob runningJob(UUID jobId) {
        AiDataJob job = new AiDataJob();
        job.setJobId(jobId);
        job.setJobType(AiDataJob.TYPE_SEED_DEMO_HISTORY);
        job.setStatus(AiDataJobStatus.RUNNING);
        job.setStartedAt(java.time.LocalDateTime.parse("2026-08-22T10:00:00"));
        return job;
    }
}
