
package com.opsoft.hrms_backend.service;

import com.opsoft.hrms_backend.dto.SalaryStructureRequest;
import com.opsoft.hrms_backend.dto.SalaryStructureUpdateRequest;
import com.opsoft.hrms_backend.dto.SalaryStructureResponse;

import com.opsoft.hrms_backend.entity.Employee;
import com.opsoft.hrms_backend.entity.SalaryStructure;

import com.opsoft.hrms_backend.repository.EmployeeRepository;
import com.opsoft.hrms_backend.repository.SalaryStructureRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
public class SalaryStructureService {

    private final SalaryStructureRepository salaryRepository;
    private final EmployeeRepository employeeRepository;

    public SalaryStructureService(
            SalaryStructureRepository salaryRepository,
            EmployeeRepository employeeRepository) {

        this.salaryRepository = salaryRepository;
        this.employeeRepository = employeeRepository;
    }

    // ==========================================
    // 1. CREATE INITIAL SALARY STRUCTURE
    //    DURING EMPLOYEE REGISTRATION
    // ==========================================

    @Transactional
    public SalaryStructureResponse createInitialSalaryStructure(
            String employeeId,
            SalaryStructureRequest request,
            BigDecimal expectedGrossSalary) {

        if (employeeId == null || employeeId.isBlank()) {
            throw new IllegalArgumentException(
                    "Employee ID is required");
        }

        Employee employee =
                employeeRepository.findByEmployeeId(employeeId);

        if (employee == null) {
            throw new IllegalArgumentException(
                    "Employee not found");
        }

        if (request == null) {
            throw new IllegalArgumentException(
                    "Initial Salary Structure is required");
        }

        if (request.getEffectiveFrom() == null) {
            throw new IllegalArgumentException(
                    "Salary effective date is required");
        }

        // Validate all salary components
        validateSalary(request);

        // Calculate total earnings
        BigDecimal calculatedGross = calculateGrossSalary(
                request.getBasicSalary(),
                request.getHra(),
                request.getConveyanceAllowance(),
                request.getMedicalAllowance(),
                request.getSpecialAllowance(),
                request.getOtherAllowances()
        );

        // Validate monthly gross salary
        if (expectedGrossSalary == null ||
                calculatedGross.compareTo(expectedGrossSalary) != 0) {

            throw new IllegalArgumentException(
                    "Salary Structure total earnings (" +
                    calculatedGross +
                    ") must equal Employee monthly gross salary (" +
                    expectedGrossSalary + ")");
        }

        // Check whether a salary structure already exists
        List<SalaryStructure> existingStructures =
                salaryRepository.findByEmployeeId(employeeId);

        if (!existingStructures.isEmpty()) {
            throw new IllegalArgumentException(
                    "A Salary Structure already exists for this employee");
        }

        // Create initial Salary Structure
        SalaryStructure salary = new SalaryStructure();

        salary.setEmployeeId(employeeId);

        salary.setBasicSalary(request.getBasicSalary());
        salary.setHra(request.getHra());
        salary.setConveyanceAllowance(
                request.getConveyanceAllowance());
        salary.setMedicalAllowance(
                request.getMedicalAllowance());
        salary.setSpecialAllowance(
                request.getSpecialAllowance());
        salary.setOtherAllowances(
                request.getOtherAllowances());

        salary.setEmployeePf(request.getEmployeePf());
        salary.setEmployerPf(request.getEmployerPf());
        salary.setProfessionalTax(
                request.getProfessionalTax());
        salary.setOtherDeductions(
                request.getOtherDeductions());

        salary.setEffectiveFrom(request.getEffectiveFrom());
        salary.setEffectiveTo(null);

        SalaryStructure savedSalary =
                salaryRepository.save(salary);

        // Synchronize Employee Profile gross salary
        employee.setSalary(calculatedGross);
        employeeRepository.save(employee);

        return convertToResponse(savedSalary);
    }

    // ==========================================
    // 2. CREATE SALARY STRUCTURE
    // ==========================================

    @Transactional
    public SalaryStructureResponse createSalaryStructure(
            SalaryStructureRequest request) {

        if (request.getEmployeeId() == null ||
                request.getEmployeeId().isBlank()) {

            throw new IllegalArgumentException(
                    "Employee ID is required");
        }

        Employee employee =
                employeeRepository.findByEmployeeId(
                        request.getEmployeeId());

        if (employee == null) {
            throw new IllegalArgumentException(
                    "Employee not found");
        }

        if (request.getEffectiveFrom() == null) {
            throw new IllegalArgumentException(
                    "Effective date is required");
        }

        validateSalary(request);

        List<SalaryStructure> existingStructures =
                salaryRepository.findByEmployeeId(
                        request.getEmployeeId());

        for (SalaryStructure existing : existingStructures) {

            if (isDateOverlapping(
                    request.getEffectiveFrom(),
                    null,
                    existing)) {

                throw new IllegalArgumentException(
                        "A salary structure already exists for this effective date");
            }
        }

        SalaryStructure previous = existingStructures.stream()
                .filter(s -> !s.getEffectiveFrom()
                        .isAfter(request.getEffectiveFrom()))
                .filter(s -> s.getEffectiveTo() == null ||
                        !s.getEffectiveTo().isBefore(
                                request.getEffectiveFrom()))
                .max(Comparator.comparing(
                        s -> s.getEffectiveFrom()))
                .orElse(null);

        if (previous != null) {

            previous.setEffectiveTo(
                    request.getEffectiveFrom().minusDays(1));

            salaryRepository.save(previous);
        }

        SalaryStructure salary = new SalaryStructure();

        salary.setEmployeeId(request.getEmployeeId());

        salary.setBasicSalary(request.getBasicSalary());
        salary.setHra(request.getHra());
        salary.setConveyanceAllowance(
                request.getConveyanceAllowance());
        salary.setMedicalAllowance(
                request.getMedicalAllowance());
        salary.setSpecialAllowance(
                request.getSpecialAllowance());
        salary.setOtherAllowances(
                request.getOtherAllowances());

        salary.setEmployeePf(request.getEmployeePf());
        salary.setEmployerPf(request.getEmployerPf());
        salary.setProfessionalTax(
                request.getProfessionalTax());
        salary.setOtherDeductions(
                request.getOtherDeductions());

        salary.setEffectiveFrom(request.getEffectiveFrom());
        salary.setEffectiveTo(null);

        SalaryStructure savedSalary =
                salaryRepository.save(salary);

        // Automatically synchronize Employee Profile salary
        synchronizeEmployeeGrossSalary(
                request.getEmployeeId());

        return convertToResponse(savedSalary);
    }


    // ==========================================
    // 3. DIRECTLY UPDATE AN EXISTING SALARY RECORD
    //    Keeps the same ID and effective dates.
    // ==========================================

    @Transactional
    public SalaryStructureResponse updateExistingSalaryStructure(
            Long id,
            SalaryStructureUpdateRequest request) {

        SalaryStructure salary = salaryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Salary structure not found"));

        validateSalary(request);

        // Update only salary components. Do not change employee ID,
        // effectiveFrom, effectiveTo, or the salary record ID.
        salary.setBasicSalary(request.getBasicSalary());
        salary.setHra(request.getHra());
        salary.setConveyanceAllowance(request.getConveyanceAllowance());
        salary.setMedicalAllowance(request.getMedicalAllowance());
        salary.setSpecialAllowance(request.getSpecialAllowance());
        salary.setOtherAllowances(request.getOtherAllowances());

        salary.setEmployeePf(request.getEmployeePf());
        salary.setEmployerPf(request.getEmployerPf());
        salary.setProfessionalTax(request.getProfessionalTax());
        salary.setOtherDeductions(request.getOtherDeductions());

        SalaryStructure updatedSalary = salaryRepository.save(salary);

        // Updates employee profile salary only when this record is
        // effective today; historical/future records won't overwrite it.
        synchronizeEmployeeGrossSalary(salary.getEmployeeId());

        return convertToResponse(updatedSalary);
    }

    // ==========================================
    // 4. REVISE SALARY STRUCTURE
    //    Creates a new record and preserves history.
    // ==========================================

    @Transactional
    public SalaryStructureResponse reviseSalaryStructure(
            Long id,
            SalaryStructureUpdateRequest request) {

        SalaryStructure current =
                salaryRepository.findById(id)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Salary structure not found"));

        if (request.getEffectiveFrom() == null) {
            throw new IllegalArgumentException(
                    "Effective date is required");
        }

        validateSalary(request);

        LocalDate newEffectiveDate =
                request.getEffectiveFrom();

        if (!newEffectiveDate.isAfter(
                current.getEffectiveFrom())) {

            throw new IllegalArgumentException(
                    "Revision date must be after the selected salary structure's start date");
        }

        if (current.getEffectiveTo() != null &&
                !newEffectiveDate.isBefore(
                        current.getEffectiveTo())) {

            throw new IllegalArgumentException(
                    "Revision date must be before the selected salary structure's end date");
        }

        List<SalaryStructure> allStructures =
                salaryRepository.findByEmployeeId(
                        current.getEmployeeId());

        for (SalaryStructure existing : allStructures) {

            if (existing.getId().equals(current.getId())) {
                continue;
            }

            if (isDateOverlapping(
                    newEffectiveDate,
                    null,
                    existing)) {

                throw new IllegalArgumentException(
                        "Another salary structure already covers this effective date");
            }
        }

        // Close previous salary structure
        current.setEffectiveTo(
                newEffectiveDate.minusDays(1));

        salaryRepository.save(current);

        // Create revised salary structure
        SalaryStructure revised = new SalaryStructure();

        revised.setEmployeeId(current.getEmployeeId());

        revised.setBasicSalary(request.getBasicSalary());
        revised.setHra(request.getHra());
        revised.setConveyanceAllowance(
                request.getConveyanceAllowance());
        revised.setMedicalAllowance(
                request.getMedicalAllowance());
        revised.setSpecialAllowance(
                request.getSpecialAllowance());
        revised.setOtherAllowances(
                request.getOtherAllowances());

        revised.setEmployeePf(request.getEmployeePf());
        revised.setEmployerPf(request.getEmployerPf());
        revised.setProfessionalTax(
                request.getProfessionalTax());
        revised.setOtherDeductions(
                request.getOtherDeductions());

        revised.setEffectiveFrom(newEffectiveDate);
        revised.setEffectiveTo(null);

        SalaryStructure savedSalary =
                salaryRepository.save(revised);

        // Synchronize Employee Profile salary
        synchronizeEmployeeGrossSalary(
                current.getEmployeeId());

        return convertToResponse(savedSalary);
    }

    // ==========================================
    // 4. AUTOMATIC EMPLOYEE SALARY SYNCHRONIZATION
    // ==========================================

    private void synchronizeEmployeeGrossSalary(
            String employeeId) {

        Employee employee =
                employeeRepository.findByEmployeeId(employeeId);

        if (employee == null) {
            throw new IllegalArgumentException(
                    "Employee not found");
        }

        LocalDate today = LocalDate.now();

        // Find the salary structure effective today
        SalaryStructure currentSalary =
                salaryRepository.findByEmployeeId(employeeId)
                        .stream()
                        .filter(s -> !s.getEffectiveFrom()
                                .isAfter(today))
                        .filter(s -> s.getEffectiveTo() == null ||
                                !s.getEffectiveTo().isBefore(today))
                        .max((first, second) ->
                                first.getEffectiveFrom().compareTo(
                                        second.getEffectiveFrom()))
                        .orElse(null);

        // Future-effective structure must not change
        // the employee's current profile salary
        if (currentSalary == null) {
            return;
        }

        BigDecimal grossSalary =
                calculateGrossSalary(currentSalary);

        employee.setSalary(grossSalary);

        employeeRepository.save(employee);
    }

    // ==========================================
    // 5. CALCULATE MONTHLY GROSS SALARY
    // ==========================================

    private BigDecimal calculateGrossSalary(
            SalaryStructure salary) {

        return calculateGrossSalary(
                salary.getBasicSalary(),
                salary.getHra(),
                salary.getConveyanceAllowance(),
                salary.getMedicalAllowance(),
                salary.getSpecialAllowance(),
                salary.getOtherAllowances()
        );
    }

    private BigDecimal calculateGrossSalary(
            BigDecimal basicSalary,
            BigDecimal hra,
            BigDecimal conveyance,
            BigDecimal medical,
            BigDecimal special,
            BigDecimal otherAllowances) {

        return basicSalary
                .add(hra)
                .add(conveyance)
                .add(medical)
                .add(special)
                .add(otherAllowances);
    }

    // ==========================================
    // 6. CHECK DATE OVERLAPPING
    // ==========================================

    private boolean isDateOverlapping(
            LocalDate newStart,
            LocalDate newEnd,
            SalaryStructure existing) {

        LocalDate existingEnd =
                existing.getEffectiveTo();

        boolean startsBeforeExistingEnds =
                existingEnd == null ||
                !newStart.isAfter(existingEnd);

        boolean endsAfterExistingStarts =
                newEnd == null ||
                !newEnd.isBefore(existing.getEffectiveFrom());

        return startsBeforeExistingEnds &&
                endsAfterExistingStarts;
    }

    // ==========================================
    // 7. VALIDATE CREATE REQUEST
    // ==========================================

    private void validateSalary(
            SalaryStructureRequest request) {

        validateAmounts(
                request.getBasicSalary(),
                request.getHra(),
                request.getConveyanceAllowance(),
                request.getMedicalAllowance(),
                request.getSpecialAllowance(),
                request.getOtherAllowances(),
                request.getEmployeePf(),
                request.getEmployerPf(),
                request.getProfessionalTax(),
                request.getOtherDeductions()
        );
    }

    // ==========================================
    // 8. VALIDATE UPDATE REQUEST
    // ==========================================

    private void validateSalary(
            SalaryStructureUpdateRequest request) {

        validateAmounts(
                request.getBasicSalary(),
                request.getHra(),
                request.getConveyanceAllowance(),
                request.getMedicalAllowance(),
                request.getSpecialAllowance(),
                request.getOtherAllowances(),
                request.getEmployeePf(),
                request.getEmployerPf(),
                request.getProfessionalTax(),
                request.getOtherDeductions()
        );
    }

    // ==========================================
    // 9. VALIDATE SALARY AMOUNTS
    // ==========================================

    private void validateAmounts(
            BigDecimal basicSalary,
            BigDecimal hra,
            BigDecimal conveyance,
            BigDecimal medical,
            BigDecimal special,
            BigDecimal otherAllowances,
            BigDecimal employeePf,
            BigDecimal employerPf,
            BigDecimal professionalTax,
            BigDecimal otherDeductions) {

        validateAmount(basicSalary, "Basic salary");
        validateAmount(hra, "HRA");
        validateAmount(conveyance, "Conveyance allowance");
        validateAmount(medical, "Medical allowance");
        validateAmount(special, "Special allowance");
        validateAmount(otherAllowances, "Other allowances");
        validateAmount(employeePf, "Employee PF");
        validateAmount(employerPf, "Employer PF");
        validateAmount(professionalTax, "Professional tax");
        validateAmount(otherDeductions, "Other deductions");

        if (basicSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Basic salary must be greater than zero");
        }

        BigDecimal gross = basicSalary
                .add(hra)
                .add(conveyance)
                .add(medical)
                .add(special)
                .add(otherAllowances);

        BigDecimal deductions = employeePf
                .add(professionalTax)
                .add(otherDeductions);

        if (deductions.compareTo(gross) > 0) {
            throw new IllegalArgumentException(
                    "Total deductions cannot exceed gross salary");
        }
    }

    private void validateAmount(
            BigDecimal amount,
            String name) {

        if (amount == null) {
            throw new IllegalArgumentException(
                    name + " is required");
        }

        if (amount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    name + " cannot be negative");
        }
    }

    // ==========================================
    // 10. GET ALL SALARY STRUCTURES
    // ==========================================

    public List<SalaryStructureResponse> getAllSalaryStructures() {

        return salaryRepository.findAll()
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // ==========================================
    // 11. GET SALARY HISTORY BY EMPLOYEE
    // ==========================================

    public List<SalaryStructureResponse> getSalaryStructuresByEmployeeId(
            String employeeId) {

        if (employeeRepository.findByEmployeeId(employeeId) == null) {
            throw new IllegalArgumentException(
                    "Employee not found");
        }

        return salaryRepository.findByEmployeeId(employeeId)
                .stream()
                .map(this::convertToResponse)
                .toList();
    }

    // ==========================================
    // 12. GET CURRENT EFFECTIVE SALARY
    // ==========================================

    public SalaryStructureResponse getCurrentSalaryStructure(
            String employeeId) {

        LocalDate today = LocalDate.now();

        List<SalaryStructure> structures =
                salaryRepository.findByEmployeeId(employeeId);

        SalaryStructure current = structures.stream()
                .filter(s -> !s.getEffectiveFrom().isAfter(today))
                .filter(s -> s.getEffectiveTo() == null ||
                        !s.getEffectiveTo().isBefore(today))
                .max((first, second) ->
                        first.getEffectiveFrom().compareTo(
                                second.getEffectiveFrom()))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No current salary structure found"));

        return convertToResponse(current);
    }

    // ==========================================
    // 13. CONVERT ENTITY TO RESPONSE DTO
    // ==========================================

    private SalaryStructureResponse convertToResponse(
            SalaryStructure salary) {

        BigDecimal grossSalary =
                calculateGrossSalary(salary);

        BigDecimal totalDeductions =
                salary.getEmployeePf()
                        .add(salary.getProfessionalTax())
                        .add(salary.getOtherDeductions());

        BigDecimal netSalary =
                grossSalary.subtract(totalDeductions);

        BigDecimal totalEmployerCost =
                grossSalary.add(salary.getEmployerPf());

        SalaryStructureResponse response =
                new SalaryStructureResponse();

        response.setId(salary.getId());
        response.setEmployeeId(salary.getEmployeeId());

        response.setBasicSalary(salary.getBasicSalary());
        response.setHra(salary.getHra());
        response.setConveyanceAllowance(
                salary.getConveyanceAllowance());
        response.setMedicalAllowance(
                salary.getMedicalAllowance());
        response.setSpecialAllowance(
                salary.getSpecialAllowance());
        response.setOtherAllowances(
                salary.getOtherAllowances());

        response.setEmployeePf(salary.getEmployeePf());
        response.setEmployerPf(salary.getEmployerPf());
        response.setProfessionalTax(
                salary.getProfessionalTax());
        response.setOtherDeductions(
                salary.getOtherDeductions());

        response.setGrossSalary(grossSalary);
        response.setTotalDeductions(totalDeductions);
        response.setNetSalary(netSalary);
        response.setTotalEmployerCost(totalEmployerCost);

        response.setEffectiveFrom(salary.getEffectiveFrom());
        response.setEffectiveTo(salary.getEffectiveTo());

        return response;
    }
}
