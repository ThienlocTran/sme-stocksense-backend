package com.smartflow.smestocksensebackend.excelimport;

import com.smartflow.smestocksensebackend.dto.excelimport.ExcelImportUploadResponse;
import com.smartflow.smestocksensebackend.entity.ExcelImport;
import com.smartflow.smestocksensebackend.entity.ExcelImportStatus;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.ExcelImportRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExcelImportUploadServiceTest {

    @Mock
    private ExcelImportRepository excelImportRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @InjectMocks
    private ExcelImportUploadService excelImportUploadService;

    @Test
    void upload_productOnlyWithoutWarehouseShouldCreatePendingMetadata() {
        when(excelImportRepository.save(any(ExcelImport.class))).thenAnswer(invocation -> {
            ExcelImport saved = invocation.getArgument(0);
            saved.setId(99L);
            return saved;
        });

        ExcelImportUploadResponse response = excelImportUploadService.upload(
                xlsxFile("products.xlsx", new byte[]{1, 2, 3}),
                ExcelImportMode.PRODUCT_ONLY.name(),
                null
        );

        ArgumentCaptor<ExcelImport> captor = ArgumentCaptor.forClass(ExcelImport.class);
        verify(excelImportRepository).save(captor.capture());
        ExcelImport saved = captor.getValue();

        assertThat(saved.getFileName()).isEqualTo("products.xlsx");
        assertThat(saved.getFilePath()).startsWith("metadata-only/");
        assertThat(saved.getImportType()).isEqualTo(ExcelImportMode.PRODUCT_ONLY.name());
        assertThat(saved.getWarehouse()).isNull();
        assertThat(saved.getStatus()).isEqualTo(ExcelImportStatus.CHO_XU_LY);
        assertThat(saved.getTotalRows()).isZero();
        assertThat(saved.getValidRows()).isZero();
        assertThat(saved.getErrorRows()).isZero();
        assertThat(response.id()).isEqualTo(99L);
        assertThat(response.trangThai()).isEqualTo(ExcelImportStatus.CHO_XU_LY.name());
    }

    @Test
    void upload_openingStockWithoutWarehouseShouldCreatePendingMetadata() {
        when(excelImportRepository.save(any(ExcelImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        excelImportUploadService.upload(
                xlsxFile("opening-stock.xlsx", new byte[]{1, 2, 3}),
                ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name(),
                null
        );

        ArgumentCaptor<ExcelImport> captor = ArgumentCaptor.forClass(ExcelImport.class);
        verify(excelImportRepository).save(captor.capture());

        assertThat(captor.getValue().getImportType()).isEqualTo(ExcelImportMode.PRODUCT_WITH_OPENING_STOCK.name());
        assertThat(captor.getValue().getWarehouse()).isNull();
    }

    @Test
    void upload_withWarehouseShouldStoreWarehouseMetadata() {
        Warehouse warehouse = new Warehouse();
        warehouse.setId(10L);

        when(warehouseRepository.findById(10L)).thenReturn(Optional.of(warehouse));
        when(excelImportRepository.save(any(ExcelImport.class))).thenAnswer(invocation -> invocation.getArgument(0));

        excelImportUploadService.upload(
                xlsxFile("products.xlsx", new byte[]{1, 2, 3}),
                ExcelImportMode.PRODUCT_ONLY.name(),
                10L
        );

        ArgumentCaptor<ExcelImport> captor = ArgumentCaptor.forClass(ExcelImport.class);
        verify(excelImportRepository).save(captor.capture());

        assertThat(captor.getValue().getWarehouse()).isSameAs(warehouse);
    }

    @Test
    void upload_missingFileShouldReturnBadRequest() {
        assertThatThrownBy(() -> excelImportUploadService.upload(null, ExcelImportMode.PRODUCT_ONLY.name(), 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("file is required.");

        verify(excelImportRepository, never()).save(any());
    }

    @Test
    void upload_emptyFileShouldReturnBadRequest() {
        assertThatThrownBy(() -> excelImportUploadService.upload(
                xlsxFile("products.xlsx", new byte[]{}),
                ExcelImportMode.PRODUCT_ONLY.name(),
                10L
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("file must not be empty.");

        verify(excelImportRepository, never()).save(any());
    }

    @Test
    void upload_nonXlsxFileShouldReturnBadRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "products.csv",
                "text/csv",
                new byte[]{1}
        );

        assertThatThrownBy(() -> excelImportUploadService.upload(file, ExcelImportMode.PRODUCT_ONLY.name(), 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("file must be .xlsx.");

        verify(excelImportRepository, never()).save(any());
    }

    @Test
    void upload_unsupportedImportModeShouldReturnBadRequest() {
        assertThatThrownBy(() -> excelImportUploadService.upload(xlsxFile("products.xlsx", new byte[]{1}), "INVALID", 10L))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("loaiImport is not supported.");

        verify(excelImportRepository, never()).save(any());
    }

    @Test
    void upload_invalidWarehouseShouldReturnBadRequestWhenProvided() {
        when(warehouseRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> excelImportUploadService.upload(
                xlsxFile("products.xlsx", new byte[]{1}),
                ExcelImportMode.PRODUCT_ONLY.name(),
                404L
        ))
                .isInstanceOf(BadRequestException.class)
                .hasMessage("khoId is invalid.");

        verify(excelImportRepository, never()).save(any());
    }

    private MockMultipartFile xlsxFile(String originalFilename, byte[] content) {
        return new MockMultipartFile(
                "file",
                originalFilename,
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                content
        );
    }
}
