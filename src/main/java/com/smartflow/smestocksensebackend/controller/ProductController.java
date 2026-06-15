package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.product.ProductPageResponse;
import com.smartflow.smestocksensebackend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/products
     * Lấy danh sách sản phẩm có phân trang và lọc động.
     *
     * @param page       Số trang, mặc định 0
     * @param size       Kích thước trang, mặc định 10
     * @param keyword    Tìm theo mã hoặc tên sản phẩm (LIKE)
     * @param categoryId Lọc theo ID danh mục
     * @param status     Lọc theo trạng thái: DANG_BAN hoặc TAM_NGUNG
     */
    @GetMapping
    public ProductPageResponse listProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String status
    ) {
        return productService.listProducts(page, size, keyword, categoryId, status);
    }
}
