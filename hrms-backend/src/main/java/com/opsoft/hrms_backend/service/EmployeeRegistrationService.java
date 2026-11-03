
package com.opsoft.hrms_backend.service;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.Set;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opsoft.hrms_backend.dto.EmployeeCreateRequest;
import com.opsoft.hrms_backend.entity.Employee;
import com.opsoft.hrms_backend.repository.EmployeeRepository;

@Service
public class EmployeeRegistrationService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final SalaryStructureService salaryStructureService;

    private static final SecureRandom SECURE_RANDOM =
            new SecureRandom();

    private static final Set<String> ALLOWED_ROLES =
            Set.of("ADMIN", "HR", "EMPLOYEE");

    public EmployeeRegistrationService(
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder,
            SalaryStructureService salaryStructureService) {

        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
        this.salaryStructureService = salaryStructureService;
    }

    @Transactional
    public String createEmployee(EmployeeCreateRequest request) {

        // Validate request
        if (request == null) {
            throw new IllegalArgumentException(
                    "Employee details are required.");
        }

        // Validate employee ID
        if (request.getEmployeeId() == null
                || request.getEmployeeId().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Employee ID is required.");
        }

        // Validate email
        if (request.getEmail() == null
                || request.getEmail().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Email is required.");
        }

        // Check duplicate Employee ID
        if (employeeRepository.findByEmployeeId(
                request.getEmployeeId().trim()) != null) {

            throw new IllegalArgumentException(
                    "Employee ID already exists.");
        }

        // Check duplicate Email
        if (employeeRepository.findByEmail(
                request.getEmail().trim()) != null) {

            throw new IllegalArgumentException(
                    "Email already exists.");
        }

        // Validate employee role
        if (request.getRole() == null
                || request.getRole().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Employee role is required.");
        }

        String role = request.getRole().trim().toUpperCase();

        if (!ALLOWED_ROLES.contains(role)) {
            throw new IllegalArgumentException(
                    "Invalid employee role.");
        }

        // Validate monthly gross salary
        if (request.getSalary() == null
                || request.getSalary().signum() <= 0) {

            throw new IllegalArgumentException(
                    "A valid monthly gross salary is required.");
        }

        // Initial Salary Structure is mandatory
        if (request.getSalaryStructure() == null) {
            throw new IllegalArgumentException(
                    "Initial Salary Structure is required. "
                    + "Employee cannot be registered without it.");
        }

        // Generate temporary password
        byte[] randomBytes = new byte[24];
        SECURE_RANDOM.nextBytes(randomBytes);

        String temporaryPassword =
                Base64.getUrlEncoder()
                        .withoutPadding()
                        .encodeToString(randomBytes);

        // Create Employee entity
        Employee employee = new Employee();

        employee.setEmployeeId(
                request.getEmployeeId().trim());

        employee.setFullName(
                request.getFullName().trim());

        employee.setEmail(
                request.getEmail().trim());

        employee.setRole(role);

        employee.setPhone(request.getPhone());
        employee.setDepartment(request.getDepartment());
        employee.setDesignation(request.getDesignation());
        employee.setJoiningDate(request.getJoiningDate());

        employee.setSalary(request.getSalary());

        employee.setStatus("ACTIVE");

        // Store only the BCrypt password hash
        employee.setPasswordHash(
                passwordEncoder.encode(temporaryPassword));

        // Force password change on first login
        employee.setMustChangePassword(true);

        // Step 1: Save employee
        employeeRepository.save(employee);

        // Step 2: Save initial Salary Structure
        // Earnings must match the employee's monthly gross salary.
        salaryStructureService.createInitialSalaryStructure(
                employee.getEmployeeId(),
                request.getSalaryStructure(),
                request.getSalary()
        );

        // Both operations participate in the same transaction.
        // If Salary Structure saving fails, employee registration
        // will also roll back.

        return temporaryPassword;
    }
}
