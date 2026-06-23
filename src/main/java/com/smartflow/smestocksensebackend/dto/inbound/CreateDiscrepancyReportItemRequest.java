package com.smartflow.smestocksensebackend.dto.inbound;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Yêu cầu chi tiết cho từng dòng sản phẩm chênh lệch khi lập biên bản.
 * Chứa ID sản phẩm chênh lệch cùng với lý do lệch và hướng xử lý đề xuất.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDiscrepancyReportItemRequest {

    @NotNull(message = "productId không được để trống.")
    @Positive(message = "productId phải lớn hơn 0.")
    private Long productId;

    @Size(max = 255, message = "reason không được vượt quá 255 ký tự.")
    private String reason;

    @Size(max = 255, message = "action không được vượt quá 255 ký tự.")
    private String action;
}
