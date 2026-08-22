package com.smartflow.smestocksensebackend.config;

import lombok.extern.slf4j.Slf4j;
import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Slf4j
@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy(DataSource dataSource) {
        return flyway -> {
            log.info(">>> [FLYWAY] Bắt đầu đồng bộ cơ sở dữ liệu...");
            try (var conn = dataSource.getConnection(); var stmt = conn.createStatement()) {
                stmt.executeUpdate("DELETE FROM flyway_schema_history WHERE version IN ('44', '45')");
            } catch (Exception e) {
                log.warn("Không thể xóa record V44/V45 trong flyway_schema_history: {}", e.getMessage());
            }
            try {
                flyway.repair();
            } catch (Exception ignored) {}
            var result = flyway.migrate();
            log.info(">>> [FLYWAY] Hoàn tất migration! Schema version: {}, Số script đã thực thi: {}",
                    result.targetSchemaVersion, result.migrationsExecuted);
        };
    }
}

