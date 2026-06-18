package com.smartflow.smestocksensebackend.service;

import com.smartflow.smestocksensebackend.config.JwtProperties;
import com.smartflow.smestocksensebackend.entity.Employee;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.WeakKeyException;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    @PostConstruct
    void init() {
        try {
            this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
        } catch (WeakKeyException exception) {
            throw new IllegalStateException("JWT secret must be at least 32 bytes for HS256.", exception);
        }
    }

    public String generateAccessToken(Employee employee) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(jwtProperties.getExpirationSeconds());

        return Jwts.builder()
                .subject(employee.getEmail())
                .claim("employeeId", employee.getId())
                .claim("role", employee.getRole().getCode().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    public boolean isTokenValid(String token) {
        return parseClaims(token).isPresent();
    }

    public Optional<String> extractSubject(String token) {
        return parseClaims(token).map(Claims::getSubject);
    }

    public Optional<Long> extractEmployeeId(String token) {
        return parseClaims(token)
                .map(claims -> claims.get("employeeId"))
                .filter(Number.class::isInstance)
                .map(Number.class::cast)
                .map(Number::longValue);
    }

    public Optional<String> extractRole(String token) {
        return parseClaims(token)
                .map(claims -> claims.get("role", String.class));
    }

    public Optional<Claims> extractClaims(String token) {
        return parseClaims(token);
    }

    public long getExpirationSeconds() {
        return jwtProperties.getExpirationSeconds();
    }

    private Optional<Claims> parseClaims(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }

        try {
            return Optional.of(Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload());
        } catch (JwtException | IllegalArgumentException exception) {
            return Optional.empty();
        }
    }
}
