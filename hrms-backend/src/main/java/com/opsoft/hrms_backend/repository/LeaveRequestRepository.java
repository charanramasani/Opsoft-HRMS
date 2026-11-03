
package com.opsoft.hrms_backend.repository;

import com.opsoft.hrms_backend.entity.LeaveRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface LeaveRequestRepository
        extends JpaRepository<LeaveRequest, Integer> {

    // Get leave requests of a particular employee
    List<LeaveRequest> findByEmployeeIdOrderByAppliedAtDesc(
            String employeeId
    );

    // Get all employee leave requests
    List<LeaveRequest> findAllByOrderByAppliedAtDesc();

    // Filter leave requests by status
    List<LeaveRequest> findByStatusOrderByAppliedAtDesc(
            String status
    );

    // Get approved leaves overlapping a payroll month
    List<LeaveRequest>
    findByEmployeeIdAndStatusIgnoreCaseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
            String employeeId,
            String status,
            LocalDate monthEnd,
            LocalDate monthStart
    );
}
