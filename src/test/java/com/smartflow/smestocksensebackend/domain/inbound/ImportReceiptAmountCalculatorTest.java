package com.smartflow.smestocksensebackend.domain.inbound;

import com.smartflow.smestocksensebackend.repository.ImportReceiptDetailRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportReceiptAmountCalculatorTest {

    @Mock
    private ImportReceiptDetailRepository importReceiptDetailRepository;

    @Test
    void calculateLineTotal_shouldMultiplyQuantityByUnitPrice() {
        ImportReceiptAmountCalculator calculator = new ImportReceiptAmountCalculator(importReceiptDetailRepository);

        BigDecimal lineTotal = calculator.calculateLineTotal(3, new BigDecimal("19.50"));

        assertEquals(new BigDecimal("58.50"), lineTotal);
    }

    @Test
    void calculateLineTotal_withZeroUnitPrice_shouldReturnZero() {
        ImportReceiptAmountCalculator calculator = new ImportReceiptAmountCalculator(importReceiptDetailRepository);

        BigDecimal lineTotal = calculator.calculateLineTotal(10, BigDecimal.ZERO);

        assertEquals(BigDecimal.ZERO, lineTotal);
    }

    @Test
    void calculateLineTotal_withNullQuantity_shouldThrowIllegalArgumentException() {
        ImportReceiptAmountCalculator calculator = new ImportReceiptAmountCalculator(importReceiptDetailRepository);

        assertThrows(IllegalArgumentException.class,
                () -> calculator.calculateLineTotal(null, new BigDecimal("19.50")));
    }

    @Test
    void calculateLineTotal_withNullUnitPrice_shouldThrowIllegalArgumentException() {
        ImportReceiptAmountCalculator calculator = new ImportReceiptAmountCalculator(importReceiptDetailRepository);

        assertThrows(IllegalArgumentException.class, () -> calculator.calculateLineTotal(3, null));
    }

    @Test
    void calculateReceiptTotal_shouldUseRepositoryAggregation() {
        ImportReceiptAmountCalculator calculator = new ImportReceiptAmountCalculator(importReceiptDetailRepository);
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(new BigDecimal("150.25"));

        BigDecimal receiptTotal = calculator.calculateReceiptTotal(123L);

        assertEquals(new BigDecimal("150.25"), receiptTotal);
        verify(importReceiptDetailRepository).sumLineTotalByReceiptId(123L);
    }

    @Test
    void calculateReceiptTotal_withoutLines_shouldReturnZero() {
        ImportReceiptAmountCalculator calculator = new ImportReceiptAmountCalculator(importReceiptDetailRepository);
        when(importReceiptDetailRepository.sumLineTotalByReceiptId(123L)).thenReturn(null);

        BigDecimal receiptTotal = calculator.calculateReceiptTotal(123L);

        assertEquals(BigDecimal.ZERO, receiptTotal);
    }
}
