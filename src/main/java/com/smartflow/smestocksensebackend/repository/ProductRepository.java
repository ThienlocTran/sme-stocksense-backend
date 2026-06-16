package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    // --- CREATE: check toàn bảng ---
    boolean existsByCodeIgnoreCase(String code);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsByBarcodeIgnoreCase(String barcode);

    // --- UPDATE: check loại trừ chính record đang sửa ---
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, Long id);
}