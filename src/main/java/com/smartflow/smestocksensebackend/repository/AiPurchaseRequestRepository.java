package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.AiPurchaseRequest;
import com.smartflow.smestocksensebackend.entity.AiPurchaseRequestStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiPurchaseRequestRepository extends JpaRepository<AiPurchaseRequest, Long> {

    boolean existsByCodeIgnoreCase(String code);

    @Override
    @EntityGraph(attributePaths = {"product", "product.partner", "warehouse", "sender", "receiver", "modelMetadata",
            "importReceipt"})
    Optional<AiPurchaseRequest> findById(Long id);

    @Override
    @EntityGraph(attributePaths = {"product", "product.partner", "warehouse", "sender", "receiver", "modelMetadata",
            "importReceipt"})
    Page<AiPurchaseRequest> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"product", "product.partner", "warehouse", "sender", "receiver", "modelMetadata",
            "importReceipt"})
    Page<AiPurchaseRequest> findByReceiverId(Long receiverId, Pageable pageable);

    List<AiPurchaseRequest> findByReceiverIdAndStatusOrderByCreatedAtDesc(Long receiverId, AiPurchaseRequestStatus status);
}
