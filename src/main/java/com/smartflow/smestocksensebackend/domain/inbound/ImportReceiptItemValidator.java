package com.smartflow.smestocksensebackend.domain.inbound;

import com.smartflow.smestocksensebackend.dto.inbound.AddImportReceiptItemRequest;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ImportReceiptDetailRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class ImportReceiptItemValidator {

    private static final int MAX_NOTE_LENGTH = 255;

    private final ProductRepository productRepository;
    private final ImportReceiptDetailRepository importReceiptDetailRepository;

    public void validateRequestFields(AddImportReceiptItemRequest request) {
        if (request.productId() == null) {
            throw new BadRequestException("productId khong duoc de trong.");
        }
        if (request.quantity() == null || request.quantity() <= 0) {
            throw new BadRequestException("quantity phai lon hon 0.");
        }
        if (request.unitPrice() == null || request.unitPrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new BadRequestException("unitPrice phai lon hon hoac bang 0.");
        }
        if (request.note() != null && request.note().length() > MAX_NOTE_LENGTH) {
            throw new BadRequestException("note khong duoc vuot qua 255 ky tu.");
        }
    }

    public Product validateForCreate(Long receiptId, AddImportReceiptItemRequest request) {
        Product product = validateProductAndSimpleRules(request);
        if (importReceiptDetailRepository.existsByDocumentIdAndProductId(receiptId, request.productId())) {
            throw duplicateProductException();
        }
        return product;
    }

    public Product validateForUpdate(Long receiptId, Long detailId, AddImportReceiptItemRequest request) {
        Product product = validateProductAndSimpleRules(request);
        if (importReceiptDetailRepository.existsByDocumentIdAndProductIdAndIdNot(
                receiptId,
                request.productId(),
                detailId
        )) {
            throw duplicateProductException();
        }
        return product;
    }

    private Product validateProductAndSimpleRules(AddImportReceiptItemRequest request) {
        validateRequestFields(request);

        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new NotFoundException("San pham khong ton tai."));
        if (product.getStatus() != ProductStatus.HOAT_DONG) {
            throw new BadRequestException("San pham khong hoat dong.");
        }

        return product;
    }

    public ConflictException duplicateProductException() {
        return new ConflictException("San pham da ton tai trong phieu nhap.");
    }
}
