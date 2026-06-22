package com.smartflow.smestocksensebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Thực thể InventoryLevel ánh xạ đến bảng "ton_kho" - tồn kho hiện tại
 * theo từng cặp (sản phẩm, kho).
 * Quan hệ:
 * - @ManyToOne với Product   (san_pham_id, bắt buộc)
 * - @ManyToOne với Warehouse (kho_id, bắt buộc)
 * Ràng buộc unique (san_pham_id, kho_id): mỗi sản phẩm chỉ có 1 dòng tồn trên 1 kho.
 * Cột version dùng cho Optimistic Lock, chặn lost update khi nhiều luồng cùng
 * cộng/trừ tồn (T73).
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
        name = "ton_kho",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_ton_kho_san_pham_kho",
                columnNames = {"san_pham_id", "kho_id"}
        )
)
public class InventoryLevel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kho_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "so_luong", nullable = false)
    @Builder.Default
    private Integer quantity = 0;

    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
