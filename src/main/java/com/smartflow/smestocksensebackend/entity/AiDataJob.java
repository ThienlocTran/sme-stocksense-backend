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

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "tac_vu_du_lieu_ai", schema = "ai")
public class AiDataJob {

    public static final String TYPE_SEED_DEMO_HISTORY = "SEED_DEMO_HISTORY";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "job_id", nullable = false, unique = true)
    private UUID jobId;

    @Column(name = "loai_tac_vu", nullable = false, length = 50)
    private String jobType;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private AiDataJobStatus status;

    @Column(name = "bat_dau_luc", nullable = false)
    private LocalDateTime startedAt;

    @Column(name = "hoan_thanh_luc")
    private LocalDateTime completedAt;

    @Column(name = "so_dong_da_them")
    private Integer rowsInserted;

    @Column(name = "so_chuoi_da_tao")
    private Integer seriesSeeded;

    @Column(name = "loi")
    private String errorMessage;
}
