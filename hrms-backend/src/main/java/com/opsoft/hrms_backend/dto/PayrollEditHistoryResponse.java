
package com.opsoft.hrms_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class PayrollEditHistoryResponse {

    private Long id;
    private Long payrollId;
    private String changedByEmployeeId;
    private String changeReason;

    private String oldComponents;
    private String newComponents;

    private BigDecimal oldGrossSalary;
    private BigDecimal newGrossSalary;

    private BigDecimal oldDeductions;
    private BigDecimal newDeductions;

    private BigDecimal oldNetSalary;
    private BigDecimal newNetSalary;

    private LocalDateTime changedAt;

    public PayrollEditHistoryResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getPayrollId() {
        return payrollId;
    }

    public void setPayrollId(Long payrollId) {
        this.payrollId = payrollId;
    }

    public String getChangedByEmployeeId() {
        return changedByEmployeeId;
    }

    public void setChangedByEmployeeId(String changedByEmployeeId) {
        this.changedByEmployeeId = changedByEmployeeId;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public String getOldComponents() {
        return oldComponents;
    }

    public void setOldComponents(String oldComponents) {
        this.oldComponents = oldComponents;
    }

    public String getNewComponents() {
        return newComponents;
    }

    public void setNewComponents(String newComponents) {
        this.newComponents = newComponents;
    }

    public BigDecimal getOldGrossSalary() {
        return oldGrossSalary;
    }

    public void setOldGrossSalary(BigDecimal oldGrossSalary) {
        this.oldGrossSalary = oldGrossSalary;
    }

    public BigDecimal getNewGrossSalary() {
        return newGrossSalary;
    }

    public void setNewGrossSalary(BigDecimal newGrossSalary) {
        this.newGrossSalary = newGrossSalary;
    }

    public BigDecimal getOldDeductions() {
        return oldDeductions;
    }

    public void setOldDeductions(BigDecimal oldDeductions) {
        this.oldDeductions = oldDeductions;
    }

    public BigDecimal getNewDeductions() {
        return newDeductions;
    }

    public void setNewDeductions(BigDecimal newDeductions) {
        this.newDeductions = newDeductions;
    }

    public BigDecimal getOldNetSalary() {
        return oldNetSalary;
    }

    public void setOldNetSalary(BigDecimal oldNetSalary) {
        this.oldNetSalary = oldNetSalary;
    }

    public BigDecimal getNewNetSalary() {
        return newNetSalary;
    }

    public void setNewNetSalary(BigDecimal newNetSalary) {
        this.newNetSalary = newNetSalary;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }
}
