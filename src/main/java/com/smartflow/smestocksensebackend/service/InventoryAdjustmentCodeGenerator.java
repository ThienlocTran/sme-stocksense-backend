package com.smartflow.smestocksensebackend.service;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class InventoryAdjustmentCodeGenerator {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.BASIC_ISO_DATE;

    public String generate() {
        String date = LocalDate.now().format(DATE_FORMAT);
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase();
        return "PDC-" + date + "-" + suffix;
    }
}
