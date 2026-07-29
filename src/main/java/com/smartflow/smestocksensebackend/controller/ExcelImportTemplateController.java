package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.excelimport.ExcelImportTemplateConstants;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/excel-imports")
@RequiredArgsConstructor
public class ExcelImportTemplateController {

    private final ExcelImportTemplateService excelImportTemplateService;

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
}
