package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.*;
import com.smartflow.smestocksensebackend.repository.*;
import com.smartflow.smestocksensebackend.service.EmailService;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
@Async // Run all methods asynchronously
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;
    private final EmployeeRepository employeeRepository;
    private final ImportReceiptRepository importReceiptRepository;
    private final ImportReceiptDetailRepository importReceiptDetailRepository;
    private final ExportReceiptRepository exportReceiptRepository;
    private final ExportReceiptDetailRepository exportReceiptDetailRepository;
    private final DiscrepancyReportRepository discrepancyReportRepository;
    private final DiscrepancyReportDetailRepository discrepancyReportDetailRepository;

    @Value("${spring.mail.username}")
    private String fromEmail;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final Locale VIETNAM_LOCALE = new Locale("vi", "VN");

    // =========================================================================
    // HELPER METHODS FOR SENDING EMAIL
    // =========================================================================

    private void sendHtmlEmail(String[] to, String subject, String htmlContent) {
        if (to == null || to.length == 0) {
            log.warn("No recipient email specified for subject: {}", subject);
            return;
        }
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(htmlContent, true);
            mailSender.send(message);
            log.info("Email sent successfully to: {} with subject: {}", String.join(", ", to), subject);
        } catch (Exception e) {
            log.error("Failed to send email to: {}", String.join(", ", to), e);
        }
    }

    private void sendHtmlEmail(String to, String subject, String htmlContent) {
        if (to == null || to.isBlank()) {
            log.warn("No recipient email specified for subject: {}", subject);
            return;
        }
        sendHtmlEmail(new String[]{to}, subject, htmlContent);
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) return "0 đ";
        NumberFormat formatter = NumberFormat.getCurrencyInstance(VIETNAM_LOCALE);
        return formatter.format(amount);
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) return "-";
        return dateTime.format(DATE_FORMATTER);
    }

    private String[] getActiveApproversEmails() {
        List<Employee> approvers = employeeRepository.findByRole_CodeInAndStatus(
                List.of(RoleCode.ADMIN, RoleCode.MANAGER),
                EmployeeStatus.HOAT_DONG
        );
        return approvers.stream()
                .map(Employee::getEmail)
                .filter(email -> email != null && !email.isBlank())
                .toArray(String[]::new);
    }

    // =========================================================================
    // EMAIL TEMPLATE GENERATOR
    // =========================================================================

    private String buildEmailTemplate(String title, String subtitle, String detailsCardHtml, String itemsTableHtml) {
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
                + "        .items-title { font-size: 15px; font-weight: bold; margin: 20px 0 10px 0; color: #1f2620; border-bottom: 2px solid #1f5c54; padding-bottom: 5px; }"
                + "        .items-table { width: 100%; border-collapse: collapse; margin-bottom: 20px; }"
                + "        .items-table th, .items-table td { border-bottom: 1px solid #d8dad2; padding: 12px 10px; text-align: left; font-size: 14px; }"
                + "        .items-table th { background-color: #f3f4ef; color: #1f2620; font-weight: bold; text-transform: uppercase; font-size: 12px; letter-spacing: 0.5px; }"
                + "        .badge { display: inline-block; padding: 3px 10px; border-radius: 999px; font-size: 12px; font-weight: bold; text-transform: uppercase; }"
                + "        .badge-pending { background-color: #f5ead4; color: #a86b16; }"
                + "        .badge-success { background-color: #e1efe4; color: #2f7d4f; }"
                + "        .badge-danger { background-color: #f6e2e0; color: #b23a34; }"
                + "        .badge-info { background-color: #dce8e4; color: #1f5c54; }"
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
                + "            " + itemsTableHtml
                + "        </div>"
                + "        <div class=\"footer\">"
                + "            Đây là email thông báo tự động từ hệ thống SME StockSense.<br>Vui lòng không trả lời trực tiếp email này."
                + "        </div>"
                + "    </div>"
                + "</body>"
                + "</html>";
    }

    // =========================================================================
    // IMPORT RECEIPT NOTIFICATIONS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public void sendImportReceiptSubmitted(ImportReceipt receiptStub) {
        ImportReceipt receipt = importReceiptRepository.findWithAllAssociationsById(receiptStub.getId()).orElse(null);
        if (receipt == null) return;

        List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(receipt.getId());

        String title = "YÊU CẦU PHÊ DUYỆT PHIẾU NHẬP KHO";
        String subtitle = "Kính gửi Ban Quản lý, có một phiếu nhập kho mới đang chờ duyệt cấp " 
                + (receipt.getStatus() == ImportReceiptStatus.CHO_DUYET_CAP_1 ? "1" : "2") + ".";

        String detailsCard = "<div class=\"card\">"
                + "<table>"
                + "    <tr><td class=\"label\">Mã Phiếu Nhập:</td><td class=\"value\"><strong>" + receipt.getCode() + "</strong></td></tr>"
                + "    <tr><td class=\"label\">Trạng thái:</td><td class=\"value\"><span class=\"badge badge-pending\">" + receipt.getStatus() + "</span></td></tr>"
                + "    <tr><td class=\"label\">Kho nhập hàng:</td><td class=\"value\">" + receipt.getWarehouse().getName() + "</td></tr>"
                + "    <tr><td class=\"label\">Nhà cung cấp:</td><td class=\"value\">" + (receipt.getSupplier() != null ? receipt.getSupplier().getName() : "-") + "</td></tr>"
                + "    <tr><td class=\"label\">Người lập phiếu:</td><td class=\"value\">" + receipt.getCreatedBy().getFullName() + " (" + receipt.getCreatedBy().getEmail() + ")</td></tr>"
                + "    <tr><td class=\"label\">Thời gian gửi duyệt:</td><td class=\"value\">" + formatDateTime(receipt.getSubmittedAt()) + "</td></tr>"
                + "    <tr><td class=\"label\">Tổng giá trị:</td><td class=\"value\" style=\"color:#b23a34; font-weight:bold; font-size:16px;\">" + formatCurrency(receipt.getTotalAmount()) + "</td></tr>"
                + "    <tr><td class=\"label\">Ghi chú:</td><td class=\"value\">" + (receipt.getNote() != null ? receipt.getNote() : "Không có") + "</td></tr>"
                + "</table>"
                + "</div>";

        StringBuilder itemsTable = new StringBuilder();
        itemsTable.append("<div class=\"items-title\">Danh sách sản phẩm yêu cầu nhập</div>")
                .append("<table class=\"items-table\">")
                .append("<thead><tr><th>STT</th><th>Sản phẩm</th><th style=\"text-align:right;\">Số lượng đặt</th><th style=\"text-align:right;\">Đơn giá dự kiến</th><th style=\"text-align:right;\">Thành tiền</th></tr></thead>")
                .append("<tbody>");

        int stt = 1;
        for (ImportReceiptDetail d : details) {
            itemsTable.append("<tr>")
                    .append("<td>").append(stt++).append("</td>")
                    .append("<td><strong>").append(d.getProduct().getName()).append("</strong> (").append(d.getProduct().getCode()).append(")</td>")
                    .append("<style>td.num{text-align:right;}</style>")
                    .append("<td style=\"text-align:right;\">").append(d.getExpectedQuantity()).append("</td>")
                    .append("<td style=\"text-align:right;\">").append(formatCurrency(d.getExpectedUnitPrice())).append("</td>")
                    .append("<td style=\"text-align:right;\">").append(formatCurrency(d.getExpectedLineTotal())).append("</td>")
                    .append("</tr>");
        }
        itemsTable.append("</tbody></table>");

        String html = buildEmailTemplate(title, subtitle, detailsCard, itemsTable.toString());
        sendHtmlEmail(getActiveApproversEmails(), "SME StockSense: Yêu cầu duyệt phiếu nhập kho " + receipt.getCode(), html);
    }

    @Override
    @Transactional(readOnly = true)
    public void sendImportReceiptApproved(ImportReceipt receiptStub, boolean isLevel1, boolean isFullyApproved) {
        ImportReceipt receipt = importReceiptRepository.findWithAllAssociationsById(receiptStub.getId()).orElse(null);
        if (receipt == null) return;

        List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(receipt.getId());

        String title = isFullyApproved ? "PHIẾU NHẬP KHO ĐÃ ĐƯỢC PHÊ DUYỆT HOÀN TOÀN" : "PHIẾU NHẬP KHO ĐÃ PHÊ DUYỆT CẤP 1";
        String subtitle = isFullyApproved 
                ? "Phiếu nhập kho của bạn đã được duyệt hoàn toàn và đang chuyển sang trạng thái chờ NCC giao hàng."
                : "Phiếu nhập kho của bạn đã được duyệt cấp 1 và đang đợi duyệt cấp 2 (do vượt hạn mức 50.000.000 đ).";

        Employee approver = isLevel1 ? receipt.getLevel1ApprovedBy() : receipt.getLevel2ApprovedBy();
        LocalDateTime approveTime = isLevel1 ? receipt.getLevel1ApprovedAt() : receipt.getLevel2ApprovedAt();

        String detailsCard = "<div class=\"card\">"
                + "<table>"
                + "    <tr><td class=\"label\">Mã Phiếu Nhập:</td><td class=\"value\"><strong>" + receipt.getCode() + "</strong></td></tr>"
                + "    <tr><td class=\"label\">Trạng thái hiện tại:</td><td class=\"value\"><span class=\"badge badge-success\">" + receipt.getStatus() + "</span></td></tr>"
                + "    <tr><td class=\"label\">Người duyệt gần nhất:</td><td class=\"value\">" + (approver != null ? approver.getFullName() : "-") + "</td></tr>"
                + "    <tr><td class=\"label\">Thời gian duyệt:</td><td class=\"value\">" + formatDateTime(approveTime) + "</td></tr>"
                + "    <tr><td class=\"label\">Tổng giá trị:</td><td class=\"value\" style=\"color:#2f7d4f; font-weight:bold;\">" + formatCurrency(receipt.getTotalAmount()) + "</td></tr>"
                + "</table>"
                + "</div>";

        StringBuilder itemsTable = new StringBuilder();
        itemsTable.append("<div class=\"items-title\">Chi tiết phiếu nhập</div>")
                .append("<table class=\"items-table\">")
                .append("<thead><tr><th>STT</th><th>Sản phẩm</th><th style=\"text-align:right;\">Số lượng đặt</th><th style=\"text-align:right;\">Đơn giá</th><th style=\"text-align:right;\">Thành tiền</th></tr></thead>")
                .append("<tbody>");

        int stt = 1;
        for (ImportReceiptDetail d : details) {
            itemsTable.append("<tr>")
                    .append("<td>").append(stt++).append("</td>")
                    .append("<td><strong>").append(d.getProduct().getName()).append("</strong></td>")
                    .append("<td style=\"text-align:right;\">").append(d.getExpectedQuantity()).append("</td>")
                    .append("<td style=\"text-align:right;\">").append(formatCurrency(d.getExpectedUnitPrice())).append("</td>")
                    .append("<td style=\"text-align:right;\">").append(formatCurrency(d.getExpectedLineTotal())).append("</td>")
                    .append("</tr>");
        }
        itemsTable.append("</tbody></table>");

        String html = buildEmailTemplate(title, subtitle, detailsCard, itemsTable.toString());

        // Notify Creator
        if (receipt.getCreatedBy() != null) {
            sendHtmlEmail(receipt.getCreatedBy().getEmail(), "SME StockSense: Phiếu nhập kho " + receipt.getCode() + " đã được duyệt", html);
        }

        // If it still needs level 2 approval, trigger level 2 notification for approvers
        if (!isFullyApproved && receipt.getStatus() == ImportReceiptStatus.CHO_DUYET_CAP_2) {
            sendImportReceiptSubmitted(receipt);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void sendImportReceiptRejected(ImportReceipt receiptStub, String reason) {
        ImportReceipt receipt = importReceiptRepository.findWithAllAssociationsById(receiptStub.getId()).orElse(null);
        if (receipt == null) return;

        String title = "PHIẾU NHẬP KHO BỊ TỪ CHỐI";
        String subtitle = "Phiếu nhập kho của bạn đã bị từ chối phê duyệt. Vui lòng chỉnh sửa lại phiếu nháp theo ý kiến của Quản lý.";

        String detailsCard = "<div class=\"card\" style=\"border-left-color: #b23a34;\">"
                + "<table>"
                + "    <tr><td class=\"label\">Mã Phiếu Nhập:</td><td class=\"value\"><strong>" + receipt.getCode() + "</strong></td></tr>"
                + "    <tr><td class=\"label\">Trạng thái:</td><td class=\"value\"><span class=\"badge badge-danger\">" + receipt.getStatus() + "</span></td></tr>"
                + "    <tr><td class=\"label\" style=\"color:#b23a34;\">Lý do từ chối:</td><td class=\"value\" style=\"color:#b23a34; font-weight:bold;\">" + reason + "</td></tr>"
                + "    <tr><td class=\"label\">Tổng giá trị:</td><td class=\"value\">" + formatCurrency(receipt.getTotalAmount()) + "</td></tr>"
                + "</table>"
                + "</div>";

        String html = buildEmailTemplate(title, subtitle, detailsCard, "");
        if (receipt.getCreatedBy() != null) {
            sendHtmlEmail(receipt.getCreatedBy().getEmail(), "SME StockSense: Phiếu nhập kho " + receipt.getCode() + " BỊ TỪ CHỐI", html);
        }
    }

    // =========================================================================
    // DISCREPANCY REPORT NOTIFICATIONS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public void sendDiscrepancyReportSubmitted(DiscrepancyReport reportStub) {
        DiscrepancyReport report = discrepancyReportRepository.findWithAllAssociationsById(reportStub.getId()).orElse(null);
        if (report == null) return;

        List<DiscrepancyReportDetail> details = discrepancyReportDetailRepository.findByReportId(report.getId());
        ImportReceipt receipt = report.getImportReceipt();

        String title = "YÊU CẦU DUYỆT BIÊN BẢN CHÊNH LỆCH";
        String subtitle = "Kính gửi Ban Quản lý, có một Biên bản chênh lệch nhập kho mới cần được phê duyệt trước khi hoàn tất nhập kho.";

        String detailsCard = "<div class=\"card\">"
                + "<table>"
                + "    <tr><td class=\"label\">Mã Biên Bản:</td><td class=\"value\"><strong>" + report.getCode() + "</strong></td></tr>"
                + "    <tr><td class=\"label\">Mã Phiếu Nhập gốc:</td><td class=\"value\">" + receipt.getCode() + "</td></tr>"
                + "    <tr><td class=\"label\">Trạng thái:</td><td class=\"value\"><span class=\"badge badge-pending\">" + report.getStatus() + "</span></td></tr>"
                + "    <tr><td class=\"label\">Người lập BB:</td><td class=\"value\">" + report.getCreatedBy().getFullName() + "</td></tr>"
                + "    <tr><td class=\"label\">Thời gian lập:</td><td class=\"value\">" + formatDateTime(report.getReportDate()) + "</td></tr>"
                + "    <tr><td class=\"label\">Ghi chú:</td><td class=\"value\">" + (report.getNote() != null ? report.getNote() : "Không có") + "</td></tr>"
                + "</table>"
                + "</div>";

        StringBuilder itemsTable = new StringBuilder();
        itemsTable.append("<div class=\"items-title\">Sản phẩm chênh lệch thực tế khi kiểm hàng</div>")
                .append("<table class=\"items-table\">")
                .append("<thead><tr><th>STT</th><th>Sản phẩm</th><th style=\"text-align:right;\">SL dự kiến</th><th style=\"text-align:right;\">SL thực nhận</th><th style=\"text-align:right;\">Lệch</th><th>Lý do</th><th>Hướng xử lý</th></tr></thead>")
                .append("<tbody>");

        int stt = 1;
        for (DiscrepancyReportDetail d : details) {
            String color = d.getDiscrepancyQuantity() < 0 ? "#b23a34" : "#2f7d4f";
            String sign = d.getDiscrepancyQuantity() > 0 ? "+" : "";
            itemsTable.append("<tr>")
                    .append("<td>").append(stt++).append("</td>")
                    .append("<td><strong>").append(d.getProduct().getName()).append("</strong></td>")
                    .append("<td style=\"text-align:right;\">").append(d.getDocumentQuantity()).append("</td>")
                    .append("<td style=\"text-align:right;\">").append(d.getActualQuantity()).append("</td>")
                    .append("<td style=\"text-align:right; font-weight:bold; color:").append(color).append(";\">").append(sign).append(d.getDiscrepancyQuantity()).append("</td>")
                    .append("<td>").append(d.getReason()).append("</td>")
                    .append("<td>").append(d.getAction()).append("</td>")
                    .append("</tr>");
        }
        itemsTable.append("</tbody></table>");

        String html = buildEmailTemplate(title, subtitle, detailsCard, itemsTable.toString());
        sendHtmlEmail(getActiveApproversEmails(), "SME StockSense: Yêu cầu duyệt biên bản chênh lệch " + report.getCode(), html);
    }

    @Override
    @Transactional(readOnly = true)
    public void sendDiscrepancyReportApproved(DiscrepancyReport reportStub) {
        DiscrepancyReport report = discrepancyReportRepository.findWithAllAssociationsById(reportStub.getId()).orElse(null);
        if (report == null) return;

        String title = "BIÊN BẢN CHÊNH LỆCH ĐÃ ĐƯỢC DUYỆT";
        String subtitle = "Biên bản chênh lệch nhập kho đã được phê duyệt. Bây giờ bạn có thể thực hiện hoàn tất phiếu nhập kho.";

        String detailsCard = "<div class=\"card\">"
                + "<table>"
                + "    <tr><td class=\"label\">Mã Biên Bản:</td><td class=\"value\"><strong>" + report.getCode() + "</strong></td></tr>"
                + "    <tr><td class=\"label\">Trạng thái:</td><td class=\"value\"><span class=\"badge badge-success\">" + report.getStatus() + "</span></td></tr>"
                + "    <tr><td class=\"label\">Người duyệt:</td><td class=\"value\">" + (report.getApprovedBy() != null ? report.getApprovedBy().getFullName() : "-") + "</td></tr>"
                + "    <tr><td class=\"label\">Thời gian duyệt:</td><td class=\"value\">" + formatDateTime(report.getApprovedAt()) + "</td></tr>"
                + "</table>"
                + "</div>";

        String html = buildEmailTemplate(title, subtitle, detailsCard, "");
        if (report.getCreatedBy() != null) {
            sendHtmlEmail(report.getCreatedBy().getEmail(), "SME StockSense: Biên bản chênh lệch " + report.getCode() + " đã được duyệt", html);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void sendDiscrepancyReportRejected(DiscrepancyReport reportStub, String reason) {
        DiscrepancyReport report = discrepancyReportRepository.findWithAllAssociationsById(reportStub.getId()).orElse(null);
        if (report == null) return;

        String title = "BIÊN BẢN CHÊNH LỆCH BỊ TỪ CHỐI";
        String subtitle = "Biên bản chênh lệch nhập kho của bạn đã bị từ chối phê duyệt. Vui lòng lập lại biên bản mới.";

        String detailsCard = "<div class=\"card\" style=\"border-left-color: #b23a34;\">"
                + "<table>"
                + "    <tr><td class=\"label\">Mã Biên Bản:</td><td class=\"value\"><strong>" + report.getCode() + "</strong></td></tr>"
                + "    <tr><td class=\"label\">Trạng thái:</td><td class=\"value\"><span class=\"badge badge-danger\">" + report.getStatus() + "</span></td></tr>"
                + "    <tr><td class=\"label\" style=\"color:#b23a34;\">Lý do từ chối:</td><td class=\"value\" style=\"color:#b23a34; font-weight:bold;\">" + reason + "</td></tr>"
                + "    <tr><td class=\"label\">Người từ chối:</td><td class=\"value\">" + (report.getRejectedBy() != null ? report.getRejectedBy().getFullName() : "-") + "</td></tr>"
                + "</table>"
                + "</div>";

        String html = buildEmailTemplate(title, subtitle, detailsCard, "");
        if (report.getCreatedBy() != null) {
            sendHtmlEmail(report.getCreatedBy().getEmail(), "SME StockSense: Biên bản chênh lệch " + report.getCode() + " BỊ TỪ CHỐI", html);
        }
    }

    // =========================================================================
    // EXPORT RECEIPT NOTIFICATIONS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public void sendExportReceiptSubmitted(ExportReceipt receiptStub) {
        ExportReceipt receipt = exportReceiptRepository.findWithAllAssociationsById(receiptStub.getId()).orElse(null);
        if (receipt == null) return;

        List<ExportReceiptDetail> details = exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(receipt.getId());

        String title = "YÊU CẦU PHÊ DUYỆT PHIẾU XUẤT KHO";
        String subtitle = "Kính gửi Ban Quản lý, có một phiếu xuất kho mới đang chờ duyệt để chuẩn bị xuất hàng.";

        String detailsCard = "<div class=\"card\">"
                + "<table>"
                + "    <tr><td class=\"label\">Mã Phiếu Xuất:</td><td class=\"value\"><strong>" + receipt.getCode() + "</strong></td></tr>"
                + "    <tr><td class=\"label\">Trạng thái:</td><td class=\"value\"><span class=\"badge badge-pending\">" + receipt.getStatus() + "</span></td></tr>"
                + "    <tr><td class=\"label\">Kho xuất hàng:</td><td class=\"value\">" + receipt.getWarehouse().getName() + "</td></tr>"
                + "    <tr><td class=\"label\">Khách hàng/Đối tác:</td><td class=\"value\">" + (receipt.getPartner() != null ? receipt.getPartner().getName() : "-") + "</td></tr>"
                + "    <tr><td class=\"label\">Người lập phiếu:</td><td class=\"value\">" + receipt.getCreatedBy().getFullName() + "</td></tr>"
                + "    <tr><td class=\"label\">Thời gian gửi duyệt:</td><td class=\"value\">" + formatDateTime(receipt.getSubmittedAt()) + "</td></tr>"
                + "    <tr><td class=\"label\">Tổng trị giá xuất:</td><td class=\"value\" style=\"color:#b23a34; font-weight:bold;\">" + formatCurrency(receipt.getTotalAmount()) + "</td></tr>"
                + "</table>"
                + "</div>";

        StringBuilder itemsTable = new StringBuilder();
        itemsTable.append("<div class=\"items-title\">Danh sách sản phẩm yêu cầu xuất</div>")
                .append("<table class=\"items-table\">")
                .append("<thead><tr><th>STT</th><th>Sản phẩm</th><th style=\"text-align:right;\">Số lượng xuất</th><th style=\"text-align:right;\">Đơn giá xuất</th><th style=\"text-align:right;\">Thành tiền</th></tr></thead>")
                .append("<tbody>");

        int stt = 1;
        for (ExportReceiptDetail d : details) {
            itemsTable.append("<tr>")
                    .append("<td>").append(stt++).append("</td>")
                    .append("<td><strong>").append(d.getProduct().getName()).append("</strong></td>")
                    .append("<td style=\"text-align:right;\">").append(d.getQuantity()).append("</td>")
                    .append("<td style=\"text-align:right;\">").append(formatCurrency(d.getUnitPrice())).append("</td>")
                    .append("<td style=\"text-align:right;\">").append(formatCurrency(d.getLineTotal())).append("</td>")
                    .append("</tr>");
        }
        itemsTable.append("</tbody></table>");

        String html = buildEmailTemplate(title, subtitle, detailsCard, itemsTable.toString());
        sendHtmlEmail(getActiveApproversEmails(), "SME StockSense: Yêu cầu duyệt phiếu xuất kho " + receipt.getCode(), html);
    }

    @Override
    @Transactional(readOnly = true)
    public void sendExportReceiptApproved(ExportReceipt receiptStub) {
        ExportReceipt receipt = exportReceiptRepository.findWithAllAssociationsById(receiptStub.getId()).orElse(null);
        if (receipt == null) return;

        String title = "PHIẾU XUẤT KHO ĐÃ ĐƯỢC DUYỆT";
        String subtitle = "Phiếu xuất kho của bạn đã được duyệt thành công. Bạn có thể tiến hành chuẩn bị hàng hóa và hoàn tất xuất kho thực tế.";

        String detailsCard = "<div class=\"card\">"
                + "<table>"
                + "    <tr><td class=\"label\">Mã Phiếu Xuất:</td><td class=\"value\"><strong>" + receipt.getCode() + "</strong></td></tr>"
                + "    <tr><td class=\"label\">Trạng thái:</td><td class=\"value\"><span class=\"badge badge-success\">" + receipt.getStatus() + "</span></td></tr>"
                + "    <tr><td class=\"label\">Người duyệt:</td><td class=\"value\">" + (receipt.getApprovedBy() != null ? receipt.getApprovedBy().getFullName() : "-") + "</td></tr>"
                + "    <tr><td class=\"label\">Thời gian duyệt:</td><td class=\"value\">" + formatDateTime(receipt.getApprovedAt()) + "</td></tr>"
                + "</table>"
                + "</div>";

        String html = buildEmailTemplate(title, subtitle, detailsCard, "");
        if (receipt.getCreatedBy() != null) {
            sendHtmlEmail(receipt.getCreatedBy().getEmail(), "SME StockSense: Phiếu xuất kho " + receipt.getCode() + " đã được duyệt", html);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void sendExportReceiptRejected(ExportReceipt receiptStub, String reason) {
        ExportReceipt receipt = exportReceiptRepository.findWithAllAssociationsById(receiptStub.getId()).orElse(null);
        if (receipt == null) return;

        String title = "PHIẾU XUẤT KHO BỊ TỪ CHỐI";
        String subtitle = "Phiếu xuất kho của bạn đã bị từ chối phê duyệt. Vui lòng cập nhật lại phiếu nháp theo lý do từ chối.";

        String detailsCard = "<div class=\"card\" style=\"border-left-color: #b23a34;\">"
                + "<table>"
                + "    <tr><td class=\"label\">Mã Phiếu Xuất:</td><td class=\"value\"><strong>" + receipt.getCode() + "</strong></td></tr>"
                + "    <tr><td class=\"label\">Trạng thái:</td><td class=\"value\"><span class=\"badge badge-danger\">" + receipt.getStatus() + "</span></td></tr>"
                + "    <tr><td class=\"label\" style=\"color:#b23a34;\">Lý do từ chối:</td><td class=\"value\" style=\"color:#b23a34; font-weight:bold;\">" + reason + "</td></tr>"
                + "    <tr><td class=\"label\">Người từ chối:</td><td class=\"value\">" + (receipt.getRejectedBy() != null ? receipt.getRejectedBy().getFullName() : "-") + "</td></tr>"
                + "</table>"
                + "</div>";

        String html = buildEmailTemplate(title, subtitle, detailsCard, "");
        if (receipt.getCreatedBy() != null) {
            sendHtmlEmail(receipt.getCreatedBy().getEmail(), "SME StockSense: Phiếu xuất kho " + receipt.getCode() + " BỊ TỪ CHỐI", html);
        }
    }

    // =========================================================================
    // COMPLETED NOTIFICATIONS
    // =========================================================================

    @Override
    @Transactional(readOnly = true)
    public void sendImportReceiptCompleted(ImportReceipt receiptStub) {
        ImportReceipt receipt = importReceiptRepository.findWithAllAssociationsById(receiptStub.getId()).orElse(null);
        if (receipt == null) return;

        List<ImportReceiptDetail> details = importReceiptDetailRepository.findByDocumentIdOrderByIdAsc(receipt.getId());

        String title = "PHÀN TẤT NHẬP KHO THÀNH CÔNG";
        String subtitle = "Phiếu nhập kho đã được ghi nhận hoàn thành. Số lượng thực tế đã được cộng tăng vào tồn kho của hệ thống.";

        String detailsCard = "<div class=\"card\" style=\"border-left-color: #2f7d4f;\">"
                + "<table>"
                + "    <tr><td class=\"label\">Mã Phiếu Nhập:</td><td class=\"value\"><strong>" + receipt.getCode() + "</strong></td></tr>"
                + "    <tr><td class=\"label\">Trạng thái:</td><td class=\"value\"><span class=\"badge badge-info\">" + receipt.getStatus() + "</span></td></tr>"
                + "    <tr><td class=\"label\">Thực tế hàng về:</td><td class=\"value\">" + formatDateTime(receipt.getActualArrivalDate()) + "</td></tr>"
                + "    <tr><td class=\"label\">Hoàn tất bởi:</td><td class=\"value\">" + (receipt.getCompletedBy() != null ? receipt.getCompletedBy().getFullName() : "-") + "</td></tr>"
                + "    <tr><td class=\"label\">Thời gian hoàn tất:</td><td class=\"value\">" + formatDateTime(receipt.getCompletedAt()) + "</td></tr>"
                + "    <tr><td class=\"label\">Tổng tiền thực tế:</td><td class=\"value\" style=\"color:#2f7d4f; font-weight:bold; font-size:16px;\">" + formatCurrency(receipt.getTotalAmount()) + "</td></tr>"
                + "</table>"
                + "</div>";

        StringBuilder itemsTable = new StringBuilder();
        itemsTable.append("<div class=\"items-title\">Chi tiết mặt hàng thực nhập</div>")
                .append("<table class=\"items-table\">")
                .append("<thead><tr><th>STT</th><th>Sản phẩm</th><th style=\"text-align:right;\">SL Đặt</th><th style=\"text-align:right;\">SL Thực Nhận</th><th>Tình trạng vật lý</th></tr></thead>")
                .append("<tbody>");

        int stt = 1;
        for (ImportReceiptDetail d : details) {
            String color = !d.getExpectedQuantity().equals(d.getActualReceivedQuantity()) ? "#b23a34" : "#2f7d4f";
            itemsTable.append("<tr>")
                    .append("<td>").append(stt++).append("</td>")
                    .append("<td><strong>").append(d.getProduct().getName()).append("</strong></td>")
                    .append("<td style=\"text-align:right;\">").append(d.getExpectedQuantity()).append("</td>")
                    .append("<td style=\"text-align:right; font-weight:bold; color:").append(color).append(";\">").append(d.getActualReceivedQuantity() != null ? d.getActualReceivedQuantity() : 0).append("</td>")
                    .append("<td>").append(d.getPhysicalStatus() != null ? d.getPhysicalStatus() : "Tốt").append("</td>")
                    .append("</tr>");
        }
        itemsTable.append("</tbody></table>");

        String html = buildEmailTemplate(title, subtitle, detailsCard, itemsTable.toString());
        if (receipt.getCreatedBy() != null) {
            sendHtmlEmail(receipt.getCreatedBy().getEmail(), "SME StockSense: Phiếu nhập kho " + receipt.getCode() + " hoàn tất thành công", html);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public void sendExportReceiptCompleted(ExportReceipt receiptStub) {
        ExportReceipt receipt = exportReceiptRepository.findWithAllAssociationsById(receiptStub.getId()).orElse(null);
        if (receipt == null) return;

        List<ExportReceiptDetail> details = exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(receipt.getId());

        String title = "HOÀN TẤT XUẤT KHO THÀNH CÔNG";
        String subtitle = "Phiếu xuất kho đã được xác nhận xuất hàng thực tế. Tồn kho sản phẩm đã bị trừ tương ứng trong hệ thống.";

        String detailsCard = "<div class=\"card\" style=\"border-left-color: #2f7d4f;\">"
                + "<table>"
                + "    <tr><td class=\"label\">Mã Phiếu Xuất:</td><td class=\"value\"><strong>" + receipt.getCode() + "</strong></td></tr>"
                + "    <tr><td class=\"label\">Trạng thái:</td><td class=\"value\"><span class=\"badge badge-info\">" + receipt.getStatus() + "</span></td></tr>"
                + "    <tr><td class=\"label\">Người hoàn tất:</td><td class=\"value\">" + (receipt.getCompletedBy() != null ? receipt.getCompletedBy().getFullName() : "-") + "</td></tr>"
                + "    <tr><td class=\"label\">Thời gian hoàn tất:</td><td class=\"value\">" + formatDateTime(receipt.getCompletedAt()) + "</td></tr>"
                + "    <tr><td class=\"label\">Tổng giá trị:</td><td class=\"value\" style=\"color:#2f7d4f; font-weight:bold;\">" + formatCurrency(receipt.getTotalAmount()) + "</td></tr>"
                + "</table>"
                + "</div>";

        StringBuilder itemsTable = new StringBuilder();
        itemsTable.append("<div class=\"items-title\">Chi tiết mặt hàng đã xuất</div>")
                .append("<table class=\"items-table\">")
                .append("<thead><tr><th>STT</th><th>Sản phẩm</th><th style=\"text-align:right;\">Số lượng xuất</th><th style=\"text-align:right;\">Đơn giá</th><th style=\"text-align:right;\">Thành tiền</th></tr></thead>")
                .append("<tbody>");

        int stt = 1;
        for (ExportReceiptDetail d : details) {
            itemsTable.append("<tr>")
                    .append("<td>").append(stt++).append("</td>")
                    .append("<td><strong>").append(d.getProduct().getName()).append("</strong></td>")
                    .append("<td style=\"text-align:right;\">").append(d.getQuantity()).append("</td>")
                    .append("<td style=\"text-align:right;\">").append(formatCurrency(d.getUnitPrice())).append("</td>")
                    .append("<td style=\"text-align:right;\">").append(formatCurrency(d.getLineTotal())).append("</td>")
                    .append("</tr>");
        }
        itemsTable.append("</tbody></table>");

        String html = buildEmailTemplate(title, subtitle, detailsCard, itemsTable.toString());
        if (receipt.getCreatedBy() != null) {
            sendHtmlEmail(receipt.getCreatedBy().getEmail(), "SME StockSense: Phiếu xuất kho " + receipt.getCode() + " hoàn tất thành công", html);
        }
    }
}
