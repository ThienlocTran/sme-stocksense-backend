package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ExportReceipt;
import com.smartflow.smestocksensebackend.entity.ExportReceiptItem;
import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ExportReceiptItemRepository;
import com.smartflow.smestocksensebackend.repository.ExportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TDD Unit Test cho Task T126 – API thêm dòng sản phẩm vào phiếu xuất kho.
 *
 * ═══════════════════════════════════════════════════════════════
 * Logic nghiệp vụ kỳ vọng (spec cho Service chưa viết):
 * ═══════════════════════════════════════════════════════════════
 * addItemToReceipt(Long receiptId, Long productId, int quantity):
 *   1. Tìm ExportReceipt theo receiptId       → NotFoundException nếu không có
 *   2. Kiểm tra trạng thái: chỉ cho phép khi DRAFT hoặc REJECTED
 *      → BadRequestException nếu trạng thái khác
 *   3. Tìm Product theo productId              → NotFoundException nếu không có
 *   4. Kiểm tra tồn kho khả dụng:
 *      - Tìm InventoryLevel(productId, receipt.warehouse.id)
 *      - availableStock = inventoryLevel.quantity (MVP: không tính reserved)
 *      - Nếu quantity > availableStock → BadRequestException
 *   5. Tạo và lưu ExportReceiptItem
 * ═══════════════════════════════════════════════════════════════
 *
 * TDD RED PHASE: File này sẽ KHÔNG compile cho đến khi tạo ExportReceiptServiceImpl.
 * Dòng "new ExportReceiptServiceImpl(...)" sẽ báo lỗi – đây là hành vi mong muốn.
 */
@ExtendWith(MockitoExtension.class)
class ExportReceiptItemServiceTest {

    // ─── Mock dependencies ──────────────────────────────────────

    @Mock
    private ExportReceiptRepository exportReceiptRepository;

    @Mock
    private ExportReceiptItemRepository exportReceiptItemRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private InventoryLevelRepository inventoryLevelRepository;

    // ─── SUT (System Under Test) ────────────────────────────────
    private ExportReceiptItemServiceImpl exportReceiptService;

    // ─── Test fixtures ──────────────────────────────────────────

    private Employee owner;
    private Warehouse warehouse;
    private ExportReceipt receipt;
    private Product product;
    private InventoryLevel inventoryLevel;

    private static final Long RECEIPT_ID = 1L;
    private static final Long PRODUCT_ID = 10L;
    private static final Long WAREHOUSE_ID = 100L;
    private static final int AVAILABLE_STOCK = 100;

    @BeforeEach
    void setUp() {
        warehouse = warehouse(WAREHOUSE_ID);
        owner = employee(5L, RoleCode.EMPLOYEE);
        product = product(PRODUCT_ID, ProductStatus.HOAT_DONG);
        receipt = exportReceipt(RECEIPT_ID, owner, warehouse, ExportReceiptStatus.NHAP);
        inventoryLevel = inventoryLevel(PRODUCT_ID, WAREHOUSE_ID, AVAILABLE_STOCK);

        authenticate(owner);

        exportReceiptService = new ExportReceiptItemServiceImpl(
                exportReceiptRepository,
                exportReceiptItemRepository,
                productRepository,
                inventoryLevelRepository
        );
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // ═══════════════════════════════════════════════════════════════
    //  HAPPY PATH – Thêm sản phẩm thành công
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Thêm sản phẩm thành công")
    class AddItem_Success {

        @Test
        @DisplayName("Thêm sản phẩm khi phiếu DRAFT và số lượng <= tồn kho → lưu ExportReceiptItem đúng")
        void shouldSaveItemWhenDraftAndStockSufficient() {
            // Arrange: phiếu DRAFT, tồn kho = 100, xuất = 30
            int exportQty = 30;
            stubHappyPath();

            // Act
            exportReceiptService.addItemToReceipt(RECEIPT_ID, PRODUCT_ID, exportQty);

            // Assert: ExportReceiptItem được save với đúng dữ liệu
            ArgumentCaptor<ExportReceiptItem> captor = ArgumentCaptor.forClass(ExportReceiptItem.class);
            verify(exportReceiptItemRepository).saveAndFlush(captor.capture());

            ExportReceiptItem saved = captor.getValue();
            assertAll("ExportReceiptItem phải được ghi đúng dữ liệu",
                    () -> assertSame(receipt, saved.getExportReceipt(),
                            "FK phải trỏ đúng về phiếu xuất"),
                    () -> assertSame(product, saved.getProduct(),
                            "FK phải trỏ đúng về sản phẩm"),
                    () -> assertEquals(exportQty, saved.getQuantity(),
                            "Số lượng xuất phải đúng")
            );
        }

        @Test
        @DisplayName("Thêm sản phẩm khi phiếu REJECTED (sửa lại phiếu bị từ chối) → vẫn thành công")
        void shouldSaveItemWhenRejectedAndStockSufficient() {
            // Arrange: phiếu REJECTED → cho phép sửa lại
            int exportQty = 20;
            receipt.setStatus(ExportReceiptStatus.TU_CHOI);
            stubHappyPath();

            // Act
            exportReceiptService.addItemToReceipt(RECEIPT_ID, PRODUCT_ID, exportQty);

            // Assert: vẫn lưu thành công
            ArgumentCaptor<ExportReceiptItem> captor = ArgumentCaptor.forClass(ExportReceiptItem.class);
            verify(exportReceiptItemRepository).saveAndFlush(captor.capture());

            ExportReceiptItem saved = captor.getValue();
            assertSame(receipt, saved.getExportReceipt());
            assertEquals(exportQty, saved.getQuantity());
        }

        @Test
        @DisplayName("Xuất đúng bằng tồn kho (boundary case: quantity == availableStock) → thành công")
        void shouldSaveItemWhenQuantityEqualsStock() {
            // Arrange: tồn kho = 100, xuất = 100 (vừa đúng)
            int exportQty = AVAILABLE_STOCK;
            stubHappyPath();

            // Act – không ném exception
            exportReceiptService.addItemToReceipt(RECEIPT_ID, PRODUCT_ID, exportQty);

            // Assert
            verify(exportReceiptItemRepository).saveAndFlush(any(ExportReceiptItem.class));
        }

        private void stubHappyPath() {
            when(exportReceiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(inventoryLevelRepository.findByProductIdAndWarehouseId(PRODUCT_ID, WAREHOUSE_ID))
                    .thenReturn(Optional.of(inventoryLevel));
            when(exportReceiptItemRepository.saveAndFlush(any(ExportReceiptItem.class)))
                    .thenAnswer(invocation -> {
                        ExportReceiptItem item = invocation.getArgument(0);
                        item.setId(1001L);
                        return item;
                    });
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  FAILURE CASE 1 – Trạng thái phiếu không cho phép thêm item
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Thất bại: Trạng thái phiếu không hợp lệ")
    class AddItem_InvalidStatus {

        @Test
        @DisplayName("Phiếu đang PENDING_APPROVAL_L1 → BadRequestException, KHÔNG lưu item")
        void shouldThrowWhenPendingApprovalL1() {
            receipt.setStatus(ExportReceiptStatus.CHO_DUYET_CAP_1);
            when(exportReceiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> exportReceiptService.addItemToReceipt(RECEIPT_ID, PRODUCT_ID, 10));

            assertNotNull(ex.getMessage(), "Thông báo lỗi không được null");
            verify(exportReceiptItemRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Phiếu đang PENDING_APPROVAL_L2 → BadRequestException, KHÔNG lưu item")
        void shouldThrowWhenPendingApprovalL2() {
            receipt.setStatus(ExportReceiptStatus.CHO_DUYET_CAP_2);
            when(exportReceiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> exportReceiptService.addItemToReceipt(RECEIPT_ID, PRODUCT_ID, 10));

            assertNotNull(ex.getMessage());
            verify(exportReceiptItemRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Phiếu đã COMPLETED → BadRequestException, KHÔNG lưu item")
        void shouldThrowWhenCompleted() {
            receipt.setStatus(ExportReceiptStatus.HOAN_THANH);
            when(exportReceiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));

            assertThrows(BadRequestException.class,
                    () -> exportReceiptService.addItemToReceipt(RECEIPT_ID, PRODUCT_ID, 10));

            verify(exportReceiptItemRepository, never()).saveAndFlush(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  FAILURE CASE 2 – Số lượng xuất vượt tồn kho khả dụng
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Thất bại: Vượt tồn kho khả dụng")
    class AddItem_ExceedsStock {

        @Test
        @DisplayName("Số lượng xuất > tồn kho hiện tại → BadRequestException, KHÔNG lưu item")
        void shouldThrowWhenQuantityExceedsAvailableStock() {
            // Arrange: tồn kho = 100, xuất = 150
            int exportQty = AVAILABLE_STOCK + 50;
            when(exportReceiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(inventoryLevelRepository.findByProductIdAndWarehouseId(PRODUCT_ID, WAREHOUSE_ID))
                    .thenReturn(Optional.of(inventoryLevel));

            // Act & Assert
            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> exportReceiptService.addItemToReceipt(RECEIPT_ID, PRODUCT_ID, exportQty));

            assertTrue(ex.getMessage().contains("tồn kho") || ex.getMessage().contains("không đủ")
                            || ex.getMessage().contains("vượt"),
                    "Thông báo lỗi phải chỉ ra lý do: tồn kho không đủ. Actual: " + ex.getMessage());

            verify(exportReceiptItemRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Sản phẩm chưa có tồn kho tại kho này (InventoryLevel not found) → BadRequestException")
        void shouldThrowWhenNoInventoryLevelExists() {
            // Arrange: không tìm thấy bản ghi tồn kho → coi như tồn = 0 → bất kỳ qty > 0 đều fail
            when(exportReceiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));
            when(productRepository.findById(PRODUCT_ID)).thenReturn(Optional.of(product));
            when(inventoryLevelRepository.findByProductIdAndWarehouseId(PRODUCT_ID, WAREHOUSE_ID))
                    .thenReturn(Optional.empty());

            assertThrows(BadRequestException.class,
                    () -> exportReceiptService.addItemToReceipt(RECEIPT_ID, PRODUCT_ID, 1));

            verify(exportReceiptItemRepository, never()).saveAndFlush(any());
        }
    }

    // ═══════════════════════════════════════════════════════════════
    //  VALIDATION – Dữ liệu đầu vào không hợp lệ
    // ═══════════════════════════════════════════════════════════════

    @Nested
    @DisplayName("Thất bại: Validation đầu vào")
    class AddItem_Validation {

        @Test
        @DisplayName("Phiếu xuất không tồn tại → NotFoundException")
        void shouldThrowWhenReceiptNotFound() {
            when(exportReceiptRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> exportReceiptService.addItemToReceipt(999L, PRODUCT_ID, 10));

            verify(exportReceiptItemRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("Sản phẩm không tồn tại → NotFoundException")
        void shouldThrowWhenProductNotFound() {
            when(exportReceiptRepository.findById(RECEIPT_ID)).thenReturn(Optional.of(receipt));
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(NotFoundException.class,
                    () -> exportReceiptService.addItemToReceipt(RECEIPT_ID, 999L, 10));

            verify(exportReceiptItemRepository, never()).saveAndFlush(any());
        }
    }

    // ─── Test fixture factories ─────────────────────────────────

    private void authenticate(Employee employee) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(employee, null, List.of())
        );
    }

    private Employee employee(Long id, RoleCode roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        Employee emp = new Employee();
        emp.setId(id);
        emp.setFullName("Nguyen Van Test");
        emp.setRole(role);
        emp.setStatus(EmployeeStatus.HOAT_DONG);
        return emp;
    }

    private Warehouse warehouse(Long id) {
        Warehouse wh = new Warehouse();
        wh.setId(id);
        wh.setCode("KHO-01");
        wh.setName("Kho chinh");
        return wh;
    }

    private ExportReceipt exportReceipt(Long id, Employee creator, Warehouse wh, ExportReceiptStatus status) {
        ExportReceipt er = new ExportReceipt();
        er.setId(id);
        er.setCode("PX-001");
        er.setCreatedBy(creator);
        er.setWarehouse(wh);
        er.setStatus(status);
        return er;
    }

    private Product product(Long id, ProductStatus status) {
        Product p = new Product();
        p.setId(id);
        p.setCode("SP-001");
        p.setName("San pham test");
        p.setUnit("cai");
        p.setStatus(status);
        return p;
    }

    private InventoryLevel inventoryLevel(Long productId, Long warehouseId, int quantity) {
        Product p = new Product();
        p.setId(productId);
        Warehouse wh = new Warehouse();
        wh.setId(warehouseId);

        InventoryLevel level = new InventoryLevel();
        level.setId(500L);
        level.setProduct(p);
        level.setWarehouse(wh);
        level.setQuantity(quantity);
        return level;
    }
}
