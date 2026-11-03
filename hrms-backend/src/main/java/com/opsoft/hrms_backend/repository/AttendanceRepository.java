
package com.opsoft.hrms_backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.opsoft.hrms_backend.entity.AttendanceRecord;

public interface AttendanceRepository
        extends JpaRepository<AttendanceRecord, Integer> {

    Optional<AttendanceRecord> findByEmployeeIdAndAttendanceDate(
            String employeeId,
            LocalDate attendanceDate
    );

    List<AttendanceRecord> findByEmployeeIdOrderByAttendanceDateDesc(
            String employeeId
    );

    List<AttendanceRecord> findByEmployeeIdAndAttendanceDateBetween(
            String employeeId,
            LocalDate startDate,
            LocalDate endDate
    );

    long countByEmployeeIdAndAttendanceDateBetween(
            String employeeId,
            LocalDate startDate,
            LocalDate endDate
    );
}
