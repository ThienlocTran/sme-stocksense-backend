package com.smartflow.smestocksensebackend.dto.inbound;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record SaveImportReceiptDraftItemRequest(
        @NotNull(message = "productId khong duoc de trong.")
        Long productId,

        @NotNull(message = "quantity khong duoc de trong.")
        @Positive(message = "quantity phai lon hon 0.")
        Integer quantity,

        @NotNull(message = "unitPrice khong duoc de trong.")
        @DecimalMin(value = "0.00", message = "unitPrice phai lon hon hoac bang 0.")
        BigDecimal unitPrice,

        @Size(max = 255, message = "note khong duoc vuot qua 255 ky tu.")
        String note
) {
    public AddImportReceiptItemRequest toAddItemRequest() {
        return new AddImportReceiptItemRequest(productId, quantity, unitPrice, note);
    }
}
