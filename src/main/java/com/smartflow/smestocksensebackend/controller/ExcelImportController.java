package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.common.PageResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportErrorResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportUploadResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportValidationResponse;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportValidationService;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/excel-imports")
@RequiredArgsConstructor
public class ExcelImportController {

    private final ExcelImportUploadService excelImportUploadService;
    private final ExcelImportValidationService excelImportValidationService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExcelImportUploadResponse> upload(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String loaiImport,
            @RequestParam(required = false) Long khoId
    ) {
        ExcelImportUploadResponse response = excelImportUploadService.upload(file, loaiImport, khoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExcelImportValidationResponse> validate(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String loaiImport,
            @RequestParam(required = false) Long khoId
    ) {
        ExcelImportValidationResponse response = excelImportValidationService.validate(file, loaiImport, khoId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/validate-errors", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ExcelImportValidationResponse> validateErrors(
            @PathVariable Long id,
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String loaiImport,
            @RequestParam(required = false) Long khoId
    ) {
        ExcelImportValidationResponse response = excelImportValidationService.validateAndPersistErrors(id, file, loaiImport, khoId);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/errors")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<ExcelImportErrorResponse>> listErrors(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(
                        Sort.Order.asc("rowNumber"),
                        Sort.Order.asc("columnName"),
                        Sort.Order.asc("id")
                )
        );
        return ResponseEntity.ok(excelImportValidationService.listErrors(id, pageRequest));
    }
}
