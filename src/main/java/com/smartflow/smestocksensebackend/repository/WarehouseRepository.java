package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.Warehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Interface Repository quản lý các giao tiếp CSDL liên quan đến thực thể Warehouse.
 * Kế thừa JpaRepository để hỗ trợ các tính năng CRUD mặc định.
 * Kế thừa JpaSpecificationExecutor để hỗ trợ các câu truy vấn động nâng cao thông qua Specification.
 */
@Repository // Đăng ký lớp này thành một Spring Bean chịu trách nhiệm kết nối dữ liệu
public interface WarehouseRepository extends JpaRepository<Warehouse, Long>, JpaSpecificationExecutor<Warehouse> {
}
