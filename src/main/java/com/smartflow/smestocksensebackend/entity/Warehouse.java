package com.smartflow.smestocksensebackend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Thực thể Warehouse ánh xạ trực tiếp đến bảng "kho" trong cơ sở dữ liệu.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "kho")
public class Warehouse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Mã kho hàng không cho trùng, không cho phép sửa (nghiệp vụ định danh)
    @Column(name = "ma_kho", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "ten_kho", nullable = false, length = 150)
    private String name;

    @Column(name = "dia_chi", length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "trang_thai", nullable = false, columnDefinition = "trang_thai_kho")
    private WarehouseStatus status = WarehouseStatus.HOAT_DONG;

    @Column(name = "suc_chua_toi_da_m3", nullable = false, precision = 12, scale = 3)
    private java.math.BigDecimal maxCapacityM3 = new java.math.BigDecimal("1500.000");

    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;
}
