package com.opsoft.hrms_backend.controller;

import com.opsoft.hrms_backend.dto.SalaryStructureRequest;
import com.opsoft.hrms_backend.dto.SalaryStructureUpdateRequest;
import com.opsoft.hrms_backend.dto.SalaryStructureResponse;
import com.opsoft.hrms_backend.service.SalaryStructureService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/salary-structures")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class SalaryStructureController {

    private final SalaryStructureService salaryService;

    public SalaryStructureController(SalaryStructureService salaryService) {
        this.salaryService = salaryService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<SalaryStructureResponse> createSalaryStructure(
            @RequestBody SalaryStructureRequest request) {
        SalaryStructureResponse response =
                salaryService.createSalaryStructure(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<SalaryStructureResponse>> getAllSalaryStructures() {
        return ResponseEntity.ok(salaryService.getAllSalaryStructures());
    }

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<SalaryStructureResponse>>
            getSalaryStructuresByEmployeeId(@PathVariable String employeeId) {
        return ResponseEntity.ok(
                salaryService.getSalaryStructuresByEmployeeId(employeeId));
    }

    @GetMapping("/employee/{employeeId}/current")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<SalaryStructureResponse>
            getCurrentSalaryStructure(@PathVariable String employeeId) {
        return ResponseEntity.ok(
                salaryService.getCurrentSalaryStructure(employeeId));
    }

    // Directly edit the selected salary record without creating a revision.
    @PutMapping("/{id}/edit")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<SalaryStructureResponse> updateExistingSalaryStructure(
            @PathVariable Long id,
            @RequestBody SalaryStructureUpdateRequest request) {
        SalaryStructureResponse response =
                salaryService.updateExistingSalaryStructure(id, request);
        return ResponseEntity.ok(response);
    }

    // Create a new salary revision and retain the previous record.
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<SalaryStructureResponse> reviseSalaryStructure(
            @PathVariable Long id,
            @RequestBody SalaryStructureUpdateRequest request) {
        SalaryStructureResponse response =
                salaryService.reviseSalaryStructure(id, request);
        return ResponseEntity.ok(response);
    }
}
