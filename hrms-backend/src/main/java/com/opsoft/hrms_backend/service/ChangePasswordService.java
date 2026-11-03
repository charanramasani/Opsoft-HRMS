
package com.opsoft.hrms_backend.service;

import java.nio.charset.StandardCharsets;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opsoft.hrms_backend.dto.ChangePasswordRequest;
import com.opsoft.hrms_backend.entity.Employee;
import com.opsoft.hrms_backend.repository.EmployeeRepository;

@Service
public class ChangePasswordService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    public ChangePasswordService(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder) {

        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public void changePassword(
            String employeeId,
            ChangePasswordRequest request) {

        // Find the logged-in employee
        Employee employee =
                employeeRepository.findByEmployeeId(employeeId);

        if (employee == null) {
            throw new IllegalArgumentException(
                    "Employee account not found.");
        }

        // Verify current password
        if (!passwordEncoder.matches(
                request.getCurrentPassword(),
                employee.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "Current password is incorrect.");
        }

        String newPassword = request.getNewPassword();

        if (newPassword == null || newPassword.length() < 12
                || newPassword.getBytes(StandardCharsets.UTF_8).length > 72) {

            throw new IllegalArgumentException(
                    "Password must contain at least 12 characters "
                    + "and must not exceed 72 UTF-8 bytes.");
        }

        // Prevent reusing the current password
        if (passwordEncoder.matches(
                newPassword,
                employee.getPasswordHash())) {

            throw new IllegalArgumentException(
                    "New password must be different from the current password.");
        }

        // Generate and save the new BCrypt hash
        employee.setPasswordHash(
                passwordEncoder.encode(newPassword));

        // Allow dashboard access after password change
        employee.setMustChangePassword(false);

        employeeRepository.save(employee);
    }
}
