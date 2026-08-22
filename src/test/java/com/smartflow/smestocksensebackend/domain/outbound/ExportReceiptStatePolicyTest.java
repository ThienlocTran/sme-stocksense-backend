package com.smartflow.smestocksensebackend.domain.outbound;

import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExportReceiptStatePolicyTest {

    @Test
    void outboundApprovalFlowShouldBeSingleLevel() {
        assertEquals(Set.of(ExportReceiptStatus.CHO_DUYET, ExportReceiptStatus.HUY),
                ExportReceiptStatePolicy.allowedNextStatuses(ExportReceiptStatus.NHAP));
        assertEquals(Set.of(ExportReceiptStatus.DA_DUYET, ExportReceiptStatus.TU_CHOI),
                ExportReceiptStatePolicy.allowedNextStatuses(ExportReceiptStatus.CHO_DUYET));
        assertEquals(Set.of(ExportReceiptStatus.HOAN_THANH, ExportReceiptStatus.HUY),
                ExportReceiptStatePolicy.allowedNextStatuses(ExportReceiptStatus.DA_DUYET));
        assertEquals(Set.of(ExportReceiptStatus.CHO_DUYET, ExportReceiptStatus.NHAP, ExportReceiptStatus.HUY),
                ExportReceiptStatePolicy.allowedNextStatuses(ExportReceiptStatus.TU_CHOI));
    }

    @Test
    void legacyTwoLevelStatusesShouldNotBeRuntimeTransitions() {
        assertFalse(ExportReceiptStatePolicy.canTransition(
                ExportReceiptStatus.NHAP, ExportReceiptStatus.CHO_DUYET_CAP_1));
        assertFalse(ExportReceiptStatePolicy.canTransition(
                ExportReceiptStatus.NHAP, ExportReceiptStatus.CHO_DUYET_CAP_2));
        assertTrue(ExportReceiptStatePolicy.allowedNextStatuses(
                ExportReceiptStatus.CHO_DUYET_CAP_1).isEmpty());
        assertTrue(ExportReceiptStatePolicy.allowedNextStatuses(
                ExportReceiptStatus.CHO_DUYET_CAP_2).isEmpty());
    }
}
