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
import java.time.LocalDateTime;

/**
 * Thực thể ForecastDriftLog ánh xạ bảng "ai.nhat_ky_lech_mo_hinh".
 * Ghi lại mỗi lần phát hiện mô hình dự báo bị lệch (sMAPE thực tế vượt ngưỡng),
 * so sánh dự báo đã lưu (ket_qua_du_bao) với thực tế xuất kho (giao_dich_kho).
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "nhat_ky_lech_mo_hinh", schema = "ai")
public class ForecastDriftLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "thong_tin_mo_hinh_id")
    private ForecastModelMetadata modelMetadata;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kho_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "smape_thuc_te", nullable = false)
    private BigDecimal actualSmape;

    @Column(name = "rolling_smape")
    private BigDecimal rollingSmape;

    @Column(name = "nguong_smape", nullable = false)
    private BigDecimal thresholdSmape;

    @Column(name = "can_train_lai", nullable = false)
    private Boolean retrainNeeded = false;

    @Column(name = "can_huan_luyen_lai", nullable = false)
    private Boolean targetRetrainNeeded = false;

    @Column(name = "so_ngay_doi_chieu", nullable = false)
    private Integer comparedDays = 0;

    @CreationTimestamp
    @Column(name = "ngay_phat_hien", updatable = false)
    private LocalDateTime detectedAt;

    @CreationTimestamp
    @Column(name = "ngay_kiem_tra", updatable = false)
    private LocalDateTime checkedAt;
}
