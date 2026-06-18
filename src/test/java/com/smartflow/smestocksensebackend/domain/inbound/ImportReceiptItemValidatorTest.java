package com.smartflow.smestocksensebackend.domain.inbound;

import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ImportReceiptDetailRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ImportReceiptItemValidatorTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private ImportReceiptDetailRepository importReceiptDetailRepository;

    private ImportReceiptItemValidator validator;
    private Product product;

    @BeforeEach
    void setUp() {
        validator = new ImportReceiptItemValidator(productRepository, importReceiptDetailRepository);
        product = product(25L, ProductStatus.HOAT_DONG);
    }

    @Test
    void validateForCreate_withValidProduct_shouldPass() {
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.existsByDocumentIdAndProductId(123L, 25L)).thenReturn(false);

        Product validated = validator.validateForCreate(123L, validRequest());

        assertSame(product, validated);
    }

    @Test
    void validateForCreate_withMissingProduct_shouldThrowNotFoundException() {
        when(productRepository.findById(25L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> validator.validateForCreate(123L, validRequest()));
    }

    @Test
    void validateForCreate_withInactiveProduct_shouldThrowBadRequestException() {
        product.setStatus(ProductStatus.NGUNG_HOAT_DONG);
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));

        assertThrows(BadRequestException.class, () -> validator.validateForCreate(123L, validRequest()));
    }

    @Test
    void validateForCreate_withNullQuantity_shouldThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> validator.validateForCreate(
                123L,
                new AddImportReceiptItemRequest(25L, null, BigDecimal.ONE, null)
        ));
    }

    @Test
    void validateForCreate_withZeroQuantity_shouldThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> validator.validateForCreate(
                123L,
                new AddImportReceiptItemRequest(25L, 0, BigDecimal.ONE, null)
        ));
    }

    @Test
    void validateForCreate_withNegativeQuantity_shouldThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> validator.validateForCreate(
                123L,
                new AddImportReceiptItemRequest(25L, -1, BigDecimal.ONE, null)
        ));
    }

    @Test
    void validateForCreate_withNullUnitPrice_shouldThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> validator.validateForCreate(
                123L,
                new AddImportReceiptItemRequest(25L, 1, null, null)
        ));
    }

    @Test
    void validateForCreate_withZeroUnitPrice_shouldPass() {
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.existsByDocumentIdAndProductId(123L, 25L)).thenReturn(false);

        Product validated = validator.validateForCreate(
                123L,
                new AddImportReceiptItemRequest(25L, 1, BigDecimal.ZERO, null)
        );

        assertSame(product, validated);
    }

    @Test
    void validateForCreate_withNegativeUnitPrice_shouldThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> validator.validateForCreate(
                123L,
                new AddImportReceiptItemRequest(25L, 1, new BigDecimal("-0.01"), null)
        ));
    }

    @Test
    void validateForCreate_withTooLongNote_shouldThrowBadRequestException() {
        assertThrows(BadRequestException.class, () -> validator.validateForCreate(
                123L,
                new AddImportReceiptItemRequest(25L, 1, BigDecimal.ONE, "a".repeat(256))
        ));
    }

    @Test
    void validateForCreate_withDuplicateProduct_shouldThrowConflictException() {
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.existsByDocumentIdAndProductId(123L, 25L)).thenReturn(true);

        assertThrows(ConflictException.class, () -> validator.validateForCreate(123L, validRequest()));
    }

    @Test
    void validateForCreate_withNonDuplicateProduct_shouldPass() {
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.existsByDocumentIdAndProductId(123L, 25L)).thenReturn(false);

        Product validated = validator.validateForCreate(123L, validRequest());

        assertEquals(25L, validated.getId());
    }

    @Test
    void validateForUpdate_withSameDetailProduct_shouldNotTreatAsDuplicate() {
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.existsByDocumentIdAndProductIdAndIdNot(123L, 25L, 1001L))
                .thenReturn(false);

        Product validated = validator.validateForUpdate(123L, 1001L, validRequest());

        assertSame(product, validated);
    }

    @Test
    void validateForUpdate_withProductOnAnotherDetail_shouldThrowConflictException() {
        when(productRepository.findById(25L)).thenReturn(Optional.of(product));
        when(importReceiptDetailRepository.existsByDocumentIdAndProductIdAndIdNot(123L, 25L, 1001L))
                .thenReturn(true);

        assertThrows(ConflictException.class, () -> validator.validateForUpdate(123L, 1001L, validRequest()));
    }

    private AddImportReceiptItemRequest validRequest() {
        return new AddImportReceiptItemRequest(25L, 10, new BigDecimal("125000"), "Lo hang thang 6");
    }

    private Product product(Long id, ProductStatus status) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        return product;
    }
}
