package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.aiassignment.AiPurchaseAssignmentResponse;
import com.smartflow.smestocksensebackend.dto.aiassignment.CreateAiPurchaseAssignmentRequest;
import com.smartflow.smestocksensebackend.dto.common.PageResponse;
import com.smartflow.smestocksensebackend.service.AiPurchaseAssignmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai-purchase-assignments")
@RequiredArgsConstructor
public class AiPurchaseAssignmentController {

    private final AiPurchaseAssignmentService aiPurchaseAssignmentService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER')")
    public ResponseEntity<AiPurchaseAssignmentResponse> createAssignment(
            @RequestBody CreateAiPurchaseAssignmentRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(aiPurchaseAssignmentService.createAssignment(request));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public PageResponse<AiPurchaseAssignmentResponse> listMyAssignments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageRequest pageable = PageRequest.of(page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id")));
        return PageResponse.from(aiPurchaseAssignmentService.listMyAssignments(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public AiPurchaseAssignmentResponse getAssignment(@PathVariable Long id) {
        return aiPurchaseAssignmentService.getAssignment(id);
    }
}
