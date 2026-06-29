package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportValidationResponse;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.exception.NotFoundException;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportMode;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportTemplateService;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportUploadService;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportValidationService;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExcelImportController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class})
class ExcelImportValidateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExcelImportTemplateService excelImportTemplateService;

    @MockitoBean
    private ExcelImportUploadService excelImportUploadService;

    @MockitoBean
    private ExcelImportValidationService excelImportValidationService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void validate_withoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(multipart("/api/excel-imports/validate")
                        .file(ExcelImportUploadControllerTestSupport.xlsxFile())
                        .param("loaiImport", ExcelImportMode.PRODUCT_ONLY.name()))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void validate_adminShouldReturnValidationResult() throws Exception {
        when(excelImportValidationService.validate(any(), eq(ExcelImportMode.PRODUCT_ONLY.name()), eq(null)))
                .thenReturn(new ExcelImportValidationResponse(
                        true,
                        ExcelImportMode.PRODUCT_ONLY.name(),
                        1,
                        1,
                        0,
                        List.of()
                ));

        mockMvc.perform(multipart("/api/excel-imports/validate")
                        .file(ExcelImportUploadControllerTestSupport.xlsxFile())
                        .param("loaiImport", ExcelImportMode.PRODUCT_ONLY.name())
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(true))
                .andExpect(jsonPath("$.loaiImport").value(ExcelImportMode.PRODUCT_ONLY.name()))
                .andExpect(jsonPath("$.tongSoDong").value(1))
                .andExpect(jsonPath("$.soDongHopLe").value(1))
                .andExpect(jsonPath("$.soDongLoi").value(0));
    }

    @Test
    void validateErrors_adminShouldPersistValidationResult() throws Exception {
        when(excelImportValidationService.validateAndPersistErrors(eq(99L), any(), eq(ExcelImportMode.PRODUCT_ONLY.name()), eq(null)))
                .thenReturn(new ExcelImportValidationResponse(
                        false,
                        ExcelImportMode.PRODUCT_ONLY.name(),
                        1,
                        0,
                        1,
                        List.of()
                ));

        mockMvc.perform(multipart("/api/excel-imports/99/validate-errors")
                        .file(ExcelImportUploadControllerTestSupport.xlsxFile())
                        .param("loaiImport", ExcelImportMode.PRODUCT_ONLY.name())
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.tongSoDong").value(1))
                .andExpect(jsonPath("$.soDongHopLe").value(0))
                .andExpect(jsonPath("$.soDongLoi").value(1));
    }

    @Test
    void validateErrors_managerShouldReturn403() throws Exception {
        mockMvc.perform(multipart("/api/excel-imports/99/validate-errors")
                        .file(ExcelImportUploadControllerTestSupport.xlsxFile())
                        .param("loaiImport", ExcelImportMode.PRODUCT_ONLY.name())
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void validateErrors_employeeShouldReturn403() throws Exception {
        mockMvc.perform(multipart("/api/excel-imports/99/validate-errors")
                        .file(ExcelImportUploadControllerTestSupport.xlsxFile())
                        .param("loaiImport", ExcelImportMode.PRODUCT_ONLY.name())
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    void validateErrors_missingSessionShouldReturn404() throws Exception {
        when(excelImportValidationService.validateAndPersistErrors(eq(404L), any(), eq(ExcelImportMode.PRODUCT_ONLY.name()), eq(null)))
                .thenThrow(new NotFoundException("Lan import khong ton tai."));

        mockMvc.perform(multipart("/api/excel-imports/404/validate-errors")
                        .file(ExcelImportUploadControllerTestSupport.xlsxFile())
                        .param("loaiImport", ExcelImportMode.PRODUCT_ONLY.name())
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Lan import khong ton tai."));
    }

    @Test
    void validate_managerShouldReturn403() throws Exception {
        mockMvc.perform(multipart("/api/excel-imports/validate")
                        .file(ExcelImportUploadControllerTestSupport.xlsxFile())
                        .param("loaiImport", ExcelImportMode.PRODUCT_ONLY.name())
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void validate_employeeShouldReturn403() throws Exception {
        mockMvc.perform(multipart("/api/excel-imports/validate")
                        .file(ExcelImportUploadControllerTestSupport.xlsxFile())
                        .param("loaiImport", ExcelImportMode.PRODUCT_ONLY.name())
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    static class ExcelImportUploadControllerTestSupport {
        static org.springframework.mock.web.MockMultipartFile xlsxFile() {
            return new org.springframework.mock.web.MockMultipartFile(
                    "file",
                    "products.xlsx",
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    new byte[]{1, 2, 3}
            );
        }
    }
}
