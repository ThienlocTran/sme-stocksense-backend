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
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "http://localhost:5173")
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

    @PatchMapping("/{id}/disable")
    public CategoryListItemResponse disableCategory(@PathVariable Long id) {
        return categoryService.disableCategory(id);
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
