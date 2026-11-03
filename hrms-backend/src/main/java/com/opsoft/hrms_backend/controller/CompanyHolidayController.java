
package com.opsoft.hrms_backend.controller;

import com.opsoft.hrms_backend.dto.CompanyHolidayRequest;
import com.opsoft.hrms_backend.dto.CompanyHolidayResponse;
import com.opsoft.hrms_backend.service.CompanyHolidayService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/holidays")
@CrossOrigin(
    origins = "http://localhost:5173",
    allowCredentials = "true"
)
public class CompanyHolidayController {

    private final CompanyHolidayService holidayService;

    public CompanyHolidayController(
            CompanyHolidayService holidayService) {

        this.holidayService = holidayService;
    }

    // 1. Add a company holiday - HR/Admin only
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<CompanyHolidayResponse> addHoliday(
            @RequestBody CompanyHolidayRequest request) {

        CompanyHolidayResponse response =
                holidayService.addHoliday(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 2. Get all holidays - HR/Admin only
    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<CompanyHolidayResponse>> getAllHolidays() {

        return ResponseEntity.ok(
                holidayService.getAllHolidays()
        );
    }

    // 3. Get active holidays - All authenticated roles
    @GetMapping("/active")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
    public ResponseEntity<List<CompanyHolidayResponse>> getActiveHolidays() {

        return ResponseEntity.ok(
                holidayService.getActiveHolidays()
        );
    }

    // 4. Update a holiday - HR/Admin only
    @PutMapping("/{holidayId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<CompanyHolidayResponse> updateHoliday(
            @PathVariable Long holidayId,
            @RequestBody CompanyHolidayRequest request) {

        return ResponseEntity.ok(
                holidayService.updateHoliday(holidayId, request)
        );
    }

    // 5. Deactivate a holiday - HR/Admin only
    @PatchMapping("/{holidayId}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<CompanyHolidayResponse> deactivateHoliday(
            @PathVariable Long holidayId) {

        return ResponseEntity.ok(
                holidayService.deactivateHoliday(holidayId)
        );
    }
}
