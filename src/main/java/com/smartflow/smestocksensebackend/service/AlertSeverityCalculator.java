package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.entity.InventoryAlert;
import com.smartflow.smestocksensebackend.entity.InventoryAlertSeverity;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Component chịu trách nhiệm tính toán mức độ nghiêm trọng
 * (Severity) của phiếu cảnh báo tồn kho và xử lý chính sách leo thang tự động
 * (Escalation Policy).
 * Tách biệt hoàn toàn (Separation of Concerns) khỏi
 * InventoryAlertDetectionService theo đúng chuẩn
 * Balanced Architect và nguyên tắc Ponytail (đơn giản, hiệu quả, dễ kiểm thử cô
 * lập).
 */
@Component
public class AlertSeverityCalculator {

    private static final String STOCK_STATUS_OUT_OF_STOCK = "OUT_OF_STOCK";

    /**
     * Phân loại mức độ nghiêm trọng dựa vào số lượng tồn kho hiện tại và trạng thái
     * mặt hàng.
     * - Nếu tồn kho <= 0 hoặc trạng thái từ SSOT là OUT_OF_STOCK -> Khẩn cấp
     * (CRITICAL).
     * - Ngược lại (tồn > 0 và sắp hết hàng dưới định mức tối thiểu) -> Cảnh báo
     * (WARNING).
     */
    public InventoryAlertSeverity calculate(int currentQuantity, String status) {
        if (currentQuantity <= 0 || STOCK_STATUS_OUT_OF_STOCK.equalsIgnoreCase(status)) {
            return InventoryAlertSeverity.CRITICAL;
        }
        return InventoryAlertSeverity.WARNING;
    }

    /**
     * Giữ phương thức nhận 3 tham số để bảo đảm tương thích ngược 100% với các mã
     * nguồn hoặc
     * kịch bản kiểm thử cũ nếu có gọi truyền thêm minStock.
     */
    public InventoryAlertSeverity calculate(int currentQuantity, int minStock, String status) {
        return calculate(currentQuantity, status);
    }

    /**
     * Khi hệ thống quét lại (Batch Scan / Spot Check) phát hiện phiếu cũ đang hoạt
     * động (OPEN / ACKNOWLEDGED):
     * 1. Kiểm tra mức độ hiện tại của phiếu và tính mức độ mới theo số lượng thực
     * tế.
     * 2. Nếu phiếu cũ đang là WARNING nhưng tồn kho tiếp tục tụt xuống cạn kiệt
     * (CRITICAL):
     * -> Tự động leo thang (Auto-Escalate): Nâng severity lên CRITICAL và bổ sung
     * log audit vào note.
     * 3. Nếu phiếu cũ đã là CRITICAL nhưng sau đó có nhập kho nhỏ lẻ phục hồi nhẹ
     * (vẫn dưới minStock):
     * -> Tuyệt đối KHÔNG tự ý hạ cấp (No De-escalation) để đảm bảo thủ kho phải
     * giải quyết triệt để.
     *
     * @return true nếu đã xảy ra leo thang (severity bị thay đổi từ WARNING ->
     *         CRITICAL), false nếu giữ nguyên.
     */
    public boolean evaluateAndApplyEscalation(InventoryAlert existingAlert, int newQuantity, String status) {
        // Bước 1: Kiểm tra tính hợp lệ của phiếu cảnh báo đầu vào
        if (existingAlert == null || existingAlert.getSeverity() == null) {
            return false;
        }

        InventoryAlertSeverity oldSeverity = existingAlert.getSeverity();
        InventoryAlertSeverity newSeverity = calculate(newQuantity, status);

        // Bước 2: Kiểm tra điều kiện leo thang (WARNING -> CRITICAL)
        if (oldSeverity == InventoryAlertSeverity.WARNING && newSeverity == InventoryAlertSeverity.CRITICAL) {
            // Nâng cấp mức độ nghiêm trọng của phiếu
            existingAlert.setSeverity(InventoryAlertSeverity.CRITICAL);

            // Tạo chuỗi ghi chú audit vết tích leo thang kèm thời gian thực
            String timestampStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            String escalationNote = String.format(
                    " | [Auto-Escalate] Mức độ nâng từ WARNING lên CRITICAL do tồn kho cạn kiệt (SL: %d) - %s",
                    newQuantity, timestampStr);

            String currentNote = existingAlert.getNote() != null ? existingAlert.getNote() : "";
            existingAlert.setNote(currentNote + escalationNote);

            return true;
        }

        // Bước 3: Các trường hợp còn lại (đã là CRITICAL hoặc phục hồi nhẹ) -> Giữ
        // nguyên (No De-escalation)
        return false;
    }
}
