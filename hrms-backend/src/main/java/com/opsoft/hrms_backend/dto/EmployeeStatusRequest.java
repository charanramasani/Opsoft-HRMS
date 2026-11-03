
package com.opsoft.hrms_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmployeeStatusRequest {

    @NotBlank(message = "Employee status is required")
    @Pattern(
        regexp = "ACTIVE|ON_NOTICE|RESIGNED|TERMINATED|INACTIVE",
        message = "Invalid employee status"
    )
    private String status;

    @Size(max = 500, message = "Reason cannot exceed 500 characters")
    private String reason;

    public EmployeeStatusRequest() {
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
