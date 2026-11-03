
package com.opsoft.hrms_backend.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opsoft.hrms_backend.entity.Employee;
import com.opsoft.hrms_backend.repository.EmployeeRepository;

@Service
public class PasswordResetService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public PasswordResetService(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder) {

        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void resetPassword(String employeeId, String newPassword) {

        Employee employee =
                employeeRepository.findByEmployeeId(employeeId);

        if (employee == null) {
            throw new IllegalArgumentException(
                    "Employee not found: " + employeeId);
        }

        if (newPassword == null || newPassword.length() < 12) {
            throw new IllegalArgumentException(
                    "Password must contain at least 12 characters.");
        }

        String hashedPassword =
                passwordEncoder.encode(newPassword);

        employee.setPasswordHash(hashedPassword);

        employeeRepository.save(employee);
    }
}
