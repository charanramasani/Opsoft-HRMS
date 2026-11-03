
package com.opsoft.hrms_backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payroll_edit_history")
public class PayrollEditHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "payroll_id", nullable = false)
    private Long payrollId;

    @Column(name = "changed_by_employee_id", length = 30)
    private String changedByEmployeeId;

    @Column(name = "change_reason", nullable = false, length = 500)
    private String changeReason;

    @Column(name = "old_components", nullable = false, columnDefinition = "json")
    private String oldComponents;

    @Column(name = "new_components", nullable = false, columnDefinition = "json")
    private String newComponents;

    @Column(name = "old_gross_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal oldGrossSalary;

    @Column(name = "new_gross_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal newGrossSalary;

    @Column(name = "old_deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal oldDeductions;

    @Column(name = "new_deductions", nullable = false, precision = 12, scale = 2)
    private BigDecimal newDeductions;

    @Column(name = "old_net_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal oldNetSalary;

    @Column(name = "new_net_salary", nullable = false, precision = 12, scale = 2)
    private BigDecimal newNetSalary;

    @Column(name = "changed_at", insertable = false, updatable = false)
    private LocalDateTime changedAt;

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
