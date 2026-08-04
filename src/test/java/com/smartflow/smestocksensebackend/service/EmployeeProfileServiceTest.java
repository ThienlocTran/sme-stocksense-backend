package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.dto.employee.ProfileResponse;
import com.smartflow.smestocksensebackend.dto.employee.UpdateProfileRequest;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.Gender;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import com.smartflow.smestocksensebackend.exception.AccountInactiveException;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.exception.FieldValidationException;
import com.smartflow.smestocksensebackend.exception.UnsupportedMediaTypeException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.awt.image.BufferedImage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmployeeProfileServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private EmployeeService employeeService;

    private Employee mockEmployee;

    @BeforeEach
    void setUp() {
        Role role = new Role();
        role.setCode(RoleCode.EMPLOYEE);
        
        mockEmployee = new Employee();
        mockEmployee.setId(1L);
        mockEmployee.setFullName("Nguyen Van A");
        mockEmployee.setEmail("a@test.com");
        mockEmployee.setRole(role);
        mockEmployee.setStatus(EmployeeStatus.HOAT_DONG);

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockEmployee);
    }

    @Test
    void getMyProfile_Success() {
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(mockEmployee));
        ProfileResponse response = employeeService.getMyProfile();
        assertNotNull(response);
        assertEquals("Nguyen Van A", response.fullName());
    }

    @Test
    void getMyProfile_InactiveAccount() {
        mockEmployee.setStatus(EmployeeStatus.NGUNG_HOAT_DONG);
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(mockEmployee));
        assertThrows(AccountInactiveException.class, () -> employeeService.getMyProfile());
    }

    @Test
    void updateMyProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Nguyen Van B", "0912345678", Gender.MALE, LocalDate.of(2000, 1, 1)
        );
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(mockEmployee));
        when(employeeRepository.existsByPhoneAndIdNot("0912345678", 1L)).thenReturn(false);
        when(employeeRepository.saveAndFlush(any())).thenReturn(mockEmployee);

        ProfileResponse response = employeeService.updateMyProfile(request);
        assertNotNull(response);
    }

    @Test
    void updateMyProfile_DuplicatePhone() {
        UpdateProfileRequest request = new UpdateProfileRequest(
                "Nguyen Van B", "+84912345678", Gender.MALE, LocalDate.of(2000, 1, 1)
        );
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(mockEmployee));
        when(employeeRepository.existsByPhoneAndIdNot("0912345678", 1L)).thenReturn(true);

        assertThrows(FieldValidationException.class, () -> employeeService.updateMyProfile(request));
    }

    @Test
    void uploadMyAvatar_Success() throws Exception {
        byte[] validImageBytes = createValidImageBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", validImageBytes);
        
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(mockEmployee));
        when(cloudinaryService.uploadAvatar(file, 1L)).thenReturn(Map.of(
                "secure_url", "https://url",
                "public_id", "new_pub_id"
        ));
        when(employeeRepository.saveAndFlush(any())).thenReturn(mockEmployee);

        ProfileResponse response = employeeService.uploadMyAvatar(file);
        assertNotNull(response);
        assertEquals("https://url", response.avatarUrl());
        verify(employeeRepository, times(1)).saveAndFlush(any());
    }

    @Test
    void uploadMyAvatar_InvalidFormat() throws Exception {
        byte[] invalidBytes = "Not an image".getBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", invalidBytes);
        
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(mockEmployee));

        assertThrows(UnsupportedMediaTypeException.class, () -> employeeService.uploadMyAvatar(file));
    }

    @Test
    void uploadMyAvatar_DbFail_RollbackCloudinary() throws Exception {
        byte[] validImageBytes = createValidImageBytes();
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", validImageBytes);
        
        when(employeeRepository.findById(1L)).thenReturn(Optional.of(mockEmployee));
        when(cloudinaryService.uploadAvatar(file, 1L)).thenReturn(Map.of(
                "secure_url", "https://url",
                "public_id", "new_pub_id"
        ));
        when(employeeRepository.saveAndFlush(any())).thenThrow(new RuntimeException("DB Error"));

        assertThrows(RuntimeException.class, () -> employeeService.uploadMyAvatar(file));
        verify(cloudinaryService, times(1)).deleteAvatarByPublicId("new_pub_id");
    }

    private byte[] createValidImageBytes() throws IOException {
        BufferedImage image = new BufferedImage(10, 10, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        javax.imageio.ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }
}
