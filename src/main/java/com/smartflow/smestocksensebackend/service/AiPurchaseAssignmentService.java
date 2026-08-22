package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.aiassignment.AiPurchaseAssignmentResponse;
import com.smartflow.smestocksensebackend.dto.aiassignment.CreateAiPurchaseAssignmentRequest;

public interface AiPurchaseAssignmentService {

    AiPurchaseAssignmentResponse createAssignment(CreateAiPurchaseAssignmentRequest request);
}
