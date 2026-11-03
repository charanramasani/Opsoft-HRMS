
package com.opsoft.hrms_backend.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opsoft.hrms_backend.dto.InitialAdminRequest;
import com.opsoft.hrms_backend.entity.Employee;
import com.opsoft.hrms_backend.repository.EmployeeRepository;

@Service
public class InitialAdminService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.bootstrap.enabled:false}")
    private boolean bootstrapEnabled;

    @Value("${app.bootstrap.secret:}")
    private String bootstrapSecret;

    public InitialAdminService(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder) {

        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public synchronized String  createInitialAdmin(
            InitialAdminRequest request,
            String suppliedSecret) {

        // Check whether initial setup is enabled
        if (!bootstrapEnabled) {
            throw new IllegalStateException(
                    "Initial Admin setup is disabled.");
        }

        // Validate setup secret
        if (bootstrapSecret == null || bootstrapSecret.isBlank()
                || suppliedSecret == null
                || !MessageDigest.isEqual(
                        bootstrapSecret.getBytes(StandardCharsets.UTF_8),
                        suppliedSecret.getBytes(StandardCharsets.UTF_8))) {

            throw new IllegalArgumentException(
                    "Invalid Admin setup authorization.");
        }

        // Allow setup only when employee table is empty
        if (employeeRepository.count() != 0) {
            throw new IllegalStateException(
                    "Admin setup is already completed.");
        }

        // Generate unique temporary password
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String temporaryPassword =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(randomBytes);

        // Create first Admin
        Employee admin = new Employee();

        admin.setEmployeeId(request.getEmployeeId().trim());
        admin.setFullName(request.getFullName().trim());
        admin.setEmail(request.getEmail().trim());

        admin.setRole("ADMIN");
        admin.setStatus("ACTIVE");

        admin.setPasswordHash(
                passwordEncoder.encode(temporaryPassword));

        admin.setMustChangePassword(true);

        // Save Admin in MySQL
        employeeRepository.save(admin);
        bootstrapEnabled = false;

        // Return temporary password only once
        return temporaryPassword;
    }
}
