
package com.opsoft.hrms_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SalaryStructureUpdateRequest {

    private BigDecimal basicSalary;
    private BigDecimal hra;
    private BigDecimal conveyanceAllowance;
    private BigDecimal medicalAllowance;
    private BigDecimal specialAllowance;
    private BigDecimal otherAllowances;

    private BigDecimal employeePf;
    private BigDecimal employerPf;
    private BigDecimal professionalTax;
    private BigDecimal otherDeductions;

    private LocalDate effectiveFrom;

    public BigDecimal getBasicSalary() {
        return basicSalary;
    }

    public void setBasicSalary(BigDecimal basicSalary) {
        this.basicSalary = basicSalary;
    }

    public BigDecimal getHra() {
        return hra;
    }

    public void setHra(BigDecimal hra) {
        this.hra = hra;
    }

    public BigDecimal getConveyanceAllowance() {
        return conveyanceAllowance;
    }

    public void setConveyanceAllowance(BigDecimal conveyanceAllowance) {
        this.conveyanceAllowance = conveyanceAllowance;
    }

    public BigDecimal getMedicalAllowance() {
        return medicalAllowance;
    }

    public void setMedicalAllowance(BigDecimal medicalAllowance) {
        this.medicalAllowance = medicalAllowance;
    }

    public BigDecimal getSpecialAllowance() {
        return specialAllowance;
    }

    public void setSpecialAllowance(BigDecimal specialAllowance) {
        this.specialAllowance = specialAllowance;
    }

    public BigDecimal getOtherAllowances() {
        return otherAllowances;
    }

    public void setOtherAllowances(BigDecimal otherAllowances) {
        this.otherAllowances = otherAllowances;
    }

    public BigDecimal getEmployeePf() {
        return employeePf;
    }

    public void setEmployeePf(BigDecimal employeePf) {
        this.employeePf = employeePf;
    }

    public BigDecimal getEmployerPf() {
        return employerPf;
    }

    public void setEmployerPf(BigDecimal employerPf) {
        this.employerPf = employerPf;
    }

    public BigDecimal getProfessionalTax() {
        return professionalTax;
    }

    public void setProfessionalTax(BigDecimal professionalTax) {
        this.professionalTax = professionalTax;
    }

    public BigDecimal getOtherDeductions() {
        return otherDeductions;
    }

    public void setOtherDeductions(BigDecimal otherDeductions) {
        this.otherDeductions = otherDeductions;
    }

    public LocalDate getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDate effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }
}
