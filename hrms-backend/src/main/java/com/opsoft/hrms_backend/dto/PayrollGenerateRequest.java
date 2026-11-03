
package com.opsoft.hrms_backend.dto;

import java.time.LocalDate;

public class PayrollGenerateRequest {

    private LocalDate payrollMonth;

    public LocalDate getPayrollMonth() {
        return payrollMonth;
    }

    public void setPayrollMonth(LocalDate payrollMonth) {
        this.payrollMonth = payrollMonth;
    }
}
