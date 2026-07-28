package com.smartflow.smestocksensebackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync // T184: Bật cơ chế xử lý bất đồng bộ cho InventoryAlertEventListener
public class SmeStocksenseBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmeStocksenseBackendApplication.class, args);
    }
}
