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
    void bothNull_shouldBeNotConfigured() {
        assertTrue(resolver.resolve(new Product(), WarehouseStockConfig.builder().build()).isEmpty());
    }
}
