package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.entity.Product;
import com.smartflow.smestocksensebackend.entity.WarehouseStockConfig;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class EffectiveMinStockResolver {
    public Optional<Integer> resolve(Product product, WarehouseStockConfig config) {
        if (config != null && config.getMinStockOverride() != null) {
            return Optional.of(config.getMinStockOverride());
        }
        return Optional.ofNullable(product.getDefaultMinStock());
    }
}
