package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.AiPurchaseRequest;
import com.smartflow.smestocksensebackend.entity.AiPurchaseRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiPurchaseRequestRepository extends JpaRepository<AiPurchaseRequest, Long> {

    boolean existsByCodeIgnoreCase(String code);

    List<AiPurchaseRequest> findByReceiverIdAndStatusOrderByCreatedAtDesc(Long receiverId, AiPurchaseRequestStatus status);
}
