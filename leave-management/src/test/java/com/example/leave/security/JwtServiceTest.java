package com.example.leave.security;

import com.example.leave.entity.Employee;
import com.example.leave.entity.Role;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private static final String SECRET = "unit-test-secret-key-that-is-at-least-32-chars-long";

    private JwtService jwtService;
    private Employee employee;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService(SECRET, 60_000);
        employee = new Employee("Asha", "asha@company.com", "hashed", Role.EMPLOYEE, "Eng", 1L);
        employee.setId(10L);
    }

    @Test
    @DisplayName("Generated token carries the email and validates for the same user")
    void generatedToken_isValid() {
        String token = jwtService.generateToken(employee);

        assertThat(jwtService.extractEmail(token)).isEqualTo("asha@company.com");
        assertThat(jwtService.isTokenValid(token, new EmployeeUserDetails(employee))).isTrue();
    }

    @Test
    @DisplayName("Tokens signed with a different key, and garbage tokens, are invalid")
    void forgedOrGarbageToken_isInvalid() {
        JwtService attacker = new JwtService("a-completely-different-secret-key-also-32-plus-chars", 60_000);
        String forged = attacker.generateToken(employee);

        assertThat(jwtService.isTokenValid(forged, new EmployeeUserDetails(employee))).isFalse();
        assertThat(jwtService.isTokenValid("not-a-jwt", new EmployeeUserDetails(employee))).isFalse();
    }

    @Test
    @DisplayName("Expired tokens are invalid")
    void expiredToken_isInvalid() {
        JwtService shortLived = new JwtService(SECRET, -1_000);
        String token = shortLived.generateToken(employee);

        assertThat(jwtService.isTokenValid(token, new EmployeeUserDetails(employee))).isFalse();
    }

    @Test
    @DisplayName("A secret shorter than 32 characters is refused at startup")
    void shortSecret_isRejected() {
        assertThatThrownBy(() -> new JwtService("too-short", 1000))
                .isInstanceOf(IllegalStateException.class);
    }
}
