package com.example.leave.dto;

public record BalanceResponse(
        Long employeeId,
        String employeeName,
        int remainingLeaveBalance
) {
}
