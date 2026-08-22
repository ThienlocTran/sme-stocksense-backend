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
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ket_qua_du_bao_hang_ngay", schema = "ai",
        uniqueConstraints = @UniqueConstraint(columnNames = {"thong_tin_mo_hinh_id", "ngay_du_bao"}))
public class DailyForecastResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "thong_tin_mo_hinh_id", nullable = false)
    private ForecastModelMetadata modelMetadata;

    @Column(name = "ngay_du_bao", nullable = false)
    private LocalDate forecastDate;

    @Column(name = "so_luong_du_bao", nullable = false, precision = 18, scale = 4)
    private BigDecimal predictedQuantity;

    @CreationTimestamp
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
