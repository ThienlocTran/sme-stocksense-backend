package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.AiPurchaseRequest;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.service.AiPurchaseAssignmentEmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AiPurchaseAssignmentEmailServiceImpl implements AiPurchaseAssignmentEmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendAssignmentNotification(AiPurchaseRequest assignment) {
        Employee receiver = assignment.getReceiver();
        if (receiver == null || receiver.getEmail() == null || receiver.getEmail().isBlank()) {
            throw new BadRequestException("Nhân viên nhận việc chưa có email.");
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(fromEmail);
        message.setTo(receiver.getEmail());
        message.setSubject("SME StockSense: Nhiệm vụ bổ sung tồn kho " + assignment.getCode());
        message.setText(body(assignment));
        mailSender.send(message);
    }

    private String body(AiPurchaseRequest assignment) {
        return """
                Xin chào %s,

                Đề xuất bổ sung tồn kho từ AI đã được quản lý xem xét và giao cho bạn xử lý.

                Sản phẩm: %s - %s
                Kho: %s - %s
                Kỳ dự báo: %s ngày
                Số lượng AI gợi ý: %s
                Số lượng yêu cầu: %s
                Nội dung: %s

                Vui lòng mở SME StockSense và tạo phiếu nhập kho thủ công theo quy trình nhập kho hiện tại.
                Email này chỉ là thông báo. Bản ghi nhiệm vụ trong StockSense là nguồn dữ liệu chính.
                """.formatted(
                value(assignment.getReceiver().getFullName()),
                value(assignment.getProduct().getCode()),
                value(assignment.getProduct().getName()),
                value(assignment.getWarehouse().getCode()),
                value(assignment.getWarehouse().getName()),
                assignment.getHorizonDays(),
                assignment.getAiSuggestedQuantity(),
                assignment.getRequestedQuantity(),
                value(assignment.getContent())
        );
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
