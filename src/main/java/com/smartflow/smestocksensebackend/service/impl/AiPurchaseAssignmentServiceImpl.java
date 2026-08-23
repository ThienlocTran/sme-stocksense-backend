package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.aiassignment.AiPurchaseAssignmentResponse;
import com.smartflow.smestocksensebackend.dto.aiassignment.CreateAiPurchaseAssignmentRequest;
import com.smartflow.smestocksensebackend.dto.replenishment.ForecastReplenishmentRecommendationResponse;
import com.smartflow.smestocksensebackend.entity.AiPurchaseRequest;
import com.smartflow.smestocksensebackend.entity.AiPurchaseRequestEmailStatus;
import com.smartflow.smestocksensebackend.entity.AiPurchaseRequestStatus;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.ForecastModelMetadata;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.AiPurchaseRequestRepository;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.repository.ForecastModelMetadataRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.AiPurchaseAssignmentEmailService;
import com.smartflow.smestocksensebackend.service.AiPurchaseAssignmentService;
import com.smartflow.smestocksensebackend.service.ForecastReplenishmentRecommendationService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.MailException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AiPurchaseAssignmentServiceImpl implements AiPurchaseAssignmentService {

    private static final DateTimeFormatter CODE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    private final AiPurchaseRequestRepository aiPurchaseRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final ForecastModelMetadataRepository forecastModelMetadataRepository;
    private final AiPurchaseAssignmentEmailService aiPurchaseAssignmentEmailService;
    private final ForecastReplenishmentRecommendationService recommendationService;
    private final PlatformTransactionManager transactionManager;

    @Override
    public AiPurchaseAssignmentResponse createAssignment(CreateAiPurchaseAssignmentRequest request) {
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        AiPurchaseRequest assignment = tx.execute(status -> persistAssignment(request));
        Long assignmentId = assignment.getId();

        try {
            aiPurchaseAssignmentEmailService.sendAssignmentNotification(assignment);
            assignment = tx.execute(status -> updateEmailStatus(assignmentId,
                    AiPurchaseRequestEmailStatus.DA_GUI, null));
        } catch (MailException | BadRequestException ex) {
            assignment = tx.execute(status -> updateEmailStatus(assignmentId,
                    AiPurchaseRequestEmailStatus.THAT_BAI, ex.getMessage()));
        }
        return AiPurchaseAssignmentResponse.from(assignment);
    }

    private AiPurchaseRequest persistAssignment(CreateAiPurchaseAssignmentRequest request) {
        Employee sender = currentEmployee();
        ensureSenderCanAssign(sender);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại."));
        Warehouse warehouse = warehouseRepository.findById(request.warehouseId())
                .orElseThrow(() -> new NotFoundException("Kho không tồn tại."));
        Employee receiver = employeeRepository.findById(request.receiverId())
                .orElseThrow(() -> new NotFoundException("Nhân viên nhận việc không tồn tại."));
        if (receiver.getRole() == null || receiver.getRole().getCode() != RoleCode.EMPLOYEE) {
            throw new BadRequestException("Người nhận phải là nhân viên.");
        }

        ForecastReplenishmentRecommendationResponse recommendation = recommendationService
                .getRecommendation(product.getId(), warehouse.getId(), request.horizonDays());
        ForecastModelMetadata modelMetadata = resolveModelMetadata(request, recommendation);

        AiPurchaseRequest assignment = new AiPurchaseRequest();
        assignment.setCode(nextCode());
        assignment.setModelMetadata(modelMetadata);
        assignment.setProduct(product);
        assignment.setWarehouse(warehouse);
        assignment.setHorizonDays(recommendation.horizonDays());
        assignment.setAiSuggestedQuantity(recommendation.suggestedQty());
        assignment.setRequestedQuantity(request.requestedQuantity());
        assignment.setSender(sender);
        assignment.setReceiver(receiver);
        assignment.setContent(normalizeContent(request.content()));
        assignment.setStatus(AiPurchaseRequestStatus.DA_GUI);
        assignment.setEmailStatus(AiPurchaseRequestEmailStatus.CHO_GUI);

        return aiPurchaseRequestRepository.saveAndFlush(assignment);
    }

    private AiPurchaseRequest updateEmailStatus(Long id, AiPurchaseRequestEmailStatus emailStatus, String error) {
        AiPurchaseRequest assignment = aiPurchaseRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Yêu cầu nhập hàng AI không tồn tại."));
        assignment.setEmailStatus(emailStatus);
        assignment.setEmailError(truncate(error));
        assignment.setEmailSentAt(emailStatus == AiPurchaseRequestEmailStatus.DA_GUI ? LocalDateTime.now() : null);
        return aiPurchaseRequestRepository.saveAndFlush(assignment);
    }

    private String truncate(String error) {
        if (error == null) {
            return null;
        }
        return error.length() <= 500 ? error : error.substring(0, 500);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AiPurchaseAssignmentResponse> listMyAssignments(Pageable pageable) {
        Employee actor = currentEmployee();
        RoleCode role = roleOf(actor);
        Page<AiPurchaseRequest> page = role == RoleCode.ADMIN || role == RoleCode.MANAGER
                ? aiPurchaseRequestRepository.findAll(pageable)
                : aiPurchaseRequestRepository.findByReceiverId(actor.getId(), pageable);
        return page.map(AiPurchaseAssignmentResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public AiPurchaseAssignmentResponse getAssignment(Long id) {
        Employee actor = currentEmployee();
        AiPurchaseRequest assignment = aiPurchaseRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Yêu cầu nhập hàng AI không tồn tại."));
        ensureCanRead(actor, assignment);
        return AiPurchaseAssignmentResponse.from(assignment);
    }

    @Override
    public AiPurchaseAssignmentResponse retryEmail(Long id) {
        Employee actor = currentEmployee();
        ensureSenderCanAssign(actor);
        TransactionTemplate tx = new TransactionTemplate(transactionManager);
        AiPurchaseRequest assignment = aiPurchaseRequestRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Yêu cầu nhập hàng AI không tồn tại."));
        if (assignment.getEmailStatus() == AiPurchaseRequestEmailStatus.DA_GUI) {
            throw new BadRequestException("Email thông báo đã được gửi.");
        }
        if (assignment.getEmailStatus() != AiPurchaseRequestEmailStatus.THAT_BAI
                && assignment.getEmailStatus() != AiPurchaseRequestEmailStatus.CHO_GUI) {
            throw new BadRequestException("Chỉ gửi email ở trạng thái chờ gửi hoặc gửi thất bại.");
        }

        try {
            aiPurchaseAssignmentEmailService.sendAssignmentNotification(assignment);
            assignment = tx.execute(status -> updateEmailStatus(id, AiPurchaseRequestEmailStatus.DA_GUI, null));
        } catch (MailException | BadRequestException ex) {
            assignment = tx.execute(status -> updateEmailStatus(id, AiPurchaseRequestEmailStatus.THAT_BAI, ex.getMessage()));
        }
        return AiPurchaseAssignmentResponse.from(assignment);
    }

    private Employee currentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Employee principal)
                || principal.getId() == null) {
            throw new AuthenticationCredentialsNotFoundException("Chưa xác thực.");
        }
        return employeeRepository.findById(principal.getId())
                .orElseThrow(() -> new AuthenticationCredentialsNotFoundException("Tài khoản không tồn tại."));
    }

    private void ensureSenderCanAssign(Employee sender) {
        RoleCode role = roleOf(sender);
        if (role != RoleCode.ADMIN && role != RoleCode.MANAGER) {
            throw new MissingRoleException("Chỉ ADMIN hoặc MANAGER được giao yêu cầu nhập hàng AI.");
        }
    }

    private void ensureCanRead(Employee actor, AiPurchaseRequest assignment) {
        RoleCode role = roleOf(actor);
        if (role == RoleCode.ADMIN || role == RoleCode.MANAGER) {
            return;
        }
        Long receiverId = assignment.getReceiver() != null ? assignment.getReceiver().getId() : null;
        if (role == RoleCode.EMPLOYEE && actor.getId().equals(receiverId)) {
            return;
        }
        throw new ConflictException("Không có quyền xem yêu cầu nhập hàng AI này.");
    }

    private RoleCode roleOf(Employee employee) {
        return employee.getRole() != null ? employee.getRole().getCode() : null;
    }

    private ForecastModelMetadata resolveModelMetadata(CreateAiPurchaseAssignmentRequest request,
            ForecastReplenishmentRecommendationResponse recommendation) {
        if (recommendation.modelMetadataId() == null) {
            throw new BadRequestException("Khuyến nghị chưa có thông tin mô hình AI.");
        }
        if (request.modelMetadataId() != null && !request.modelMetadataId().equals(recommendation.modelMetadataId())) {
            throw new BadRequestException("Thông tin mô hình AI không khớp khuyến nghị.");
        }
        return forecastModelMetadataRepository.findById(recommendation.modelMetadataId())
                .orElseThrow(() -> new NotFoundException("Thông tin mô hình AI không tồn tại."));
    }

    private String normalizeContent(String content) {
        return content == null || content.isBlank() ? null : content.trim();
    }

    private String nextCode() {
        for (int i = 0; i < 5; i++) {
            String code = "YCAI-" + LocalDateTime.now().format(CODE_TIME) + "-"
                    + ThreadLocalRandom.current().nextInt(100, 1000);
            if (!aiPurchaseRequestRepository.existsByCodeIgnoreCase(code)) {
                return code;
            }
        }
        throw new BadRequestException("Không tạo được mã yêu cầu nhập hàng AI.");
    }
}
