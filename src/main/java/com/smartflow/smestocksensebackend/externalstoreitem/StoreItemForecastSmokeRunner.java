package com.smartflow.smestocksensebackend.externalstoreitem;

import com.smartflow.smestocksensebackend.dto.forecast.ForecastResponse;
import com.smartflow.smestocksensebackend.entity.SalesHistorySource;
import com.smartflow.smestocksensebackend.repository.ProductRepository;
import com.smartflow.smestocksensebackend.repository.WarehouseRepository;
import com.smartflow.smestocksensebackend.service.ForecastService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;

@Component
public class StoreItemForecastSmokeRunner implements ApplicationRunner {

    private final ProductRepository productRepository;
    private final WarehouseRepository warehouseRepository;
    private final ForecastService forecastService;
    private final TransactionTemplate transactionTemplate;
    private final ConfigurableApplicationContext context;

    public StoreItemForecastSmokeRunner(ProductRepository productRepository, WarehouseRepository warehouseRepository,
            ForecastService forecastService, TransactionTemplate transactionTemplate, ConfigurableApplicationContext context) {
        this.productRepository = productRepository;
        this.warehouseRepository = warehouseRepository;
        this.forecastService = forecastService;
        this.transactionTemplate = transactionTemplate;
        this.context = context;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!args.containsOption("store-item-forecast-smoke")) {
            return;
        }
        List<Pair> pairs = List.of(new Pair("SP001", "K001"), new Pair("SP025", "K002"), new Pair("SP050", "K003"));
        transactionTemplate.execute(status -> {
            for (Pair pair : pairs) {
                Long productId = productRepository.findByCodeIgnoreCase(pair.productCode())
                        .orElseThrow(() -> new IllegalStateException("Missing product: " + pair.productCode()))
                        .getId();
                Long warehouseId = warehouseRepository.findByCodeIgnoreCase(pair.warehouseCode())
                        .orElseThrow(() -> new IllegalStateException("Missing warehouse: " + pair.warehouseCode()))
                        .getId();
                ForecastResponse response = forecastService.runForecast(productId, warehouseId,
                        SalesHistorySource.EXTERNAL_STORE_ITEM);
                System.out.printf("STORE_ITEM_SMOKE %s/%s PASS source=%s dataset=%s mode=%s dataDays=%d smape=%s mae=%s rmse=%s forecast7d=%s%n",
                        pair.productCode(), pair.warehouseCode(), response.source(), response.datasetType(),
                        response.mode(), response.dataDays(), response.smape(), response.mae(), response.rmse(),
                        response.forecast7d());
            }
            status.setRollbackOnly();
            return null;
        });
        System.out.println("STORE_ITEM_SMOKE rollback=true");
        context.close();
    }

    record Pair(String productCode, String warehouseCode) {
    }
}
