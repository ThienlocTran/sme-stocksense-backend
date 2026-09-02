package com.smartflow.smestocksensebackend;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

import java.util.TimeZone;

@SpringBootApplication
@EnableAsync // T184: Bật cơ chế xử lý bất đồng bộ cho InventoryAlertEventListener
public class SmeStocksenseBackendApplication {

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
    }

    public static void main(String[] args) {
        SpringApplication.run(SmeStocksenseBackendApplication.class, args);
    }
}

