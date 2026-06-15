package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.product.ProductPageResponse;

public interface ProductService {

    /**
     * Lấy danh sách sản phẩm có phân trang và lọc động.
     *
     * @param page       Số trang (base-0)
     * @param size       Kích thước trang (1–100)
     * @param keyword    Tìm tương đối theo mã hoặc tên sản phẩm
     * @param categoryId Lọc theo ID danh mục
     * @param status     Lọc theo trạng thái (DANG_BAN / TAM_NGUNG)
     */
    ProductPageResponse listProducts(int page, int size, String keyword, Long categoryId, String status);
}
