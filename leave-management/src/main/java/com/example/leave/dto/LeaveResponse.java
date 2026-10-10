package com.example.leave.dto;

import com.example.leave.entity.LeaveRequest;
import com.example.leave.entity.LeaveStatus;
import com.example.leave.entity.LeaveType;

import java.time.LocalDate;

public record LeaveResponse(
        Long id,
        Long employeeId,
        String employeeName,
        LeaveType leaveType,
        LocalDate startDate,
        LocalDate endDate,
        int numberOfDays,
        LeaveStatus status,
        String reason,
        LocalDate appliedOn,
        Long reviewedBy,
        String managerComment
) {
    public static LeaveResponse from(LeaveRequest l, String employeeName) {
        return new LeaveResponse(l.getId(), l.getEmployeeId(), employeeName, l.getLeaveType(),
                l.getStartDate(), l.getEndDate(), l.getNumberOfDays(), l.getStatus(), l.getReason(),
                l.getAppliedOn(), l.getReviewedBy(), l.getManagerComment());
    }
}
