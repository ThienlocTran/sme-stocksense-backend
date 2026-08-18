package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.common.PageResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportApplyResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportConfirmResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportErrorResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportUploadResponse;
import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportValidationResponse;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportApplyService;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportValidationService;
import com.smartflow.smestocksensebackend.excelimport.ExcelImportUploadService;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
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

    private static final int MAX_PAGE_SIZE = 100;

    private final ExcelImportUploadService excelImportUploadService;
    private final ExcelImportValidationService excelImportValidationService;
    private final ExcelImportApplyService excelImportApplyService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExcelImportUploadResponse> upload(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String loaiImport,
            @RequestParam(required = false) Long khoId
    ) {
        ExcelImportUploadResponse response = excelImportUploadService.upload(file, loaiImport, khoId);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping(value = "/validate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExcelImportValidationResponse> validate(
            @RequestParam(required = false) MultipartFile file,
            @RequestParam(required = false) String loaiImport,
            @RequestParam(required = false) Long khoId
    ) {
        ExcelImportValidationResponse response = excelImportValidationService.validate(file, loaiImport, khoId);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/{id}/validate-errors", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
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
    public ResponseEntity<PageResponse<ExcelImportErrorResponse>> listErrors(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        validatePageRequest(page, size);
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

    @PostMapping("/{id}/confirm")
    public ResponseEntity<ExcelImportConfirmResponse> confirm(@PathVariable Long id) {
        return ResponseEntity.ok(excelImportValidationService.confirm(id));
    }

    @PostMapping(value = "/{id}/apply", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ExcelImportApplyResponse> apply(
            @PathVariable Long id,
            @RequestParam(required = false) MultipartFile file
    ) {
        return ResponseEntity.ok(excelImportApplyService.apply(id, file));
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0) {
            throw new BadRequestException("page phai lon hon hoac bang 0.");
        }
        if (size < 1 || size > MAX_PAGE_SIZE) {
            throw new BadRequestException("size phai nam trong khoang 1 den 100.");
        }
    }
}
