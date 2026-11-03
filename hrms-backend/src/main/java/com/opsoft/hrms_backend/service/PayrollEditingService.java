
package com.opsoft.hrms_backend.service;

import com.opsoft.hrms_backend.dto.PayrollEditRequest;
import com.opsoft.hrms_backend.dto.PayrollComponentResponse;
import com.opsoft.hrms_backend.dto.PayrollEditHistoryResponse;

import com.opsoft.hrms_backend.entity.PayrollComponent;
import com.opsoft.hrms_backend.entity.PayrollEditHistory;
import com.opsoft.hrms_backend.entity.PayrollRecord;
import com.opsoft.hrms_backend.entity.SalaryStructure;

import com.opsoft.hrms_backend.repository.PayrollComponentRepository;
import com.opsoft.hrms_backend.repository.PayrollEditHistoryRepository;
import com.opsoft.hrms_backend.repository.PayrollRecordRepository;
import com.opsoft.hrms_backend.repository.SalaryStructureRepository;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.LocalDate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PayrollEditingService {

    private final PayrollRecordRepository payrollRepository;
    private final PayrollComponentRepository componentRepository;
    private final PayrollEditHistoryRepository historyRepository;
    private final SalaryStructureRepository salaryStructureRepository;

    public PayrollEditingService(
            PayrollRecordRepository payrollRepository,
            PayrollComponentRepository componentRepository,
            PayrollEditHistoryRepository historyRepository,
            SalaryStructureRepository salaryStructureRepository) {

        this.payrollRepository = payrollRepository;
        this.componentRepository = componentRepository;
        this.historyRepository = historyRepository;
        this.salaryStructureRepository = salaryStructureRepository;
    }

    // ==========================================
    // 1. EDIT PAYROLL COMPONENTS
    // ==========================================

    @Transactional
    public PayrollRecord editPayroll(
            Long payrollId,
            PayrollEditRequest request) {

        PayrollRecord payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payroll record not found"));

        if (payroll.getPaymentStatus() == null ||
                "PAID".equals(payroll.getPaymentStatus().name())) {

            throw new IllegalArgumentException(
                    "Paid payroll cannot be edited");
        }

        if (request == null ||
                request.getChangeReason() == null ||
                request.getChangeReason().trim().isEmpty()) {

            throw new IllegalArgumentException(
                    "Please provide a reason for editing payroll");
        }

        if (request.getChangeReason().trim().length() > 500) {
            throw new IllegalArgumentException(
                    "Change reason cannot exceed 500 characters");
        }

        if (request.getComponents() == null ||
                request.getComponents().isEmpty()) {

            throw new IllegalArgumentException(
                    "At least one payroll component is required");
        }

        List<PayrollComponent> oldComponents =
                componentRepository.findByPayrollId(payrollId);

        // Initialize components for older payroll records.
        if (oldComponents.isEmpty()) {
            oldComponents = initializeComponents(payroll);
        }

        String oldSnapshot = createJsonSnapshot(oldComponents);

        List<PayrollComponent> newComponents = new ArrayList<>();

        Set<String> uniqueComponents = new HashSet<>();

        BigDecimal grossSalary = BigDecimal.ZERO;
        BigDecimal deductions = BigDecimal.ZERO;

        boolean basicSalaryFound = false;

        for (PayrollEditRequest.ComponentRequest item :
                request.getComponents()) {

            if (item.getComponentName() == null ||
                    !item.getComponentName().trim()
                            .matches("[A-Za-z0-9_ ]{1,100}")) {

                throw new IllegalArgumentException(
                        "Invalid component name");
            }

            String componentName = item.getComponentName()
                    .trim()
                    .toUpperCase()
                    .replaceAll("\\s+", "_");

            if ("EMPLOYER_PF".equals(componentName)) {
                throw new IllegalArgumentException(
                        "Employer PF must not be included in employee payroll totals");
            }

            if (!uniqueComponents.add(componentName)) {
                throw new IllegalArgumentException(
                        "Duplicate component: " + componentName);
            }

            if (item.getComponentType() == null) {
                throw new IllegalArgumentException(
                        "Component type is required");
            }

            PayrollComponent.ComponentType type;

            try {
                type = PayrollComponent.ComponentType.valueOf(
                        item.getComponentType().trim().toUpperCase());

            } catch (IllegalArgumentException ex) {

                throw new IllegalArgumentException(
                        "Component type must be EARNING or DEDUCTION");
            }

            if (item.getAmount() == null ||
                    item.getAmount().compareTo(BigDecimal.ZERO) < 0) {

                throw new IllegalArgumentException(
                        "Component amount cannot be negative or empty");
            }

            PayrollComponent component = new PayrollComponent();

            component.setPayrollId(payrollId);
            component.setComponentName(componentName);
            component.setComponentType(type);
            component.setAmount(item.getAmount());

            newComponents.add(component);

            if (type == PayrollComponent.ComponentType.EARNING) {

                grossSalary = grossSalary.add(item.getAmount());

                if ("BASIC_SALARY".equals(componentName)) {
                    basicSalaryFound = true;
                }

            } else {

                deductions = deductions.add(item.getAmount());
            }
        }

        if (!basicSalaryFound) {
            throw new IllegalArgumentException(
                    "BASIC_SALARY earning component is required");
        }

        if (grossSalary.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Gross salary must be greater than zero");
        }

        if (deductions.compareTo(grossSalary) > 0) {
            throw new IllegalArgumentException(
                    "Total deductions cannot exceed gross salary");
        }

        grossSalary = grossSalary.setScale(2, RoundingMode.HALF_UP);
        deductions = deductions.setScale(2, RoundingMode.HALF_UP);

        BigDecimal netSalary = grossSalary.subtract(deductions);

        String newSnapshot = createJsonSnapshot(newComponents);

        BigDecimal oldGross = payroll.getGrossSalary();
        BigDecimal oldDeductions = payroll.getDeductions();
        BigDecimal oldNet = payroll.getNetSalary();

        // Replace old components with new components.
        componentRepository.deleteByPayrollId(payrollId);

        componentRepository.saveAll(newComponents);

        // Update payroll totals.
        payroll.setGrossSalary(grossSalary);
        payroll.setDeductions(deductions);
        payroll.setNetSalary(netSalary);

        PayrollRecord updatedPayroll =
                payrollRepository.save(payroll);

        // Identify the logged-in Admin or HR.
        Authentication authentication =
                SecurityContextHolder.getContext().getAuthentication();

        String changedBy = authentication != null
                ? authentication.getName()
                : null;

        // Save editing history.
        PayrollEditHistory history = new PayrollEditHistory();

        history.setPayrollId(payrollId);
        history.setChangedByEmployeeId(changedBy);
        history.setChangeReason(request.getChangeReason().trim());

        history.setOldComponents(oldSnapshot);
        history.setNewComponents(newSnapshot);

        history.setOldGrossSalary(oldGross);
        history.setNewGrossSalary(grossSalary);

        history.setOldDeductions(oldDeductions);
        history.setNewDeductions(deductions);

        history.setOldNetSalary(oldNet);
        history.setNewNetSalary(netSalary);

        historyRepository.save(history);

        return updatedPayroll;
    }

    // ==========================================
    // 2. GET PAYROLL COMPONENTS
    // ==========================================

    @Transactional
    public List<PayrollComponentResponse> getPayrollComponents(
            Long payrollId) {

        PayrollRecord payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Payroll record not found"));

        List<PayrollComponent> components =
                componentRepository.findByPayrollId(payrollId);

        // Initialize components for older payroll records.
        if (components.isEmpty()) {
            components = initializeComponents(payroll);
        }

        return components.stream()
                .map(component -> new PayrollComponentResponse(
                        component.getId(),
                        component.getComponentName(),
                        component.getComponentType().name(),
                        component.getAmount()))
                .toList();
    }

    // ==========================================
    // 3. GET COMPONENTS FOR LOGGED-IN EMPLOYEE
    // ==========================================

    @Transactional
    public List<PayrollComponentResponse> getMyPayrollComponents(
            Long payrollId,
            String authenticatedEmployeeId) {

        if (authenticatedEmployeeId == null
                || authenticatedEmployeeId.isBlank()) {
            throw new IllegalArgumentException(
                    "Authenticated employee is required");
        }

        PayrollRecord payroll = payrollRepository.findById(payrollId)
                .orElseThrow(() ->
                        new IllegalArgumentException("Payslip not found"));

        // Never allow an employee to access another employee's components.
        if (!authenticatedEmployeeId.equals(payroll.getEmployeeId())) {
            throw new IllegalArgumentException("Payslip not found");
        }

        // Employees can view only payslips released by HR/Admin.
        if (payroll.getPaymentStatus() == null
                || (payroll.getPaymentStatus()
                        != PayrollRecord.PaymentStatus.PUBLISHED
                    && payroll.getPaymentStatus()
                        != PayrollRecord.PaymentStatus.PAID)) {
            throw new IllegalArgumentException("Payslip not found");
        }

        return getPayrollComponents(payrollId);
    }

    // ==========================================
    // 3. GET PAYROLL EDIT HISTORY
    // ==========================================

    @Transactional(readOnly = true)
    public List<PayrollEditHistoryResponse> getPayrollEditHistory(
            Long payrollId) {

        if (!payrollRepository.existsById(payrollId)) {
            throw new IllegalArgumentException(
                    "Payroll record not found");
        }

        return historyRepository
                .findByPayrollIdOrderByChangedAtDesc(payrollId)
                .stream()
                .map(history -> {

                    PayrollEditHistoryResponse response =
                            new PayrollEditHistoryResponse();

                    response.setId(history.getId());

                    response.setPayrollId(
                            history.getPayrollId());

                    response.setChangedByEmployeeId(
                            history.getChangedByEmployeeId());

                    response.setChangeReason(
                            history.getChangeReason());

                    response.setOldComponents(
                            history.getOldComponents());

                    response.setNewComponents(
                            history.getNewComponents());

                    response.setOldGrossSalary(
                            history.getOldGrossSalary());

                    response.setNewGrossSalary(
                            history.getNewGrossSalary());

                    response.setOldDeductions(
                            history.getOldDeductions());

                    response.setNewDeductions(
                            history.getNewDeductions());

                    response.setOldNetSalary(
                            history.getOldNetSalary());

                    response.setNewNetSalary(
                            history.getNewNetSalary());

                    response.setChangedAt(
                            history.getChangedAt());

                    return response;
                })
                .toList();
    }

    // ==========================================
    // 4. INITIALIZE PAYROLL COMPONENTS
    // ==========================================
@SuppressWarnings("null")
    private List<PayrollComponent> initializeComponents(
            PayrollRecord payroll) {

        LocalDate monthStart =
                payroll.getPayrollMonth().withDayOfMonth(1);

        LocalDate monthEnd =
                monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        List<SalaryStructure> structures =
                salaryStructureRepository.findByEmployeeId(
                        payroll.getEmployeeId());

        SalaryStructure applicableStructure = structures.stream()
                .filter(s -> !s.getEffectiveFrom().isAfter(monthEnd))
                .filter(s -> s.getEffectiveTo() == null
                        || !s.getEffectiveTo().isBefore(monthStart))
                .max(Comparator.comparing(
                        SalaryStructure::getEffectiveFrom))
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "No applicable salary structure found for this payroll"));

        List<PayrollComponent> components = new ArrayList<>();

        // Add earnings from the salary structure applicable to this month.
        addComponent(components, payroll.getId(), "BASIC_SALARY",
                PayrollComponent.ComponentType.EARNING,
                applicableStructure.getBasicSalary());
        addComponent(components, payroll.getId(), "HRA",
                PayrollComponent.ComponentType.EARNING,
                applicableStructure.getHra());
        addComponent(components, payroll.getId(), "CONVEYANCE",
                PayrollComponent.ComponentType.EARNING,
                applicableStructure.getConveyanceAllowance());
        addComponent(components, payroll.getId(), "MEDICAL_ALLOWANCE",
                PayrollComponent.ComponentType.EARNING,
                applicableStructure.getMedicalAllowance());
        addComponent(components, payroll.getId(), "SPECIAL_ALLOWANCE",
                PayrollComponent.ComponentType.EARNING,
                applicableStructure.getSpecialAllowance());
        addComponent(components, payroll.getId(), "OTHER_ALLOWANCES",
                PayrollComponent.ComponentType.EARNING,
                applicableStructure.getOtherAllowances());

        // Add the original deduction components from the applicable structure.
        addComponent(components, payroll.getId(), "EMPLOYEE_PF",
                PayrollComponent.ComponentType.DEDUCTION,
                applicableStructure.getEmployeePf());
        addComponent(components, payroll.getId(), "PROFESSIONAL_TAX",
                PayrollComponent.ComponentType.DEDUCTION,
                applicableStructure.getProfessionalTax());
        addComponent(components, payroll.getId(), "OTHER_DEDUCTIONS",
                PayrollComponent.ComponentType.DEDUCTION,
                applicableStructure.getOtherDeductions());

        BigDecimal originalGross = components.stream()
                .filter(c -> c.getComponentType()
                        == PayrollComponent.ComponentType.EARNING)
                .map(PayrollComponent::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal originalDeductions = components.stream()
                .filter(c -> c.getComponentType()
                        == PayrollComponent.ComponentType.DEDUCTION)
                .map(PayrollComponent::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (originalGross.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException(
                    "Original salary structure gross must be greater than zero");
        }

        // Preserve the gross salary already stored in this payroll record.
        BigDecimal targetGross = payroll.getGrossSalary() == null
                ? BigDecimal.ZERO
                : payroll.getGrossSalary().setScale(2, RoundingMode.HALF_UP);

        if (targetGross.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Payroll gross salary cannot be negative");
        }

        BigDecimal scaleFactor = targetGross.divide(
                originalGross, 12, RoundingMode.HALF_UP);

        BigDecimal adjustedEarningsTotal = BigDecimal.ZERO;
        PayrollComponent lastEarningComponent = null;

        for (PayrollComponent component : components) {
            if (component.getComponentType()
                    == PayrollComponent.ComponentType.EARNING) {

                BigDecimal adjustedAmount = component.getAmount()
                        .multiply(scaleFactor)
                        .setScale(2, RoundingMode.HALF_UP);

                component.setAmount(adjustedAmount);
                adjustedEarningsTotal =
                        adjustedEarningsTotal.add(adjustedAmount);
                lastEarningComponent = component;
            }
        }

        if (lastEarningComponent == null) {
            throw new IllegalArgumentException(
                    "No earning components found");
        }

        // Absorb rounding so earning components exactly equal stored gross salary.
        BigDecimal earningRoundingDifference =
                targetGross.subtract(adjustedEarningsTotal);
        lastEarningComponent.setAmount(
                lastEarningComponent.getAmount()
                        .add(earningRoundingDifference));

        // Preserve the deductions already stored in this payroll record.
        // If the stored amount is higher, show the difference as a separate,
        // auditable payroll adjustment instead of changing PF or tax silently.
        BigDecimal targetDeductions = payroll.getDeductions() == null
                ? BigDecimal.ZERO
                : payroll.getDeductions().setScale(2, RoundingMode.HALF_UP);

        if (targetDeductions.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    "Payroll deductions cannot be negative");
        }

        if (targetDeductions.compareTo(originalDeductions) > 0) {
            BigDecimal adjustment = targetDeductions.subtract(originalDeductions);
            addComponent(components, payroll.getId(),
                    "PAYROLL_DEDUCTION_ADJUSTMENT",
                    PayrollComponent.ComponentType.DEDUCTION,
                    adjustment);

        } else if (targetDeductions.compareTo(originalDeductions) < 0) {
            // If the stored payroll deduction is lower than the current
            // structure total, scale the deduction components proportionally.
            if (originalDeductions.compareTo(BigDecimal.ZERO) == 0) {
                if (targetDeductions.compareTo(BigDecimal.ZERO) > 0) {
                    addComponent(components, payroll.getId(),
                            "PAYROLL_DEDUCTION_ADJUSTMENT",
                            PayrollComponent.ComponentType.DEDUCTION,
                            targetDeductions);
                }
            } else {
                BigDecimal deductionScale = targetDeductions.divide(
                        originalDeductions, 12, RoundingMode.HALF_UP);
                BigDecimal adjustedDeductionTotal = BigDecimal.ZERO;
                PayrollComponent lastDeductionComponent = null;

                for (PayrollComponent component : components) {
                    if (component.getComponentType()
                            == PayrollComponent.ComponentType.DEDUCTION) {
                        BigDecimal adjustedAmount = component.getAmount()
                                .multiply(deductionScale)
                                .setScale(2, RoundingMode.HALF_UP);
                        component.setAmount(adjustedAmount);
                        adjustedDeductionTotal =
                                adjustedDeductionTotal.add(adjustedAmount);
                        lastDeductionComponent = component;
                    }
                }

                if (lastDeductionComponent != null) {
                    BigDecimal deductionRoundingDifference =
                            targetDeductions.subtract(adjustedDeductionTotal);
                    lastDeductionComponent.setAmount(
                            lastDeductionComponent.getAmount()
                                    .add(deductionRoundingDifference));
                }
            }
        }

        BigDecimal verifiedGross = components.stream()
                .filter(c -> c.getComponentType()
                        == PayrollComponent.ComponentType.EARNING)
                .map(PayrollComponent::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal verifiedDeductions = components.stream()
                .filter(c -> c.getComponentType()
                        == PayrollComponent.ComponentType.DEDUCTION)
                .map(PayrollComponent::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (verifiedGross.compareTo(targetGross) != 0) {
            throw new IllegalArgumentException(
                    "Payroll earning components do not match gross salary");
        }

        if (verifiedDeductions.compareTo(targetDeductions) != 0) {
            throw new IllegalArgumentException(
                    "Payroll deduction components do not match stored deductions");
        }

        return componentRepository.saveAll(components);
    }

    // ==========================================
    // 5. ADD PAYROLL COMPONENT
    // ==========================================

    private void addComponent(
            List<PayrollComponent> components,
            Long payrollId,
            String name,
            PayrollComponent.ComponentType type,
            BigDecimal amount) {

        PayrollComponent component = new PayrollComponent();

        component.setPayrollId(payrollId);
        component.setComponentName(name);
        component.setComponentType(type);

        component.setAmount(
                amount == null ? BigDecimal.ZERO : amount);

        components.add(component);
    }

    // ==========================================
    // 6. CREATE JSON SNAPSHOT
    // ==========================================

    private String createJsonSnapshot(
            List<PayrollComponent> components) {

        StringBuilder json = new StringBuilder("[");

        boolean first = true;

        for (PayrollComponent component : components) {

            if (!first) {
                json.append(",");
            }

            json.append("{")

                    .append("\"componentName\":\"")
                    .append(component.getComponentName())
                    .append("\",")

                    .append("\"componentType\":\"")
                    .append(component.getComponentType().name())
                    .append("\",")

                    .append("\"amount\":")
                    .append(component.getAmount().toPlainString())

                    .append("}");

            first = false;
        }

        json.append("]");

        return json.toString();
    }
}
