package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.excelimport.ExcelImportTemplateConstants;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportTemplateService;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportUploadResponse;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/excel-imports")
@RequiredArgsConstructor
public class ExcelImportTemplateController {

    private final ExcelImportTemplateService excelImportTemplateService;
    private final ExcelImportUploadService excelImportUploadService;

    @GetMapping("/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] template = excelImportTemplateService.generateTemplate();

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ExcelImportTemplateService.CONTENT_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(ExcelImportTemplateConstants.WORKBOOK_NAME)
                        .build()
                        .toString())
                .body(template);
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MANAGER','EMPLOYEE')")
    public ResponseEntity<ExcelImportUploadResponse> upload(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String loaiImport,
            @RequestParam(required = false) Long khoId
    ) {
        ExcelImportUploadResponse response = excelImportUploadService.upload(file, loaiImport, khoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
