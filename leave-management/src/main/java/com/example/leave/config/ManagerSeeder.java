package com.example.leave.config;

import com.example.leave.entity.Employee;
import com.example.leave.entity.Role;
import com.example.leave.repository.EmployeeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Registration always creates EMPLOYEEs, so without this nobody could ever approve a request.
 * Seeds one MANAGER account on startup (credentials come from application.yml / env vars).
 */
@Component
public class ManagerSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(ManagerSeeder.class);

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed.manager.enabled:true}")
    private boolean enabled;
    @Value("${app.seed.manager.name}")
    private String name;
    @Value("${app.seed.manager.email}")
    private String email;
    @Value("${app.seed.manager.password}")
    private String password;
    @Value("${app.seed.manager.department}")
    private String department;

    public ManagerSeeder(EmployeeRepository employeeRepository, PasswordEncoder passwordEncoder) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!enabled || employeeRepository.existsByEmail(email.toLowerCase())) {
            return;
        }
        Employee manager = new Employee(name, email.toLowerCase(), passwordEncoder.encode(password),
                Role.MANAGER, department, null);
        Employee saved = employeeRepository.save(manager);
        log.info("Seeded default manager '{}' with id {}", saved.getEmail(), saved.getId());
    }
}
