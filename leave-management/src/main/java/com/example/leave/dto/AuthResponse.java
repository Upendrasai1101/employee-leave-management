package com.example.leave.dto;

import com.example.leave.entity.Role;

public record AuthResponse(
        String token,
        String tokenType,
        Long employeeId,
        String email,
        Role role
) {
    public static AuthResponse bearer(String token, Long employeeId, String email, Role role) {
        return new AuthResponse(token, "Bearer", employeeId, email, role);
    }
}
