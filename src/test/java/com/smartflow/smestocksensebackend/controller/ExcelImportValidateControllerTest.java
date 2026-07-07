package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.dto.common.PageResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportErrorResponse;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
    void listErrors_adminShouldReturnStablePageResponse() throws Exception {
        when(excelImportValidationService.listErrors(eq(99L), any(Pageable.class)))
                .thenReturn(new PageResponse<>(
                        List.of(new ExcelImportErrorResponse(
                                7L,
                                99L,
                                2,
                                "ma_san_pham",
                                "",
                                "Ma san pham khong duoc de trong.",
                                "Nhap ma san pham."
                        )),
                        0,
                        20,
                        1,
                        1
                ));

        mockMvc.perform(get("/api/excel-imports/99/errors")
                        .param("page", "0")
                        .param("size", "20")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(7))
                .andExpect(jsonPath("$.content[0].importId").value(99))
                .andExpect(jsonPath("$.content[0].rowNumber").value(2))
                .andExpect(jsonPath("$.content[0].columnName").value("ma_san_pham"))
                .andExpect(jsonPath("$.content[0].originalValue").value(""))
                .andExpect(jsonPath("$.content[0].message").value("Ma san pham khong duoc de trong."))
                .andExpect(jsonPath("$.content[0].suggestion").value("Nhap ma san pham."))
                .andExpect(jsonPath("$.page").value(0))
                .andExpect(jsonPath("$.size").value(20))
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.totalPages").value(1));

        verify(excelImportValidationService).listErrors(eq(99L), org.mockito.ArgumentMatchers.argThat(pageable ->
                pageable.getPageNumber() == 0
                        && pageable.getPageSize() == 20
                        && pageable.getSort().getOrderFor("rowNumber").getDirection() == Sort.Direction.ASC
                        && pageable.getSort().getOrderFor("columnName").getDirection() == Sort.Direction.ASC
                        && pageable.getSort().getOrderFor("id").getDirection() == Sort.Direction.ASC
        ));
        verify(excelImportValidationService, never()).validate(any(), any(), any());
        verify(excelImportValidationService, never()).validateAndPersistErrors(any(), any(), any(), any());
    }

    @Test
    void listErrors_emptySessionShouldReturnEmptyPage() throws Exception {
        when(excelImportValidationService.listErrors(eq(100L), any(Pageable.class)))
                .thenReturn(new PageResponse<>(List.of(), 0, 20, 0, 0));

        mockMvc.perform(get("/api/excel-imports/100/errors")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void listErrors_missingSessionShouldReturn404() throws Exception {
        when(excelImportValidationService.listErrors(eq(404L), any(Pageable.class)))
                .thenThrow(new NotFoundException("Lan import khong ton tai."));

        mockMvc.perform(get("/api/excel-imports/404/errors")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Lan import khong ton tai."));
    }

    @Test
    void listErrors_managerShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/excel-imports/99/errors")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void listErrors_employeeShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/excel-imports/99/errors")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
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
