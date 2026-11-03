
package com.opsoft.hrms_backend.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public class EmployeeCreateRequest {

    @NotBlank(message = "Employee ID is required")
    @Size(max = 30)
    private String employeeId;

    @NotBlank(message = "Employee name is required")
    @Size(max = 100)
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email address")
    @Size(max = 150)
    private String email;

    @NotBlank(message = "Role is required")
    @Pattern(
        regexp = "ADMIN|HR|EMPLOYEE",
        message = "Role must be ADMIN, HR or EMPLOYEE"
    )
    private String role;

    @Size(max = 20)
    private String phone;

    @Size(max = 100)
    private String department;

    @Size(max = 100)
    private String designation;

    private LocalDate joiningDate;

    @NotNull(message = "Monthly gross salary is required")
    @DecimalMin(
        value = "0.01",
        message = "Monthly gross salary must be greater than zero"
    )
    private BigDecimal salary;

    // Initial Salary Structure is mandatory
    @NotNull(message = "Initial Salary Structure is required")
    @Valid
    private SalaryStructureRequest salaryStructure;

    public EmployeeCreateRequest() {
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getDesignation() {
        return designation;
    }

    public void setDesignation(String designation) {
        this.designation = designation;
    }

    public LocalDate getJoiningDate() {
        return joiningDate;
    }

    public void setJoiningDate(LocalDate joiningDate) {
        this.joiningDate = joiningDate;
    }

    public BigDecimal getSalary() {
        return salary;
    }

    public void setSalary(BigDecimal salary) {
        this.salary = salary;
    }

    public SalaryStructureRequest getSalaryStructure() {
        return salaryStructure;
    }

    public void setSalaryStructure(
            SalaryStructureRequest salaryStructure) {
        this.salaryStructure = salaryStructure;
    }
}
