package com.smartflow.smestocksensebackend.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter @NoArgsConstructor @Entity @Table(name="chi_tiet_kiem_ke", uniqueConstraints=@UniqueConstraint(columnNames={"dot_kiem_ke_id","san_pham_id"}))
public class InventoryCountDetail {
    @Id @GeneratedValue(strategy=GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="dot_kiem_ke_id") private InventoryCount inventoryCount;
    @ManyToOne(fetch=FetchType.LAZY, optional=false) @JoinColumn(name="san_pham_id") private Product product;
    @Column(name="so_luong_he_thong", nullable=false) private Integer systemQuantity;
    @Column(name="so_luong_thuc_te") private Integer actualQuantity;
    @Column(name="chenh_lech") private Integer differenceQuantity;
    @Column(name="ly_do_chenh_lech", length=255) private String reason;
    @Column(name="ghi_chu", length=500) private String note;
    @Version private Long version;
}
