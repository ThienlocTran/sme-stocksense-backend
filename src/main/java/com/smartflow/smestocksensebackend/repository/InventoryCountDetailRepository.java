package com.smartflow.smestocksensebackend.repository;
import com.smartflow.smestocksensebackend.entity.InventoryCountDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface InventoryCountDetailRepository extends JpaRepository<InventoryCountDetail,Long> {
    List<InventoryCountDetail> findByInventoryCountIdOrderByIdAsc(Long inventoryCountId);
}
