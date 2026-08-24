package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.inventoryadjustment.RejectInventoryAdjustmentRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InventoryAdjustmentControllerSecurityTest {

    @Test
    void createDraft_shouldAllowOperationalRolesOnly() throws NoSuchMethodException {
        PreAuthorize annotation = InventoryAdjustmentController.class
                .getDeclaredMethod("getOrCreateDraft", Long.class)
                .getAnnotation(PreAuthorize.class);

        assertEquals("hasAnyRole('ADMIN','EMPLOYEE')", annotation.value());
    }

    @Test
    void readEndpoints_shouldKeepInventoryCountReadRoles() throws NoSuchMethodException {
        assertEquals("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')", InventoryAdjustmentController.class
                .getDeclaredMethod("getByCount", Long.class)
                .getAnnotation(PreAuthorize.class)
                .value());
        assertEquals("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')", InventoryAdjustmentController.class
                .getDeclaredMethod("get", Long.class)
                .getAnnotation(PreAuthorize.class)
                .value());
    }

    @Test
    void submit_shouldAllowOperationalRolesOnly() throws NoSuchMethodException {
        assertEquals("hasAnyRole('ADMIN','EMPLOYEE')", InventoryAdjustmentController.class
                .getDeclaredMethod("submit", Long.class)
                .getAnnotation(PreAuthorize.class)
                .value());
    }

    @Test
    void approveAndReject_shouldAllowApprovalRolesOnly() throws NoSuchMethodException {
        assertEquals("hasAnyRole('ADMIN','MANAGER')", InventoryAdjustmentController.class
                .getDeclaredMethod("approve", Long.class)
                .getAnnotation(PreAuthorize.class)
                .value());
        assertEquals("hasAnyRole('ADMIN','MANAGER')", InventoryAdjustmentController.class
                .getDeclaredMethod("reject", Long.class, RejectInventoryAdjustmentRequest.class)
                .getAnnotation(PreAuthorize.class)
                .value());
    }

    @Test
    void apply_shouldAllowApprovalRolesOnly() throws NoSuchMethodException {
        assertEquals("hasAnyRole('ADMIN','MANAGER')", InventoryAdjustmentController.class
                .getDeclaredMethod("apply", Long.class)
                .getAnnotation(PreAuthorize.class)
                .value());
    }
}
