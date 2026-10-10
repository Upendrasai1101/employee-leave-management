package com.example.leave.security;

import com.example.leave.entity.Employee;
import com.example.leave.entity.Role;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/** Security principal. Carries the employee id so controllers never trust ids sent by the client. */
public class EmployeeUserDetails implements UserDetails {

    private final Long id;
    private final String email;
    private final String password;
    private final Role role;

    public EmployeeUserDetails(Employee employee) {
        this.id = employee.getId();
        this.email = employee.getEmail();
        this.password = employee.getPassword();
        this.role = employee.getRole();
    }

    public Long getId() {
        return id;
    }

    public Role getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }
}
