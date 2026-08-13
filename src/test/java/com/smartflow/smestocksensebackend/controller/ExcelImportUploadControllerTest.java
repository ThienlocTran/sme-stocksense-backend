package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportUploadResponse;
import com.smartflow.smestocksensebackend.entity.ExcelImportStatus;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportApplyService;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportMode;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportTemplateService;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportValidationService;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportUploadService;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExcelImportController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ExcelImportUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExcelImportTemplateService excelImportTemplateService;

    @MockitoBean
    private ExcelImportUploadService excelImportUploadService;

    @MockitoBean
    private ExcelImportValidationService excelImportValidationService;

    @MockitoBean
    private ExcelImportApplyService excelImportApplyService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void upload_withoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(multipart("/api/excel-imports")
                        .file(xlsxFile())
                        .param("loaiImport", ExcelImportMode.PRODUCT_ONLY.name()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void upload_adminProductOnlyWithoutWarehouseShouldReturnValidatedMetadata() throws Exception {
        when(excelImportUploadService.upload(any(), eq(ExcelImportMode.PRODUCT_ONLY.name()), eq(null)))
                .thenReturn(new ExcelImportUploadResponse(
                        99L,
                        "products.xlsx",
                        ExcelImportMode.PRODUCT_ONLY.name(),
                        ExcelImportStatus.SAN_SANG_IMPORT.name(),
                        2,
                        2,
                        0,
                        LocalDateTime.of(2026, 6, 25, 9, 0)
                ));

        mockMvc.perform(multipart("/api/excel-imports")
                        .file(xlsxFile())
                        .param("loaiImport", ExcelImportMode.PRODUCT_ONLY.name())
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.tenFile").value("products.xlsx"))
                .andExpect(jsonPath("$.loaiImport").value(ExcelImportMode.PRODUCT_ONLY.name()))
                .andExpect(jsonPath("$.trangThai").value(ExcelImportStatus.SAN_SANG_IMPORT.name()))
                .andExpect(jsonPath("$.tongSoDong").value(2))
                .andExpect(jsonPath("$.soDongHopLe").value(2))
                .andExpect(jsonPath("$.soDongLoi").value(0))
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.canConfirm").value(true));
    }

    @Test
    void upload_openingStockWithoutWarehouseShouldReturnCreatedMetadata() throws Exception {
        when(excelImportUploadService.upload(any(), eq(ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name()), eq(null)))
                .thenReturn(new ExcelImportUploadResponse(
                        100L,
                        "opening-stock.xlsx",
                        ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name(),
                        ExcelImportStatus.CHO_XU_LY.name(),
                        0,
                        0,
                        0,
                        LocalDateTime.of(2026, 6, 25, 9, 0)
                ));

        mockMvc.perform(multipart("/api/excel-imports")
                        .file(xlsxFile())
                        .param("loaiImport", ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name())
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(100))
                .andExpect(jsonPath("$.loaiImport").value(ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name()));
    }

    @Test
    void upload_missingFileShouldReturn400() throws Exception {
        when(excelImportUploadService.upload(eq(null), eq(ExcelImportMode.PRODUCT_ONLY.name()), eq(10L)))
                .thenThrow(new BadRequestException("file is required."));

        mockMvc.perform(multipart("/api/excel-imports")
                        .param("loaiImport", ExcelImportMode.PRODUCT_ONLY.name())
                        .param("khoId", "10")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("file is required."));
    }

    @Test
    void upload_managerShouldReturn403() throws Exception {
        mockMvc.perform(multipart("/api/excel-imports")
                        .file(xlsxFile())
                        .param("loaiImport", ExcelImportMode.PRODUCT_ONLY.name())
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void upload_employeeShouldReturn403() throws Exception {
        mockMvc.perform(multipart("/api/excel-imports")
                        .file(xlsxFile())
                        .param("loaiImport", ExcelImportMode.PRODUCT_ONLY.name())
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    private MockMultipartFile xlsxFile() {
        return new MockMultipartFile(
                "file",
                "products.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                new byte[]{1, 2, 3}
        );
    }
}
