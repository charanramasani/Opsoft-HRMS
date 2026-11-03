
package com.opsoft.hrms_backend.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsoft.hrms_backend.dto.LoginRequest;
import com.opsoft.hrms_backend.dto.LoginResponse;
import com.opsoft.hrms_backend.entity.Employee;
import com.opsoft.hrms_backend.repository.EmployeeRepository;
import com.opsoft.hrms_backend.security.JwtService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final EmployeeRepository employeeRepository;
    private final JwtService jwtService;

    public AuthController(
            AuthenticationManager authenticationManager,
            EmployeeRepository employeeRepository,
            JwtService jwtService) {

        this.authenticationManager = authenticationManager;
        this.employeeRepository = employeeRepository;
        this.jwtService = jwtService;
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(
            @RequestBody LoginRequest request) {

        try {

            // Authenticate employee ID and password
            Authentication authentication =
                    authenticationManager.authenticate(
                            new UsernamePasswordAuthenticationToken(
                                    request.getEmployeeId(),
                                    request.getPassword()
                            )
                    );

            // Get authenticated employee details
            Employee employee =
                    employeeRepository.findByEmployeeId(
                            authentication.getName()
                    );

            if (employee == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(Map.of("message", "Invalid login details."));
            }

            // Generate JWT access token
            UserDetails userDetails =
                    (UserDetails) authentication.getPrincipal();

            String token = jwtService.generateToken(userDetails);

            // Prepare login response
            LoginResponse response = new LoginResponse(
                    "Login successful",
                    employee.getEmployeeId(),
                    employee.getFullName(),
                    employee.getRole(),
                    employee.isMustChangePassword(),
                    token
            );

            return ResponseEntity.ok(response);

        } catch (AuthenticationException exception) {

            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of(
                            "message",
                            "Invalid employee ID or password."
                    ));
        }
    }
}
