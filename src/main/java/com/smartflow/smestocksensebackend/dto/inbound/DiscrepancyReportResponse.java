package com.smartflow.smestocksensebackend.dto.inbound;

import com.smartflow.smestocksensebackend.entity.DiscrepancyReport;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Phản hồi chứa thông tin biên bản chênh lệch nhập kho.
 * Gồm các thông tin tổng quát về phiếu nhập liên kết, mã biên bản, ngày lập, người lập,
 * ghi chú và danh sách các chi tiết sản phẩm bị lệch.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DiscrepancyReportResponse {
    private Long id;
    private Long receiptId;
    private String receiptCode;
    private String code;
    private String status;
    private LocalDateTime reportDate;
    private Long createdById;
    private String createdByName;
    private String note;
    private List<DiscrepancyReportDetailResponse> details;
    private Integer detailCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;

    /**
     * Chuyển đổi từ entity DiscrepancyReport sang DTO DiscrepancyReportResponse.
     *
     * @param report Entity biên bản chênh lệch
     * @return DTO chứa thông tin biên bản chênh lệch
     */
    public static DiscrepancyReportResponse from(DiscrepancyReport report) {
        List<DiscrepancyReportDetailResponse> detailResponses = report.getDetails() != null
                ? report.getDetails().stream().map(DiscrepancyReportDetailResponse::from).toList()
                : List.of();

        return DiscrepancyReportResponse.builder()
                .id(report.getId())
                .receiptId(report.getImportReceipt() != null ? report.getImportReceipt().getId() : null)
                .receiptCode(report.getImportReceipt() != null ? report.getImportReceipt().getCode() : null)
                .code(report.getCode())
                .status(report.getStatus() != null ? report.getStatus().name() : null)
                .reportDate(report.getReportDate())
                .createdById(report.getCreatedBy() != null ? report.getCreatedBy().getId() : null)
                .createdByName(report.getCreatedBy() != null ? report.getCreatedBy().getFullName() : null)
                .note(report.getNote())
                .details(detailResponses)
                .detailCount(detailResponses.size())
                .createdAt(report.getCreatedAt())
                .updatedAt(report.getUpdatedAt())
                .version(report.getVersion())
                .build();
    }
}
