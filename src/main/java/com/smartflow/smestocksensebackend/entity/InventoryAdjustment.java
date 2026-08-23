package com.smartflow.smestocksensebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "phieu_dieu_chinh_kiem_ke")
public class InventoryAdjustment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_phieu", nullable = false, unique = true, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dot_kiem_ke_id", nullable = false)
    private InventoryCount inventoryCount;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private InventoryAdjustmentStatus status = InventoryAdjustmentStatus.NHAP;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_tao_id", nullable = false)
    private Employee createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_gui_duyet_id")
    private Employee submittedBy;

    @Column(name = "ngay_gui_duyet")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_duyet_id")
    private Employee approvedBy;

    @Column(name = "ngay_duyet")
    private LocalDateTime approvedAt;

    @Column(name = "ngay_ap_dung")
    private LocalDateTime appliedAt;

    @Column(name = "ghi_chu", length = 500)
    private String note;

    @Column(name = "ly_do_tu_choi", length = 500)
    private String rejectionReason;

    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;
}
