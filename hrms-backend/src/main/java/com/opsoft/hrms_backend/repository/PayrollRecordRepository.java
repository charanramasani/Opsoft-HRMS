
package com.opsoft.hrms_backend.repository;

import com.opsoft.hrms_backend.entity.PayrollRecord;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface PayrollRecordRepository
        extends JpaRepository<PayrollRecord, Long> {

    // Get payroll records for an employee
    List<PayrollRecord> findByEmployeeIdOrderByPayrollMonthDesc(
            String employeeId);

    // Get all payroll records for a particular month
    List<PayrollRecord> findByPayrollMonth(
            LocalDate payrollMonth);

    // Check whether payroll already exists for an employee and month
    boolean existsByEmployeeIdAndPayrollMonth(
            String employeeId,
            LocalDate payrollMonth);

    // Get one employee's payroll for a particular month
    Optional<PayrollRecord> findByEmployeeIdAndPayrollMonth(
            String employeeId,
            LocalDate payrollMonth);

    // Get all payroll records, latest month first
    List<PayrollRecord> findAllByOrderByPayrollMonthDesc();

    // Get all draft payroll records for a particular month
    List<PayrollRecord> findByPayrollMonthAndPaymentStatus(
            LocalDate payrollMonth,
            PayrollRecord.PaymentStatus paymentStatus);

    // Get only published or paid payslips belonging to an employee
    List<PayrollRecord>
            findByEmployeeIdAndPaymentStatusInOrderByPayrollMonthDesc(
                    String employeeId,
                    List<PayrollRecord.PaymentStatus> paymentStatuses);
}
