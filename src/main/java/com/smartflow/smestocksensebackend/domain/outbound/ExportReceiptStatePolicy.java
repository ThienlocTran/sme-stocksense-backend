package com.smartflow.smestocksensebackend.domain.outbound;

import com.smartflow.smestocksensebackend.entity.ExportReceiptStatus;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ExportReceiptStatePolicy {

    private static final Map<ExportReceiptStatus, Set<ExportReceiptStatus>> TRANSITIONS = new EnumMap<>(ExportReceiptStatus.class);
    
    private static final Set<ExportReceiptStatus> TERMINAL_STATUSES = EnumSet.of(
            ExportReceiptStatus.HOAN_THANH,
            ExportReceiptStatus.HUY
    );
    
    private static final Set<ExportReceiptStatus> EDITABLE_STATUSES = EnumSet.of(
            ExportReceiptStatus.NHAP,
            ExportReceiptStatus.TU_CHOI
    );

    static {
        TRANSITIONS.put(ExportReceiptStatus.NHAP, EnumSet.of(
                ExportReceiptStatus.CHO_DUYET_CAP_1,
                ExportReceiptStatus.CHO_DUYET_CAP_2,
                ExportReceiptStatus.HUY
        ));
        
        TRANSITIONS.put(ExportReceiptStatus.CHO_DUYET_CAP_1, EnumSet.of(
                ExportReceiptStatus.CHO_DUYET_CAP_2,
                ExportReceiptStatus.TU_CHOI
        ));
        
        TRANSITIONS.put(ExportReceiptStatus.CHO_DUYET_CAP_2, EnumSet.of(
                ExportReceiptStatus.HOAN_THANH,
                ExportReceiptStatus.TU_CHOI
        ));
        
        TRANSITIONS.put(ExportReceiptStatus.TU_CHOI, EnumSet.of(
                ExportReceiptStatus.CHO_DUYET_CAP_1,
                ExportReceiptStatus.CHO_DUYET_CAP_2,
                ExportReceiptStatus.NHAP,
                ExportReceiptStatus.HUY
        ));
        
        TRANSITIONS.put(ExportReceiptStatus.HOAN_THANH, EnumSet.noneOf(ExportReceiptStatus.class));
        TRANSITIONS.put(ExportReceiptStatus.HUY, EnumSet.noneOf(ExportReceiptStatus.class));
    }

    private ExportReceiptStatePolicy() {
    }

    public static boolean canTransition(ExportReceiptStatus current, ExportReceiptStatus next) {
        return TRANSITIONS.getOrDefault(current, Set.of()).contains(next);
    }

    public static boolean isTerminal(ExportReceiptStatus status) {
        return TERMINAL_STATUSES.contains(status);
    }

    public static boolean isEditable(ExportReceiptStatus status) {
        return EDITABLE_STATUSES.contains(status);
    }

    public static Set<ExportReceiptStatus> allowedNextStatuses(ExportReceiptStatus status) {
        return Set.copyOf(TRANSITIONS.getOrDefault(status, Set.of()));
    }
}
