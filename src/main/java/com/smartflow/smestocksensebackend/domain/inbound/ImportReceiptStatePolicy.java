package com.smartflow.smestocksensebackend.domain.inbound;

import com.smartflow.smestocksensebackend.entity.ImportReceiptStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ImportReceiptStatePolicy {

    private static final Map<ImportReceiptStatus, Set<ImportReceiptStatus>> TRANSITIONS = new EnumMap<>(ImportReceiptStatus.class);
    private static final Set<ImportReceiptStatus> TERMINAL_STATUSES = EnumSet.of(
            ImportReceiptStatus.HOAN_THANH,
            ImportReceiptStatus.HUY
    );
    private static final Set<ImportReceiptStatus> EDITABLE_STATUSES = EnumSet.of(
            ImportReceiptStatus.NHAP,
            ImportReceiptStatus.TU_CHOI
    );

    static {
        TRANSITIONS.put(ImportReceiptStatus.NHAP, EnumSet.of(
                ImportReceiptStatus.CHO_DUYET_CAP_1,
                ImportReceiptStatus.CHO_DUYET_CAP_2,
                ImportReceiptStatus.HUY
        ));
        TRANSITIONS.put(ImportReceiptStatus.TU_CHOI, EnumSet.of(
                ImportReceiptStatus.CHO_DUYET_CAP_1,
                ImportReceiptStatus.CHO_DUYET_CAP_2,
                ImportReceiptStatus.NHAP,
                ImportReceiptStatus.HUY
        ));
        TRANSITIONS.put(ImportReceiptStatus.CHO_DUYET_CAP_1, EnumSet.of(
                ImportReceiptStatus.CHO_DUYET_CAP_2,
                ImportReceiptStatus.CHO_HANG_VE,
                ImportReceiptStatus.TU_CHOI
        ));
        TRANSITIONS.put(ImportReceiptStatus.CHO_DUYET_CAP_2, EnumSet.of(
                ImportReceiptStatus.CHO_HANG_VE,
                ImportReceiptStatus.TU_CHOI
        ));
        TRANSITIONS.put(ImportReceiptStatus.CHO_HANG_VE, EnumSet.of(
                ImportReceiptStatus.CHO_KIEM_HANG
        ));
        TRANSITIONS.put(ImportReceiptStatus.CHO_KIEM_HANG, EnumSet.of(
                ImportReceiptStatus.HOAN_THANH
        ));
        TRANSITIONS.put(ImportReceiptStatus.HOAN_THANH, EnumSet.noneOf(ImportReceiptStatus.class));
        TRANSITIONS.put(ImportReceiptStatus.HUY, EnumSet.noneOf(ImportReceiptStatus.class));
    }

    private ImportReceiptStatePolicy() {
    }

    public static boolean canTransition(ImportReceiptStatus current, ImportReceiptStatus next) {
        return TRANSITIONS.getOrDefault(current, Set.of()).contains(next);
    }

    public static boolean isTerminal(ImportReceiptStatus status) {
        return TERMINAL_STATUSES.contains(status);
    }

    public static boolean isEditable(ImportReceiptStatus status) {
        return EDITABLE_STATUSES.contains(status);
    }

    public static Set<ImportReceiptStatus> allowedNextStatuses(ImportReceiptStatus status) {
        return Set.copyOf(TRANSITIONS.getOrDefault(status, Set.of()));
    }
}
