package com.smartflow.smestocksensebackend.dto.inbound;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Yêu cầu tạo biên bản chênh lệch khi kiểm đếm hàng thực tế nhập kho.
 * Chứa ghi chú tổng quát và danh sách lý do/hướng xử lý cho từng sản phẩm bị chênh lệch.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateDiscrepancyReportRequest {

    @Size(max = 255, message = "note không được vượt quá 255 ký tự.")
    private String note;

    @NotEmpty(message = "Danh sách sản phẩm chênh lệch không được để trống.")
    @Valid
    private List<CreateDiscrepancyReportItemRequest> items;
}
