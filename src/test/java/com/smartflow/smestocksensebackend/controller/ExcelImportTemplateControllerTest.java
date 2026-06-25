package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportTemplateConstants;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportTemplateService;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportUploadService;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ExcelImportTemplateController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class})
class ExcelImportTemplateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ExcelImportTemplateService excelImportTemplateService;

    @MockitoBean
    private ExcelImportUploadService excelImportUploadService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void downloadTemplate_withoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/excel-imports/template"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void downloadTemplate_adminShouldReturnWorkbookBytes() throws Exception {
        byte[] bytes = new byte[]{1, 2, 3};
        when(excelImportTemplateService.generateTemplate()).thenReturn(bytes);

        mockMvc.perform(get("/api/excel-imports/template")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ExcelImportTemplateService.CONTENT_TYPE))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION,
                        containsString("attachment; filename=\"" + ExcelImportTemplateConstants.WORKBOOK_NAME + "\"")))
                .andExpect(content().bytes(bytes));
    }

    @Test
    void downloadTemplate_managerShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/excel-imports/template")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void downloadTemplate_employeeShouldReturn403() throws Exception {
        mockMvc.perform(get("/api/excel-imports/template")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }
}
