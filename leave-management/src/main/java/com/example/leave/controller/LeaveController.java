package com.example.leave.controller;

import com.example.leave.dto.BalanceResponse;
import com.example.leave.dto.LeaveApplyRequest;
import com.example.leave.dto.LeaveResponse;
import com.example.leave.dto.LeaveReviewRequest;
import com.example.leave.security.EmployeeUserDetails;
import com.example.leave.service.LeaveService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    // ---------------- EMPLOYEE ----------------

    @PostMapping
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<LeaveResponse> apply(@AuthenticationPrincipal EmployeeUserDetails user,
                                               @Valid @RequestBody LeaveApplyRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(leaveService.applyLeave(user.getId(), request));
    }

    @GetMapping("/my-history")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<LeaveResponse>> myHistory(@AuthenticationPrincipal EmployeeUserDetails user) {
        return ResponseEntity.ok(leaveService.getMyHistory(user.getId()));
    }

    @GetMapping("/balance")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<BalanceResponse> balance(@AuthenticationPrincipal EmployeeUserDetails user) {
        return ResponseEntity.ok(leaveService.getBalance(user.getId()));
    }

    // ---------------- MANAGER ----------------

    @GetMapping("/team")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<List<LeaveResponse>> team(@AuthenticationPrincipal EmployeeUserDetails user) {
        return ResponseEntity.ok(leaveService.getTeamRequests(user.getId()));
    }

    @PutMapping("/{id}/approve")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<LeaveResponse> approve(@AuthenticationPrincipal EmployeeUserDetails user,
                                                 @PathVariable Long id) {
        return ResponseEntity.ok(leaveService.approve(user.getId(), id));
    }

    @PutMapping("/{id}/reject")
    @PreAuthorize("hasRole('MANAGER')")
    public ResponseEntity<LeaveResponse> reject(@AuthenticationPrincipal EmployeeUserDetails user,
                                                @PathVariable Long id,
                                                @Valid @RequestBody(required = false) LeaveReviewRequest request) {
        String comment = request != null ? request.comment() : null;
        return ResponseEntity.ok(leaveService.reject(user.getId(), id, comment));
    }
}
