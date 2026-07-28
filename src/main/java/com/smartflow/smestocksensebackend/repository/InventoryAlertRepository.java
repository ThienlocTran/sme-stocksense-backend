package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.InventoryAlert;
import com.smartflow.smestocksensebackend.entity.InventoryAlertStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Giao diện truy vấn dữ liệu cảnh báo tồn kho từ DB.
 * Kế thừa JpaRepository và JpaSpecificationExecutor để hỗ trợ lọc động cho
 * Dashboard.
 */
@Repository
public interface InventoryAlertRepository
        extends JpaRepository<InventoryAlert, Long>, JpaSpecificationExecutor<InventoryAlert> {

    /**
     * Kiểm tra nhanh sự tồn tại của phiếu cảnh báo theo danh sách trạng thái (Phục
     * vụ Deduplication ở T179).
     * 
     * @return true nếu đã tồn tại phiếu cảnh báo trong danh sách trạng thái cho cặp
     *         SP + Kho.
     */
    boolean existsByProductIdAndWarehouseIdAndStatusIn(Long productId, Long warehouseId,
            Collection<InventoryAlertStatus> statuses);

    /**
     * Tìm phiếu cảnh báo đầu tiên theo danh sách trạng thái (Phục vụ cập nhật hoặc
     * tự động giải quyết khi nhập kho ở T183/T184).
     */
    Optional<InventoryAlert> findFirstByProductIdAndWarehouseIdAndStatusIn(Long productId, Long warehouseId,
            Collection<InventoryAlertStatus> statuses);

    /**
     * Lấy danh sách phiếu cảnh báo theo kho hàng và trạng thái, sắp xếp mới nhất
     * (Phục vụ Dashboard/List ở T181/T182).
     */
    List<InventoryAlert> findByWarehouseIdAndStatusOrderByCreatedAtDesc(Long warehouseId, InventoryAlertStatus status);

    /**
     * Đếm số lượng phiếu cảnh báo theo kho hàng và trạng thái (Phục vụ KPI huy hiệu
     * trên Dashboard).
     */
    long countByWarehouseIdAndStatus(Long warehouseId, InventoryAlertStatus status);
}
