package com.smartflow.smestocksensebackend;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

@SpringBootApplication
public class SmeStocksenseBackendApplication {

    public static void main(String[] args) {
        ConfigurableApplicationContext context = SpringApplication.run(SmeStocksenseBackendApplication.class, args);

        // --- CODE TỰ FIX MẬT KHẨU KHI NHẤN NÚT PLAY ---
        try {
            EmployeeRepository repo = context.getBean(EmployeeRepository.class);
            repo.findByEmailIgnoreCase("admin@example.com").ifPresent(admin -> {
                BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
                String newHash = encoder.encode("123456");
                admin.setPasswordHash(newHash);
                repo.save(admin);
                System.out.println("🔥 [AUTO-FIX] Mật khẩu admin đã ép về 123456! Hash mới: " + newHash);
            });
        } catch (Exception e) {
            System.out.println("⚠️ [AUTO-FIX] Chưa fix được: " + e.getMessage());
        }
        // ----------------------------------------------
    }
}