package com.smartflow.smestocksensebackend.repository;

import com.smartflow.smestocksensebackend.entity.Employee;
import com.smartflow.smestocksensebackend.entity.RoleCode;
import com.smartflow.smestocksensebackend.entity.EmployeeStatus;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.Optional;
import java.util.List;
import java.util.Collection;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    @EntityGraph(attributePaths = "role")
    Optional<Employee> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "role")
    Optional<Employee> findById(Long id);

    @EntityGraph(attributePaths = "role")
    List<Employee> findByRole_CodeInAndStatus(Collection<RoleCode> roleCodes, EmployeeStatus status);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    boolean existsByPhone(String phone);

    boolean existsByPhoneAndIdNot(String phone, Long id);
}
