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
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "yeu_cau_nhap_hang_ai")
public class AiPurchaseRequest {

    private static final Set<Short> VALID_HORIZONS = Set.of((short) 7, (short) 14, (short) 30);

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_yeu_cau", nullable = false, unique = true, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thong_tin_mo_hinh_id", nullable = false)
    private ForecastModelMetadata modelMetadata;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kho_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "horizon_days", nullable = false)
    private Short horizonDays;

    @Column(name = "so_luong_ai_goi_y", nullable = false)
    private Integer aiSuggestedQuantity;

    @Column(name = "so_luong_yeu_cau", nullable = false)
    private Integer requestedQuantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_gui_id", nullable = false)
    private Employee sender;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_nhan_id", nullable = false)
    private Employee receiver;

    @Column(name = "noi_dung", length = 1000)
    private String content;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 30)
    private AiPurchaseRequestStatus status = AiPurchaseRequestStatus.DA_GUI;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai_email", nullable = false, length = 30)
    private AiPurchaseRequestEmailStatus emailStatus = AiPurchaseRequestEmailStatus.CHO_GUI;

    @Column(name = "ngay_gui_email")
    private LocalDateTime emailSentAt;

    @Column(name = "loi_gui_email", length = 500)
    private String emailError;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phieu_nhap_id")
    private ImportReceipt importReceipt;

    @CreationTimestamp
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ngay_tiep_nhan")
    private LocalDateTime acceptedAt;

    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    private void validate() {
        if (!VALID_HORIZONS.contains(horizonDays)) {
            throw new IllegalStateException("horizonDays must be 7, 14, or 30.");
        }
        if (aiSuggestedQuantity == null || aiSuggestedQuantity < 0) {
            throw new IllegalStateException("aiSuggestedQuantity must be non-negative.");
        }
        if (requestedQuantity == null || requestedQuantity < 0) {
            throw new IllegalStateException("requestedQuantity must be non-negative.");
        }
        validateRoles();
    }

    private void validateRoles() {
        RoleCode senderRole = sender != null && sender.getRole() != null ? sender.getRole().getCode() : null;
        if (senderRole != null && senderRole != RoleCode.ADMIN && senderRole != RoleCode.MANAGER) {
            throw new IllegalStateException("sender must be ADMIN or MANAGER.");
        }
        RoleCode receiverRole = receiver != null && receiver.getRole() != null ? receiver.getRole().getCode() : null;
        if (receiverRole != null && receiverRole != RoleCode.EMPLOYEE) {
            throw new IllegalStateException("receiver must be EMPLOYEE.");
        }
    }
}
