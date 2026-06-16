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
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "san_pham")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_san_pham", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "ten_san_pham", nullable = false, length = 200)
    private String name;

    @Column(name = "sku", unique = true, length = 100)
    private String sku;

    @Column(name = "ma_vach", unique = true, length = 100)
    private String barcode;

    @Column(name = "don_vi_tinh", nullable = false, length = 50)
    private String unit;

    @Column(name = "price", precision = 15, scale = 2)
    private BigDecimal price;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "danh_muc_id")
    private Category category;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doi_tac_cung_cap_id")
    private Partner partner;

    @Column(name = "ton_toi_thieu", nullable = false)
    private Integer minStock = 0;

    @Column(name = "ton_toi_da")
    private Integer maxStock;

    @Column(name = "thoi_gian_giao_hang", nullable = false)
    private Integer leadTimeDays = 1;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "trang_thai", nullable = false, columnDefinition = "trang_thai_san_pham")
    private ProductStatus status = ProductStatus.HOAT_DONG;

    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;
}
