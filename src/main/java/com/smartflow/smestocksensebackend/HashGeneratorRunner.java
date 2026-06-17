package com.smartflow.smestocksensebackend;

import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class HashGeneratorRunner implements CommandLineRunner {

    private final PasswordEncoder passwordEncoder;

    public HashGeneratorRunner(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        String rawPassword = "123456";
        String encodedPassword = passwordEncoder.encode(rawPassword);
        System.out.println("\n========================================================");
        System.out.println("🔥 [HASH GENERATOR] Mật khẩu gốc: " + rawPassword);
        System.out.println("🔥 [HASH GENERATOR] Mã Hash BCrypt chuẩn sinh ra từ code:");
        System.out.println(encodedPassword);
        System.out.println("========================================================\n");
    }
}
