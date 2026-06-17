package com.smartflow.smestocksensebackend.seeder;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.Role;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.repository.EmployeeRepository;
import com.smartflow.smestocksensebackend.repository.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class UserSeeder implements CommandLineRunner {

    private final EmployeeRepository employeeRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public UserSeeder(EmployeeRepository employeeRepository, 
                      RoleRepository roleRepository, 
                      PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        System.out.println("🔄 [UserSeeder] Đang kiểm tra và đồng bộ dữ liệu người dùng...");

        seedUser("Thien Loc IT", "thienloct.it@gmail.com", "12345678", RoleCode.ADMIN);
        seedUser("Tran Thien Loc Nina", "tranthienloc.nina@gmail.com", "12345678", RoleCode.MANAGER);
        seedUser("Tran Thien Loc", "tranthienloc21102005@gmail.com", "12345678", RoleCode.EMPLOYEE);
        seedUser("Admin Dự Phòng", "admin@example.com", "12345678", RoleCode.ADMIN);

        System.out.println("✅ [UserSeeder] Đã hoàn thành đồng bộ dữ liệu người dùng.");
    }

    private void seedUser(String fullName, String email, String rawPassword, RoleCode roleCode) {
        // 1. Kiểm tra Role có tồn tại chưa
        Optional<Role> roleOpt = roleRepository.findByCode(roleCode);
        if (roleOpt.isEmpty()) {
            System.err.println("❌ [UserSeeder] Vai trò " + roleCode + " chưa tồn tại trong bảng vai_tro, bỏ qua " + email);
            return;
        }

        Role role = roleOpt.get();
        
        // 2. Hash mật khẩu an toàn
        String hashedPassword = passwordEncoder.encode(rawPassword);

        // 3. Kiểm tra xem user đã tồn tại chưa bằng email
        Optional<Employee> existingEmployee = employeeRepository.findByEmailIgnoreCase(email);
        
        if (existingEmployee.isPresent()) {
            Employee employee = existingEmployee.get();
            // Cập nhật lại role và password
            employee.setPasswordHash(hashedPassword);
            employee.setRole(role);
            employeeRepository.save(employee);
            System.out.println("   -> Đã cập nhật (Reset mật khẩu & phân quyền): " + email + " (" + roleCode + ")");
        } else {
            // Chèn mới hoàn toàn
            Employee newEmployee = new Employee();
            newEmployee.setFullName(fullName);
            newEmployee.setEmail(email);
            newEmployee.setPasswordHash(hashedPassword);
            newEmployee.setRole(role);
            employeeRepository.save(newEmployee);
            System.out.println("   -> Đã tạo mới: " + email + " (" + roleCode + ")");
        }
    }
}
