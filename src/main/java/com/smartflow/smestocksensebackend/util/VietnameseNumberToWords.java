package com.smartflow.smestocksensebackend.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class VietnameseNumberToWords {

    private static final String[] DIGITS = {
            "không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"
    };
    private static final String[] SCALES = { "", "nghìn", "triệu", "tỷ" };

    private VietnameseNumberToWords() {
    }

    public static String currency(BigDecimal amount) {
        if (amount == null) {
            return "";
        }

        long value = amount.setScale(0, RoundingMode.HALF_UP).longValue();
        if (value == 0L) {
            return "Không đồng";
        }

        String words = trim(readGroups(value));
        return capitalize(words) + " đồng";
    }

    private static String readGroups(long value) {
        StringBuilder result = new StringBuilder();
        long remaining = value;
        int scaleIndex = 0;
        while (remaining > 0L) {
            int group = (int) (remaining % 1_000L);
            if (group > 0) {
                boolean forceZeroPrefix = remaining >= 1_000L && group < 100;
                result.insert(0, readTriple(group, forceZeroPrefix) + SCALES[scaleIndex % SCALES.length] + " ");
            }
            remaining /= 1_000L;
            scaleIndex++;
        }
        return result.toString();
    }

    private static String readTriple(int number, boolean forceZeroPrefix) {
        if (number == 0) {
            return forceZeroPrefix ? "không trăm " : "";
        }

        int hundreds = number / 100;
        int tens = (number % 100) / 10;
        int ones = number % 10;

        StringBuilder result = new StringBuilder();
        if (hundreds > 0 || forceZeroPrefix) {
            result.append(DIGITS[hundreds]).append(" trăm ");
        }
        if (tens > 1) {
            result.append(DIGITS[tens]).append(" mươi ");
            if (ones == 1) {
                result.append("mốt ");
            } else if (ones == 5) {
                result.append("lăm ");
            } else if (ones > 0) {
                result.append(DIGITS[ones]).append(' ');
            }
        } else if (tens == 1) {
            result.append("mười ");
            if (ones == 5) {
                result.append("lăm ");
            } else if (ones > 0) {
                result.append(DIGITS[ones]).append(' ');
            }
        } else if (ones > 0) {
            if (hundreds > 0 || forceZeroPrefix) {
                result.append("lẻ ");
            }
            result.append(DIGITS[ones]).append(' ');
        }
        return result.toString();
    }

    private static String trim(String value) {
        return value.replaceAll("\\s+", " ").trim();
    }

    private static String capitalize(String value) {
        if (value.isBlank()) {
            return value;
        }
        return Character.toUpperCase(value.charAt(0)) + value.substring(1);
    }
}
