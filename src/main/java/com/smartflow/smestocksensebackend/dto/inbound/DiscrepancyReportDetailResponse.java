package com.smartflow.smestocksensebackend.dto.inbound;

import com.smartflow.smestocksensebackend.entity.DiscrepancyReportDetail;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Phản hồi chứa chi tiết sản phẩm chênh lệch trong biên bản.
 * Gồm mã và tên sản phẩm, số lượng theo chứng từ gốc, số lượng thực tế nhận được,
 * số lượng chênh lệch (âm là thiếu, dương là thừa), lý do chênh lệch và hướng xử lý.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscrepancyReportDetailResponse {
    private Long id;
    private Long productId;
    private String productCode;
    private String productName;
    private Integer documentQuantity;
    private Integer actualQuantity;
    private Integer discrepancyQuantity;
    private String reason;
    private String action;

    /**
     * Chuyển đổi từ entity DiscrepancyReportDetail sang DTO DiscrepancyReportDetailResponse.
     *
     * @param detail Entity chi tiết biên bản chênh lệch
     * @return DTO chứa thông tin chi tiết sản phẩm chênh lệch
     */
    public static DiscrepancyReportDetailResponse from(DiscrepancyReportDetail detail) {
        return DiscrepancyReportDetailResponse.builder()
                .id(detail.getId())
                .productId(detail.getProduct() != null ? detail.getProduct().getId() : null)
                .productCode(detail.getProduct() != null ? detail.getProduct().getCode() : null)
                .productName(detail.getProduct() != null ? detail.getProduct().getName() : null)
                .documentQuantity(detail.getDocumentQuantity())
                .actualQuantity(detail.getActualQuantity())
                .discrepancyQuantity(detail.getDiscrepancyQuantity())
                .reason(detail.getReason())
                .action(detail.getAction())
                .build();
    }
}
