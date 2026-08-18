package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.config.JwtAuthenticationFilter;
import com.smartflow.smestocksensebackend.config.SecurityConfig;
import com.smartflow.smestocksensebackend.exception.ApiExceptionHandler;
import com.smartflow.smestocksensebackend.exception.MissingRoleException;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.service.JwtService;
import com.smartflow.smestocksensebackend.service.StockDocumentExportService;
import com.smartflow.smestocksensebackend.service.document.GeneratedDocument;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = StockDocumentExportController.class)
@Import({ SecurityConfig.class, JwtAuthenticationFilter.class, ApiExceptionHandler.class })
class StockDocumentExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private StockDocumentExportService stockDocumentExportService;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private EmployeeRepository employeeRepository;

    @Test
    void exportImportPdf_authorizedRoleShouldReturnDownloadHeaders() throws Exception {
        when(stockDocumentExportService.exportImportReceiptPdf(1L))
                .thenReturn(new GeneratedDocument("%PDF".getBytes(), "application/pdf", "phieu-nhap-pn-01.pdf"));

        mockMvc.perform(get("/api/import-receipts/1/export/pdf")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "application/pdf"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"phieu-nhap-pn-01.pdf\""));
    }

    @Test
    void exportReceiptExcel_authorizedRoleShouldReturnDownloadHeaders() throws Exception {
        when(stockDocumentExportService.exportExportReceiptExcel(2L))
                .thenReturn(new GeneratedDocument(new byte[] { 1, 2 },
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                        "phieu-xuat-px-01.xlsx"));

        mockMvc.perform(get("/api/export-receipts/2/export/excel")
                        .with(user("manager@example.com").roles("MANAGER")))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type",
                        "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string("Content-Disposition",
                        "attachment; filename=\"phieu-xuat-px-01.xlsx\""));
    }

    @Test
    void exportWithoutTokenShouldReturn401() throws Exception {
        mockMvc.perform(get("/api/import-receipts/1/export/pdf"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void exportNotVisibleShouldReturn403() throws Exception {
        when(stockDocumentExportService.exportExportReceiptPdf(3L))
                .thenThrow(new MissingRoleException("Khong co quyen xem phieu xuat."));

        mockMvc.perform(get("/api/export-receipts/3/export/pdf")
                        .with(user("employee@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }
}
