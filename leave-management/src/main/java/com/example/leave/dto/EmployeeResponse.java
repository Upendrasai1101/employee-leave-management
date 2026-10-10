package com.example.leave.dto;

import com.example.leave.entity.Employee;
import com.example.leave.entity.Role;

/** Public view of an employee - never includes the password hash. */
public record EmployeeResponse(
        Long id,
        String name,
        String email,
        Role role,
        String department,
        Long managerId,
        int annualLeaveBalance
) {
    public static EmployeeResponse from(Employee e) {
        return new EmployeeResponse(e.getId(), e.getName(), e.getEmail(), e.getRole(),
                e.getDepartment(), e.getManagerId(), e.getAnnualLeaveBalance());
    }
}
