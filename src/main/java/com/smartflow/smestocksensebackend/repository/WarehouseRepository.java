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
@Repository
public interface WarehouseRepository extends JpaRepository<Warehouse, Long>, JpaSpecificationExecutor<Warehouse> {

    /**
     * Kiểm tra sự tồn tại của mã kho hàng trong cơ sở dữ liệu (không phân biệt chữ hoa/thường).
     * Phục vụ cho việc validate trùng mã kho khi tạo mới.
     *
     * @param code Mã kho cần kiểm tra
     * @return true nếu đã tồn tại mã kho này, ngược lại là false
     */
    boolean existsByCodeIgnoreCase(String code);
}
