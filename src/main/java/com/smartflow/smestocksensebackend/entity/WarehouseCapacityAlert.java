package com.smartflow.smestocksensebackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "canh_bao_suc_chua_kho")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseCapacityAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kho_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "used_capacity_m3", nullable = false, precision = 12, scale = 3)
    private BigDecimal usedCapacityM3;

    @Column(name = "max_capacity_m3", nullable = false, precision = 12, scale = 3)
    private BigDecimal maxCapacityM3;

    @Column(name = "usage_percentage", nullable = false, precision = 5, scale = 2)
    private BigDecimal usagePercentage;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 50)
    private WarehouseCapacityAlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private InventoryAlertStatus status;

    @Column(name = "message", length = 500)
    private String message;

    @CreationTimestamp
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "ngay_giai_quyet")
    private LocalDateTime resolvedAt;

    @Column(name = "nguoi_giai_quyet", length = 100)
    private String resolvedBy;

    public void resolve(String actor) {
        this.status = InventoryAlertStatus.RESOLVED;
        this.resolvedAt = LocalDateTime.now();
        this.resolvedBy = actor;
    }
}
