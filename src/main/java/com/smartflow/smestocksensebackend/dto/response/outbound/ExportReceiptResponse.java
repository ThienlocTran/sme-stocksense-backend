package com.smartflow.smestocksensebackend.dto.response.outbound;

import com.smartflow.smestocksensebackend.entity.ExportReceipt;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DTO trả về thông tin phiếu xuất.
 */
@Getter
@Setter
public class ExportReceiptResponse {
    private Long id;
    private String code;
    private Long warehouseId;
    private String warehouseName;
    private Long partnerId;
    private String partnerName;
    private String status;
    private BigDecimal totalAmount;
    private String note;
    private String rejectionReason;
    private Long version;
    private Long createdById;
    private String createdByName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ExportReceiptDetailResponse> details;

    public static ExportReceiptResponse from(ExportReceipt entity) {
        if (entity == null) return null;
        ExportReceiptResponse response = new ExportReceiptResponse();
        response.setId(entity.getId());
        response.setCode(entity.getCode());
        
        if (entity.getWarehouse() != null) {
            response.setWarehouseId(entity.getWarehouse().getId());
            response.setWarehouseName(entity.getWarehouse().getName());
        }
        
        if (entity.getPartner() != null) {
            response.setPartnerId(entity.getPartner().getId());
            response.setPartnerName(entity.getPartner().getName());
        }
        
        response.setStatus(entity.getStatus() != null ? entity.getStatus().name() : null);
        response.setTotalAmount(entity.getTotalAmount());
        response.setNote(entity.getNote());
        response.setRejectionReason(entity.getRejectionReason());
        response.setVersion(entity.getVersion());
        
        if (entity.getCreatedBy() != null) {
            response.setCreatedById(entity.getCreatedBy().getId());
            response.setCreatedByName(entity.getCreatedBy().getFullName());
        }
        
        response.setCreatedAt(entity.getCreatedAt());
        response.setUpdatedAt(entity.getUpdatedAt());
        return response;
    }

    public static ExportReceiptResponse from(ExportReceipt entity, List<com.smartflow.smestocksensebackend.entity.ExportReceiptDetail> details) {
        if (entity == null) return null;
        ExportReceiptResponse response = from(entity);
        if (details != null) {
            response.setDetails(details.stream().map(ExportReceiptDetailResponse::from).collect(Collectors.toList()));
        }
        return response;
    }
}
