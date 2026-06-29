package com.smartflow.smestocksensebackend;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

/**
 * Kiểm tra Spring context khởi động đầy đủ.
 *
 * <p>Cấu hình DB (DB_URL/DB_USERNAME/DB_PASSWORD/JWT_SECRET) được nạp từ file
 * {@code .env} ở thư mục module thông qua {@code spring.config.import} trong
 * application.yml. Flyway được tắt trong test này để không chạy migration lên
 * DB dùng chung (tránh thao tác ghi và xung đột version), chỉ xác nhận toàn bộ
 * bean của ứng dụng wiring được với schema sẵn có.</p>
 */
@SpringBootTest
@TestPropertySource(properties = "spring.flyway.enabled=false")
class SmeStocksenseBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
