
package com.opsoft.hrms_backend.controller;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import com.opsoft.hrms_backend.dto.AttendanceResponse;
import com.opsoft.hrms_backend.entity.AttendanceRecord;
import com.opsoft.hrms_backend.service.AttendanceService;

@RestController
@RequestMapping("/api/attendance")
@CrossOrigin(origins = "http://localhost:5173")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(AttendanceService attendanceService) {
        this.attendanceService = attendanceService;
    }

    // Request body for GPS location
    public record AttendanceLocationRequest(
            BigDecimal latitude,
            BigDecimal longitude,
            String address) {
    }

    // Employee Check-In
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
    @PostMapping("/check-in")
    public AttendanceRecord checkIn(
            Authentication authentication,
            @RequestBody AttendanceLocationRequest location) {

        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in."
            );
        }

        if (location == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "GPS location is required."
            );
        }

        return attendanceService.checkIn(
                authentication.getName(),
                location.latitude(),
                location.longitude(),
                location.address()
        );
    }

    // Employee Check-Out
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
    @PostMapping("/check-out")
    public AttendanceRecord checkOut(
            Authentication authentication,
            @RequestBody AttendanceLocationRequest location) {

        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in."
            );
        }

        if (location == null) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "GPS location is required."
            );
        }

        return attendanceService.checkOut(
                authentication.getName(),
                location.latitude(),
                location.longitude(),
                location.address()
        );
    }

    // Get Logged-in Employee Attendance
    @PreAuthorize("hasAnyRole('ADMIN', 'HR', 'EMPLOYEE')")
    @GetMapping("/me")
    public List<AttendanceRecord> getMyAttendance(
            Authentication authentication) {

        if (authentication == null) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Please log in."
            );
        }

        return attendanceService.getMyAttendance(authentication.getName());
    }

    // Get All Employees Attendance (Admin and HR Only)
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    @GetMapping("/all")
    public List<AttendanceResponse> getAllAttendance() {

        return attendanceService.getAllAttendance();
    }
}
