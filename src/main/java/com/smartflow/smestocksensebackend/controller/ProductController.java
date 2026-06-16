package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.product.ProductCreateRequest;
import com.smartflow.smestocksensebackend.dto.product.ProductListItemResponse;
import com.smartflow.smestocksensebackend.dto.product.ProductPageResponse;
import com.smartflow.smestocksensebackend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/products
     * Lấy danh sách sản phẩm có phân trang, tìm kiếm và lọc theo trạng thái.
     *
     * @param page    trang hiện tại (bắt đầu từ 0)
     * @param size    số lượng bản ghi mỗi trang
     * @param keyword tìm kiếm theo tên hoặc SKU (không phân biệt hoa thường)
     * @param status  lọc theo trạng thái: HOAT_DONG | NGUNG_HOAT_DONG
     */
    @GetMapping
    public ProductPageResponse listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        PageRequest pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        return productService.listProducts(keyword, status, pageable);
    }

    @PostMapping
    public ResponseEntity<ProductListItemResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }
}
