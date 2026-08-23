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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Thực thể SalesHistory ánh xạ bảng "ai.lich_su_ban_hang".
 * Chuỗi thời gian (theo ngày) dùng làm đầu vào huấn luyện mô hình dự báo XGBoost.
 * Nguồn dữ liệu ban đầu là seed giả lập cho demo (xem SalesHistorySource).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lich_su_ban_hang", schema = "ai",
        uniqueConstraints = @UniqueConstraint(columnNames = { "san_pham_id", "kho_id", "ngay", "nguon_du_lieu" }))
public class SalesHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kho_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "ngay", nullable = false)
    private LocalDate ngay;

    @Column(name = "so_luong", nullable = false)
    private Integer quantity;

    @Column(name = "gia_ban_binh_quan", precision = 15, scale = 2)
    private BigDecimal averageSellingPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "nguon_du_lieu", nullable = false, length = 50)
    private SalesHistorySource source = SalesHistorySource.SEED_DEMO;

    @Column(name = "tham_chieu_nguon", length = 255)
    private String sourceReference;

    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;
}
