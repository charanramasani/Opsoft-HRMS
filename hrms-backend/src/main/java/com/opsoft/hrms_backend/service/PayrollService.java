
package com.opsoft.hrms_backend.service;

import com.opsoft.hrms_backend.dto.PayrollGenerateRequest;
import com.opsoft.hrms_backend.dto.PayrollResponse;
import com.opsoft.hrms_backend.dto.PayrollGenerationResponse;

import com.opsoft.hrms_backend.entity.Employee;
import com.opsoft.hrms_backend.entity.PayrollRecord;
import com.opsoft.hrms_backend.entity.SalaryStructure;
import com.opsoft.hrms_backend.entity.AttendanceRecord;
import com.opsoft.hrms_backend.entity.LeaveRequest;
import com.opsoft.hrms_backend.entity.CompanyHoliday;

import com.opsoft.hrms_backend.repository.EmployeeRepository;
import com.opsoft.hrms_backend.repository.PayrollRecordRepository;
import com.opsoft.hrms_backend.repository.SalaryStructureRepository;
import com.opsoft.hrms_backend.repository.AttendanceRepository;
import com.opsoft.hrms_backend.repository.LeaveRequestRepository;
import com.opsoft.hrms_backend.repository.CompanyHolidayRepository;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class PayrollService {

    private final EmployeeRepository employeeRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final PayrollRecordRepository payrollRecordRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final CompanyHolidayRepository companyHolidayRepository;

    public PayrollService(
            EmployeeRepository employeeRepository,
            SalaryStructureRepository salaryStructureRepository,
            PayrollRecordRepository payrollRecordRepository,
            AttendanceRepository attendanceRepository,
            LeaveRequestRepository leaveRequestRepository,
            CompanyHolidayRepository companyHolidayRepository) {

        this.employeeRepository = employeeRepository;
        this.salaryStructureRepository = salaryStructureRepository;
        this.payrollRecordRepository = payrollRecordRepository;
        this.attendanceRepository = attendanceRepository;
        this.leaveRequestRepository = leaveRequestRepository;
        this.companyHolidayRepository = companyHolidayRepository;
    }

    // 1. Generate monthly payroll using partial generation.
    @Transactional
    public PayrollGenerationResponse generatePayroll(
            PayrollGenerateRequest request) {

        if (request == null || request.getPayrollMonth() == null) {
            throw new IllegalArgumentException(
                    "Payroll month is required");
        }

        LocalDate monthStart =
                request.getPayrollMonth().withDayOfMonth(1);

        LocalDate monthEnd =
                monthStart.withDayOfMonth(monthStart.lengthOfMonth());

        int calendarDays = monthStart.lengthOfMonth();

        LocalDate today = LocalDate.now();

        List<Employee> employees = employeeRepository.findAll();

        List<PayrollResponse> generatedPayrolls =
                new ArrayList<>();

        List<String> skippedEmployees =
                new ArrayList<>();

        for (Employee employee : employees) {

            String status = employee.getStatus();

            if (!"ACTIVE".equalsIgnoreCase(status)
                    && !"ON_NOTICE".equalsIgnoreCase(status)) {
                continue;
            }

            // ADMIN account is not treated as a payroll employee.
            if ("ADMIN".equalsIgnoreCase(employee.getRole())) {
                continue;
            }

            String employeeId = employee.getEmployeeId();

            // Do not overwrite existing payroll records.
            if (payrollRecordRepository
                    .existsByEmployeeIdAndPayrollMonth(
                            employeeId, monthStart)) {

                skippedEmployees.add(
                        employeeId
                                + " - Payroll already exists for this month.");

                continue;
            }

            // Find applicable Salary Structure.
            List<SalaryStructure> structures =
                    salaryStructureRepository.findByEmployeeId(
                            employeeId);

            SalaryStructure applicableSalary =
                    findApplicableSalaryStructure(
                            structures, monthStart, monthEnd);

            // Skip employee if Salary Structure is missing.
            if (applicableSalary == null) {

                skippedEmployees.add(
                        employeeId
                                + " - No Salary Structure applicable for "
                                + monthStart.getMonth()
                                + " "
                                + monthStart.getYear());

                continue;
            }

            // Joining date is required.
            LocalDate joiningDate = employee.getJoiningDate();

            if (joiningDate == null) {

                skippedEmployees.add(
                        employeeId + " - Joining Date is missing.");

                continue;
            }

            LocalDate payableStart = monthStart;

            if (joiningDate.isAfter(payableStart)) {
                payableStart = joiningDate;
            }

            if (payableStart.isAfter(monthEnd)) {

                skippedEmployees.add(
                        employeeId
                                + " - Joining Date is after the payroll month.");

                continue;
            }

            // Calculate gross salary from six earning components.
            BigDecimal monthlyGross =
                    calculateGrossSalary(applicableSalary);

            // Validate current Employee Profile gross salary.
            if (isStructureEffectiveToday(
                    applicableSalary, today)) {

                BigDecimal profileSalary = employee.getSalary();

                if (profileSalary == null
                        || profileSalary.compareTo(monthlyGross) != 0) {

                    skippedEmployees.add(
                            employeeId
                                    + " - Salary mismatch. Profile salary: "
                                    + profileSalary
                                    + ", Salary Structure gross: "
                                    + monthlyGross);

                    continue;
                }
            }

            // Salary per calendar day.
            BigDecimal perDaySalary =
                    monthlyGross.divide(
                            BigDecimal.valueOf(calendarDays),
                            10,
                            RoundingMode.HALF_UP);

            // Calculate employed calendar days.
            long employedDays =
                    payableStart.datesUntil(
                            monthEnd.plusDays(1)).count();

            // Fetch attendance records.
            List<AttendanceRecord> attendanceRecords =
                    attendanceRepository
                            .findByEmployeeIdAndAttendanceDateBetween(
                                    employeeId,
                                    monthStart,
                                    monthEnd);

            Set<LocalDate> attendanceDates = new HashSet<>();

            for (AttendanceRecord attendance : attendanceRecords) {

                attendanceDates.add(
                        attendance.getAttendanceDate());
            }

            // Fetch approved leave requests.
            List<LeaveRequest> approvedLeaves =
                    leaveRequestRepository
                            .findByEmployeeIdAndStatusIgnoreCaseAndStartDateLessThanEqualAndEndDateGreaterThanEqual(
                                    employeeId,
                                    "APPROVED",
                                    monthEnd,
                                    monthStart);

            // Fetch active company holidays.
            List<CompanyHoliday> holidays =
                    companyHolidayRepository
                            .findByIsActiveTrueAndHolidayDateBetweenOrderByHolidayDateAsc(
                                    monthStart,
                                    monthEnd);

            Set<LocalDate> holidayDates = new HashSet<>();

            for (CompanyHoliday holiday : holidays) {

                if (Boolean.TRUE.equals(holiday.getIsActive())) {
                    holidayDates.add(holiday.getHolidayDate());
                }
            }

            // Calculate Loss of Pay days.
            int lopDays = 0;

            for (LocalDate date = payableStart;
                    !date.isAfter(monthEnd);
                    date = date.plusDays(1)) {

                DayOfWeek dayOfWeek = date.getDayOfWeek();

                // Saturday and Sunday are paid.
                if (dayOfWeek == DayOfWeek.SATURDAY
                        || dayOfWeek == DayOfWeek.SUNDAY) {
                    continue;
                }

                // Company holidays are paid.
                if (holidayDates.contains(date)) {
                    continue;
                }

                // Find approved leave covering this date.
                LeaveRequest coveringLeave = null;

                for (LeaveRequest leave : approvedLeaves) {

                    boolean withinLeavePeriod =
                            !date.isBefore(leave.getStartDate())
                                    && !date.isAfter(leave.getEndDate());

                    if (withinLeavePeriod) {
                        coveringLeave = leave;
                        break;
                    }
                }

                if (coveringLeave != null) {

                    // Approved UNPAID leave causes LOP.
                    if ("UNPAID".equalsIgnoreCase(
                            coveringLeave.getLeaveType())) {

                        lopDays++;
                    }

                    // Approved SICK, CASUAL and EARNED leave are paid.
                    continue;
                }

                // Attendance record means employee was present.
                if (attendanceDates.contains(date)) {
                    continue;
                }

                // Missing working-day attendance causes LOP.
                lopDays++;
            }

            // Salary for employed calendar days.
            BigDecimal employedSalary =
                    perDaySalary.multiply(
                            BigDecimal.valueOf(employedDays));

            // LOP deduction.
            BigDecimal lopDeduction =
                    perDaySalary.multiply(
                            BigDecimal.valueOf(lopDays));

            // Final gross salary after LOP.
            BigDecimal grossSalary =
                    employedSalary.subtract(lopDeduction)
                            .setScale(2, RoundingMode.HALF_UP);

            // Employee deductions.
            BigDecimal deductions =
                    applicableSalary.getEmployeePf()
                            .add(applicableSalary.getProfessionalTax())
                            .add(applicableSalary.getOtherDeductions())
                            .setScale(2, RoundingMode.HALF_UP);

            // Employer PF is not deducted from employee net salary.
            BigDecimal netSalary =
                    grossSalary.subtract(deductions)
                            .setScale(2, RoundingMode.HALF_UP);

            // Create payroll record as GENERATED draft.
            PayrollRecord payroll = new PayrollRecord();

            payroll.setEmployeeId(employeeId);
            payroll.setPayrollMonth(monthStart);
            payroll.setGrossSalary(grossSalary);
            payroll.setDeductions(deductions);
            payroll.setNetSalary(netSalary);

            payroll.setPaymentStatus(
                    PayrollRecord.PaymentStatus.GENERATED);

            PayrollRecord savedPayroll =
                    payrollRecordRepository.save(payroll);

            generatedPayrolls.add(
                    convertToResponse(savedPayroll, employee));
        }

        // Display payroll generation report in backend console.
        System.out.println(
                "========== PAYROLL GENERATION REPORT ==========");

        System.out.println("Payroll Month: " + monthStart);

        System.out.println(
                "Generated Employees: " + generatedPayrolls.size());

        System.out.println(
                "Skipped Employees: " + skippedEmployees.size());

        for (String reason : skippedEmployees) {
            System.out.println("SKIPPED: " + reason);
        }

        System.out.println(
                "================================================");

        // Return generated payrolls and skipped employee report.
        return new PayrollGenerationResponse(
                generatedPayrolls.size(),
                skippedEmployees.size(),
                generatedPayrolls,
                skippedEmployees);
    }

    // Find the latest salary structure overlapping the payroll month.
    private SalaryStructure findApplicableSalaryStructure(
            List<SalaryStructure> structures,
            LocalDate monthStart,
            LocalDate monthEnd) {

        SalaryStructure applicableSalary = null;

        for (SalaryStructure structure : structures) {

            if (structure.getEffectiveFrom() == null) {
                continue;
            }

            boolean started =
                    !structure.getEffectiveFrom().isAfter(monthEnd);

            boolean notEnded =
                    structure.getEffectiveTo() == null
                            || !structure.getEffectiveTo().isBefore(monthStart);

            if (started && notEnded) {

                if (applicableSalary == null
                        || structure.getEffectiveFrom().isAfter(
                                applicableSalary.getEffectiveFrom())) {

                    applicableSalary = structure;
                }
            }
        }

        return applicableSalary;
    }

    // Calculate total monthly earnings.
    private BigDecimal calculateGrossSalary(
            SalaryStructure salary) {

        return salary.getBasicSalary()
                .add(salary.getHra())
                .add(salary.getConveyanceAllowance())
                .add(salary.getMedicalAllowance())
                .add(salary.getSpecialAllowance())
                .add(salary.getOtherAllowances())
                .setScale(2, RoundingMode.HALF_UP);
    }

    // Check whether salary structure is effective today.
    private boolean isStructureEffectiveToday(
            SalaryStructure salary,
            LocalDate today) {

        if (salary.getEffectiveFrom() == null) {
            return false;
        }

        boolean started =
                !salary.getEffectiveFrom().isAfter(today);

        boolean notEnded =
                salary.getEffectiveTo() == null
                        || !salary.getEffectiveTo().isBefore(today);

        return started && notEnded;
    }

    // 2. Publish one employee's payroll.
    @Transactional
    public PayrollResponse publishPayroll(Long payrollId) {

        PayrollRecord payroll =
                payrollRecordRepository.findById(payrollId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Payroll record not found"));

        if (payroll.getPaymentStatus()
                != PayrollRecord.PaymentStatus.GENERATED) {

            throw new IllegalArgumentException(
                    "Only GENERATED payroll can be published.");
        }

        payroll.setPaymentStatus(
                PayrollRecord.PaymentStatus.PUBLISHED);

        PayrollRecord savedPayroll =
                payrollRecordRepository.save(payroll);

        Employee employee =
                employeeRepository.findByEmployeeId(
                        savedPayroll.getEmployeeId());

        return convertToResponse(savedPayroll, employee);
    }

    // 3. Publish all GENERATED payroll records for a month.
    @Transactional
    public List<PayrollResponse> publishAllPayrolls(
            LocalDate payrollMonth) {

        if (payrollMonth == null) {
            throw new IllegalArgumentException(
                    "Payroll month is required");
        }

        LocalDate monthStart =
                payrollMonth.withDayOfMonth(1);

        List<PayrollRecord> records =
                payrollRecordRepository
                        .findByPayrollMonthAndPaymentStatus(
                                monthStart,
                                PayrollRecord.PaymentStatus.GENERATED);

        List<PayrollResponse> responses =
                new ArrayList<>();

        for (PayrollRecord payroll : records) {

            payroll.setPaymentStatus(
                    PayrollRecord.PaymentStatus.PUBLISHED);

            PayrollRecord savedPayroll =
                    payrollRecordRepository.save(payroll);

            Employee employee =
                    employeeRepository.findByEmployeeId(
                            savedPayroll.getEmployeeId());

            responses.add(
                    convertToResponse(savedPayroll, employee));
        }

        return responses;
    }

    // 4. Get published payslips for the authenticated employee.
    public List<PayrollResponse> getMyPublishedPayslips(
            String authenticatedEmployeeId) {

        if (authenticatedEmployeeId == null
                || authenticatedEmployeeId.isBlank()) {

            throw new IllegalArgumentException(
                    "Authenticated employee is required");
        }

        List<PayrollRecord> records =
                payrollRecordRepository
                        .findByEmployeeIdAndPaymentStatusInOrderByPayrollMonthDesc(
                                authenticatedEmployeeId,
                                Arrays.asList(
                                        PayrollRecord.PaymentStatus.PUBLISHED,
                                        PayrollRecord.PaymentStatus.PAID));

        List<PayrollResponse> responses =
                new ArrayList<>();

        Employee employee =
                employeeRepository.findByEmployeeId(
                        authenticatedEmployeeId);

        for (PayrollRecord record : records) {

            responses.add(
                    convertToResponse(record, employee));
        }

        return responses;
    }

    // 5. Get one published payslip belonging to the authenticated employee.
    @Transactional(readOnly = true)
    public PayrollResponse getMyPublishedPayslip(
            Long payrollId,
            String authenticatedEmployeeId) {

        PayrollRecord payroll =
                payrollRecordRepository.findById(payrollId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Payslip not found"));

        // Prevent access to another employee's payslip.
        if (!payroll.getEmployeeId().equals(
                authenticatedEmployeeId)) {

            throw new IllegalArgumentException(
                    "Payslip not found");
        }

        // Only published or paid payslips are visible.
        if (payroll.getPaymentStatus()
                != PayrollRecord.PaymentStatus.PUBLISHED
                && payroll.getPaymentStatus()
                != PayrollRecord.PaymentStatus.PAID) {

            throw new IllegalArgumentException(
                    "Payslip not found");
        }

        Employee employee =
                employeeRepository.findByEmployeeId(
                        authenticatedEmployeeId);

        return convertToResponse(payroll, employee);
    }

    // 6. Mark payroll as PAID.
    // Payroll must be published before payment.
    @Transactional
    public PayrollResponse markPayrollAsPaid(Long payrollId) {

        PayrollRecord payroll =
                payrollRecordRepository.findById(payrollId)
                        .orElseThrow(() ->
                                new IllegalArgumentException(
                                        "Payroll record not found"));

        if (payroll.getPaymentStatus()
                == PayrollRecord.PaymentStatus.PAID) {

            throw new IllegalArgumentException(
                    "This payroll has already been marked as PAID");
        }

        if (payroll.getPaymentStatus()
                != PayrollRecord.PaymentStatus.PUBLISHED) {

            throw new IllegalArgumentException(
                    "Only PUBLISHED payroll can be marked as PAID");
        }

        payroll.setPaymentStatus(
                PayrollRecord.PaymentStatus.PAID);

        payroll.setPaidAt(LocalDateTime.now());

        PayrollRecord savedPayroll =
                payrollRecordRepository.save(payroll);

        Employee employee =
                employeeRepository.findByEmployeeId(
                        savedPayroll.getEmployeeId());

        return convertToResponse(savedPayroll, employee);
    }

    // 7. Get all payroll records for HR/Admin.
    public List<PayrollResponse> getAllPayrollRecords() {

        List<PayrollRecord> records =
                payrollRecordRepository
                        .findAllByOrderByPayrollMonthDesc();

        List<PayrollResponse> responses =
                new ArrayList<>();

        for (PayrollRecord record : records) {

            Employee employee =
                    employeeRepository.findByEmployeeId(
                            record.getEmployeeId());

            responses.add(
                    convertToResponse(record, employee));
        }

        return responses;
    }

    // 8. Get payroll by employee ID for HR/Admin.
    public List<PayrollResponse> getEmployeePayroll(
            String employeeId) {

        List<PayrollRecord> records =
                payrollRecordRepository
                        .findByEmployeeIdOrderByPayrollMonthDesc(
                                employeeId);

        List<PayrollResponse> responses =
                new ArrayList<>();

        Employee employee =
                employeeRepository.findByEmployeeId(employeeId);

        for (PayrollRecord record : records) {

            responses.add(
                    convertToResponse(record, employee));
        }

        return responses;
    }

    // 9. Get payroll records by month for HR/Admin.
    public List<PayrollResponse> getPayrollByMonth(
            LocalDate payrollMonth) {

        if (payrollMonth == null) {
            throw new IllegalArgumentException(
                    "Payroll month is required");
        }

        LocalDate monthStart =
                payrollMonth.withDayOfMonth(1);

        List<PayrollRecord> records =
                payrollRecordRepository.findByPayrollMonth(
                        monthStart);

        List<PayrollResponse> responses =
                new ArrayList<>();

        for (PayrollRecord record : records) {

            Employee employee =
                    employeeRepository.findByEmployeeId(
                            record.getEmployeeId());

            responses.add(
                    convertToResponse(record, employee));
        }

        return responses;
    }

    // 10. Convert entity into response DTO.
    private PayrollResponse convertToResponse(
            PayrollRecord record,
            Employee employee) {

        PayrollResponse response = new PayrollResponse();

        response.setId(record.getId());
        response.setEmployeeId(record.getEmployeeId());

        if (employee != null) {
            response.setEmployeeName(employee.getFullName());
            response.setDesignation(employee.getDesignation());
            response.setJoiningDate(employee.getJoiningDate());
            response.setDepartment(employee.getDepartment());
        }

        response.setPayrollMonth(record.getPayrollMonth());
        response.setGrossSalary(record.getGrossSalary());
        response.setDeductions(record.getDeductions());
        response.setNetSalary(record.getNetSalary());

        response.setPaymentStatus(
                record.getPaymentStatus().name());

        response.setGeneratedAt(record.getGeneratedAt());
        response.setPaidAt(record.getPaidAt());

        return response;
    }
}
