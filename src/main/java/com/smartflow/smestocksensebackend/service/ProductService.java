package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.product.ProductCreateRequest;
import com.smartflow.smestocksensebackend.dto.product.ProductListItemResponse;
import com.smartflow.smestocksensebackend.dto.product.ProductPageResponse;
import com.smartflow.smestocksensebackend.dto.product.ProductUpdateRequest;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductPageResponse listProducts(String keyword, String status, Pageable pageable);

    ProductListItemResponse createProduct(ProductCreateRequest request);

    ProductListItemResponse updateProduct(Long id, ProductUpdateRequest request);
}
