
package com.opsoft.hrms_backend.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.opsoft.hrms_backend.dto.EmployeeCreateRequest;
import com.opsoft.hrms_backend.dto.EmployeeUpdateRequest;
import com.opsoft.hrms_backend.dto.EmployeeStatusRequest;
import com.opsoft.hrms_backend.dto.EmployeeResponse;
import com.opsoft.hrms_backend.entity.EmployeeStatusHistory;

import com.opsoft.hrms_backend.service.EmployeeRegistrationService;
import com.opsoft.hrms_backend.service.EmployeeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/employees")
@CrossOrigin(origins = "http://localhost:5173")
public class EmployeeController {

    private final EmployeeService employeeService;
    private final EmployeeRegistrationService registrationService;

    public EmployeeController(
            EmployeeService employeeService,
            EmployeeRegistrationService registrationService) {

        this.employeeService = employeeService;
        this.registrationService = registrationService;
    }

    // ADMIN and HR can view all employees
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @GetMapping
    public List<EmployeeResponse> getAllEmployees() {

        return employeeService.getAllEmployees();
    }

    // ADMIN and HR can register new employees
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @PostMapping
    public ResponseEntity<Map<String, String>> createEmployee(
            @Valid @RequestBody EmployeeCreateRequest request) {

        try {

            String temporaryPassword =
                    registrationService.createEmployee(request);

            Map<String, String> response = new HashMap<>();

            response.put("message", "Employee created successfully.");
            response.put("employeeId", request.getEmployeeId());
            response.put("temporaryPassword", temporaryPassword);

            return ResponseEntity
                    .status(HttpStatus.CREATED)
                    .body(response);

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        }
    }

    // ADMIN and HR can update employee details
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @PutMapping("/{employeeId}")
    public ResponseEntity<EmployeeResponse> updateEmployee(
            @PathVariable String employeeId,
            @Valid @RequestBody EmployeeUpdateRequest request) {

        try {

            EmployeeResponse updatedEmployee =
                    employeeService.updateEmployee(employeeId, request);

            return ResponseEntity.ok(updatedEmployee);

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        }
    }

    // ADMIN and HR can change employee status
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @PatchMapping("/{employeeId}/status")
    public ResponseEntity<EmployeeResponse> updateEmployeeStatus(
            @PathVariable String employeeId,
            @Valid @RequestBody EmployeeStatusRequest request) {

        try {

            EmployeeResponse updatedEmployee =
                    employeeService.updateEmployeeStatus(
                            employeeId,
                            request
                    );

            return ResponseEntity.ok(updatedEmployee);

        } catch (IllegalArgumentException exception) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    exception.getMessage()
            );
        }
    }

    // ADMIN and HR can view employee status history
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @GetMapping("/{employeeId}/status-history")
    public List<EmployeeStatusHistory> getEmployeeStatusHistory(
            @PathVariable String employeeId) {

        return employeeService.getEmployeeStatusHistory(employeeId);
    }

    // Logged-in user can view their own profile
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
    @GetMapping("/me")
    public EmployeeResponse getMyProfile(
            Authentication authentication) {

        String employeeId = authentication.getName();

        return employeeService.getEmployeeByEmployeeId(employeeId);
    }
}
