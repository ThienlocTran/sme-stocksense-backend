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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
        name = "chi_tiet_dieu_chinh_kiem_ke",
        uniqueConstraints = @UniqueConstraint(columnNames = {"phieu_dieu_chinh_id", "san_pham_id"})
)
public class InventoryAdjustmentLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phieu_dieu_chinh_id", nullable = false)
    private InventoryAdjustment adjustment;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private Product product;

    @Column(name = "so_luong_he_thong", nullable = false)
    private Integer systemQuantity;

    @Column(name = "so_luong_thuc_te", nullable = false)
    private Integer actualQuantity;

    @Column(name = "chenh_lech", nullable = false)
    private Integer differenceQuantity;

    @Column(name = "ly_do", length = 255)
    private String reason;

    @Column(name = "ghi_chu", length = 500)
    private String note;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
}
