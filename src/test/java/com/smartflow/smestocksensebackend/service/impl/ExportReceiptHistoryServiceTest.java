package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptDetailResponse;
import com.smartflow.smestocksensebackend.dto.outbound.ExportReceiptHistoryResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.entity.ExportReceipt;
import com.smartflow.smestocksensebackend.entity.ExportReceiptAction;
import com.smartflow.smestocksensebackend.entity.ExportReceiptDetail;
import com.smartflow.smestocksensebackend.entity.ExportReceiptHistory;
import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.ExportReceiptDetailRepository;
import com.smartflow.smestocksensebackend.repository.ExportReceiptHistoryRepository;
import com.smartflow.smestocksensebackend.repository.ExportReceiptRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** BUG-02: lịch sử duyệt phiếu xuất phải đọc được cả hành động cũ đã lưu ngoài enum hiện tại. */
@ExtendWith(MockitoExtension.class)
class ExportReceiptHistoryServiceTest {

    @Mock
    private ExportReceiptRepository exportReceiptRepository;

    @Mock
    private ExportReceiptDetailRepository exportReceiptDetailRepository;

    @Mock
    private InventoryLevelRepository inventoryLevelRepository;

    @Mock
    private ExportReceiptHistoryRepository exportReceiptHistoryRepository;

    @InjectMocks
    private ExportReceiptServiceImpl exportReceiptService;

    private Employee manager;
    private Employee creator;
    private ExportReceipt receipt;

    @BeforeEach
    void setUp() {
        manager = employeeWith(7L, RoleCode.MANAGER, "Tran Thi Quan Ly");
        creator = employeeWith(12L, RoleCode.EMPLOYEE, "Nguyen Van Nhan Vien");

        Warehouse warehouse = new Warehouse();
        warehouse.setId(1L);
        warehouse.setName("Kho tong");

        receipt = new ExportReceipt();
        receipt.setId(1009L);
        receipt.setCode("FD-PX-1");
        receipt.setStatus(ExportReceiptStatus.CHO_DUYET);
        receipt.setWarehouse(warehouse);
        receipt.setCreatedBy(creator);

        ReflectionTestUtils.setField(exportReceiptService, "exportReceiptHistoryRepository",
                exportReceiptHistoryRepository);
        authenticate(manager);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticate(Employee employee) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(new UsernamePasswordAuthenticationToken(employee, null, List.of()));
        SecurityContextHolder.setContext(context);
    }

    private Employee employeeWith(Long id, RoleCode roleCode, String fullName) {
        Role role = new Role();
        role.setCode(roleCode);
        Employee employee = new Employee();
        employee.setId(id);
        employee.setFullName(fullName);
        employee.setStatus(EmployeeStatus.HOAT_DONG);
        employee.setRole(role);
        return employee;
    }

    private ExportReceiptHistory history(Long id, ExportReceiptAction action, Employee actor, String note,
            LocalDateTime createdAt) {
        ExportReceiptHistory row = new ExportReceiptHistory();
        row.setId(id);
        row.setDocument(receipt);
        row.setActor(actor);
        row.setAction(action);
        row.setNote(note);
        row.setCreatedAt(createdAt);
        return row;
    }

    /** Mô phỏng hàng lịch sử cũ do Hibernate nạp trực tiếp vào field, giá trị nằm ngoài enum hiện tại. */
    private ExportReceiptHistory legacyHistory(Long id, String rawAction, Employee actor, String note,
            LocalDateTime createdAt) {
        ExportReceiptHistory row = history(id, ExportReceiptAction.DUYET, actor, note, createdAt);
        ReflectionTestUtils.setField(row, "action", rawAction);
        return row;
    }

    @Test
    void getHistory_currentOneLevelHistoryShouldMapInRepositoryChronology() {
        LocalDateTime submittedAt = LocalDateTime.of(2026, 8, 20, 10, 0);
        LocalDateTime approvedAt = LocalDateTime.of(2026, 8, 20, 11, 30);
        LocalDateTime completedAt = LocalDateTime.of(2026, 8, 20, 16, 0);
        when(exportReceiptRepository.findById(1009L)).thenReturn(Optional.of(receipt));
        when(exportReceiptHistoryRepository.findByDocumentIdOrderByCreatedAtDesc(1009L)).thenReturn(List.of(
                history(3L, ExportReceiptAction.HOAN_THANH, creator, null, completedAt),
                history(2L, ExportReceiptAction.DUYET, manager, null, approvedAt),
                history(1L, ExportReceiptAction.GUI_DUYET, creator, "Gui duyet", submittedAt)));

        List<ExportReceiptHistoryResponse> history = exportReceiptService.getHistory(1009L);

        List<String> actions = history.stream().map(ExportReceiptHistoryResponse::action).toList();
        assertEquals(List.of("HOAN_THANH", "DUYET", "GUI_DUYET"), actions);
        assertFalse(actions.stream().anyMatch(action -> action.startsWith("DUYET_CAP_")));
        assertEquals(List.of(completedAt, approvedAt, submittedAt),
                history.stream().map(ExportReceiptHistoryResponse::createdAt).toList());
        assertEquals(1009L, history.get(0).receiptId());
        assertEquals("Tran Thi Quan Ly", history.get(1).actorName());
        assertEquals("Gui duyet", history.get(2).note());
    }

    @Test
    void getHistory_legacyActionOutsideCurrentEnumShouldStillBeReturned() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 22, 15, 0);
        when(exportReceiptRepository.findById(1009L)).thenReturn(Optional.of(receipt));
        when(exportReceiptHistoryRepository.findByDocumentIdOrderByCreatedAtDesc(1009L))
                .thenReturn(List.of(legacyHistory(3548L, "TAO", creator, "Final demo outbound", createdAt)));

        List<ExportReceiptHistoryResponse> history = exportReceiptService.getHistory(1009L);

        assertEquals(1, history.size());
        assertEquals("TAO", history.get(0).action());
        assertEquals("Nguyen Van Nhan Vien", history.get(0).actorName());
        assertEquals("Final demo outbound", history.get(0).note());
        assertEquals(createdAt, history.get(0).createdAt());
    }

    @Test
    void getHistory_rowWithoutActorShouldMapNullActorName() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 8, 22, 15, 30);
        when(exportReceiptRepository.findById(1009L)).thenReturn(Optional.of(receipt));
        when(exportReceiptHistoryRepository.findByDocumentIdOrderByCreatedAtDesc(1009L))
                .thenReturn(List.of(history(9L, ExportReceiptAction.HUY, null, "Huy phieu", createdAt)));

        List<ExportReceiptHistoryResponse> history = exportReceiptService.getHistory(1009L);

        assertEquals(1, history.size());
        assertNull(history.get(0).actorName());
        assertEquals("HUY", history.get(0).action());
    }

    @Test
    void getHistory_missingReceiptShouldThrowNotFound() {
        when(exportReceiptRepository.findById(9999L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> exportReceiptService.getHistory(9999L));
        verify(exportReceiptHistoryRepository, never()).findByDocumentIdOrderByCreatedAtDesc(any());
    }

    @Test
    void getHistory_employeeReadingOtherReceiptShouldThrowMissingRole() {
        authenticate(employeeWith(99L, RoleCode.EMPLOYEE, "Nhan vien khac"));
        when(exportReceiptRepository.findById(1009L)).thenReturn(Optional.of(receipt));

        assertThrows(MissingRoleException.class, () -> exportReceiptService.getHistory(1009L));
        verify(exportReceiptHistoryRepository, never()).findByDocumentIdOrderByCreatedAtDesc(any());
    }

    @Test
    void getHistory_creatorShouldReadOwnReceiptHistory() {
        authenticate(creator);
        when(exportReceiptRepository.findById(1009L)).thenReturn(Optional.of(receipt));
        when(exportReceiptHistoryRepository.findByDocumentIdOrderByCreatedAtDesc(1009L))
                .thenReturn(List.of(legacyHistory(3548L, "TAO", creator, "Final demo outbound",
                        LocalDateTime.of(2026, 8, 22, 15, 0))));

        assertEquals(1, exportReceiptService.getHistory(1009L).size());
    }

    @Test
    void approve_shouldWriteExactlyOneApprovalHistoryRow() {
        Product product = new Product();
        product.setId(10L);
        ExportReceiptDetail detail = new ExportReceiptDetail();
        detail.setProduct(product);
        detail.setQuantity(5);
        InventoryLevel inventoryLevel = new InventoryLevel();
        inventoryLevel.setQuantity(8);
        when(exportReceiptRepository.findById(1009L)).thenReturn(Optional.of(receipt));
        when(exportReceiptRepository.saveAndFlush(any(ExportReceipt.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(exportReceiptDetailRepository.findByExportReceiptIdOrderByIdAsc(1009L)).thenReturn(List.of(detail));
        when(inventoryLevelRepository.findByProductIdAndWarehouseId(10L, 1L)).thenReturn(Optional.of(inventoryLevel));

        ExportReceiptDetailResponse response = exportReceiptService.approve(1009L);

        assertEquals("DA_DUYET", response.status());
        ArgumentCaptor<ExportReceiptHistory> captor = ArgumentCaptor.forClass(ExportReceiptHistory.class);
        verify(exportReceiptHistoryRepository).save(captor.capture());
        assertEquals("DUYET", captor.getValue().getAction());
    }
}
