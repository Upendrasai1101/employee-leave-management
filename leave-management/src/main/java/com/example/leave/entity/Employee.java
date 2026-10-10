package com.example.leave.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "employees", indexes = {
        @Index(name = "idx_employee_manager", columnList = "manager_id")
})
public class Employee {

    public static final int DEFAULT_ANNUAL_LEAVE_BALANCE = 12;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String email;

    /** BCrypt hash - never the raw password. */
    @Column(nullable = false)
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role = Role.EMPLOYEE;

    private String department;

    /** Id of the manager this employee reports to (null for managers without a superior). */
    @Column(name = "manager_id")
    private Long managerId;

    @Column(nullable = false)
    private int annualLeaveBalance = DEFAULT_ANNUAL_LEAVE_BALANCE;

    /** Optimistic locking - prevents two concurrent approvals from over-deducting the balance. */
    @Version
    private Long version;

    public Employee() {
    }

    public Employee(String name, String email, String password, Role role, String department, Long managerId) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.role = role;
        this.department = department;
        this.managerId = managerId;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Long getManagerId() { return managerId; }
    public void setManagerId(Long managerId) { this.managerId = managerId; }

    public int getAnnualLeaveBalance() { return annualLeaveBalance; }
    public void setAnnualLeaveBalance(int annualLeaveBalance) { this.annualLeaveBalance = annualLeaveBalance; }
}
