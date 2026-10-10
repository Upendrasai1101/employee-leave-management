package com.example.leave.service;

import com.example.leave.dto.LeaveApplyRequest;
import com.example.leave.dto.LeaveResponse;
import com.example.leave.entity.Employee;
import com.example.leave.entity.LeaveRequest;
import com.example.leave.entity.LeaveStatus;
import com.example.leave.entity.LeaveType;
import com.example.leave.entity.Role;
import com.example.leave.exception.ConflictException;
import com.example.leave.exception.InsufficientLeaveBalanceException;
import com.example.leave.exception.InvalidLeaveRequestException;
import com.example.leave.exception.ResourceNotFoundException;
import com.example.leave.exception.UnauthorizedActionException;
import com.example.leave.repository.EmployeeRepository;
import com.example.leave.repository.LeaveRequestRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LeaveServiceTest {

    private static final LocalDate TODAY = LocalDate.of(2026, 10, 9);
    private static final Long MANAGER_ID = 1L;
    private static final Long EMPLOYEE_ID = 10L;

    @Mock
    private LeaveRequestRepository leaveRequestRepository;
    @Mock
    private EmployeeRepository employeeRepository;

    private LeaveService leaveService;
    private Employee employee;

    @BeforeEach
    void setUp() {
        Clock fixedClock = Clock.fixed(Instant.parse("2026-10-09T00:00:00Z"), ZoneId.of("UTC"));
        leaveService = new LeaveService(leaveRequestRepository, employeeRepository, fixedClock);

        employee = new Employee("Asha", "asha@company.com", "hashed", Role.EMPLOYEE, "Engineering", MANAGER_ID);
        employee.setId(EMPLOYEE_ID);
        employee.setAnnualLeaveBalance(12);
    }

    private LeaveApplyRequest request(LocalDate start, LocalDate end) {
        return new LeaveApplyRequest(LeaveType.CASUAL, start, end, "Family function");
    }

    private LeaveRequest pendingLeave(Long id, int days) {
        LeaveRequest leave = new LeaveRequest();
        leave.setId(id);
        leave.setEmployeeId(EMPLOYEE_ID);
        leave.setLeaveType(LeaveType.CASUAL);
        leave.setStartDate(TODAY.plusDays(5));
        leave.setEndDate(TODAY.plusDays(4 + days));
        leave.setNumberOfDays(days);
        leave.setStatus(LeaveStatus.PENDING);
        leave.setAppliedOn(TODAY);
        return leave;
    }

    // ---------------------------------------------------------------
    // 1. Leave balance / days calculation logic
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Number of days is inclusive of both start and end date")
    void calculateNumberOfDays_isInclusive() {
        assertThat(LeaveService.calculateNumberOfDays(TODAY, TODAY)).isEqualTo(1);
        assertThat(LeaveService.calculateNumberOfDays(TODAY, TODAY.plusDays(4))).isEqualTo(5);
        assertThat(LeaveService.calculateNumberOfDays(LocalDate.of(2026, 12, 30), LocalDate.of(2027, 1, 2)))
                .isEqualTo(4);
    }

    @Test
    @DisplayName("End date before start date is rejected when calculating days")
    void calculateNumberOfDays_endBeforeStart_throws() {
        assertThatThrownBy(() -> LeaveService.calculateNumberOfDays(TODAY.plusDays(3), TODAY))
                .isInstanceOf(InvalidLeaveRequestException.class);
    }

    @Test
    @DisplayName("Valid application is saved as PENDING with calculated days and does NOT touch the balance")
    void applyLeave_valid_savedAsPending_balanceUnchanged() {
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> {
            LeaveRequest l = inv.getArgument(0);
            l.setId(100L);
            return l;
        });

        LeaveResponse response = leaveService.applyLeave(EMPLOYEE_ID,
                request(TODAY.plusDays(2), TODAY.plusDays(4)));

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.numberOfDays()).isEqualTo(3);
        assertThat(response.status()).isEqualTo(LeaveStatus.PENDING);
        assertThat(response.appliedOn()).isEqualTo(TODAY);
        assertThat(employee.getAnnualLeaveBalance()).isEqualTo(12);
        verify(employeeRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // 2. Insufficient balance
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Request is rejected when days exceed remaining balance")
    void applyLeave_insufficientBalance_throws() {
        employee.setAnnualLeaveBalance(2);
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> leaveService.applyLeave(EMPLOYEE_ID,
                request(TODAY.plusDays(1), TODAY.plusDays(3)))) // 3 days > balance 2
                .isInstanceOf(InsufficientLeaveBalanceException.class)
                .hasMessageContaining("requested 3")
                .hasMessageContaining("only 2");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Request using exactly the remaining balance is allowed")
    void applyLeave_exactBalance_allowed() {
        employee.setAnnualLeaveBalance(3);
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveResponse response = leaveService.applyLeave(EMPLOYEE_ID,
                request(TODAY.plusDays(1), TODAY.plusDays(3)));

        assertThat(response.numberOfDays()).isEqualTo(3);
    }

    // ---------------------------------------------------------------
    // 3. Past start date
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Request with a start date in the past is rejected")
    void applyLeave_pastStartDate_throws() {
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> leaveService.applyLeave(EMPLOYEE_ID,
                request(TODAY.minusDays(1), TODAY.plusDays(1))))
                .isInstanceOf(InvalidLeaveRequestException.class)
                .hasMessageContaining("past");

        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("Request starting today is allowed")
    void applyLeave_startToday_allowed() {
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveResponse response = leaveService.applyLeave(EMPLOYEE_ID, request(TODAY, TODAY));

        assertThat(response.numberOfDays()).isEqualTo(1);
    }

    @Test
    @DisplayName("Overlapping with an existing pending/approved leave is rejected")
    void applyLeave_overlapping_throwsConflict() {
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.existsOverlapping(any(), any(), any(), any())).thenReturn(true);

        assertThatThrownBy(() -> leaveService.applyLeave(EMPLOYEE_ID,
                request(TODAY.plusDays(1), TODAY.plusDays(2))))
                .isInstanceOf(ConflictException.class);

        verify(leaveRequestRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // 4. Approval flow deducts balance
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Approving a request deducts the days from the employee's balance")
    void approve_deductsBalance() {
        LeaveRequest leave = pendingLeave(50L, 4);
        when(leaveRequestRepository.findById(50L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveResponse response = leaveService.approve(MANAGER_ID, 50L);

        assertThat(response.status()).isEqualTo(LeaveStatus.APPROVED);
        assertThat(response.reviewedBy()).isEqualTo(MANAGER_ID);

        ArgumentCaptor<Employee> captor = ArgumentCaptor.forClass(Employee.class);
        verify(employeeRepository).save(captor.capture());
        assertThat(captor.getValue().getAnnualLeaveBalance()).isEqualTo(8); // 12 - 4
    }

    @Test
    @DisplayName("Approval fails if the balance was used up since the request was made")
    void approve_balanceNoLongerSufficient_throws() {
        employee.setAnnualLeaveBalance(2);
        LeaveRequest leave = pendingLeave(50L, 4);
        when(leaveRequestRepository.findById(50L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> leaveService.approve(MANAGER_ID, 50L))
                .isInstanceOf(InsufficientLeaveBalanceException.class);

        assertThat(employee.getAnnualLeaveBalance()).isEqualTo(2);
        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.PENDING);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("An already-reviewed request cannot be approved again (no double deduction)")
    void approve_alreadyApproved_throwsConflict() {
        LeaveRequest leave = pendingLeave(50L, 4);
        leave.setStatus(LeaveStatus.APPROVED);
        when(leaveRequestRepository.findById(50L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> leaveService.approve(MANAGER_ID, 50L))
                .isInstanceOf(ConflictException.class);

        assertThat(employee.getAnnualLeaveBalance()).isEqualTo(12);
        verify(employeeRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // 5. Only the employee's own manager can approve/reject
    // ---------------------------------------------------------------

    @Test
    @DisplayName("A manager who is not the employee's manager cannot approve")
    void approve_wrongManager_throwsUnauthorized() {
        Long otherManagerId = 99L;
        LeaveRequest leave = pendingLeave(50L, 4);
        when(leaveRequestRepository.findById(50L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> leaveService.approve(otherManagerId, 50L))
                .isInstanceOf(UnauthorizedActionException.class);

        assertThat(employee.getAnnualLeaveBalance()).isEqualTo(12);
        assertThat(leave.getStatus()).isEqualTo(LeaveStatus.PENDING);
        verify(employeeRepository, never()).save(any());
        verify(leaveRequestRepository, never()).save(any());
    }

    @Test
    @DisplayName("A manager who is not the employee's manager cannot reject")
    void reject_wrongManager_throwsUnauthorized() {
        LeaveRequest leave = pendingLeave(50L, 2);
        when(leaveRequestRepository.findById(50L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));

        assertThatThrownBy(() -> leaveService.reject(99L, 50L, "nope"))
                .isInstanceOf(UnauthorizedActionException.class);

        verify(leaveRequestRepository, never()).save(any());
    }

    // ---------------------------------------------------------------
    // Rejection does not deduct balance
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Rejecting stores the comment and does NOT deduct the balance")
    void reject_doesNotDeductBalance() {
        LeaveRequest leave = pendingLeave(50L, 4);
        when(leaveRequestRepository.findById(50L)).thenReturn(Optional.of(leave));
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        when(leaveRequestRepository.save(any(LeaveRequest.class))).thenAnswer(inv -> inv.getArgument(0));

        LeaveResponse response = leaveService.reject(MANAGER_ID, 50L, "Release week, please reschedule");

        assertThat(response.status()).isEqualTo(LeaveStatus.REJECTED);
        assertThat(response.managerComment()).isEqualTo("Release week, please reschedule");
        assertThat(response.reviewedBy()).isEqualTo(MANAGER_ID);
        assertThat(employee.getAnnualLeaveBalance()).isEqualTo(12);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Approving an unknown leave id returns not found")
    void approve_unknownLeave_throwsNotFound() {
        when(leaveRequestRepository.findById(404L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> leaveService.approve(MANAGER_ID, 404L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    // ---------------------------------------------------------------
    // Team view
    // ---------------------------------------------------------------

    @Test
    @DisplayName("Team view returns an empty list when the manager has no direct reports")
    void getTeamRequests_noReports_returnsEmpty() {
        when(employeeRepository.findByManagerId(MANAGER_ID)).thenReturn(List.of());

        assertThat(leaveService.getTeamRequests(MANAGER_ID)).isEmpty();
        verify(leaveRequestRepository, never()).findByEmployeeIdInOrderByAppliedOnDescIdDesc(any());
    }

    @Test
    @DisplayName("Team view returns leave requests of direct reports with employee names")
    void getTeamRequests_returnsReportsLeaves() {
        LeaveRequest leave = pendingLeave(50L, 2);
        when(employeeRepository.findByManagerId(MANAGER_ID)).thenReturn(List.of(employee));
        when(leaveRequestRepository.findByEmployeeIdInOrderByAppliedOnDescIdDesc(any()))
                .thenReturn(List.of(leave));

        List<LeaveResponse> team = leaveService.getTeamRequests(MANAGER_ID);

        assertThat(team).hasSize(1);
        assertThat(team.get(0).employeeName()).isEqualTo("Asha");
    }
}
