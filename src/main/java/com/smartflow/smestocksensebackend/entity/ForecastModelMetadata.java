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
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDate;
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

    @Column(name = "smape")
    private BigDecimal smape;

    @Column(name = "phien_ban", nullable = false)
    private Integer version = 1;

    @Column(name = "so_ngay_du_lieu", nullable = false)
    private Integer dataDays;

    @Enumerated(EnumType.STRING)
    @Column(name = "che_do", nullable = false, length = 20)
    private ForecastMode mode = ForecastMode.XGBOOST;

    @Enumerated(EnumType.STRING)
    @Column(name = "kieu_tap_du_lieu", nullable = false, length = 30)
    private ForecastDatasetType datasetType = ForecastDatasetType.LEGACY_UNKNOWN;

    @Enumerated(EnumType.STRING)
    @Column(name = "nguon_du_lieu", length = 50)
    private SalesHistorySource historySource;

    @Column(name = "ngay_bat_dau_du_lieu")
    private LocalDate historyStartDate;

    @Column(name = "ngay_ket_thuc_du_lieu")
    private LocalDate historyEndDate;

    @Column(name = "mae", precision = 18, scale = 4)
    private BigDecimal mae;

    @Column(name = "rmse", precision = 18, scale = 4)
    private BigDecimal rmse;

    @Column(name = "tham_so_mo_hinh", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String modelParamsSnapshot;

    @Column(name = "dac_trung_su_dung", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    private String featureSnapshot;

    @CreationTimestamp
    @Column(name = "ngay_huan_luyen", updatable = false)
    private LocalDateTime trainedAt;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime createdAt;
}
