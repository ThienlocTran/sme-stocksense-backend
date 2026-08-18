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

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Thực thể ForecastModelMetadata ánh xạ bảng "ai.thong_tin_mo_hinh".
 * Lưu metadata của một lần huấn luyện: độ chính xác (sMAPE), phiên bản, chế độ (XGBoost hay cold-start).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "thong_tin_mo_hinh", schema = "ai")
public class ForecastModelMetadata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kho_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "smape", nullable = false)
    private BigDecimal smape;

    @Column(name = "phien_ban", nullable = false)
    private Integer version = 1;

    @Column(name = "so_ngay_du_lieu", nullable = false)
    private Integer dataDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "che_do", nullable = false, length = 20)
    private ForecastMode mode = ForecastMode.XGBOOST;

    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;
}
