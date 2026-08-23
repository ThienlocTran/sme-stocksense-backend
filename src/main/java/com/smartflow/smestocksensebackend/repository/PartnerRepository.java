package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.Partner;
import com.smartflow.smestocksensebackend.entity.PartnerStatus;
import com.smartflow.smestocksensebackend.entity.PartnerType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Interface Repository quản lý các giao tiếp CSDL liên quan đến thực thể Partner.
 * Kế thừa JpaRepository để hỗ trợ các tính năng CRUD mặc định.
 * Kế thừa JpaSpecificationExecutor để hỗ trợ các câu truy vấn động nâng cao thông qua Specification.
 */
@Repository
public interface PartnerRepository extends JpaRepository<Partner, Long>, JpaSpecificationExecutor<Partner> {

    /**
     * Kiểm tra sự tồn tại của mã đối tác trong cơ sở dữ liệu (không phân biệt chữ hoa/thường).
     *
     * @param code Mã đối tác cần kiểm tra
     * @return true nếu đã tồn tại mã đối tác này, ngược lại là false
     */
    boolean existsByCodeIgnoreCase(String code);

    Optional<Partner> findByCodeIgnoreCase(String code);

    boolean existsByCodeIgnoreCaseAndType(String code, PartnerType type);

    List<Partner> findByTypeAndStatusOrderByNameAsc(PartnerType type, PartnerStatus status);
}
