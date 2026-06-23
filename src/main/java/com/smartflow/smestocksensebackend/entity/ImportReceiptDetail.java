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

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "chi_tiet_phieu_nhap")
public class ImportReceiptDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phieu_nhap_id", nullable = false)
    private ImportReceipt document;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private Product product;

    @Column(name = "so_luong", nullable = false)
    private Integer expectedQuantity;

    @Column(name = "so_luong_thuc_nhan")
    private Integer actualReceivedQuantity;

    @Column(name = "don_gia", precision = 15, scale = 2)
    private BigDecimal expectedUnitPrice;

    @Column(name = "thanh_tien", precision = 15, scale = 2)
    private BigDecimal expectedLineTotal;

    @Column(name = "ghi_chu", length = 255)
    private String note;

    @Column(name = "tinh_trang", length = 255)
    private String physicalStatus;

    @Column(name = "han_su_dung")
    private java.time.LocalDateTime expiryDate;

    @Column(name = "trang_thai_dong", length = 50)
    private String rowStatus;

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
