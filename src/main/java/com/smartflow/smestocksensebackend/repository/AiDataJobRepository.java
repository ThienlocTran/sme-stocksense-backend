package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.AiDataJob;
import com.smartflow.smestocksensebackend.entity.AiDataJobStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AiDataJobRepository extends JpaRepository<AiDataJob, Long> {

    Optional<AiDataJob> findByJobId(UUID jobId);

    Optional<AiDataJob> findFirstByJobTypeAndStatusOrderByStartedAtDesc(String jobType, AiDataJobStatus status);
}
