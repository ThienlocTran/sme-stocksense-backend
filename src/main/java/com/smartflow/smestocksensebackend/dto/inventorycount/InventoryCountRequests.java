package com.smartflow.smestocksensebackend.dto.inventorycount;

import jakarta.validation.constraints.*;
import java.util.List;

public final class InventoryCountRequests {
    private InventoryCountRequests() {}
    public record Create(@NotNull @Positive Long warehouseId, List<@Positive Long> productIds, @Size(max=500) String note) {}
    public record RecordActual(@NotNull @PositiveOrZero Integer actualQuantity, @Size(max=500) String note, @NotNull @PositiveOrZero Long version) {}
    public record Finalize(@NotNull @PositiveOrZero Long version) {}
    public record Cancel(@NotBlank @Size(max=500) String reason, @NotNull @PositiveOrZero Long version) {}
}
