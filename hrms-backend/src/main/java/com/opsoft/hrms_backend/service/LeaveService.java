package com.opsoft.hrms_backend.service;

import com.opsoft.hrms_backend.dto.LeaveApplyRequest;
import com.opsoft.hrms_backend.dto.LeaveRequestDTO;
import com.opsoft.hrms_backend.dto.LeaveReviewRequest;
import com.opsoft.hrms_backend.entity.Employee;
import com.opsoft.hrms_backend.entity.LeaveRequest;
import com.opsoft.hrms_backend.repository.EmployeeRepository;
import com.opsoft.hrms_backend.repository.LeaveRequestRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class LeaveService {

    private final LeaveRequestRepository leaveRepository;
    private final EmployeeRepository employeeRepository;

    public LeaveService(
            LeaveRequestRepository leaveRepository,
            EmployeeRepository employeeRepository) {

        this.leaveRepository = leaveRepository;
        this.employeeRepository = employeeRepository;
    }

    // Get currently logged-in employee ID
    private String getLoggedInEmployeeId() {

        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null ||
                !authentication.isAuthenticated()) {

            throw new RuntimeException("User is not authenticated");
        }

        return authentication.getName();
    }

    // 1. Employee applies for leave
    @Transactional
    public LeaveRequestDTO applyForLeave(LeaveApplyRequest request) {

        String employeeId = getLoggedInEmployeeId();

        Employee employee =
                employeeRepository.findByEmployeeId(employeeId);

        if (employee == null) {
            throw new RuntimeException("Employee not found");
        }

        if (request.getLeaveType() == null ||
                request.getLeaveType().isBlank()) {

            throw new RuntimeException("Leave type is required");
        }

        if (request.getStartDate() == null ||
                request.getEndDate() == null) {

            throw new RuntimeException("Leave dates are required");
        }

        if (request.getStartDate().isBefore(LocalDate.now())) {
            throw new RuntimeException(
                    "Leave start date cannot be in the past"
            );
        }

        if (request.getEndDate().isBefore(request.getStartDate())) {
            throw new RuntimeException(
                    "End date cannot be before start date"
            );
        }

        if (request.getReason() == null ||
                request.getReason().isBlank()) {

            throw new RuntimeException("Leave reason is required");
        }

        LeaveRequest leave = new LeaveRequest();

        leave.setEmployeeId(employeeId);
        leave.setLeaveType(request.getLeaveType().trim());
        leave.setStartDate(request.getStartDate());
        leave.setEndDate(request.getEndDate());
        leave.setReason(request.getReason().trim());

        leave.setStatus("PENDING");
        leave.setAppliedAt(LocalDateTime.now());

        LeaveRequest savedLeave = leaveRepository.save(leave);

        return convertToDTO(savedLeave);
    }

    // 2. Employee views their own leave history
    public List<LeaveRequestDTO> getMyLeaveRequests() {

        String employeeId = getLoggedInEmployeeId();

        List<LeaveRequest> requests =
                leaveRepository.findByEmployeeIdOrderByAppliedAtDesc(
                        employeeId
                );

        return requests.stream()
                .map(this::convertToDTO)
                .toList();
    }

    // 3. Admin/HR views all employee leave requests
    public List<LeaveRequestDTO> getAllLeaveRequests() {

        List<LeaveRequest> requests =
                leaveRepository.findAllByOrderByAppliedAtDesc();

        return requests.stream()
                .map(this::convertToDTO)
                .toList();
    }

    // 4. Admin/HR approves or rejects leave
    @Transactional
    public LeaveRequestDTO reviewLeaveRequest(
            Integer leaveId,
            LeaveReviewRequest request) {

        String reviewerId = getLoggedInEmployeeId();

        LeaveRequest leave = leaveRepository.findById(leaveId)
                .orElseThrow(() ->
                        new RuntimeException("Leave request not found")
                );

        // Prevent self-approval
        if (leave.getEmployeeId().equalsIgnoreCase(reviewerId)) {
            throw new RuntimeException(
                    "You cannot approve or reject your own leave request"
            );
        }

        if (request.getStatus() == null) {
            throw new RuntimeException("Leave status is required");
        }

        String newStatus = request.getStatus().trim().toUpperCase();

        if (!newStatus.equals("APPROVED") &&
                !newStatus.equals("REJECTED")) {

            throw new RuntimeException(
                    "Status must be APPROVED or REJECTED"
            );
        }

        // Only pending requests can be reviewed
        if (!"PENDING".equalsIgnoreCase(leave.getStatus())) {
            throw new RuntimeException(
                    "This leave request has already been reviewed"
            );
        }

        leave.setStatus(newStatus);
        leave.setReviewedBy(reviewerId);
        leave.setReviewedAt(LocalDateTime.now());
        leave.setReviewerComment(request.getReviewerComment());

        LeaveRequest updatedLeave = leaveRepository.save(leave);

        return convertToDTO(updatedLeave);
    }

    // 5. Convert entity into response DTO
    private LeaveRequestDTO convertToDTO(LeaveRequest leave) {

        LeaveRequestDTO dto = new LeaveRequestDTO();

        dto.setId(leave.getId());
        dto.setEmployeeId(leave.getEmployeeId());
        dto.setLeaveType(leave.getLeaveType());
        dto.setStartDate(leave.getStartDate());
        dto.setEndDate(leave.getEndDate());
        dto.setReason(leave.getReason());
        dto.setStatus(leave.getStatus());
        dto.setAppliedAt(leave.getAppliedAt());
        dto.setReviewedBy(leave.getReviewedBy());
        dto.setReviewedAt(leave.getReviewedAt());
        dto.setReviewerComment(leave.getReviewerComment());

        Employee employee =
                employeeRepository.findByEmployeeId(
                        leave.getEmployeeId()
                );

        if (employee != null) {
            dto.setEmployeeName(employee.getFullName());
            dto.setDepartment(employee.getDepartment());
        }

        return dto;
    }
}