package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.domain.outbound.ExportReceiptStatePolicy;
import com.smartflow.smestocksensebackend.dto.request.outbound.ExportReceiptItemRequest;
import com.smartflow.smestocksensebackend.dto.response.outbound.ExportReceiptItemResponse;
import com.smartflow.smestocksensebackend.dto.response.outbound.StockAvailabilityResponse;
import com.smartflow.smestocksensebackend.entity.*;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportReceiptItemServiceImpl {

    private final ExportReceiptRepository receiptRepository;
    private final ExportReceiptItemRepository itemRepository;
    private final ProductRepository productRepository;
    private final InventoryLevelRepository inventoryRepository;

    @Transactional(readOnly = true)
    public List<ExportReceiptItemResponse> list(Long receiptId) {
        ExportReceipt receipt = findEditableOrReadableReceipt(receiptId);
        return itemRepository.findByExportReceiptIdOrderByIdAsc(receiptId).stream()
                .map(item -> ExportReceiptItemResponse.from(item,
                        available(item.getProduct().getId(), receipt.getWarehouse().getId())))
                .toList();
    }

    @Transactional
    public ExportReceiptItemResponse add(Long receiptId, ExportReceiptItemRequest request) {
        ExportReceipt receipt = findEditableReceipt(receiptId);
        Product product = activeProduct(request.productId());
        if (itemRepository.existsByExportReceiptIdAndProductId(receiptId, request.productId())) {
            throw new ConflictException("Sản phẩm đã có trong phiếu xuất.");
        }
        validateQuantity(request.quantity(), available(product.getId(), receipt.getWarehouse().getId()));
        ExportReceiptItem item = new ExportReceiptItem();
        item.setExportReceipt(receipt);
        item.setProduct(product);
        apply(item, request);
        ExportReceiptItem saved = itemRepository.saveAndFlush(item);
        recalculateTotal(receipt);
        return ExportReceiptItemResponse.from(saved, available(product.getId(), receipt.getWarehouse().getId()));
    }

    public ExportReceiptItem addItemToReceipt(Long receiptId, Long productId, int quantity) {
        add(receiptId, new ExportReceiptItemRequest(productId, quantity, BigDecimal.ZERO, null));
        return null;
    }

    @Transactional
    public ExportReceiptItemResponse update(Long receiptId, Long itemId, ExportReceiptItemRequest request) {
        ExportReceipt receipt = findEditableReceipt(receiptId);
        ExportReceiptItem item = itemRepository.findByIdAndExportReceiptId(itemId, receiptId)
                .orElseThrow(() -> new NotFoundException("Dòng sản phẩm không tồn tại."));
        if (!item.getProduct().getId().equals(request.productId())
                && itemRepository.existsByExportReceiptIdAndProductId(receiptId, request.productId())) {
            throw new ConflictException("Sản phẩm đã có trong phiếu xuất.");
        }
        Product product = activeProduct(request.productId());
        int stock = available(product.getId(), receipt.getWarehouse().getId());
        validateQuantity(request.quantity(), stock);
        item.setProduct(product);
        apply(item, request);
        ExportReceiptItem saved = itemRepository.saveAndFlush(item);
        recalculateTotal(receipt);
        return ExportReceiptItemResponse.from(saved, stock);
    }

    @Transactional
    public void delete(Long receiptId, Long itemId) {
        ExportReceipt receipt = findEditableReceipt(receiptId);
        ExportReceiptItem item = itemRepository.findByIdAndExportReceiptId(itemId, receiptId)
                .orElseThrow(() -> new NotFoundException("Dòng sản phẩm không tồn tại."));
        itemRepository.delete(item);
        itemRepository.flush();
        recalculateTotal(receipt);
    }

    @Transactional(readOnly = true)
    public StockAvailabilityResponse availability(Long receiptId, Long productId) {
        ExportReceipt receipt = findEditableOrReadableReceipt(receiptId);
        activeProduct(productId);
        return new StockAvailabilityResponse(receipt.getWarehouse().getId(), productId,
                available(productId, receipt.getWarehouse().getId()));
    }

    private void apply(ExportReceiptItem item, ExportReceiptItemRequest request) {
        BigDecimal price = request.unitPrice() == null ? BigDecimal.ZERO : request.unitPrice();
        item.setQuantity(request.quantity());
        item.setUnitPrice(price);
        item.setLineTotal(price.multiply(BigDecimal.valueOf(request.quantity())));
        item.setNote(request.note());
    }

    private void recalculateTotal(ExportReceipt receipt) {
        BigDecimal total = itemRepository.findByExportReceiptIdOrderByIdAsc(receipt.getId()).stream()
                .map(ExportReceiptItem::getLineTotal)
                .filter(value -> value != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        receipt.setTotalAmount(total);
        receiptRepository.saveAndFlush(receipt);
    }

    private ExportReceipt findEditableReceipt(Long id) {
        ExportReceipt receipt = findEditableOrReadableReceipt(id);
        if (!ExportReceiptStatePolicy.isEditable(receipt.getStatus())) {
            throw new BadRequestException("Chỉ được sửa dòng khi phiếu ở trạng thái NHAP hoặc TU_CHOI.");
        }
        return receipt;
    }

    private ExportReceipt findEditableOrReadableReceipt(Long id) {
        ExportReceipt receipt = receiptRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Phiếu xuất không tồn tại."));
        Object principal = SecurityContextHolder.getContext().getAuthentication() != null
                ? SecurityContextHolder.getContext().getAuthentication().getPrincipal() : null;
        if (!(principal instanceof Employee actor)) {
            throw new AuthenticationCredentialsNotFoundException("Chưa xác thực.");
        }
        boolean owner = receipt.getCreatedBy() != null && actor.getId().equals(receipt.getCreatedBy().getId());
        boolean admin = actor.getRole() != null && actor.getRole().getCode() == RoleCode.ADMIN;
        if (!owner && !admin) {
            throw new MissingRoleException("Bạn không có quyền thao tác phiếu xuất này.");
        }
        return receipt;
    }

    private Product activeProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Sản phẩm không tồn tại."));
        if (product.getStatus() != ProductStatus.HOAT_DONG) {
            throw new BadRequestException("Sản phẩm không hoạt động.");
        }
        return product;
    }

    private int available(Long productId, Long warehouseId) {
        return inventoryRepository.findByProductIdAndWarehouseId(productId, warehouseId)
                .map(InventoryLevel::getQuantity).orElse(0);
    }

    private void validateQuantity(Integer quantity, int stock) {
        if (quantity == null || quantity <= 0) {
            throw new BadRequestException("Số lượng xuất phải lớn hơn 0.");
        }
        if (quantity > stock) {
            throw new BadRequestException("Số lượng xuất vượt tồn kho khả dụng (" + stock + ").");
        }
    }
}
