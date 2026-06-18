package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.product.ProductCreateRequest;
import com.smartflow.smestocksensebackend.dto.product.ProductDetailResponse;
import com.smartflow.smestocksensebackend.dto.product.ProductListItemResponse;
import com.smartflow.smestocksensebackend.dto.product.ProductPageResponse;
import com.smartflow.smestocksensebackend.dto.product.ProductUpdateRequest;
import com.smartflow.smestocksensebackend.dto.product.UpdateProductStatusRequest;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductPageResponse listProducts(String keyword, Long categoryId, String status, Pageable pageable);

    ProductDetailResponse getProductById(Long id);

    ProductListItemResponse createProduct(ProductCreateRequest request);

    ProductListItemResponse updateProduct(Long id, ProductUpdateRequest request);

    ProductListItemResponse updateStatus(Long id, UpdateProductStatusRequest request);
}