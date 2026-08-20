package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.SystemSetting;
import com.smartflow.smestocksensebackend.entity.SystemSettingHistory;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.repository.SystemSettingHistoryRepository;
import com.smartflow.smestocksensebackend.repository.SystemSettingRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemSettingControllerTest {

    @Mock
    private SystemSettingRepository systemSettingRepository;

    @Mock
    private SystemSettingHistoryRepository historyRepository;

    @InjectMocks
    private SystemSettingController controller;

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void updateImportReceiptThreshold_adminShouldSaveHistory() {
        authenticate(RoleCode.ADMIN);
        SystemSetting setting = SystemSetting.builder()
                .key("IMPORT_RECEIPT_SECOND_APPROVAL_THRESHOLD")
                .value("50000000")
                .build();
        when(systemSettingRepository.findById("IMPORT_RECEIPT_SECOND_APPROVAL_THRESHOLD"))
                .thenReturn(Optional.of(setting));

        Map<String, Object> result = controller.updateImportReceiptThreshold(Map.of(
                "value", "30000000",
                "reason", "Lower threshold"
        ));

        assertEquals("30000000", result.get("value"));
        verify(systemSettingRepository).save(setting);
        verify(historyRepository).save(any(SystemSettingHistory.class));
        assertEquals("30000000", setting.getValue());
    }

    @Test
    void updateImportReceiptThreshold_managerShouldBeDenied() {
        authenticate(RoleCode.MANAGER);

        assertThrows(MissingRoleException.class, () -> controller.updateImportReceiptThreshold(Map.of(
                "value", "30000000",
                "reason", "Lower threshold"
        )));
    }

    @Test
    void getImportReceiptThresholdHistory_shouldReturnSavedRows() {
        authenticate(RoleCode.ADMIN);
        SystemSettingHistory history = new SystemSettingHistory();
        history.setSettingKey("IMPORT_RECEIPT_SECOND_APPROVAL_THRESHOLD");
        history.setOldValue("50000000");
        history.setNewValue("30000000");
        history.setReason("Lower threshold");
        history.setChangedBy(admin());
        history.setChangedAt(LocalDateTime.now());
        when(historyRepository.findBySettingKeyOrderByChangedAtDesc("IMPORT_RECEIPT_SECOND_APPROVAL_THRESHOLD"))
                .thenReturn(List.of(history));

        List<Map<String, Object>> rows = controller.getImportReceiptThresholdHistory();

        assertEquals(1, rows.size());
        assertEquals("30000000", rows.get(0).get("newValue"));
    }

    private void authenticate(RoleCode roleCode) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(roleCode == RoleCode.ADMIN ? admin() : manager(), null, List.of()));
    }

    private Employee admin() {
        return employee(1L, RoleCode.ADMIN);
    }

    private Employee manager() {
        return employee(2L, RoleCode.MANAGER);
    }

    private Employee employee(long id, RoleCode roleCode) {
        Role role = new Role();
        role.setCode(roleCode);
        Employee employee = new Employee();
        employee.setId(id);
        employee.setFullName(roleCode.name());
        employee.setRole(role);
        return employee;
    }
}
