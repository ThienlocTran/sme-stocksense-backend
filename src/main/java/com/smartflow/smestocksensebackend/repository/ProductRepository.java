package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;

public interface ProductRepository extends JpaRepository<Product, Long>, JpaSpecificationExecutor<Product> {

    Optional<Product> findByCodeIgnoreCase(String code);

    Optional<Product> findByCode(String code);

    Optional<Product> findBySkuIgnoreCase(String sku);

    Optional<Product> findBySku(String sku);

    Optional<Product> findByBarcodeIgnoreCase(String barcode);

    Optional<Product> findByBarcode(String barcode);

    // --- CREATE: check toàn bảng ---
    boolean existsByCodeIgnoreCase(String code);

    boolean existsByCode(String code);

    boolean existsBySkuIgnoreCase(String sku);

    boolean existsBySku(String sku);

    boolean existsByBarcodeIgnoreCase(String barcode);

    boolean existsByBarcode(String barcode);

    // --- UPDATE: check loại trừ chính record đang sửa ---
    boolean existsByCodeIgnoreCaseAndIdNot(String code, Long id);

    boolean existsBySkuIgnoreCaseAndIdNot(String sku, Long id);

    boolean existsByBarcodeIgnoreCaseAndIdNot(String barcode, Long id);
}
