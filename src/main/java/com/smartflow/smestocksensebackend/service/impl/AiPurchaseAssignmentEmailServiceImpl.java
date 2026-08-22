package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.AiPurchaseRequest;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.service.AiPurchaseAssignmentEmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
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

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(receiver.getEmail());
            helper.setSubject("SME StockSense: Nhiệm vụ bổ sung tồn kho " + assignment.getCode());
            helper.setText(htmlBody(assignment), true);
            mailSender.send(message);
        } catch (jakarta.mail.MessagingException e) {
            throw new org.springframework.mail.MailPreparationException("Lỗi chuẩn bị email", e);
        }
    }

    private String htmlBody(AiPurchaseRequest assignment) {
        String title = "NHIỆM VỤ BỔ SUNG TỒN KHO";
        String subtitle = "Xin chào %s, đề xuất bổ sung tồn kho từ AI đã được quản lý xem xét và giao cho bạn xử lý.".formatted(value(assignment.getReceiver().getFullName()));

        String detailsCardHtml = """
                <div class="card">
                    <table>
                        <tr><td class="label">Mã Yêu Cầu:</td><td class="value"><strong>%s</strong></td></tr>
                        <tr><td class="label">Sản phẩm:</td><td class="value">%s - %s</td></tr>
                        <tr><td class="label">Kho hàng:</td><td class="value">%s - %s</td></tr>
                        <tr><td class="label">Kỳ dự báo:</td><td class="value">%s ngày</td></tr>
                        <tr><td class="label">Số lượng AI gợi ý:</td><td class="value">%s</td></tr>
                        <tr><td class="label">Số lượng yêu cầu:</td><td class="value" style="color:#b23a34; font-weight:bold; font-size:16px;">%s</td></tr>
                        <tr><td class="label">Nội dung:</td><td class="value">%s</td></tr>
                    </table>
                </div>
                """.formatted(
                assignment.getCode(),
                value(assignment.getProduct().getCode()),
                value(assignment.getProduct().getName()),
                value(assignment.getWarehouse().getCode()),
                value(assignment.getWarehouse().getName()),
                assignment.getHorizonDays(),
                assignment.getAiSuggestedQuantity(),
                assignment.getRequestedQuantity(),
                value(assignment.getContent())
        );

        return buildEmailTemplate(title, subtitle, detailsCardHtml);
    }

    private String buildEmailTemplate(String title, String subtitle, String detailsCardHtml) {
        return "<!DOCTYPE html>"
                + "<html>"
                + "<head>"
                + "    <meta charset=\"utf-8\">"
                + "    <style>"
                + "        body { font-family: 'Segoe UI', Calibri, Arial, sans-serif; background-color: #f3f4ef; color: #1f2620; margin: 0; padding: 20px; }"
                + "        .container { max-width: 650px; margin: 0 auto; background-color: #ffffff; border-radius: 4px; overflow: hidden; box-shadow: 0 2px 5px rgba(0,0,0,0.05); border: 1px solid #d8dad2; }"
                + "        .header { background-color: #1f5c54; color: #ffffff; padding: 25px; text-align: center; }"
                + "        .header h2 { margin: 0; font-size: 24px; font-weight: 600; letter-spacing: 0.5px; }"
                + "        .header p { margin: 5px 0 0 0; font-size: 14px; opacity: 0.9; }"
                + "        .content { padding: 30px; line-height: 1.6; }"
                + "        .subtitle { font-size: 16px; font-weight: bold; margin-bottom: 20px; color: #1f5c54; }"
                + "        .card { background-color: #f9faf8; border-left: 4px solid #1f5c54; padding: 20px; margin-bottom: 25px; border-radius: 0 4px 4px 0; border-top: 1px solid #eceee7; border-right: 1px solid #eceee7; border-bottom: 1px solid #eceee7; }"
                + "        .card table { width: 100%; border-collapse: collapse; }"
                + "        .card td { padding: 8px 0; font-size: 14px; vertical-align: top; }"
                + "        .card td.label { font-weight: bold; width: 35%; color: #5b6259; }"
                + "        .card td.value { color: #1f2620; }"
                + "        .footer { background-color: #f3f4ef; color: #5b6259; text-align: center; padding: 20px; font-size: 12px; border-top: 1px solid #d8dad2; }"
                + "    </style>"
                + "</head>"
                + "<body>"
                + "    <div class=\"container\">"
                + "        <div class=\"header\">"
                + "            <h2>" + title + "</h2>"
                + "            <p>SME StockSense — Hệ thống Quản lý Kho thông minh</p>"
                + "        </div>"
                + "        <div class=\"content\">"
                + "            <div class=\"subtitle\">" + subtitle + "</div>"
                + "            " + detailsCardHtml
                + "            <p style=\"font-size: 14px; color: #5b6259; margin-top: 25px;\">"
                + "                Vui lòng mở <strong>SME StockSense</strong> và tạo phiếu nhập kho thủ công theo quy trình nhập kho hiện tại.<br>"
                + "                Email này chỉ là thông báo. Bản ghi nhiệm vụ trong StockSense là nguồn dữ liệu chính."
                + "            </p>"
                + "        </div>"
                + "        <div class=\"footer\">"
                + "            Đây là email thông báo tự động từ hệ thống SME StockSense.<br>Vui lòng không trả lời trực tiếp email này."
                + "        </div>"
                + "    </div>"
                + "</body>"
                + "</html>";
    }

    private String value(String value) {
        return value == null || value.isBlank() ? "-" : value;
    }
}
