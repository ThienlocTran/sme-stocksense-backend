package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.ProductStatus;
import com.smartflow.smestocksensebackend.entity.Warehouse;
import com.smartflow.smestocksensebackend.entity.WarehouseStatus;
import com.smartflow.smestocksensebackend.entity.WarehouseStockConfig;
import com.smartflow.smestocksensebackend.exception.BadRequestException;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseStockConfigRepository;
import com.smartflow.smestocksensebackend.service.EffectiveMinStockResolver;
import com.smartflow.smestocksensebackend.service.InventoryAlertDetectionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WarehouseStockConfigControllerTest {

    @Mock
    private WarehouseStockConfigRepository warehouseStockConfigRepository;

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WarehouseRepository warehouseRepository;

    @Mock
    private InventoryAlertDetectionService inventoryAlertDetectionService;

    private EffectiveMinStockResolver effectiveMinStockResolver = new EffectiveMinStockResolver();

    private WarehouseStockConfigController controller;

    @Test
    void saveConfig_shouldCreateOrUpdateConfig() {
        Product product = new Product();
        product.setId(1L);
        product.setStatus(ProductStatus.HOAT_DONG);
        product.setDefaultMinStock(10);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(2L);
        warehouse.setStatus(WarehouseStatus.HOAT_DONG);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));
        when(warehouseStockConfigRepository.findByProductIdAndWarehouseId(1L, 2L))
                .thenReturn(Optional.empty());
        when(warehouseStockConfigRepository.save(org.mockito.ArgumentMatchers.any(WarehouseStockConfig.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        controller = new WarehouseStockConfigController(warehouseStockConfigRepository, productRepository, warehouseRepository,
                effectiveMinStockResolver, inventoryAlertDetectionService);
        WarehouseStockConfigController.ConfigResponse result = controller.saveConfig(new WarehouseStockConfigController.SaveConfigRequest(1L, 2L, 25));

        assertEquals(25, result.minStockOverride());
        assertEquals(25, result.effectiveMinStock());
        assertEquals(false, result.usesDefault());
    }

    @Test
    void saveConfig_inactiveProductShouldThrowBadRequest() {
        Product product = new Product();
        product.setId(1L);
        product.setStatus(ProductStatus.NGUNG_HOAT_DONG);

        Warehouse warehouse = new Warehouse();
        warehouse.setId(2L);
        warehouse.setStatus(WarehouseStatus.HOAT_DONG);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(warehouseRepository.findById(2L)).thenReturn(Optional.of(warehouse));

        controller = new WarehouseStockConfigController(warehouseStockConfigRepository, productRepository, warehouseRepository,
                effectiveMinStockResolver, inventoryAlertDetectionService);
        BadRequestException exception = assertThrows(BadRequestException.class,
                () -> controller.saveConfig(new WarehouseStockConfigController.SaveConfigRequest(1L, 2L, 25)));

        assertEquals("Sản phẩm không hoạt động.", exception.getMessage());
    }
}
