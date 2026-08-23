package com.smartflow.smestocksensebackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "phieu_xuat_kho_lich_su")
public class ExportReceiptHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "phieu_xuat_id", nullable = false)
    private ExportReceipt document;
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "nguoi_thuc_hien_id", nullable = false)
    private Employee actor;
    // Đọc thô theo đúng kiểu cột varchar: lịch sử đã lưu có thể chứa hành động cũ ngoài enum hiện tại.
    // Ghi vẫn bị giới hạn bởi ExportReceiptAction qua setter bên dưới.
    @Column(name = "hanh_dong", nullable = false, length = 50)
    private String action;
    @Column(name = "ghi_chu", length = 500)
    private String note;
    @CreationTimestamp
    @Column(name = "ngay_thuc_hien", updatable = false)
    private LocalDateTime createdAt;

    public void setAction(ExportReceiptAction action) {
        this.action = action != null ? action.name() : null;
    }
}
