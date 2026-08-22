package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.aiassignment.AiPurchaseAssignmentResponse;
import com.smartflow.smestocksensebackend.dto.aiassignment.CreateAiPurchaseAssignmentRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AiPurchaseAssignmentService {

    AiPurchaseAssignmentResponse createAssignment(CreateAiPurchaseAssignmentRequest request);

    Page<AiPurchaseAssignmentResponse> listMyAssignments(Pageable pageable);

    AiPurchaseAssignmentResponse getAssignment(Long id);
}
