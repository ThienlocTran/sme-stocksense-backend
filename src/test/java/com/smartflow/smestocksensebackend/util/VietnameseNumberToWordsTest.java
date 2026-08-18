package com.smartflow.smestocksensebackend.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class VietnameseNumberToWordsTest {

    @Test
    void currency_shouldRenderBasicAmount() {
        assertEquals("Năm nghìn đồng", VietnameseNumberToWords.currency(new BigDecimal("5000")));
    }

    @Test
    void currency_shouldRenderMillions() {
        assertEquals("Một triệu không trăm lẻ năm nghìn đồng", VietnameseNumberToWords.currency(new BigDecimal("1005000")));
    }
}
