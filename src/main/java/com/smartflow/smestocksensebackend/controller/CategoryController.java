package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.category.CategoryListItemResponse;
import com.smartflow.smestocksensebackend.dto.category.CategoryPageResponse;
import com.smartflow.smestocksensebackend.dto.category.CreateCategoryRequest;
import com.smartflow.smestocksensebackend.dto.category.UpdateCategoryRequest;
import com.smartflow.smestocksensebackend.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryListItemResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(categoryService.createCategory(request));
    }

    @PutMapping("/{id}")
    public CategoryListItemResponse updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return categoryService.updateCategory(id, request);
    }

    @GetMapping
    public CategoryPageResponse listCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status
    ) {
        return categoryService.listCategories(page, size, keyword, status);
    }
}
