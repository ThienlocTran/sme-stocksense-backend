package com.smartflow.smestocksensebackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * Cấu hình RestClient dùng để gọi đồng bộ sang AI Forecast service (Python/FastAPI, stateless,
 * chỉ chạy XGBoost). Đây là HTTP client bên ngoài đầu tiên của backend (trước đó chỉ có Cloudinary SDK).
 */
@Configuration
public class AiServiceClientConfig {

    @Value("${ai-service.base-url}")
    private String baseUrl;

    @Value("${ai-service.timeout-seconds}")
    private int timeoutSeconds;

    @Bean
    public RestClient aiServiceRestClient() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        int timeoutMillis = timeoutSeconds * 1000;
        requestFactory.setConnectTimeout(timeoutMillis);
        requestFactory.setReadTimeout(timeoutMillis);

        return RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
    }
}
