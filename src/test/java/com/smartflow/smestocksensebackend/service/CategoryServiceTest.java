package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.category.CreateCategoryRequest;
import com.smartflow.smestocksensebackend.dto.category.UpdateCategoryRequest;
import com.smartflow.smestocksensebackend.entity.Category;
import com.smartflow.smestocksensebackend.entity.CategoryStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.FieldValidationException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void listCategories_shouldReturnPagedCategories() {
        Category category = category(1L, "DM001", "Nguyên liệu", "Nhóm nguyên liệu", CategoryStatus.HOAT_DONG);
        Mockito.when(categoryRepository.findAll(any(Specification.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(category)));

        var response = categoryService.listCategories(0, 10, "nguyen", "HOAT_DONG");

        assertEquals(1, response.totalElements());
        assertEquals("DM001", response.content().get(0).code());
        assertEquals("HOAT_DONG", response.content().get(0).status());
    }

    @Test
    void createCategory_withValidPayload_shouldCreateCategory() {
        Mockito.when(categoryRepository.existsByNormalizedCode("DM002")).thenReturn(false);
        Mockito.when(categoryRepository.existsByNormalizedName("Thành phẩm")).thenReturn(false);
        Mockito.when(categoryRepository.saveAndFlush(any(Category.class))).thenAnswer(invocation -> {
            Category category = invocation.getArgument(0);
            category.setId(2L);
            return category;
        });

        var response = categoryService.createCategory(new CreateCategoryRequest(
                "dm002", "Thành phẩm", "Nhóm sản phẩm hoàn thiện", "HOAT_DONG"
        ));

        assertEquals(2L, response.id());
        assertEquals("DM002", response.code());
        assertEquals("Thành phẩm", response.name());
        assertEquals("HOAT_DONG", response.status());
    }

    @Test
    void createCategory_withDuplicateCodeAndName_shouldReturnFieldErrors() {
        Mockito.when(categoryRepository.existsByNormalizedCode("DM003")).thenReturn(true);
        Mockito.when(categoryRepository.existsByNormalizedName("Phụ kiện")).thenReturn(true);

        FieldValidationException exception = assertThrows(FieldValidationException.class, () ->
                categoryService.createCategory(new CreateCategoryRequest(
                        "dm003", "Phụ kiện", null, "HOAT_DONG"
                ))
        );

        assertEquals("Mã danh mục đã tồn tại.", exception.getErrors().get("code"));
        assertEquals("Tên danh mục đã tồn tại.", exception.getErrors().get("name"));
    }

    @Test
    void updateCategory_withValidPayload_shouldUpdateCategory() {
        Category existing = category(4L, "DM004", "Cũ", null, CategoryStatus.HOAT_DONG);
        Mockito.when(categoryRepository.findById(4L)).thenReturn(Optional.of(existing));
        Mockito.when(categoryRepository.existsByNormalizedCodeAndIdNot("DM004", 4L)).thenReturn(false);
        Mockito.when(categoryRepository.existsByNormalizedNameAndIdNot("Tên mới", 4L)).thenReturn(false);
        Mockito.when(categoryRepository.saveAndFlush(existing)).thenReturn(existing);

        var response = categoryService.updateCategory(4L, new UpdateCategoryRequest(
                "dm004", "Tên mới", "Mô tả mới", "NGUNG_HOAT_DONG"
        ));

        assertEquals("DM004", response.code());
        assertEquals("Tên mới", response.name());
        assertEquals("Mô tả mới", response.description());
        assertEquals("NGUNG_HOAT_DONG", response.status());
    }

    @Test
    void updateCategory_withInvalidStatus_shouldThrowBadRequestException() {
        Category existing = category(5L, "DM005", "Danh mục", null, CategoryStatus.HOAT_DONG);
        Mockito.when(categoryRepository.findById(5L)).thenReturn(Optional.of(existing));

        assertThrows(BadRequestException.class, () ->
                categoryService.updateCategory(5L, new UpdateCategoryRequest(
                        "DM005", "Danh mục", null, "INVALID"
                ))
        );
    }

    @Test
    void disableCategory_shouldSetStatusToInactive() {
        Category existing = category(6L, "DM006", "Cần ngừng", null, CategoryStatus.HOAT_DONG);
        Mockito.when(categoryRepository.findById(6L)).thenReturn(Optional.of(existing));
        Mockito.when(categoryRepository.saveAndFlush(existing)).thenReturn(existing);

        var response = categoryService.disableCategory(6L);

        assertEquals("NGUNG_HOAT_DONG", response.status());
    }

    @Test
    void disableCategory_withMissingId_shouldThrowNotFoundException() {
        Mockito.when(categoryRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> categoryService.disableCategory(404L));
    }

    private Category category(Long id, String code, String name, String description, CategoryStatus status) {
        Category category = new Category();
        category.setId(id);
        category.setCode(code);
        category.setName(name);
        category.setDescription(description);
        category.setStatus(status);
        return category;
    }
}
