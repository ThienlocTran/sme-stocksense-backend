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

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "lan_import_excel")
public class ExcelImport {

    public static final String OPENING_INVENTORY_TYPE = "TON_DAU_KY";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ten_file", nullable = false, length = 255)
    private String fileName;

    @Column(name = "duong_dan_file", length = 255)
    private String filePath;

    @Column(name = "loai_import", nullable = false, length = 50)
    private String importType = OPENING_INVENTORY_TYPE;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "kho_id")
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "trang_thai", nullable = false, columnDefinition = "trang_thai_import")
    private ExcelImportStatus status = ExcelImportStatus.CHO_XU_LY;

    @Column(name = "tong_so_dong", nullable = false)
    private Integer totalRows = 0;

    @Column(name = "so_dong_hop_le", nullable = false)
    private Integer validRows = 0;

    @Column(name = "so_dong_loi", nullable = false)
    private Integer errorRows = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_tao_id")
    private Employee createdBy;

    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "ngay_hoan_thanh")
    private LocalDateTime completedAt;
}
