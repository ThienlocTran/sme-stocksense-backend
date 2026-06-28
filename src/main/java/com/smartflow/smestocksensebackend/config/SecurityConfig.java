package com.smartflow.smestocksensebackend.config;

import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

        private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource(
                        @Value("${app.cors.allowed-origins:http://localhost:5173,http://127.0.0.1:5173,http://localhost:3000,http://127.0.0.1:3000}") String allowedOrigins) {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOrigins(Arrays.stream(allowedOrigins.split(","))
                                .map(String::trim)
                                .filter(origin -> !origin.isBlank())
                                .toList());
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*")); // Mở toàn bộ headers để pass Preflight
                configuration.setExposedHeaders(List.of("Authorization"));
                configuration.setAllowCredentials(true);
                configuration.setMaxAge(3600L); // Cache Preflight 1 giờ, giảm tải OPTIONS request

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(
                        HttpSecurity http,
                        JwtAuthenticationFilter jwtAuthenticationFilter,
                        CorsConfigurationSource corsConfigurationSource) throws Exception {
                return http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource))
                                .csrf(AbstractHttpConfigurer::disable)
                                .formLogin(AbstractHttpConfigurer::disable)
                                .httpBasic(AbstractHttpConfigurer::disable)
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        log.warn("AuthenticationEntryPoint triggered for {} {} -> {}",
                                                                        request.getMethod(), request.getRequestURI(),
                                                                        authException.getClass().getName());
                                                        writeError(
                                                                        response,
                                                                        HttpStatus.UNAUTHORIZED,
                                                                        "Chưa xác thực.");
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        log.warn("AccessDeniedHandler triggered for {} {} -> {}",
                                                                        request.getMethod(), request.getRequestURI(),
                                                                        accessDeniedException.getClass().getName());
                                                        writeError(
                                                                        response,
                                                                        HttpStatus.FORBIDDEN,
                                                                        "Không có quyền truy cập.");
                                                }))
                                .authorizeHttpRequests(authorize -> authorize
                                                // Luôn permit toàn bộ OPTIONS preflight để CORS không bị chặn
                                                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                                                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                                                .requestMatchers(HttpMethod.PATCH, "/api/auth/change-password")
                                                .authenticated()
                                                .requestMatchers(HttpMethod.POST, "/api/employees").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PUT, "/api/employees/*").hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/employees/*/reset-password")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/employees/*/lock")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.PATCH, "/api/employees/*/unlock")
                                                .hasRole("ADMIN")
                                                .requestMatchers(HttpMethod.GET, "/api/employees")
                                                .hasAnyRole("ADMIN", "MANAGER")
                                                .requestMatchers(HttpMethod.GET, "/api/warehouses")
                                                .hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.POST, "/api/warehouses")
                                                .hasAnyRole("ADMIN", "MANAGER")
                                                .requestMatchers(HttpMethod.PUT, "/api/warehouses/*")
                                                .hasAnyRole("ADMIN", "MANAGER")
                                                .requestMatchers(HttpMethod.DELETE, "/api/warehouses/*")
                                                .hasAnyRole("ADMIN", "MANAGER")
                                                .requestMatchers(HttpMethod.POST, "/api/categories")
                                                .hasAnyRole("ADMIN", "MANAGER")
                                                .requestMatchers(HttpMethod.PUT, "/api/categories/*")
                                                .hasAnyRole("ADMIN", "MANAGER")
                                                .requestMatchers(HttpMethod.PATCH, "/api/categories/*/disable")
                                                .hasAnyRole("ADMIN", "MANAGER")
                                                .requestMatchers(HttpMethod.GET, "/api/categories")
                                                .hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.GET, "/api/partners")
                                                .hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.POST, "/api/partners")
                                                .hasAnyRole("ADMIN", "MANAGER")
                                                .requestMatchers(HttpMethod.PUT, "/api/partners/*")
                                                .hasAnyRole("ADMIN", "MANAGER")
                                                .requestMatchers(HttpMethod.GET, "/api/import-receipts/my")
                                                .hasAnyRole("ADMIN", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.GET, "/api/import-receipts/*")
                                                .hasAnyRole("ADMIN", "MANAGER", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.GET, "/api/import-receipts/*")
                                                .hasAnyRole("ADMIN", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.PUT, "/api/import-receipts/*/arrival")
                                                .hasAnyRole("ADMIN", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.PUT, "/api/import-receipts/*/submit")
                                                .hasAnyRole("ADMIN", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.PUT, "/api/import-receipts/*/inspect")
                                                .hasAnyRole("ADMIN", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.PUT, "/api/import-receipts/*/hoan-tat")
                                                .hasAnyRole("ADMIN", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.PUT, "/api/v1/phieu-nhap/*/hoan-tat")
                                                .hasAnyRole("ADMIN", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/import-receipts/*/discrepancy-report")
                                                .hasAnyRole("ADMIN", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.PUT, "/api/import-receipts/*/cancel")
                                                .hasAnyRole("ADMIN", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.PUT, "/api/import-receipts/*/draft")
                                                .hasAnyRole("ADMIN", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.PUT, "/api/import-receipts/*")
                                                .hasAnyRole("ADMIN", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.POST, "/api/import-receipts/*/items")
                                                .hasAnyRole("ADMIN", "EMPLOYEE")
                                                .requestMatchers(HttpMethod.POST, "/api/import-receipts")
                                                .hasAnyRole("ADMIN", "EMPLOYEE")
                                                .anyRequest().permitAll())
                                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                                .build();
        }

        private static void writeError(HttpServletResponse response, HttpStatus status, String message)
                        throws IOException {
                response.setStatus(status.value());
                response.setContentType("application/json;charset=UTF-8");
                response.getWriter().write("{\"message\":\"" + message + "\",\"errors\":{}}");
        }
}
