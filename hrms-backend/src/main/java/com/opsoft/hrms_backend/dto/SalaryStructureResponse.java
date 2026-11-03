
package com.opsoft.hrms_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public class SalaryStructureResponse {

    private Long id;
    private String employeeId;

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

    private BigDecimal grossSalary;
    private BigDecimal totalDeductions;
    private BigDecimal netSalary;
    private BigDecimal totalEmployerCost;

    private LocalDate effectiveFrom;
    private LocalDate effectiveTo;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getEmployeeId() { return employeeId; }
    public void setEmployeeId(String employeeId) { this.employeeId = employeeId; }

    public BigDecimal getBasicSalary() { return basicSalary; }
    public void setBasicSalary(BigDecimal value) { basicSalary = value; }

    public BigDecimal getHra() { return hra; }
    public void setHra(BigDecimal value) { hra = value; }

    public BigDecimal getConveyanceAllowance() { return conveyanceAllowance; }
    public void setConveyanceAllowance(BigDecimal value) { conveyanceAllowance = value; }

    public BigDecimal getMedicalAllowance() { return medicalAllowance; }
    public void setMedicalAllowance(BigDecimal value) { medicalAllowance = value; }

    public BigDecimal getSpecialAllowance() { return specialAllowance; }
    public void setSpecialAllowance(BigDecimal value) { specialAllowance = value; }

    public BigDecimal getOtherAllowances() { return otherAllowances; }
    public void setOtherAllowances(BigDecimal value) { otherAllowances = value; }

    public BigDecimal getEmployeePf() { return employeePf; }
    public void setEmployeePf(BigDecimal value) { employeePf = value; }

    public BigDecimal getEmployerPf() { return employerPf; }
    public void setEmployerPf(BigDecimal value) { employerPf = value; }

    public BigDecimal getProfessionalTax() { return professionalTax; }
    public void setProfessionalTax(BigDecimal value) { professionalTax = value; }

    public BigDecimal getOtherDeductions() { return otherDeductions; }
    public void setOtherDeductions(BigDecimal value) { otherDeductions = value; }

    public BigDecimal getGrossSalary() { return grossSalary; }
    public void setGrossSalary(BigDecimal value) { grossSalary = value; }

    public BigDecimal getTotalDeductions() { return totalDeductions; }
    public void setTotalDeductions(BigDecimal value) { totalDeductions = value; }

    public BigDecimal getNetSalary() { return netSalary; }
    public void setNetSalary(BigDecimal value) { netSalary = value; }

    public BigDecimal getTotalEmployerCost() { return totalEmployerCost; }
    public void setTotalEmployerCost(BigDecimal value) { totalEmployerCost = value; }

    public LocalDate getEffectiveFrom() { return effectiveFrom; }
    public void setEffectiveFrom(LocalDate value) { effectiveFrom = value; }

    public LocalDate getEffectiveTo() { return effectiveTo; }
    public void setEffectiveTo(LocalDate value) { effectiveTo = value; }
}
