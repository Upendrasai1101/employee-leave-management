package com.example.leave.repository;

import com.example.leave.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmail(String email);

    /** All employees who report to the given manager. */
    List<Employee> findByManagerId(Long managerId);
}
