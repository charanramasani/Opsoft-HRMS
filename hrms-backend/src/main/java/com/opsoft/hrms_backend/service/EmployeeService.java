
package com.opsoft.hrms_backend.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opsoft.hrms_backend.dto.EmployeeResponse;
import com.opsoft.hrms_backend.dto.EmployeeUpdateRequest;
import com.opsoft.hrms_backend.dto.EmployeeStatusRequest;

import com.opsoft.hrms_backend.entity.Employee;
import com.opsoft.hrms_backend.entity.EmployeeStatusHistory;

import com.opsoft.hrms_backend.repository.EmployeeRepository;
import com.opsoft.hrms_backend.repository.EmployeeStatusHistoryRepository;

@Service
public class EmployeeService {

    private final EmployeeRepository employeeRepository;
    private final EmployeeStatusHistoryRepository statusHistoryRepository;

    public EmployeeService(
            EmployeeRepository employeeRepository,
            EmployeeStatusHistoryRepository statusHistoryRepository) {

        this.employeeRepository = employeeRepository;
        this.statusHistoryRepository = statusHistoryRepository;
    }

    // ==========================================
    // 1. GET ALL EMPLOYEES
    // ==========================================

    public List<EmployeeResponse> getAllEmployees() {

        return employeeRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // ==========================================
    // 2. GET EMPLOYEE BY EMPLOYEE ID
    // ==========================================

    public EmployeeResponse getEmployeeByEmployeeId(String employeeId) {

        Employee employee =
                employeeRepository.findByEmployeeId(employeeId);

        if (employee == null) {
            throw new RuntimeException("Employee not found");
        }

        return convertToResponse(employee);
    }

    // ==========================================
    // 3. UPDATE EMPLOYEE DETAILS
    // ==========================================

    @Transactional
    public EmployeeResponse updateEmployee(
            String employeeId,
            EmployeeUpdateRequest request) {

        Employee employee =
                employeeRepository.findByEmployeeId(employeeId);

        if (employee == null) {
            throw new IllegalArgumentException("Employee not found");
        }

        // Check whether another employee is using this email
        boolean emailExists =
                employeeRepository.existsByEmailAndEmployeeIdNot(
                        request.getEmail(),
                        employeeId
                );

        if (emailExists) {
            throw new IllegalArgumentException(
                    "This email is already registered with another employee"
            );
        }

        // Update permitted employee details only.
        // Salary is managed separately through Salary Structure.
        employee.setFullName(request.getFullName());
        employee.setEmail(request.getEmail());
        employee.setPhone(request.getPhone());
        employee.setDepartment(request.getDepartment());
        employee.setDesignation(request.getDesignation());
        employee.setJoiningDate(request.getJoiningDate());

        Employee updatedEmployee =
                employeeRepository.save(employee);

        return convertToResponse(updatedEmployee);
    }

    // ==========================================
    // 4. UPDATE EMPLOYEE STATUS
    //    AND SAVE STATUS HISTORY
    // ==========================================

    @Transactional
    public EmployeeResponse updateEmployeeStatus(
            String employeeId,
            EmployeeStatusRequest request) {

        Employee employee =
                employeeRepository.findByEmployeeId(employeeId);

        if (employee == null) {
            throw new IllegalArgumentException("Employee not found");
        }

        if (request.getStatus() == null ||
                request.getStatus().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Employee status is required"
            );
        }

        // Capture previous status
        String previousStatus = employee.getStatus();

        // Get new status
        String newStatus =
                request.getStatus().trim().toUpperCase();

        // Validate employee status
        if (!newStatus.matches(
                "ACTIVE|ON_NOTICE|RESIGNED|TERMINATED|INACTIVE")) {

            throw new IllegalArgumentException(
                    "Invalid employee status"
            );
        }

        // Prevent changing to the same status
        if (newStatus.equalsIgnoreCase(previousStatus)) {
            throw new IllegalArgumentException(
                    "Employee already has this status"
            );
        }

        // Get logged-in Admin/HR employee ID
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String changedByEmployeeId = null;

        if (authentication != null &&
                authentication.isAuthenticated() &&
                !"anonymousUser".equals(authentication.getName())) {

            changedByEmployeeId = authentication.getName();
        }

        // Update employee status
        employee.setStatus(newStatus);

        Employee updatedEmployee =
                employeeRepository.save(employee);

        // Create status history record
        EmployeeStatusHistory history =
                new EmployeeStatusHistory();

        history.setEmployeeId(employee.getEmployeeId());
        history.setPreviousStatus(previousStatus);
        history.setNewStatus(newStatus);

        // Save reason
        String reason = request.getReason();

        if (reason != null && !reason.trim().isEmpty()) {
            history.setReason(reason.trim());
        } else {
            history.setReason(null);
        }

        // Save Admin/HR employee ID
        history.setChangedByEmployeeId(changedByEmployeeId);

        // Insert history record
        statusHistoryRepository.save(history);

        return convertToResponse(updatedEmployee);
    }

    // ==========================================
    // 5. GET EMPLOYEE STATUS HISTORY
    // ==========================================

    public List<EmployeeStatusHistory> getEmployeeStatusHistory(
            String employeeId) {

        Employee employee =
                employeeRepository.findByEmployeeId(employeeId);

        if (employee == null) {
            throw new IllegalArgumentException("Employee not found");
        }

        return statusHistoryRepository
                .findByEmployeeIdOrderByChangedAtDesc(employeeId);
    }

    // ==========================================
    // 6. CONVERT EMPLOYEE ENTITY TO RESPONSE DTO
    // ==========================================

    private EmployeeResponse convertToResponse(Employee employee) {

        EmployeeResponse response = new EmployeeResponse();

        response.setId(employee.getId());
        response.setEmployeeId(employee.getEmployeeId());
        response.setFullName(employee.getFullName());
        response.setEmail(employee.getEmail());
        response.setRole(employee.getRole());
        response.setStatus(employee.getStatus());
        response.setPhone(employee.getPhone());
        response.setDepartment(employee.getDepartment());
        response.setDesignation(employee.getDesignation());
        response.setJoiningDate(employee.getJoiningDate());
        response.setSalary(employee.getSalary());

        return response;
    }
}
