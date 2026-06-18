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
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.UpdateTimestamp;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;

/**
 * Thực thể Partner ánh xạ trực tiếp đến bảng "doi_tac" trong cơ sở dữ liệu.
 * Nghiệp vụ:
 * - Hệ thống không xóa vật lý đối tác để bảo toàn lịch sử giao dịch.
 * - Mã đối tác là duy nhất để nhận diện trong toàn bộ hệ thống.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "doi_tac")
public class Partner {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ma_doi_tac", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "ten_doi_tac", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "loai_doi_tac", nullable = false, columnDefinition = "loai_doi_tac")
    private PartnerType type;

    @Column(name = "nguoi_lien_he", length = 150)
    private String contactPerson;

    @Column(name = "so_dien_thoai", length = 30)
    private String phoneNumber;

    @Column(name = "email", length = 150)
    private String email;

    @Column(name = "dia_chi", length = 255)
    private String address;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "trang_thai", nullable = false, columnDefinition = "trang_thai_doi_tac")
    private PartnerStatus status = PartnerStatus.HOAT_DONG;

    @CreationTimestamp
    @Column(name = "ngay_tao", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "ngay_cap_nhat")
    private LocalDateTime updatedAt;
}
