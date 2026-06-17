package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.product.ProductCreateRequest;
import com.smartflow.smestocksensebackend.dto.product.ProductDetailResponse;
import com.smartflow.smestocksensebackend.dto.product.ProductListItemResponse;
import com.smartflow.smestocksensebackend.dto.product.ProductPageResponse;
import com.smartflow.smestocksensebackend.dto.product.ProductUpdateRequest;
import com.smartflow.smestocksensebackend.dto.product.UpdateProductStatusRequest;
import com.smartflow.smestocksensebackend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
     * Lấy danh sách sản phẩm có phân trang, tìm kiếm và lọc theo danh mục, trạng thái.
     *
     * @param page      trang hiện tại (bắt đầu từ 0)
     * @param size      số lượng bản ghi mỗi trang
     * @param keyword   tìm kiếm theo tên / mã SP / SKU (không phân biệt hoa thường)
     * @param danhMucId lọc theo id danh mục
     * @param trangThai lọc theo trạng thái: ACTIVE | INACTIVE | HOAT_DONG | NGUNG_HOAT_DONG
     */
    @GetMapping
    public ProductPageResponse listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long danhMucId,
            @RequestParam(required = false) String trangThai
    ) {
        PageRequest pageable = PageRequest.of(
                page, size,
                Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        return productService.listProducts(keyword, danhMucId, trangThai, pageable);
    }

    @GetMapping("/{id}")
    public ProductDetailResponse getProduct(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PostMapping
    public ResponseEntity<ProductListItemResponse> createProduct(
            @Valid @RequestBody ProductCreateRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(productService.createProduct(request));
    }

    @PutMapping("/{id}")
    public ProductListItemResponse updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        return productService.updateProduct(id, request);
    }

    @PatchMapping("/{id}/status")
    public ProductListItemResponse updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateProductStatusRequest request
    ) {
        return productService.updateStatus(id, request);
    }
}