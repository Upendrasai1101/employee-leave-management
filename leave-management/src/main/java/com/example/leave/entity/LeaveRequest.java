package com.example.leave.entity;

import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "leave_requests", indexes = {
        @Index(name = "idx_leave_employee", columnList = "employee_id"),
        @Index(name = "idx_leave_status", columnList = "status")
})
public class LeaveRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false)
    private Long employeeId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveType leaveType;

    @Column(nullable = false)
    private LocalDate startDate;

    @Column(nullable = false)
    private LocalDate endDate;

    /** Calculated (inclusive) from startDate and endDate - never taken from the client. */
    @Column(nullable = false)
    private int numberOfDays;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LeaveStatus status = LeaveStatus.PENDING;

    @Column(length = 500)
    private String reason;

    @Column(nullable = false)
    private LocalDate appliedOn;

    /** Id of the manager who approved/rejected the request. */
    @Column(name = "reviewed_by")
    private Long reviewedBy;

    /** Optional comment left by the manager (mainly on rejection). */
    @Column(length = 500)
    private String managerComment;

    public LeaveRequest() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getEmployeeId() { return employeeId; }
    public void setEmployeeId(Long employeeId) { this.employeeId = employeeId; }

    public LeaveType getLeaveType() { return leaveType; }
    public void setLeaveType(LeaveType leaveType) { this.leaveType = leaveType; }

    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }

    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }

    public int getNumberOfDays() { return numberOfDays; }
    public void setNumberOfDays(int numberOfDays) { this.numberOfDays = numberOfDays; }

    public LeaveStatus getStatus() { return status; }
    public void setStatus(LeaveStatus status) { this.status = status; }

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }

    public LocalDate getAppliedOn() { return appliedOn; }
    public void setAppliedOn(LocalDate appliedOn) { this.appliedOn = appliedOn; }

    public Long getReviewedBy() { return reviewedBy; }
    public void setReviewedBy(Long reviewedBy) { this.reviewedBy = reviewedBy; }

    public String getManagerComment() { return managerComment; }
    public void setManagerComment(String managerComment) { this.managerComment = managerComment; }
}
