package com.smartflow.smestocksensebackend.excelimport;

import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportUploadResponse;
import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.ExcelImport;
import com.smartflow.smestocksensebackend.entity.ExcelImportStatus;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.ExcelImportRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExcelImportUploadService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/octet-stream"
    );
    private static final long MAX_FILE_SIZE_BYTES = 10L * 1024 * 1024;
    private static final String METADATA_ONLY_PATH_PREFIX = "metadata-only/";

    private final ExcelImportRepository excelImportRepository;
    private final WarehouseRepository warehouseRepository;
    private final ExcelImportValidationService excelImportValidationService;

    public ExcelImportUploadResponse upload(MultipartFile file, String loaiImport, Long khoId) {
        String fileName = file == null || file.isEmpty() ? null : safeOriginalFileName(file);
        validateFile(file, fileName);
        ExcelImportMode importMode = parseImportMode(loaiImport);
        Warehouse warehouse = findWarehouseIfProvided(khoId);

        ExcelImport excelImport = new ExcelImport();
        excelImport.setFileName(fileName);
        excelImport.setFilePath(metadataOnlyPath(fileName));
        excelImport.setImportType(importMode.name());
        excelImport.setWarehouse(warehouse);
        excelImport.setStatus(ExcelImportStatus.CHO_XU_LY);
        excelImport.setTotalRows(0);
        excelImport.setValidRows(0);
        excelImport.setErrorRows(0);
        excelImport.setCreatedBy(currentEmployee());

        ExcelImport saved = excelImportRepository.save(excelImport);
        var validation = excelImportValidationService.validateAndPersistErrors(saved.getId(), file, importMode.name(), khoId);
        saved.setTotalRows(validation.tongSoDong());
        saved.setValidRows(validation.soDongHopLe());
        saved.setErrorRows(validation.soDongLoi());
        saved.setStatus(validation.valid() ? ExcelImportStatus.SAN_SANG_IMPORT : ExcelImportStatus.CO_LOI);
        return ExcelImportUploadResponse.from(saved);
    }

    private Employee currentEmployee() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof Employee employee)) {
            throw new AuthenticationCredentialsNotFoundException("Authentication required.");
        }
        return employee;
    }

    private void validateFile(MultipartFile file, String fileName) {
        if (file == null) {
            throw new BadRequestException("file is required.");
        }
        if (file.isEmpty()) {
            throw new BadRequestException("file must not be empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BadRequestException("file size must not exceed 10MB.");
        }

        if (!fileName.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new BadRequestException("file must be .xlsx.");
        }

        String contentType = file.getContentType();
        if (contentType != null && !contentType.isBlank() && !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BadRequestException("file content type is not supported.");
        }
    }

    private ExcelImportMode parseImportMode(String loaiImport) {
        if (loaiImport == null || loaiImport.isBlank()) {
            throw new BadRequestException("loaiImport is required.");
        }

        try {
            return ExcelImportMode.valueOf(loaiImport.trim());
        } catch (IllegalArgumentException exception) {
            throw new BadRequestException("loaiImport is not supported.", exception);
        }
    }

    private Warehouse findWarehouseIfProvided(Long khoId) {
        if (khoId == null) {
            return null;
        }

        return warehouseRepository.findById(khoId)
                .orElseThrow(() -> new BadRequestException("khoId is invalid."));
    }

    private String safeOriginalFileName(MultipartFile file) {
        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            throw new BadRequestException("file name is required.");
        }

        String normalized = originalFileName.replace('\\', '/');
        String fileName = normalized.substring(normalized.lastIndexOf('/') + 1).trim();
        if (fileName.isBlank()) {
            throw new BadRequestException("file name is required.");
        }
        if (fileName.length() > 255) {
            throw new BadRequestException("file name is too long.");
        }
        return fileName;
    }

    private String metadataOnlyPath(String fileName) {
        String safeName = fileName.replaceAll("[^A-Za-z0-9._-]", "_");
        String path = METADATA_ONLY_PATH_PREFIX + UUID.randomUUID() + "/" + safeName;
        if (path.length() <= 255) {
            return path;
        }
        return path.substring(0, 255);
    }
}
