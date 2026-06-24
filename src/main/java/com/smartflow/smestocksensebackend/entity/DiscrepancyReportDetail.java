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
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Thực thể (Entity) đại diện cho Chi tiết biên bản chênh lệch nhập kho (Discrepancy Report Detail).
 * Lưu trữ chi tiết về từng sản phẩm bị chênh lệch số lượng, bao gồm số lượng chứng từ,
 * số lượng thực tế nhận được, chênh lệch thực tế, lý do chênh lệch và hướng xử lý đề xuất.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chi_tiet_bien_ban_chenh_lech")
public class DiscrepancyReportDetail {

    /**
     * ID tự tăng của chi tiết biên bản chênh lệch.
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Biên bản chênh lệch cha mà dòng chi tiết này thuộc về (quan hệ N-1).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "bien_ban_id", nullable = false)
    private DiscrepancyReport report;

    /**
     * Sản phẩm bị chênh lệch số lượng (quan hệ N-1 với bảng sản phẩm).
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private Product product;

    /**
     * Số lượng hàng hóa dự kiến nhập theo chứng từ gốc.
     */
    @Column(name = "so_luong_chung_tu", nullable = false)
    private Integer documentQuantity;

    /**
     * Số lượng hàng hóa thực tế nhận được sau kiểm đếm.
     */
    @Column(name = "so_luong_thuc_te", nullable = false)
    private Integer actualQuantity;

    /**
     * Số lượng chênh lệch (Thực tế - Chứng từ).
     * Số âm thể hiện thiếu hàng, số dương thể hiện thừa hàng.
     */
    @Column(name = "so_luong_lech", nullable = false)
    private Integer discrepancyQuantity;

    /**
     * Lý do xảy ra chênh lệch cho sản phẩm này (ví dụ: Nhà cung cấp giao thiếu, hư hỏng khi vận chuyển...).
     */
    @Column(name = "ly_do", length = 255)
    private String reason;

    /**
     * Hướng xử lý đề xuất (ví dụ: Yêu cầu giao bù, hoàn tiền, nhận luôn thừa và cập nhật chứng từ...).
     */
    @Column(name = "huong_xu_ly", length = 255)
    private String action;

    /**
     * Thời điểm dòng chi tiết được tạo.
     */
    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    /**
     * Thời điểm dòng chi tiết được cập nhật lần cuối.
     */
    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;

    /**
     * Số phiên bản phục vụ khóa lạc quan (Optimistic Locking).
     */
    @Version
    @Column(name = "version", nullable = false)
    private Long version;
}
