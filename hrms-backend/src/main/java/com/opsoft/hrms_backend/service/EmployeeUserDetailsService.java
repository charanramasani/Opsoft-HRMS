
package com.opsoft.hrms_backend.service;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import com.opsoft.hrms_backend.entity.Employee;
import com.opsoft.hrms_backend.repository.EmployeeRepository;

@Service
public class EmployeeUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    public EmployeeUserDetailsService(EmployeeRepository employeeRepository) {
        this.employeeRepository = employeeRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String employeeId)
            throws UsernameNotFoundException {

        Employee employee = employeeRepository.findByEmployeeId(employeeId);

        if (employee == null) {
            throw new UsernameNotFoundException(
                "Employee not found: " + employeeId
            );
        }

        // Allow login for ACTIVE and ON_NOTICE employees
        String status = employee.getStatus();

        boolean accountDisabled =
                !("ACTIVE".equalsIgnoreCase(status)
                || "ON_NOTICE".equalsIgnoreCase(status));

        return User.builder()
                .username(employee.getEmployeeId())
                .password(employee.getPasswordHash())
                .roles(employee.getRole())
                .disabled(accountDisabled)
                .build();
    }
}
