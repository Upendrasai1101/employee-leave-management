package com.example.leave.service;

import com.example.leave.dto.BalanceResponse;
import com.example.leave.dto.LeaveApplyRequest;
import com.example.leave.dto.LeaveResponse;
import com.example.leave.entity.Employee;
import com.example.leave.entity.LeaveRequest;
import com.example.leave.entity.LeaveStatus;
import com.example.leave.exception.ConflictException;
import com.example.leave.exception.InsufficientLeaveBalanceException;
import com.example.leave.exception.InvalidLeaveRequestException;
import com.example.leave.exception.ResourceNotFoundException;
import com.example.leave.exception.UnauthorizedActionException;
import com.example.leave.repository.EmployeeRepository;
import com.example.leave.repository.LeaveRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LeaveService {

    private final LeaveRequestRepository leaveRequestRepository;
    private final EmployeeRepository employeeRepository;
    private final Clock clock;

    public LeaveService(LeaveRequestRepository leaveRequestRepository,
                        EmployeeRepository employeeRepository,
                        Clock clock) {
        this.leaveRequestRepository = leaveRequestRepository;
        this.employeeRepository = employeeRepository;
        this.clock = clock;
    }

    // ------------------------------------------------------------------
    // Employee operations
    // ------------------------------------------------------------------

    @Transactional
    public LeaveResponse applyLeave(Long employeeId, LeaveApplyRequest request) {
        Employee employee = findEmployee(employeeId);
        LocalDate today = LocalDate.now(clock);

        if (request.startDate().isBefore(today)) {
            throw new InvalidLeaveRequestException("Start date cannot be in the past");
        }
        // Also throws if endDate < startDate
        int days = calculateNumberOfDays(request.startDate(), request.endDate());

        if (days > employee.getAnnualLeaveBalance()) {
            throw new InsufficientLeaveBalanceException(days, employee.getAnnualLeaveBalance());
        }

        boolean overlaps = leaveRequestRepository.existsOverlapping(
                employeeId, request.startDate(), request.endDate(),
                List.of(LeaveStatus.PENDING, LeaveStatus.APPROVED));
        if (overlaps) {
            throw new ConflictException("You already have a pending or approved leave overlapping these dates");
        }

        LeaveRequest leave = new LeaveRequest();
        leave.setEmployeeId(employeeId);
        leave.setLeaveType(request.leaveType());
        leave.setStartDate(request.startDate());
        leave.setEndDate(request.endDate());
        leave.setNumberOfDays(days);
        leave.setReason(request.reason());
        leave.setStatus(LeaveStatus.PENDING);
        leave.setAppliedOn(today);

        LeaveRequest saved = leaveRequestRepository.save(leave);
        return LeaveResponse.from(saved, employee.getName());
    }

    @Transactional(readOnly = true)
    public List<LeaveResponse> getMyHistory(Long employeeId) {
        Employee employee = findEmployee(employeeId);
        return leaveRequestRepository.findByEmployeeIdOrderByAppliedOnDescIdDesc(employeeId).stream()
                .map(l -> LeaveResponse.from(l, employee.getName()))
                .toList();
    }

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(Long employeeId) {
        Employee employee = findEmployee(employeeId);
        return new BalanceResponse(employee.getId(), employee.getName(), employee.getAnnualLeaveBalance());
    }

    // ------------------------------------------------------------------
    // Manager operations
    // ------------------------------------------------------------------

    @Transactional(readOnly = true)
    public List<LeaveResponse> getTeamRequests(Long managerId) {
        List<Employee> team = employeeRepository.findByManagerId(managerId);
        if (team.isEmpty()) {
            return List.of();
        }
        Map<Long, String> namesById = team.stream()
                .collect(Collectors.toMap(Employee::getId, Employee::getName, (a, b) -> a));

        return leaveRequestRepository.findByEmployeeIdInOrderByAppliedOnDescIdDesc(namesById.keySet()).stream()
                .map(l -> LeaveResponse.from(l, namesById.get(l.getEmployeeId())))
                .toList();
    }

    @Transactional
    public LeaveResponse approve(Long managerId, Long leaveId) {
        LeaveRequest leave = findLeave(leaveId);
        Employee employee = findEmployee(leave.getEmployeeId());

        assertIsEmployeesManager(managerId, employee);
        assertPending(leave);

        // Balance may have changed since the request was made (e.g. other approvals), so re-check.
        if (leave.getNumberOfDays() > employee.getAnnualLeaveBalance()) {
            throw new InsufficientLeaveBalanceException(leave.getNumberOfDays(), employee.getAnnualLeaveBalance());
        }

        employee.setAnnualLeaveBalance(employee.getAnnualLeaveBalance() - leave.getNumberOfDays());
        leave.setStatus(LeaveStatus.APPROVED);
        leave.setReviewedBy(managerId);

        employeeRepository.save(employee);
        LeaveRequest saved = leaveRequestRepository.save(leave);
        return LeaveResponse.from(saved, employee.getName());
    }

    @Transactional
    public LeaveResponse reject(Long managerId, Long leaveId, String comment) {
        LeaveRequest leave = findLeave(leaveId);
        Employee employee = findEmployee(leave.getEmployeeId());

        assertIsEmployeesManager(managerId, employee);
        assertPending(leave);

        // No balance deduction on rejection
        leave.setStatus(LeaveStatus.REJECTED);
        leave.setReviewedBy(managerId);
        leave.setManagerComment(comment);

        LeaveRequest saved = leaveRequestRepository.save(leave);
        return LeaveResponse.from(saved, employee.getName());
    }

    // ------------------------------------------------------------------
    // Helpers
    // ------------------------------------------------------------------

    /** Inclusive number of calendar days between start and end (same day = 1 day). */
    public static int calculateNumberOfDays(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new InvalidLeaveRequestException("End date cannot be before start date");
        }
        return (int) ChronoUnit.DAYS.between(startDate, endDate) + 1;
    }

    private Employee findEmployee(Long id) {
        return employeeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Employee with id " + id + " not found"));
    }

    private LeaveRequest findLeave(Long id) {
        return leaveRequestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Leave request with id " + id + " not found"));
    }

    private void assertIsEmployeesManager(Long managerId, Employee employee) {
        if (employee.getManagerId() == null || !employee.getManagerId().equals(managerId)) {
            throw new UnauthorizedActionException(
                    "Only the employee's own manager can approve or reject this leave request");
        }
    }

    private void assertPending(LeaveRequest leave) {
        if (leave.getStatus() != LeaveStatus.PENDING) {
            throw new ConflictException("Leave request " + leave.getId() + " has already been "
                    + leave.getStatus().name().toLowerCase());
        }
    }
}
