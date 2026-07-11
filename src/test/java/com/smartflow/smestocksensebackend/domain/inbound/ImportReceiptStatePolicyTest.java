package com.smartflow.smestocksensebackend.domain.inbound;

import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImportReceiptStatePolicyTest {

    @Test
    void canTransition_shouldAllowBacklogTransitions() {
        assertTrue(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.NHAP, ImportReceiptStatus.CHO_DUYET_CAP_1));
        assertTrue(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.NHAP, ImportReceiptStatus.HUY));
        assertTrue(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.TU_CHOI, ImportReceiptStatus.NHAP));
        assertTrue(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.TU_CHOI, ImportReceiptStatus.HUY));
        assertTrue(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.CHO_DUYET_CAP_1, ImportReceiptStatus.CHO_DUYET_CAP_2));
        assertTrue(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.CHO_DUYET_CAP_1, ImportReceiptStatus.CHO_HANG_VE));
        assertTrue(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.CHO_DUYET_CAP_1, ImportReceiptStatus.TU_CHOI));
        assertTrue(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.CHO_DUYET_CAP_2, ImportReceiptStatus.CHO_HANG_VE));
        assertTrue(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.CHO_DUYET_CAP_2, ImportReceiptStatus.TU_CHOI));
        assertTrue(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.CHO_HANG_VE, ImportReceiptStatus.CHO_KIEM_HANG));
        assertTrue(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.CHO_KIEM_HANG, ImportReceiptStatus.HOAN_THANH));
    }

    @Test
    void canTransition_shouldRejectSkippedOrTerminalTransitions() {
        assertFalse(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.NHAP, ImportReceiptStatus.HOAN_THANH));
        assertFalse(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.CHO_HANG_VE, ImportReceiptStatus.HOAN_THANH));
        assertFalse(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.HOAN_THANH, ImportReceiptStatus.NHAP));
        assertFalse(ImportReceiptStatePolicy.canTransition(ImportReceiptStatus.HUY, ImportReceiptStatus.NHAP));
    }

    @Test
    void terminalAndEditableStatusRules_shouldMatchWorkflow() {
        assertTrue(ImportReceiptStatePolicy.isTerminal(ImportReceiptStatus.HOAN_THANH));
        assertTrue(ImportReceiptStatePolicy.isTerminal(ImportReceiptStatus.HUY));
        assertFalse(ImportReceiptStatePolicy.isTerminal(ImportReceiptStatus.CHO_KIEM_HANG));

        assertTrue(ImportReceiptStatePolicy.isEditable(ImportReceiptStatus.NHAP));
        assertTrue(ImportReceiptStatePolicy.isEditable(ImportReceiptStatus.TU_CHOI));
        assertFalse(ImportReceiptStatePolicy.isEditable(ImportReceiptStatus.CHO_DUYET_CAP_1));
        assertFalse(ImportReceiptStatePolicy.isEditable(ImportReceiptStatus.HOAN_THANH));
    }
}
