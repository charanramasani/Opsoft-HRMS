
package com.opsoft.hrms_backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "salary_structures")
public class SalaryStructure {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "employee_id", nullable = false, length = 30)
    private String employeeId;

    @Column(name = "basic_salary", nullable = false)
    private BigDecimal basicSalary = BigDecimal.ZERO;

    @Column(nullable = false)
    private BigDecimal hra = BigDecimal.ZERO;

    @Column(name = "conveyance_allowance", nullable = false)
    private BigDecimal conveyanceAllowance = BigDecimal.ZERO;

    @Column(name = "medical_allowance", nullable = false)
    private BigDecimal medicalAllowance = BigDecimal.ZERO;

    @Column(name = "special_allowance", nullable = false)
    private BigDecimal specialAllowance = BigDecimal.ZERO;

    @Column(name = "other_allowances", nullable = false)
    private BigDecimal otherAllowances = BigDecimal.ZERO;

    @Column(name = "employee_pf", nullable = false)
    private BigDecimal employeePf = BigDecimal.ZERO;

    @Column(name = "employer_pf", nullable = false)
    private BigDecimal employerPf = BigDecimal.ZERO;

    @Column(name = "professional_tax", nullable = false)
    private BigDecimal professionalTax = BigDecimal.ZERO;

    @Column(name = "other_deductions", nullable = false)
    private BigDecimal otherDeductions = BigDecimal.ZERO;

    @Column(name = "effective_from", nullable = false)
    private LocalDate effectiveFrom;

    @Column(name = "effective_to")
    private LocalDate effectiveTo;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

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

    public LocalDate getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDate effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
