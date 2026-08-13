package com.smartflow.smestocksensebackend.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Thực thể (Entity) đại diện cho Biên bản chênh lệch nhập kho (Discrepancy Report).
 * Biên bản này được lập khi có sự khác biệt giữa số lượng hàng hóa thực nhận khi kiểm đếm
 * và số lượng hàng hóa trên chứng từ gốc của phiếu nhập kho (Import Receipt).
 * Liên kết 1-1 với phiếu nhập kho.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "bien_ban_chenh_lech")
public class DiscrepancyReport {

    /**
     * ID tự tăng của biên bản chênh lệch.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Phiếu nhập kho liên kết với biên bản chênh lệch này (quan hệ 1-1).
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phieu_nhap_id", nullable = false, unique = true)
    private ImportReceipt importReceipt;

    /**
     * Mã biên bản chênh lệch (định dạng: BBCL-[Mã Phiếu Nhập]).
     */
    @Column(name = "ma_bien_ban", nullable = false, unique = true, length = 50)
    private String code;

    /**
     * Ngày lập biên bản chênh lệch.
     */
    @Column(name = "ngay_lap", nullable = false)
    private LocalDateTime reportDate = LocalDateTime.now();

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private DiscrepancyReportStatus status = DiscrepancyReportStatus.CHO_DUYET;

    /**
     * Nhân viên lập biên bản chênh lệch (người thực hiện kiểm đếm và phát hiện chênh lệch).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_lap_id", nullable = false)
    private Employee createdBy;

    /**
     * Ghi chú chung về biên bản chênh lệch.
     */
    @Column(name = "ghi_chu", length = 255)
    private String note;

    /**
     * Danh sách các dòng chi tiết sản phẩm bị chênh lệch số lượng.
     */
    @OneToMany(mappedBy = "report", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<DiscrepancyReportDetail> details = new ArrayList<>();

    /**
     * Thời điểm bản ghi được tạo trong hệ thống.
     */
    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm bản ghi được cập nhật lần cuối trong hệ thống.
     */
    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;

    /**
     * Số phiên bản phục vụ cho cơ chế khóa lạc quan (Optimistic Locking).
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
