package com.example.leave.dto;

import com.example.leave.entity.LeaveType;
import com.example.leave.validation.ValidDateRange;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

@ValidDateRange
public record LeaveApplyRequest(
        @NotNull(message = "leaveType is required (SICK, CASUAL or EARNED)")
        LeaveType leaveType,

        @NotNull(message = "startDate is required")
        @FutureOrPresent(message = "startDate cannot be in the past")
        LocalDate startDate,

        @NotNull(message = "endDate is required")
        LocalDate endDate,

        @NotBlank(message = "reason is required")
        @Size(max = 500, message = "reason must be at most 500 characters")
        String reason
) {
}
