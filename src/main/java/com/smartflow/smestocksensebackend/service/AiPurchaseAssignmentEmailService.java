package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.entity.AiPurchaseRequest;

public interface AiPurchaseAssignmentEmailService {

    void sendAssignmentNotification(AiPurchaseRequest assignment);
}
