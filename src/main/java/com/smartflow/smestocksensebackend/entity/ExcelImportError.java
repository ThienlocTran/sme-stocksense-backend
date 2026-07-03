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

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "loi_import_excel")
public class ExcelImportError {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "lan_import_id", nullable = false)
    private ExcelImport excelImport;

    @Column(name = "so_dong")
    private Integer rowNumber;

    @Column(name = "ten_cot", length = 100)
    private String columnName;

    @Column(name = "gia_tri_goc", length = 255)
    private String originalValue;

    @Column(name = "noi_dung_loi", length = 255)
    private String message;

    @Column(name = "goi_y_sua", length = 255)
    private String suggestion;

    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;
}
