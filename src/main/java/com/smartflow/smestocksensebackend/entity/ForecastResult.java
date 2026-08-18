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

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Thực thể ForecastResult ánh xạ bảng "ai.ket_qua_du_bao".
 * Mỗi lần chạy dự báo (runForecast) ghi 3 dòng - horizon 7/14/30 ngày - cùng một "phien_ban".
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "ket_qua_du_bao", schema = "ai")
public class ForecastResult {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kho_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "ngay_du_bao", nullable = false)
    private LocalDate forecastDate;

    @Column(name = "so_ngay_du_bao", nullable = false)
    private Integer horizonDays;

    @Column(name = "so_luong_du_bao", nullable = false)
    private BigDecimal predictedQuantity;

    @Column(name = "phien_ban", nullable = false)
    private Integer version = 1;

    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;
}
