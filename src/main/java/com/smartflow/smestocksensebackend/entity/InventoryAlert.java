package com.smartflow.smestocksensebackend.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

/**
 * Note: [T177 - Khối Entity] Thực thể InventoryAlert ánh xạ bảng canh_bao_ton_kho.
 * - Lưu trữ snapshot số lượng tồn kho (so_luong_hien_tai, ton_toi_thieu, ton_toi_da) tại thời điểm sinh cảnh báo.
 * - Tích hợp Transition Guard (canAcknowledge / canResolve / acknowledge / resolve) bảo vệ toàn vẹn vòng đời xử lý.
 * - Sử dụng @Version Optimistic Lock chống xung đột cập nhật đồng thời.
 */
@Entity
@Table(name = "canh_bao_ton_kho")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "san_pham_id", nullable = false)
    private Product product;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kho_id", nullable = false)
    private Warehouse warehouse;

    @Column(name = "so_luong_hien_tai", nullable = false)
    private Integer currentQuantity;

    @Column(name = "ton_toi_thieu")
    private Integer minStock;

    @Column(name = "ton_toi_da")
    private Integer maxStock;

    @Enumerated(EnumType.STRING)
    @Column(name = "muc_do", nullable = false, length = 20)
    private InventoryAlertSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "trang_thai", nullable = false, length = 20)
    private InventoryAlertStatus status;

    @Column(name = "ghi_chu", length = 500)
    private String note;

    @Column(name = "nguoi_xu_ly", length = 100)
    private String handledBy;

    @Version
    @Column(name = "version", nullable = false)
    @Builder.Default
    private Long version = 0L;

    @CreationTimestamp
    @Column(name = "ngay_tao", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "ngay_giai_quyet")
    private LocalDateTime resolvedAt;

    // Note: [T177 - Khối Transition Guard] Kiểm tra hợp lệ luồng chuyển trạng thái
    public boolean canAcknowledge() {
        return this.status == InventoryAlertStatus.OPEN;
    }

    public boolean canResolve() {
        return this.status == InventoryAlertStatus.OPEN || this.status == InventoryAlertStatus.ACKNOWLEDGED;
    }

    /**
     * Chuyển trạng thái sang ACKNOWLEDGED (Đã xem/đang xử lý).
     * @throws IllegalStateException nếu trạng thái hiện tại không phải OPEN.
     */
    public void acknowledge(String actor, String note) {
        if (!canAcknowledge()) {
            throw new IllegalStateException("Chỉ có thể xác nhận xử lý cho phiếu cảnh báo đang mở (OPEN).");
        }
        this.status = InventoryAlertStatus.ACKNOWLEDGED;
        this.handledBy = actor;
        this.note = note;
    }

    /**
     * Chuyển trạng thái sang RESOLVED (Đã giải quyết). Có tính idempotent.
     * @throws IllegalStateException nếu trạng thái hiện tại không hợp lệ.
     */
    public void resolve(String actor) {
        if (this.status == InventoryAlertStatus.RESOLVED) {
            return; // Idempotent: gọi nhiều lần không lỗi khi đã RESOLVED
        }
        if (!canResolve()) {
            throw new IllegalStateException("Trạng thái hiện tại không hợp lệ để giải quyết.");
        }
        this.status = InventoryAlertStatus.RESOLVED;
        if (actor != null && !actor.isBlank()) {
            this.handledBy = actor;
        }
        this.resolvedAt = LocalDateTime.now();
    }
}
