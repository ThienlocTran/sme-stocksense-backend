package com.smartflow.smestocksensebackend.dto.response.outbound;

import com.smartflow.smestocksensebackend.entity.ExportReceiptDetail;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO trả về thông tin dòng sản phẩm trong phiếu xuất.
 */
@Getter
@Setter
public class ExportReceiptDetailResponse {
    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
    private String note;

    public static ExportReceiptDetailResponse from(ExportReceiptDetail entity) {
        if (entity == null) return null;
        ExportReceiptDetailResponse response = new ExportReceiptDetailResponse();
        response.setId(entity.getId());
        if (entity.getProduct() != null) {
            response.setProductId(entity.getProduct().getId());
            response.setProductCode(entity.getProduct().getCode());
            response.setProductName(entity.getProduct().getName());
        }
        response.setQuantity(entity.getQuantity());
        response.setUnitPrice(entity.getUnitPrice());
        response.setTotalPrice(entity.getLineTotal());
        response.setNote(entity.getNote());
        return response;
    }
}
