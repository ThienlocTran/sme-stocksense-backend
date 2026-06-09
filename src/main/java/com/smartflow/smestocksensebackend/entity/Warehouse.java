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
 * Thực thể Warehouse ánh xạ trực tiếp đến bảng "warehouses" trong cơ sở dữ liệu.
 */
@Getter // Tự động sinh ra các hàm Getter cho toàn bộ thuộc tính
@Setter // Tự động sinh ra các hàm Setter cho toàn bộ thuộc tính
@NoArgsConstructor // Tự động tạo Constructor mặc định không có đối số
@Entity // Khai báo đây là một thực thể được quản lý bởi JPA/Hibernate
@Table(name = "warehouses") // Ánh xạ thực thể này với bảng "warehouses" trong DB
public class Warehouse {

    @Id // Khai báo đây là cột khóa chính
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Sử dụng cơ chế tự sinh ID tự động tăng của DB (IDENTITY)
    private Long id; // ID định danh duy nhất của kho hàng

    @Column(name = "code", nullable = false, unique = true, length = 50) // Cột "code", bắt buộc điền, là duy nhất và dài tối đa 50 ký tự
    private String code; // Mã kho hàng (Ví dụ: WH001)

    @Column(name = "name", nullable = false, length = 150) // Cột "name", bắt buộc điền, độ dài tối đa 150 ký tự
    private String name; // Tên kho hàng (Ví dụ: Kho tổng Hà Nội)

    @Column(name = "address", length = 255) // Cột "address", độ dài tối đa 255 ký tự, cho phép rỗng
    private String address; // Địa chỉ kho hàng

    @Enumerated(EnumType.STRING) // Lưu enum dưới dạng chuỗi chữ (VARCHAR) trong database
    @JdbcTypeCode(SqlTypes.NAMED_ENUM) // Khai báo cho Hibernate biết đây là kiểu ENUM được định nghĩa trong PostgreSQL
    @Column(name = "status", nullable = false, columnDefinition = "warehouse_status") // Liên kết với kiểu dữ liệu tùy chỉnh 'warehouse_status' trong Postgres
    private WarehouseStatus status = WarehouseStatus.ACTIVE; // Trạng thái hoạt động của kho, mặc định là ACTIVE khi tạo mới

    @CreationTimestamp // Tự động lấy thời gian hệ thống và lưu khi tạo mới bản ghi
    @Column(name = "created_at", updatable = false) // Chỉ ghi nhận lúc khởi tạo, không được cập nhật khi chỉnh sửa bản ghi
    private LocalDateTime createdAt; // Thời điểm tạo kho hàng

    @UpdateTimestamp // Tự động cập nhật thời gian hệ thống hiện tại mỗi khi bản ghi có bất kỳ thay đổi nào
    @Column(name = "updated_at")
    private LocalDateTime updatedAt; // Thời điểm cập nhật kho hàng gần nhất
}
