package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.WarehouseStockConfig;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class EffectiveMinStockResolverTest {
    private final EffectiveMinStockResolver resolver = new EffectiveMinStockResolver();

    @Test
    void overrideNull_shouldUseDefault() {
        Product product = new Product();
        product.setDefaultMinStock(10);
        assertEquals(10, resolver.resolve(product, WarehouseStockConfig.builder().build()).orElseThrow());
    }

    @Test
    void overridePresent_shouldUseOverride() {
        Product product = new Product();
        product.setDefaultMinStock(10);
        WarehouseStockConfig config = WarehouseStockConfig.builder().minStockOverride(25).build();
        assertEquals(25, resolver.resolve(product, config).orElseThrow());
    }

    @Test
    void zeroOverride_shouldUseZeroOverride() {
        Product product = new Product();
        product.setDefaultMinStock(10);
        WarehouseStockConfig config = WarehouseStockConfig.builder().minStockOverride(0).build();
        assertEquals(0, resolver.resolve(product, config).orElseThrow());
    }

    @Test
    void warehouseOverride_shouldNotAffectAnotherWarehouse() {
        Product product = new Product();
        product.setDefaultMinStock(10);

        WarehouseStockConfig firstWarehouse = WarehouseStockConfig.builder().minStockOverride(25).build();
        WarehouseStockConfig secondWarehouse = WarehouseStockConfig.builder().build();

        assertEquals(25, resolver.resolve(product, firstWarehouse).orElseThrow());
        assertEquals(10, resolver.resolve(product, secondWarehouse).orElseThrow());
    }

    @Test
    void resolve_shouldNotMutateProductOrConfig() {
        Product product = new Product();
        product.setDefaultMinStock(10);
        WarehouseStockConfig config = WarehouseStockConfig.builder().minStockOverride(25).build();

        assertEquals(25, resolver.resolve(product, config).orElseThrow());

        assertEquals(10, product.getDefaultMinStock());
        assertEquals(25, config.getMinStockOverride());
    }

    @Test
    void bothNull_shouldBeNotConfigured() {
        assertTrue(resolver.resolve(new Product(), WarehouseStockConfig.builder().build()).isEmpty());
    }
}
