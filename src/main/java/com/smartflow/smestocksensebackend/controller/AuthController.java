package com.smartflow.smestocksensebackend.controller;

import com.smartflow.smestocksensebackend.dto.auth.ChangePasswordRequest;
import com.smartflow.smestocksensebackend.dto.auth.ChangePasswordResponse;
import com.smartflow.smestocksensebackend.dto.auth.LoginRequest;
import com.smartflow.smestocksensebackend.dto.auth.LoginResponse;
import com.smartflow.smestocksensebackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<ChangePasswordResponse> changePassword(
            @Valid @RequestBody ChangePasswordRequest request
    ) {
        return ResponseEntity.ok(authService.changeOwnPassword(request));
    }
}
