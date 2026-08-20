package com.smartflow.smestocksensebackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "cau_hinh_ton_kho", uniqueConstraints = {@UniqueConstraint(columnNames = {"san_pham_id", "kho_id"})})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WarehouseStockConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kho_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "ton_toi_thieu_ghi_de")
    private Integer minStockOverride;

    @CreationTimestamp
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat", nullable = false)
    private LocalDateTime updatedAt;
}
