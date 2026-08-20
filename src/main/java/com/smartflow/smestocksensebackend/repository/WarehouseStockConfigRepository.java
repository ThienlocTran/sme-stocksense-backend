package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.WarehouseStockConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarehouseStockConfigRepository extends JpaRepository<WarehouseStockConfig, Long> {
    Optional<WarehouseStockConfig> findByProductIdAndWarehouseId(Long productId, Long warehouseId);
    List<WarehouseStockConfig> findByWarehouseId(Long warehouseId);

    @Query(value = "SELECT COALESCE(SUM(sp.the_tich_don_vi_m3 * COALESCE(cc.ton_toi_thieu_ghi_de, sp.ton_toi_thieu_mac_dinh)), 0) "
                    + "FROM ton_kho t "
                    + "JOIN san_pham sp ON sp.id = t.san_pham_id "
                    + "LEFT JOIN cau_hinh_ton_kho cc ON cc.san_pham_id = t.san_pham_id AND cc.kho_id = t.kho_id "
                    + "WHERE t.kho_id = :warehouseId "
                    + "AND sp.the_tich_don_vi_m3 IS NOT NULL "
                    + "AND COALESCE(cc.ton_toi_thieu_ghi_de, sp.ton_toi_thieu_mac_dinh) IS NOT NULL", nativeQuery = true)
    java.math.BigDecimal sumMinimumSafeVolumeByWarehouseId(@Param("warehouseId") Long warehouseId);

    @Query(value = "SELECT COUNT(*) "
                    + "FROM ton_kho t "
                    + "JOIN san_pham sp ON sp.id = t.san_pham_id "
                    + "LEFT JOIN cau_hinh_ton_kho cc ON cc.san_pham_id = t.san_pham_id AND cc.kho_id = t.kho_id "
                    + "WHERE t.kho_id = :warehouseId "
                    + "AND (sp.the_tich_don_vi_m3 IS NULL OR COALESCE(cc.ton_toi_thieu_ghi_de, sp.ton_toi_thieu_mac_dinh) IS NULL)", nativeQuery = true)
    long countMissingSafeVolumeConfigByWarehouseId(@Param("warehouseId") Long warehouseId);
}
