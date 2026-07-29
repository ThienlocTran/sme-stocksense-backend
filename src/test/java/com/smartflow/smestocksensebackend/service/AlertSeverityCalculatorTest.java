package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.entity.InventoryAlert;
import com.smartflow.smestocksensebackend.entity.InventoryAlertSeverity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Kiểm thử cô lập 100% cho AlertSeverityCalculator.
 * Đảm bảo các quy tắc phân loại Warning/Critical và chính sách leo thang tự
 * động
 * hoạt động chính xác trong mọi kịch bản (Low Stock, Out of Stock, Recovery
 * Edge Case).
 */
class AlertSeverityCalculatorTest {

    private AlertSeverityCalculator calculator;

    @BeforeEach
    void setUp() {
        calculator = new AlertSeverityCalculator();
    }

    @Test
    @DisplayName("calculate: Khi số lượng > 0 và trạng thái LOW_STOCK -> Trả về WARNING")
    void testCalculate_WhenQtyPositiveAndLowStock_ShouldReturnWarning() {
        InventoryAlertSeverity severity = calculator.calculate(15, "LOW_STOCK");
        assertEquals(InventoryAlertSeverity.WARNING, severity, "Số lượng dương phải được cảnh báo ở mức WARNING");
    }

    @Test
    @DisplayName("calculate: Khi số lượng <= 0 hoặc trạng thái OUT_OF_STOCK -> Trả về CRITICAL")
    void testCalculate_WhenQtyZeroOrNegative_ShouldReturnCritical() {
        assertEquals(InventoryAlertSeverity.CRITICAL, calculator.calculate(0, "LOW_STOCK"),
                "Tồn bằng 0 phải là CRITICAL");
        assertEquals(InventoryAlertSeverity.CRITICAL, calculator.calculate(-5, "LOW_STOCK"), "Tồn âm phải là CRITICAL");
        assertEquals(InventoryAlertSeverity.CRITICAL, calculator.calculate(10, "OUT_OF_STOCK"),
                "Trạng thái OUT_OF_STOCK phải là CRITICAL");
    }

    @Test
    @DisplayName("evaluateAndApplyEscalation: Khi phiếu đang WARNING tụt xuống 0 -> Leo thang lên CRITICAL và bổ sung note")
    void testEvaluateAndApplyEscalation_FromWarningToCritical_ShouldEscalateAndAppendNote() {
        InventoryAlert alert = InventoryAlert.builder()
                .id(1L)
                .currentQuantity(10)
                .severity(InventoryAlertSeverity.WARNING)
                .note("Phiếu cảnh báo ban đầu")
                .build();

        boolean escalated = calculator.evaluateAndApplyEscalation(alert, 0, "LOW_STOCK");

        assertTrue(escalated, "Phải trả về true khi có leo thang từ WARNING lên CRITICAL");
        assertEquals(InventoryAlertSeverity.CRITICAL, alert.getSeverity(), "Severity của phiếu phải nâng lên CRITICAL");
        assertTrue(alert.getNote().contains("[Auto-Escalate]"), "Note phải chứa chú thích leo thang tự động");
    }

    @Test
    @DisplayName("evaluateAndApplyEscalation: Khi phiếu đã là CRITICAL -> Giữ nguyên, trả về false")
    void testEvaluateAndApplyEscalation_WhenAlreadyCritical_ShouldNotChange() {
        InventoryAlert alert = InventoryAlert.builder()
                .id(2L)
                .currentQuantity(0)
                .severity(InventoryAlertSeverity.CRITICAL)
                .note("Phiếu CRITICAL")
                .build();

        boolean escalated = calculator.evaluateAndApplyEscalation(alert, 0, "OUT_OF_STOCK");

        assertFalse(escalated, "Không có sự thay đổi severity thì trả về false");
        assertEquals(InventoryAlertSeverity.CRITICAL, alert.getSeverity());
        assertEquals("Phiếu CRITICAL", alert.getNote(), "Note không được bị thay đổi khi không leo thang");
    }

    @Test
    @DisplayName("evaluateAndApplyEscalation: Edge Case phục hồi nhẹ từ 0 lên 5 -> KHÔNG tự ý hạ cấp, trả về false")
    void testEvaluateAndApplyEscalation_RecoveryEdgeCase_ShouldNotDeescalate() {
        InventoryAlert alert = InventoryAlert.builder()
                .id(3L)
                .currentQuantity(0)
                .severity(InventoryAlertSeverity.CRITICAL)
                .note("Phiếu CRITICAL ban đầu")
                .build();

        // Giả lập nhập kho nhỏ lẻ lên 5 (vẫn dưới định mức tối thiểu)
        boolean escalated = calculator.evaluateAndApplyEscalation(alert, 5, "LOW_STOCK");

        assertFalse(escalated, "Không được tự ý hạ cấp severity (No De-escalation)");
        assertEquals(InventoryAlertSeverity.CRITICAL, alert.getSeverity(), "Severity phải được giữ nguyên ở CRITICAL");
    }
}
