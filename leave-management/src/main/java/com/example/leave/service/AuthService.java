package com.example.leave.service;

import com.example.leave.dto.AuthResponse;
import com.example.leave.dto.EmployeeResponse;
import com.example.leave.dto.LoginRequest;
import com.example.leave.dto.RegisterRequest;
import com.example.leave.entity.Employee;
import com.example.leave.entity.Role;
import com.example.leave.exception.ConflictException;
import com.example.leave.exception.InvalidLeaveRequestException;
import com.example.leave.exception.ResourceNotFoundException;
import com.example.leave.repository.EmployeeRepository;
import com.example.leave.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;

    public AuthService(EmployeeRepository employeeRepository,
                       PasswordEncoder passwordEncoder,
                       AuthenticationManager authenticationManager,
                       JwtService jwtService) {
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
    }

    /** Registers a new employee. Role is always EMPLOYEE here - it cannot be chosen by the caller. */
    @Transactional
    public EmployeeResponse register(RegisterRequest request) {
        String email = request.email().trim().toLowerCase();
        if (employeeRepository.existsByEmail(email)) {
            throw new ConflictException("An employee with email '" + email + "' already exists");
        }

        if (request.managerId() != null) {
            Employee manager = employeeRepository.findById(request.managerId())
                    .orElseThrow(() -> new ResourceNotFoundException(
                            "Manager with id " + request.managerId() + " not found"));
            if (manager.getRole() != Role.MANAGER) {
                throw new InvalidLeaveRequestException("Employee " + request.managerId() + " is not a manager");
            }
        }

        Employee employee = new Employee(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                Role.EMPLOYEE,
                request.department(),
                request.managerId());
        employee.setAnnualLeaveBalance(Employee.DEFAULT_ANNUAL_LEAVE_BALANCE);

        return EmployeeResponse.from(employeeRepository.save(employee));
    }

    public AuthResponse login(LoginRequest request) {
        String email = request.email().trim().toLowerCase();
        // Throws BadCredentialsException (-> 401) when email/password do not match
        authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, request.password()));

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        String token = jwtService.generateToken(employee);
        return AuthResponse.bearer(token, employee.getId(), employee.getEmail(), employee.getRole());
    }
}
