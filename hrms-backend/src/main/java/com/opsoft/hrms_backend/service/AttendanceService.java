
package com.opsoft.hrms_backend.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Duration;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import com.opsoft.hrms_backend.dto.AttendanceResponse;
import com.opsoft.hrms_backend.entity.AttendanceRecord;
import com.opsoft.hrms_backend.entity.Employee;
import com.opsoft.hrms_backend.repository.AttendanceRepository;
import com.opsoft.hrms_backend.repository.EmployeeRepository;

@Service
public class AttendanceService {

    private final AttendanceRepository attendanceRepository;
    private final EmployeeRepository employeeRepository;

    private final ZoneId indiaZone = ZoneId.of("Asia/Kolkata");

    public AttendanceService(
            AttendanceRepository attendanceRepository,
            EmployeeRepository employeeRepository) {

        this.attendanceRepository = attendanceRepository;
        this.employeeRepository = employeeRepository;
    }

    // Validate GPS location details
    private void validateLocation(
            BigDecimal latitude,
            BigDecimal longitude,
            String address) {

        if (latitude == null || longitude == null
                || address == null || address.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "GPS coordinates and address are required."
            );
        }

        if (latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid latitude."
            );
        }

        if (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0) {

            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Invalid longitude."
            );
        }

        if (address.trim().length() > 500) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Address cannot exceed 500 characters."
            );
        }
    }

    // Employee Check-In
    public AttendanceRecord checkIn(
            String employeeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String address) {

        validateLocation(latitude, longitude, address);

        LocalDate today = LocalDate.now(indiaZone);

        if (attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employeeId, today)
                .isPresent()) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already checked in today."
            );
        }

        AttendanceRecord record = new AttendanceRecord();

        record.setEmployeeId(employeeId);
        record.setAttendanceDate(today);
        record.setCheckIn(LocalDateTime.now(indiaZone));

        record.setCheckInLatitude(latitude);
        record.setCheckInLongitude(longitude);
        record.setCheckInAddress(address.trim());

        return attendanceRepository.save(record);
    }

    // Employee Check-Out
    public AttendanceRecord checkOut(
            String employeeId,
            BigDecimal latitude,
            BigDecimal longitude,
            String address) {

        validateLocation(latitude, longitude, address);

        LocalDate today = LocalDate.now(indiaZone);

        AttendanceRecord record = attendanceRepository
                .findByEmployeeIdAndAttendanceDate(employeeId, today)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.BAD_REQUEST,
                        "Please check in before checking out."
                ));

        if (record.getCheckOut() != null) {

            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "You have already checked out today."
            );
        }

        record.setCheckOut(LocalDateTime.now(indiaZone));

        record.setCheckOutLatitude(latitude);
        record.setCheckOutLongitude(longitude);
        record.setCheckOutAddress(address.trim());

        return attendanceRepository.save(record);
    }

    // Get Logged-in Employee Attendance
    public List<AttendanceRecord> getMyAttendance(String employeeId) {

        return attendanceRepository
                .findByEmployeeIdOrderByAttendanceDateDesc(employeeId);
    }

    // Get All Employees Attendance with Employee Details and GPS Locations
    public List<AttendanceResponse> getAllAttendance() {

        List<AttendanceRecord> records = attendanceRepository.findAll();

        return records.stream().map(record -> {

            AttendanceResponse response = new AttendanceResponse();

            Employee employee = employeeRepository
                    .findByEmployeeId(record.getEmployeeId());

            response.setEmployeeId(record.getEmployeeId());

            if (employee != null) {
                response.setEmployeeName(employee.getFullName());
                response.setDepartment(employee.getDepartment());
                response.setDesignation(employee.getDesignation());
            } else {
                response.setEmployeeName("Employee Not Found");
            }

            response.setAttendanceDate(record.getAttendanceDate());
            response.setCheckIn(record.getCheckIn());
            response.setCheckOut(record.getCheckOut());

            // Check-In GPS Location
            response.setCheckInLatitude(record.getCheckInLatitude());
            response.setCheckInLongitude(record.getCheckInLongitude());
            response.setCheckInAddress(record.getCheckInAddress());

            // Check-Out GPS Location
            response.setCheckOutLatitude(record.getCheckOutLatitude());
            response.setCheckOutLongitude(record.getCheckOutLongitude());
            response.setCheckOutAddress(record.getCheckOutAddress());

            // Attendance Status and Working Hours
            if (record.getCheckOut() == null) {

                response.setAttendanceStatus("CHECKED_IN");
                response.setWorkingMinutes(null);

            } else {

                response.setAttendanceStatus("COMPLETED");

                long minutes = Duration.between(
                        record.getCheckIn(),
                        record.getCheckOut()
                ).toMinutes();

                response.setWorkingMinutes(minutes);
            }

            return response;

        }).toList();
    }
}
