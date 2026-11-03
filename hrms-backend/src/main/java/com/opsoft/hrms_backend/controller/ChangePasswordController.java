
package com.opsoft.hrms_backend.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.opsoft.hrms_backend.dto.ChangePasswordRequest;
import com.opsoft.hrms_backend.service.ChangePasswordService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/auth")
public class ChangePasswordController {

    private final ChangePasswordService changePasswordService;

    public ChangePasswordController(
            ChangePasswordService changePasswordService) {

        this.changePasswordService = changePasswordService;
    }

    @PostMapping("/change-password")
    public ResponseEntity<?> changePassword(
            @Valid @RequestBody ChangePasswordRequest request,
            Authentication authentication) {

        if (authentication == null || !authentication.isAuthenticated()) {
            return ResponseEntity.status(401)
                    .body(Map.of("message", "Please log in first."));
        }

        String employeeId = authentication.getName();

        try {
            changePasswordService.changePassword(
                    employeeId, request);

            return ResponseEntity.ok(
                    Map.of("message", "Password changed successfully.")
            );

        } catch (IllegalArgumentException exception) {

            return ResponseEntity.badRequest()
                    .body(Map.of("message", exception.getMessage()));
        }
    }
}
