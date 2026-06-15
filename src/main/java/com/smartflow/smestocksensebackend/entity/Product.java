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
@Table(name = "san_pham")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mã nội bộ hệ thống (bắt buộc, unique)
    @Column(name = "ma_san_pham", nullable = false, unique = true, length = 50)
    private String code;

    // Tên sản phẩm
    @Column(name = "ten_san_pham", nullable = false, length = 200)
    private String name;

    // SKU (mã vạch/mã thương mại, có thể null)
    @Column(name = "sku", unique = true, length = 100)
    private String sku;

    // Đơn vị tính
    @Column(name = "don_vi_tinh", nullable = false, length = 50)
    private String unit;

    // Danh mục
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "danh_muc_id")
    private Category category;

    // Ngưỡng tồn tối thiểu để cảnh báo
    @Column(name = "ton_toi_thieu", nullable = false)
    private Integer minThreshold;

    // Trạng thái hoạt động (true = đang bán, false = tạm ngưng)
    @Column(name = "dang_hoat_dong", nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;
}
