package com.smartflow.smestocksensebackend.domain.inbound;

import com.smartflow.smestocksensebackend.repository.ImportReceiptDetailRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ImportReceiptAmountCalculator {

    private final ImportReceiptDetailRepository importReceiptDetailRepository;

    public BigDecimal calculateLineTotal(Integer quantity, BigDecimal unitPrice) {
        return unitPrice.multiply(BigDecimal.valueOf(quantity));
    }

    public BigDecimal calculateReceiptTotal(Long receiptId) {
        BigDecimal total = importReceiptDetailRepository.sumLineTotalByReceiptId(receiptId);
        return total != null ? total : BigDecimal.ZERO;
    }
}
