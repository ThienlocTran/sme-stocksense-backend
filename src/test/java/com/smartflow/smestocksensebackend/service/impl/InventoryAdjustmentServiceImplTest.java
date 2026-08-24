package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventoryadjustment.InventoryAdjustmentResponse;
import com.smartflow.smestocksensebackend.dto.inventoryadjustment.RejectInventoryAdjustmentRequest;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.InventoryAdjustment;
import com.smartflow.smestocksensebackend.entity.InventoryAdjustmentStatus;
import com.smartflow.smestocksensebackend.entity.InventoryCount;
import com.smartflow.smestocksensebackend.entity.InventoryCountDetail;
import com.smartflow.smestocksensebackend.entity.InventoryCountStatus;
import com.smartflow.smestocksensebackend.entity.InventoryLevel;
import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.ConflictException;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.repository.InventoryAdjustmentRepository;
import com.smartflow.smestocksensebackend.repository.InventoryCountDetailRepository;
import com.smartflow.smestocksensebackend.repository.InventoryCountRepository;
import com.smartflow.smestocksensebackend.repository.InventoryLevelRepository;
import com.smartflow.smestocksensebackend.service.InventoryAdjustmentCodeGenerator;
import com.smartflow.smestocksensebackend.service.InventoryTransactionService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentMatchers;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
@ExtendWith(MockitoExtension.class)
class InventoryAdjustmentServiceImplTest {

    @Mock InventoryAdjustmentRepository adjustmentRepository;
    @Mock InventoryCountRepository countRepository;
    @Mock InventoryCountDetailRepository detailRepository;
    @Mock InventoryLevelRepository inventoryRepository;
    @Mock InventoryTransactionService inventoryTransactionService;
    @Mock InventoryAdjustmentCodeGenerator codeGenerator;

    InventoryAdjustmentServiceImpl service;
    Employee actor;
    InventoryCount count;
    InventoryCountDetail detail;
    AutoCloseable mocks;

    @BeforeEach
    void setup() {
        mocks = MockitoAnnotations.openMocks(this);
        service = new InventoryAdjustmentServiceImpl(adjustmentRepository, countRepository, detailRepository,
                inventoryRepository, inventoryTransactionService, codeGenerator);
        actor = new Employee(); actor.setId(1L); actor.setFullName("Toan");
        actor.setRole(role(RoleCode.EMPLOYEE));
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, List.of()));
        Warehouse warehouse = new Warehouse(); warehouse.setId(2L); warehouse.setName("Kho A");
        count = new InventoryCount(); count.setId(3L); count.setCode("KK-1"); count.setWarehouse(warehouse); count.setStatus(InventoryCountStatus.DANG_KIEM_KE); count.setCreatedBy(actor);
        Product product = new Product(); product.setId(4L); product.setCode("SP4"); product.setName("San pham");
        detail = new InventoryCountDetail(); detail.setId(5L); detail.setInventoryCount(count); detail.setProduct(product);
        detail.setSystemQuantity(10); detail.setActualQuantity(7); detail.setDifferenceQuantity(-3);
        detail.setReason("Hang hong"); detail.setNote("Ghi chu rieng");
    }

    @AfterEach
    void clear() throws Exception {
        SecurityContextHolder.clearContext();
        mocks.close();
    }

    @Test
    void getOrCreateDraft_shouldCreateDraftFromOpenCountWithDiscrepancy() {
        when(adjustmentRepository.findByInventoryCountId(3L)).thenReturn(Optional.empty());
        when(countRepository.findById(3L)).thenReturn(Optional.of(count));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));
        when(codeGenerator.generate()).thenReturn("PDC-20260824-ABCDEF123456");
        when(adjustmentRepository.existsByCodeIgnoreCase("PDC-20260824-ABCDEF123456")).thenReturn(false);
        when(adjustmentRepository.saveAndFlush(any())).thenAnswer(invocation -> {
            InventoryAdjustment adjustment = invocation.getArgument(0);
            adjustment.setId(9L);
            adjustment.setVersion(0L);
            return adjustment;
        });

        InventoryAdjustmentResponse response = service.getOrCreateDraft(3L);

        assertEquals(9L, response.id());
        assertEquals("PDC-20260824-ABCDEF123456", response.code());
        assertEquals("NHAP", response.status());
        assertEquals(3L, response.inventoryCount().id());
        assertEquals(1L, response.createdById());
        assertEquals(1, response.details().size());
        assertEquals("Hang hong", response.details().getFirst().reason());
        assertEquals("Ghi chu rieng", response.details().getFirst().note());
        verify(adjustmentRepository).saveAndFlush(any(InventoryAdjustment.class));
    }

    @Test
    void getOrCreateDraft_shouldReturnExistingHeaderWithoutCreatingSecondOne() {
        InventoryAdjustment existing = adjustment();
        when(adjustmentRepository.findByInventoryCountId(3L)).thenReturn(Optional.of(existing));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));

        InventoryAdjustmentResponse response = service.getOrCreateDraft(3L);

        assertEquals(9L, response.id());
        verify(adjustmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void getOrCreateDraft_shouldRejectNoDiscrepancy() {
        detail.setActualQuantity(10); detail.setDifferenceQuantity(0);
        when(adjustmentRepository.findByInventoryCountId(3L)).thenReturn(Optional.empty());
        when(countRepository.findById(3L)).thenReturn(Optional.of(count));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));

        assertThrows(BadRequestException.class, () -> service.getOrCreateDraft(3L));
        verify(adjustmentRepository, never()).saveAndFlush(any());
    }

    @Test
    void getOrCreateDraft_shouldRejectIncompleteActualQuantity() {
        detail.setActualQuantity(null); detail.setDifferenceQuantity(null);
        when(adjustmentRepository.findByInventoryCountId(3L)).thenReturn(Optional.empty());
        when(countRepository.findById(3L)).thenReturn(Optional.of(count));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));

        assertThrows(BadRequestException.class, () -> service.getOrCreateDraft(3L));
    }

    @Test
    void getOrCreateDraft_shouldRejectClosedOrCancelledCount() {
        when(adjustmentRepository.findByInventoryCountId(3L)).thenReturn(Optional.empty());
        when(countRepository.findById(3L)).thenReturn(Optional.of(count));

        count.setStatus(InventoryCountStatus.DA_CHOT);
        assertThrows(ConflictException.class, () -> service.getOrCreateDraft(3L));

        count.setStatus(InventoryCountStatus.DA_HUY);
        assertThrows(ConflictException.class, () -> service.getOrCreateDraft(3L));
    }

    @Test
    void getOrCreateDraft_shouldRejectUnknownCount() {
        when(adjustmentRepository.findByInventoryCountId(404L)).thenReturn(Optional.empty());
        when(countRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getOrCreateDraft(404L));
    }

    @Test
    void get_shouldReadExistingAdjustmentDetailsFromInventoryCountDetail() {
        InventoryAdjustment existing = adjustment();
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(existing));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));

        InventoryAdjustmentResponse response = service.get(9L);

        assertEquals(9L, response.id());
        assertEquals(-3, response.details().getFirst().differenceQuantity());
        assertEquals("Hang hong", response.details().getFirst().discrepancyReason());
    }

    @Test
    void get_shouldRejectUnknownAdjustment() {
        when(adjustmentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.get(404L));
    }

    @Test
    void getByInventoryCountId_shouldReadExistingAdjustment() {
        InventoryAdjustment existing = adjustment();
        when(adjustmentRepository.findByInventoryCountId(3L)).thenReturn(Optional.of(existing));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));

        assertEquals(9L, service.getByInventoryCountId(3L).id());
    }

    @Test
    void getByInventoryCountId_shouldRejectMissingAdjustment() {
        when(adjustmentRepository.findByInventoryCountId(3L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.getByInventoryCountId(3L));
    }

    @Test
    void response_shouldOnlyReturnDiscrepancyLines() {
        InventoryCountDetail matching = new InventoryCountDetail();
        matching.setId(6L); matching.setInventoryCount(count); matching.setProduct(detail.getProduct());
        matching.setSystemQuantity(10); matching.setActualQuantity(10); matching.setDifferenceQuantity(0);
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(adjustment()));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail, matching));

        InventoryAdjustmentResponse response = service.get(9L);

        assertEquals(1, response.details().size());
        assertTrue(response.details().stream().allMatch(line -> line.differenceQuantity() != 0));
    }

    @Test
    void submit_shouldMoveDraftToPendingApprovalAndPersistSubmitter() {
        InventoryAdjustment adjustment = adjustment();
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(adjustment));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));
        when(adjustmentRepository.saveAndFlush(adjustment)).thenReturn(adjustment);

        InventoryAdjustmentResponse response = service.submit(9L);

        assertEquals("CHO_DUYET", response.status());
        assertEquals(actor, adjustment.getSubmittedBy());
        assertEquals(1L, response.submittedById());
        assertTrue(adjustment.getSubmittedAt() != null);
        assertEquals(InventoryCountStatus.DANG_KIEM_KE, count.getStatus());
    }

    @Test
    void submit_shouldRejectRepeatedSubmit() {
        InventoryAdjustment adjustment = adjustment();
        adjustment.setStatus(InventoryAdjustmentStatus.CHO_DUYET);
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(adjustment));

        assertThrows(ConflictException.class, () -> service.submit(9L));
    }

    @Test
    void submit_shouldResubmitRejectedAndClearDecisionFields() {
        InventoryAdjustment adjustment = adjustment();
        Employee manager = employee(2L, RoleCode.MANAGER);
        adjustment.setStatus(InventoryAdjustmentStatus.TU_CHOI);
        adjustment.setApprovedBy(manager);
        adjustment.setApprovedAt(java.time.LocalDateTime.now().minusDays(1));
        adjustment.setRejectionReason("Sai ly do.");
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(adjustment));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));
        when(adjustmentRepository.saveAndFlush(adjustment)).thenReturn(adjustment);

        InventoryAdjustmentResponse response = service.submit(9L);

        assertEquals("CHO_DUYET", response.status());
        assertEquals(actor, adjustment.getSubmittedBy());
        assertEquals(null, adjustment.getApprovedBy());
        assertEquals(null, adjustment.getApprovedAt());
        assertEquals(null, adjustment.getRejectionReason());
    }

    @Test
    void submit_shouldRejectUnknownAdjustment() {
        when(adjustmentRepository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.submit(404L));
    }

    @Test
    void submit_shouldRejectNoDiscrepancy() {
        detail.setActualQuantity(10); detail.setDifferenceQuantity(0);
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(adjustment()));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));

        assertThrows(BadRequestException.class, () -> service.submit(9L));
    }

    @Test
    void submit_shouldRejectIncompleteActualQuantity() {
        detail.setActualQuantity(null); detail.setDifferenceQuantity(null);
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(adjustment()));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));

        assertThrows(BadRequestException.class, () -> service.submit(9L));
    }

    @Test
    void submit_shouldRejectMissingDiscrepancyReason() {
        detail.setReason(" ");
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(adjustment()));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));

        assertThrows(BadRequestException.class, () -> service.submit(9L));
    }

    @Test
    void submit_shouldRejectUnauthorizedRole() {
        actor.setRole(role(RoleCode.MANAGER));

        assertThrows(AccessDeniedException.class, () -> service.submit(9L));
    }

    @Test
    void submit_shouldRejectWrongOwner() {
        InventoryAdjustment adjustment = adjustment();
        Employee creator = new Employee();
        creator.setId(1L);
        adjustment.setCreatedBy(creator);
        actor.setId(2L);
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(adjustment));

        assertThrows(AccessDeniedException.class, () -> service.submit(9L));
    }

    @Test
    void submit_adminCanSubmitOtherCreatorDraft() {
        actor.setId(2L);
        actor.setRole(role(RoleCode.ADMIN));
        InventoryAdjustment adjustment = adjustment();
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(adjustment));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));
        when(adjustmentRepository.saveAndFlush(adjustment)).thenReturn(adjustment);

        assertEquals("CHO_DUYET", service.submit(9L).status());
    }

    @Test
    void approve_shouldMovePendingToApprovedAndPersistDecisionActor() {
        actor = employee(2L, RoleCode.MANAGER);
        authenticate(actor);
        InventoryAdjustment adjustment = pendingAdjustment();
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(adjustment));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));
        when(adjustmentRepository.saveAndFlush(adjustment)).thenReturn(adjustment);

        InventoryAdjustmentResponse response = service.approve(9L);

        assertEquals("DA_DUYET", response.status());
        assertEquals(actor, adjustment.getApprovedBy());
        assertTrue(adjustment.getApprovedAt() != null);
        assertEquals(InventoryCountStatus.DANG_KIEM_KE, count.getStatus());
    }

    @Test
    void approve_shouldRejectEmployeeRole() {
        assertThrows(AccessDeniedException.class, () -> service.approve(9L));
    }

    @Test
    void approve_shouldRejectWrongStatusOrUnknownId() {
        actor.setRole(role(RoleCode.MANAGER));
        InventoryAdjustment adjustment = adjustment();
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(adjustment));
        assertThrows(ConflictException.class, () -> service.approve(9L));

        when(adjustmentRepository.findById(404L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.approve(404L));
    }

    @Test
    void approve_shouldRejectCreatorOrSubmitterDecisionActor() {
        actor.setRole(role(RoleCode.ADMIN));
        InventoryAdjustment adjustment = pendingAdjustment();
        adjustment.setCreatedBy(actor);
        adjustment.setSubmittedBy(actor);
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(adjustment));

        assertThrows(BadRequestException.class, () -> service.approve(9L));
    }

    @Test
    void reject_shouldMovePendingToRejectedWithTrimmedReason() {
        actor = employee(2L, RoleCode.MANAGER);
        authenticate(actor);
        InventoryAdjustment adjustment = pendingAdjustment();
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(adjustment));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));
        when(adjustmentRepository.saveAndFlush(adjustment)).thenReturn(adjustment);

        InventoryAdjustmentResponse response = service.reject(9L, new RejectInventoryAdjustmentRequest("  Sai so lieu  "));

        assertEquals("TU_CHOI", response.status());
        assertEquals("Sai so lieu", adjustment.getRejectionReason());
        assertEquals(actor, adjustment.getApprovedBy());
        assertTrue(adjustment.getApprovedAt() != null);
    }

    @Test
    void reject_shouldRequireReasonAndRejectDuplicateReject() {
        assertThrows(BadRequestException.class, () -> service.reject(9L, new RejectInventoryAdjustmentRequest(" ")));

        actor = employee(2L, RoleCode.MANAGER);
        authenticate(actor);
        InventoryAdjustment adjustment = pendingAdjustment();
        adjustment.setStatus(InventoryAdjustmentStatus.TU_CHOI);
        when(adjustmentRepository.findById(9L)).thenReturn(Optional.of(adjustment));
        assertThrows(ConflictException.class, () -> service.reject(9L, new RejectInventoryAdjustmentRequest("Ly do.")));
    }

    @Test
    void apply_shouldMoveApprovedToAppliedCloseCountAndUseCurrentStockPlusDifference() {
        actor = employee(2L, RoleCode.MANAGER);
        authenticate(actor);
        InventoryAdjustment adjustment = approvedAdjustment();
        detail.setDifferenceQuantity(-3);
        InventoryLevel stock = stock(20);
        when(adjustmentRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(adjustment));
        when(countRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(count));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));
        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(4L, 2L)).thenReturn(Optional.of(stock));
        when(countRepository.saveAndFlush(count)).thenReturn(count);
        when(adjustmentRepository.saveAndFlush(adjustment)).thenReturn(adjustment);

        InventoryAdjustmentResponse response = service.apply(9L);

        assertEquals("DA_AP_DUNG", response.status());
        assertEquals("DA_CHOT", response.inventoryCount().status());
        assertEquals(17, stock.getQuantity());
        assertEquals(actor, count.getFinalizedBy());
        assertTrue(adjustment.getAppliedAt() != null);
        verify(inventoryTransactionService).recordTransaction(ArgumentMatchers.eq(4L), ArgumentMatchers.eq(2L), ArgumentMatchers.eq(com.smartflow.smestocksensebackend.entity.InventoryTransactionType.DIEU_CHINH_GIAM), ArgumentMatchers.eq(3), ArgumentMatchers.eq(20), ArgumentMatchers.eq(17), ArgumentMatchers.isNull(), ArgumentMatchers.eq("Dieu chinh kiem ke KK-1"));
    }

    @Test
    void apply_positiveDifferenceShouldUseIncreaseTransaction() {
        actor = employee(2L, RoleCode.ADMIN);
        authenticate(actor);
        InventoryAdjustment adjustment = approvedAdjustment();
        detail.setActualQuantity(13); detail.setDifferenceQuantity(3);
        InventoryLevel stock = stock(20);
        when(adjustmentRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(adjustment));
        when(countRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(count));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));
        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(4L, 2L)).thenReturn(Optional.of(stock));
        when(countRepository.saveAndFlush(count)).thenReturn(count);
        when(adjustmentRepository.saveAndFlush(adjustment)).thenReturn(adjustment);

        service.apply(9L);

        assertEquals(23, stock.getQuantity());
        verify(inventoryTransactionService).recordTransaction(ArgumentMatchers.eq(4L), ArgumentMatchers.eq(2L), ArgumentMatchers.eq(com.smartflow.smestocksensebackend.entity.InventoryTransactionType.DIEU_CHINH_TANG), ArgumentMatchers.eq(3), ArgumentMatchers.eq(20), ArgumentMatchers.eq(23), ArgumentMatchers.isNull(), any());
    }

    @Test
    void apply_zeroLineShouldNotCreateTransactionForThatLine() {
        actor = employee(2L, RoleCode.MANAGER);
        authenticate(actor);
        InventoryAdjustment adjustment = approvedAdjustment();
        InventoryCountDetail zero = detail(6L, 10, 10, 0);
        when(adjustmentRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(adjustment));
        when(countRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(count));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail, zero));
        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(4L, 2L)).thenReturn(Optional.of(stock(20)));
        when(countRepository.saveAndFlush(count)).thenReturn(count);
        when(adjustmentRepository.saveAndFlush(adjustment)).thenReturn(adjustment);

        service.apply(9L);

        verify(inventoryTransactionService).recordTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void apply_shouldRejectInvalidStatesAndUnknownId() {
        actor = employee(2L, RoleCode.MANAGER);
        authenticate(actor);
        when(adjustmentRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.apply(404L));

        for (InventoryAdjustmentStatus status : List.of(InventoryAdjustmentStatus.NHAP, InventoryAdjustmentStatus.CHO_DUYET,
                InventoryAdjustmentStatus.TU_CHOI, InventoryAdjustmentStatus.DA_AP_DUNG)) {
            InventoryAdjustment adjustment = approvedAdjustment();
            adjustment.setStatus(status);
            when(adjustmentRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(adjustment));
            assertThrows(ConflictException.class, () -> service.apply(9L));
        }
    }

    @Test
    void apply_shouldRejectEmployeeRoleWithoutMutation() {
        assertThrows(AccessDeniedException.class, () -> service.apply(9L));
        verify(adjustmentRepository, never()).findByIdForUpdate(any());
    }

    @Test
    void apply_shouldRejectClosedCountBeforeMutation() {
        actor = employee(2L, RoleCode.MANAGER);
        authenticate(actor);
        InventoryAdjustment adjustment = approvedAdjustment();
        count.setStatus(InventoryCountStatus.DA_CHOT);
        when(adjustmentRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(adjustment));
        when(countRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(count));

        assertThrows(ConflictException.class, () -> service.apply(9L));
        verify(inventoryRepository, never()).saveAndFlush(any());
        assertEquals(InventoryAdjustmentStatus.DA_DUYET, adjustment.getStatus());
    }

    @Test
    void apply_shouldRejectNegativeResultBeforePersisting() {
        actor = employee(2L, RoleCode.MANAGER);
        authenticate(actor);
        InventoryAdjustment adjustment = approvedAdjustment();
        detail.setDifferenceQuantity(-30);
        InventoryLevel stock = stock(20);
        when(adjustmentRepository.findByIdForUpdate(9L)).thenReturn(Optional.of(adjustment));
        when(countRepository.findByIdForUpdate(3L)).thenReturn(Optional.of(count));
        when(detailRepository.findByInventoryCountIdOrderByIdAsc(3L)).thenReturn(List.of(detail));
        when(inventoryRepository.findByProductIdAndWarehouseIdForUpdate(4L, 2L)).thenReturn(Optional.of(stock));

        assertThrows(ConflictException.class, () -> service.apply(9L));

        assertEquals(20, stock.getQuantity());
        assertEquals(InventoryAdjustmentStatus.DA_DUYET, adjustment.getStatus());
        assertEquals(InventoryCountStatus.DANG_KIEM_KE, count.getStatus());
        verify(inventoryRepository, never()).saveAndFlush(any());
        verify(inventoryTransactionService, never()).recordTransaction(any(), any(), any(), any(), any(), any(), any(), any());
    }

    private InventoryAdjustment adjustment() {
        InventoryAdjustment adjustment = new InventoryAdjustment();
        adjustment.setId(9L);
        adjustment.setCode("PDC-20260824-ABCDEF123456");
        adjustment.setInventoryCount(count);
        adjustment.setStatus(InventoryAdjustmentStatus.NHAP);
        adjustment.setCreatedBy(actor);
        adjustment.setVersion(0L);
        return adjustment;
    }

    private InventoryAdjustment pendingAdjustment() {
        Employee creator = employee(1L, RoleCode.EMPLOYEE);
        InventoryAdjustment adjustment = adjustment();
        adjustment.setCreatedBy(creator);
        adjustment.setSubmittedBy(creator);
        adjustment.setStatus(InventoryAdjustmentStatus.CHO_DUYET);
        return adjustment;
    }

    private InventoryAdjustment approvedAdjustment() {
        InventoryAdjustment adjustment = pendingAdjustment();
        adjustment.setStatus(InventoryAdjustmentStatus.DA_DUYET);
        adjustment.setApprovedBy(employee(2L, RoleCode.MANAGER));
        adjustment.setApprovedAt(java.time.LocalDateTime.now().minusHours(1));
        return adjustment;
    }

    private InventoryLevel stock(int quantity) {
        InventoryLevel stock = new InventoryLevel();
        stock.setProduct(detail.getProduct());
        stock.setWarehouse(count.getWarehouse());
        stock.setQuantity(quantity);
        return stock;
    }

    private InventoryCountDetail detail(Long id, int system, int actual, int difference) {
        InventoryCountDetail line = new InventoryCountDetail();
        line.setId(id);
        line.setInventoryCount(count);
        line.setProduct(detail.getProduct());
        line.setSystemQuantity(system);
        line.setActualQuantity(actual);
        line.setDifferenceQuantity(difference);
        line.setReason("Khong lech");
        return line;
    }

    private Employee employee(Long id, RoleCode roleCode) {
        Employee employee = new Employee();
        employee.setId(id);
        employee.setFullName("User " + id);
        employee.setRole(role(roleCode));
        return employee;
    }

    private void authenticate(Employee employee) {
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(employee, null, List.of()));
    }

    private Role role(RoleCode roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        return role;
    }
}
