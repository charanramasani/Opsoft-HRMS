
package com.opsoft.hrms_backend.dto;

import java.util.List;

public class PayrollGenerationResponse {

    private int generatedCount;
    private int skippedCount;

    private List<PayrollResponse> generatedPayrolls;
    private List<String> skippedEmployees;

    public PayrollGenerationResponse() {
    }

    public PayrollGenerationResponse(
            int generatedCount,
            int skippedCount,
            List<PayrollResponse> generatedPayrolls,
            List<String> skippedEmployees) {

        this.generatedCount = generatedCount;
        this.skippedCount = skippedCount;
        this.generatedPayrolls = generatedPayrolls;
        this.skippedEmployees = skippedEmployees;
    }

    public int getGeneratedCount() {
        return generatedCount;
    }

    public void setGeneratedCount(int generatedCount) {
        this.generatedCount = generatedCount;
    }

    public int getSkippedCount() {
        return skippedCount;
    }

    public void setSkippedCount(int skippedCount) {
        this.skippedCount = skippedCount;
    }

    public List<PayrollResponse> getGeneratedPayrolls() {
        return generatedPayrolls;
    }

    public void setGeneratedPayrolls(
            List<PayrollResponse> generatedPayrolls) {
        this.generatedPayrolls = generatedPayrolls;
    }

    public List<String> getSkippedEmployees() {
        return skippedEmployees;
    }

    public void setSkippedEmployees(
            List<String> skippedEmployees) {
        this.skippedEmployees = skippedEmployees;
    }
}
