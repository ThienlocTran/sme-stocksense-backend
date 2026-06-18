package com.smartflow.smestocksensebackend.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertTrue;

class SecurityConfigTest {

    @Test
    void corsAllowedOrigins_shouldHaveSafeDefault() throws NoSuchMethodException {
        Method method = SecurityConfig.class.getDeclaredMethod("corsConfigurationSource", String.class);
        Value annotation = method.getParameters()[0].getAnnotation(Value.class);

        assertTrue(annotation.value().contains(":"), "CORS origins placeholder must define a default value.");
        assertTrue(annotation.value().contains("http://localhost:5173"));
    }
}
