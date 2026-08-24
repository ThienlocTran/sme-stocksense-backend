package com.smartflow.smestocksensebackend.service.impl;

import com.smartflow.smestocksensebackend.dto.inventoryadjustment.InventoryAdjustmentResponse;
import com.smartflow.smestocksensebackend.dto.inventoryadjustment.RejectInventoryAdjustmentRequest;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.InventoryAdjustment;
import com.smartflow.smestocksensebackend.entity.InventoryAdjustmentStatus;
import com.smartflow.smestocksensebackend.entity.InventoryCount;
import com.smartflow.smestocksensebackend.entity.InventoryCountDetail;
import com.smartflow.smestocksensebackend.entity.InventoryCountStatus;
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
import com.smartflow.smestocksensebackend.service.InventoryAdjustmentCodeGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
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
    @Mock InventoryAdjustmentCodeGenerator codeGenerator;

    InventoryAdjustmentServiceImpl service;
    Employee actor;
    InventoryCount count;
    InventoryCountDetail detail;

    @BeforeEach
    void setup() {
        service = new InventoryAdjustmentServiceImpl(adjustmentRepository, countRepository, detailRepository, codeGenerator);
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
    void clear() {
        SecurityContextHolder.clearContext();
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
