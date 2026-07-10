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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể ExportReceipt ánh xạ đến bảng "phieu_xuat_kho".
 * Phiếu xuất kho sử dụng luồng duyệt 2 cấp.
 * Quan hệ:
 * - @ManyToOne với Warehouse (kho xuất)
 * - @ManyToOne với Partner  (khách hàng nhận, nullable)
 * - @ManyToOne với Employee (người tạo, người duyệt)
 * - @OneToMany với ExportReceiptItem (danh sách dòng chi tiết)
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "phieu_xuat_kho")
public class ExportReceipt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_phieu_xuat", nullable = false, unique = true, length = 50)
    private String code;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kho_id", nullable = false)
    private Warehouse warehouse;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doi_tac_id")
    private Partner customer;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "trang_thai", nullable = false, columnDefinition = "trang_thai_chung_tu_kho")
    private ExportReceiptStatus status = ExportReceiptStatus.DRAFT;

    @Column(name = "tong_tien", precision = 15, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "ghi_chu", length = 255)
    private String note;

    @Column(name = "ly_do_tu_choi", length = 500)
    private String rejectionReason;

    // ─── Người thực hiện các bước workflow ────────────────────────

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_tao_id")
    private Employee createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_gui_duyet_id")
    private Employee submittedBy;

    @Column(name = "ngay_gui_duyet")
    private LocalDateTime submittedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_duyet_cap_1_id")
    private Employee level1ApprovedBy;

    @Column(name = "ngay_duyet_cap_1")
    private LocalDateTime level1ApprovedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_duyet_cap_2_id")
    private Employee level2ApprovedBy;

    @Column(name = "ngay_duyet_cap_2")
    private LocalDateTime level2ApprovedAt;

    @Column(name = "ngay_hoan_thanh")
    private LocalDateTime completedAt;

    // ─── Quan hệ dòng chi tiết ───────────────────────────────────

    @OneToMany(mappedBy = "exportReceipt")
    private List<ExportReceiptItem> items = new ArrayList<>();

    // ─── Audit & Optimistic lock ─────────────────────────────────

    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
