
package com.opsoft.hrms_backend.controller;

import com.opsoft.hrms_backend.dto.LeaveApplyRequest;
import com.opsoft.hrms_backend.dto.LeaveRequestDTO;
import com.opsoft.hrms_backend.dto.LeaveReviewRequest;
import com.opsoft.hrms_backend.service.LeaveService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/leaves")
@CrossOrigin(
    origins = "http://localhost:5173",
    allowCredentials = "true"
)
public class LeaveController {

    private final LeaveService leaveService;

    public LeaveController(LeaveService leaveService) {
        this.leaveService = leaveService;
    }

    // 1. Employee and HR can apply for leave
    @PostMapping("/apply")
    @PreAuthorize("hasAnyRole('HR', 'EMPLOYEE')")
    public ResponseEntity<LeaveRequestDTO> applyForLeave(
            @RequestBody LeaveApplyRequest request) {

        LeaveRequestDTO response =
                leaveService.applyForLeave(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 2. Employee and HR can view their own leave history
    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('HR', 'EMPLOYEE')")
    public ResponseEntity<List<LeaveRequestDTO>> getMyLeaveRequests() {

        List<LeaveRequestDTO> requests =
                leaveService.getMyLeaveRequests();

        return ResponseEntity.ok(requests);
    }

    // 3. Admin and HR can view all leave requests
    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<LeaveRequestDTO>> getAllLeaveRequests() {

        List<LeaveRequestDTO> requests =
                leaveService.getAllLeaveRequests();

        return ResponseEntity.ok(requests);
    }

    // 4. Admin and HR can approve or reject leave
    @PatchMapping("/{id}/review")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<LeaveRequestDTO> reviewLeaveRequest(
            @PathVariable Integer id,
            @RequestBody LeaveReviewRequest request) {

        LeaveRequestDTO response =
                leaveService.reviewLeaveRequest(id, request);

        return ResponseEntity.ok(response);
    }
}
