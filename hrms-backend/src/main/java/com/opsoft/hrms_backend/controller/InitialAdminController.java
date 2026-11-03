
package com.opsoft.hrms_backend.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import com.opsoft.hrms_backend.dto.InitialAdminRequest;
import com.opsoft.hrms_backend.service.InitialAdminService;

@RestController
@RequestMapping("/api/setup")
public class InitialAdminController {

    private final InitialAdminService initialAdminService;

    public InitialAdminController(
            InitialAdminService initialAdminService) {
        this.initialAdminService = initialAdminService;
    }

    @PostMapping("/initial-admin")
    public ResponseEntity<?> createInitialAdmin(
            @Valid @RequestBody InitialAdminRequest request,
            @RequestHeader("X-Setup-Secret") String setupSecret) {

        String temporaryPassword =
                initialAdminService.createInitialAdmin(
                        request, setupSecret);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(java.util.Map.of(
                        "message", "Initial Admin created successfully.",
                        "employeeId", request.getEmployeeId(),
                        "temporaryPassword", temporaryPassword,
                        "instruction",
                        "Save this temporary password securely. It will not be shown again."
                ));
    }
}
