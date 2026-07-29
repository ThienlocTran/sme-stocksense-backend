package com.smartflow.smestocksensebackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @Entity @Table(name = "dot_kiem_ke")
public class InventoryCount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name="ma_dot", nullable=false, unique=true, length=40) private String code;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="kho_id") private Warehouse warehouse;
    @Enumerated(EnumType.STRING) @Column(name="trang_thai", nullable=false, length=20) private InventoryCountStatus status;
    @Column(name="ghi_chu", length=500) private String note;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="nguoi_tao_id") private Employee createdBy;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="nguoi_chot_id") private Employee finalizedBy;
    @Column(name="ngay_chot") private LocalDateTime finalizedAt;
    @ManyToOne(fetch=FetchType.LAZY) @JoinColumn(name="nguoi_huy_id") private Employee cancelledBy;
    @Column(name="ngay_huy") private LocalDateTime cancelledAt;
    @Column(name="ly_do_huy", length=500) private String cancellationReason;
    @CreationTimestamp @Column(name="ngay_tao", updatable=false) private LocalDateTime createdAt;
    @UpdateTimestamp @Column(name="ngay_cap_nhat") private LocalDateTime updatedAt;
    @Version private Long version;
}
