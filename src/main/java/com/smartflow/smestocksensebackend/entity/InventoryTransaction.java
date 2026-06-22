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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Thực thể InventoryTransaction ánh xạ đến bảng "giao_dich_kho" - sổ lịch sử
 * biến động tồn kho (append-only). Mỗi dòng ghi lại 1 lần thay đổi tồn kèm
 * số lượng trước/sau và nguồn chứng từ.
 * Quan hệ:
 * - @ManyToOne Product   (san_pham_id, bắt buộc)
 * - @ManyToOne Warehouse (kho_id, bắt buộc)
 * - @ManyToOne ImportReceipt (phieu_nhap_id, nullable)
 * - @ManyToOne Employee  (nguoi_tao_id, nullable)
 * Lưu ý: phieu_xuat_id và lan_import_id được map dạng Long thuần vì các entity
 * ExportReceipt / ImportBatch chưa tồn tại trong dự án. Khi bổ sung 2 entity đó
 * có thể nâng cấp thành @ManyToOne.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "giao_dich_kho")
public class InventoryTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kho_id", nullable = false)
    private Warehouse warehouse;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "loai_giao_dich", nullable = false, columnDefinition = "loai_giao_dich_kho")
    private InventoryTransactionType transactionType;

    @Column(name = "so_luong", nullable = false)
    private Integer quantity;

    @Column(name = "so_luong_truoc", nullable = false)
    private Integer quantityBefore;

    @Column(name = "so_luong_sau", nullable = false)
    private Integer quantityAfter;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "phieu_nhap_id")
    private ImportReceipt importReceipt;

    @Column(name = "phieu_xuat_id")
    private Long exportReceiptId;

    @Column(name = "lan_import_id")
    private Long importBatchId;

    @Column(name = "ghi_chu", length = 255)
    private String note;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nguoi_tao_id")
    private Employee createdBy;

    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;
}
