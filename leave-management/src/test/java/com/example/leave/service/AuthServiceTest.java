package com.example.leave.service;

import com.example.leave.dto.EmployeeResponse;
import com.example.leave.dto.RegisterRequest;
import com.example.leave.entity.Employee;
import com.example.leave.entity.Role;
import com.example.leave.exception.ConflictException;
import com.example.leave.exception.InvalidLeaveRequestException;
import com.example.leave.repository.EmployeeRepository;
import com.example.leave.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private EmployeeRepository employeeRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private JwtService jwtService;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("Register hashes the password, forces EMPLOYEE role and gives 12 days balance")
    void register_hashesPassword_defaultsRoleAndBalance() {
        when(employeeRepository.existsByEmail("ravi@company.com")).thenReturn(false);
        when(passwordEncoder.encode("Secret@123")).thenReturn("$2a$hashed");
        when(employeeRepository.save(any(Employee.class))).thenAnswer(inv -> {
            Employee e = inv.getArgument(0);
            e.setId(5L);
            return e;
        });

        EmployeeResponse response = authService.register(
                new RegisterRequest("Ravi", "  Ravi@Company.com ", "Secret@123", "QA", null));

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        Employee saved = captor.getValue();

        assertThat(saved.getPassword()).isEqualTo("$2a$hashed");
        assertThat(saved.getRole()).isEqualTo(Role.EMPLOYEE);
        assertThat(saved.getAnnualLeaveBalance()).isEqualTo(12);
        assertThat(saved.getEmail()).isEqualTo("ravi@company.com");
        assertThat(response.id()).isEqualTo(5L);
        assertThat(response.annualLeaveBalance()).isEqualTo(12);
    }

    @Test
    @DisplayName("Register with an existing email is a conflict")
    void register_duplicateEmail_throwsConflict() {
        when(employeeRepository.existsByEmail("ravi@company.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ravi", "ravi@company.com", "Secret@123", "QA", null)))
                .isInstanceOf(ConflictException.class);

        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Register with a managerId that belongs to a non-manager is rejected")
    void register_managerIdNotAManager_throws() {
        Employee notManager = new Employee("Sam", "sam@company.com", "x", Role.EMPLOYEE, "QA", null);
        notManager.setId(7L);
        when(employeeRepository.existsByEmail("ravi@company.com")).thenReturn(false);
        when(employeeRepository.findById(7L)).thenReturn(Optional.of(notManager));

        assertThatThrownBy(() -> authService.register(
                new RegisterRequest("Ravi", "ravi@company.com", "Secret@123", "QA", 7L)))
                .isInstanceOf(InvalidLeaveRequestException.class)
                .hasMessageContaining("not a manager");

        verify(employeeRepository, never()).save(any());
    }
}
