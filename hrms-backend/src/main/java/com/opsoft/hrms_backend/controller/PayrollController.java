package com.opsoft.hrms_backend.controller;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.opsoft.hrms_backend.dto.PayrollComponentResponse;
import com.opsoft.hrms_backend.dto.PayrollEditHistoryResponse;
import com.opsoft.hrms_backend.dto.PayrollEditRequest;
import com.opsoft.hrms_backend.dto.PayrollGenerateRequest;
import com.opsoft.hrms_backend.dto.PayrollGenerationResponse;
import com.opsoft.hrms_backend.dto.PayrollResponse;
import com.opsoft.hrms_backend.entity.PayrollRecord;
import com.opsoft.hrms_backend.service.PayrollEditingService;
import com.opsoft.hrms_backend.service.PayrollService;
import com.opsoft.hrms_backend.service.PayslipPdfService;

@RestController
@RequestMapping("/api/payroll")
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class PayrollController {

    private final PayrollService payrollService;
    private final PayrollEditingService payrollEditingService;
    private final PayslipPdfService payslipPdfService;

    public PayrollController(
            PayrollService payrollService,
            PayrollEditingService payrollEditingService,
            PayslipPdfService payslipPdfService) {

        this.payrollService = payrollService;
        this.payrollEditingService = payrollEditingService;
        this.payslipPdfService = payslipPdfService;
    }

    // =====================================================
    // ADMIN / HR - GENERATE PAYROLL
    // =====================================================

    @PostMapping("/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<PayrollGenerationResponse> generatePayroll(
            @RequestBody PayrollGenerateRequest request) {

        return ResponseEntity.ok(
                payrollService.generatePayroll(request)
        );
    }

    // =====================================================
    // ADMIN / HR - GET ALL PAYROLL RECORDS
    // =====================================================

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<PayrollResponse>> getAllPayrolls() {

        return ResponseEntity.ok(
                payrollService.getAllPayrollRecords()
        );
    }

    // =====================================================
    // ADMIN / HR - GET PAYROLL BY EMPLOYEE
    // =====================================================

    @GetMapping("/employee/{employeeId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<PayrollResponse>> getPayrollByEmployee(
            @PathVariable String employeeId) {

        return ResponseEntity.ok(
                payrollService.getEmployeePayroll(employeeId)
        );
    }

    // =====================================================
    // ADMIN / HR - GET PAYROLL BY MONTH
    // =====================================================

    @GetMapping("/month/{payrollMonth}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<PayrollResponse>> getPayrollByMonth(
            @PathVariable LocalDate payrollMonth) {

        return ResponseEntity.ok(
                payrollService.getPayrollByMonth(payrollMonth)
        );
    }

    // =====================================================
    // ADMIN / HR - PUBLISH ONE PAYSLIP
    // =====================================================

    @PatchMapping("/{payrollId}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<PayrollResponse> publishPayroll(
            @PathVariable Long payrollId) {

        return ResponseEntity.ok(
                payrollService.publishPayroll(payrollId)
        );
    }

    // =====================================================
    // ADMIN / HR - PUBLISH ALL PAYSLIPS
    // =====================================================

    @PatchMapping("/publish-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<PayrollResponse>> publishAllPayrolls(
            @RequestParam LocalDate payrollMonth) {

        return ResponseEntity.ok(
                payrollService.publishAllPayrolls(payrollMonth)
        );
    }

    // =====================================================
    // ADMIN / HR - MARK PAYROLL AS PAID
    // =====================================================

    @PatchMapping("/{payrollId}/pay")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<PayrollResponse> markPayrollAsPaid(
            @PathVariable Long payrollId) {

        return ResponseEntity.ok(
                payrollService.markPayrollAsPaid(payrollId)
        );
    }

    // =====================================================
    // EMPLOYEE - GET MY PAYSLIPS
    // =====================================================

    @GetMapping("/my-payslips")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<PayrollResponse>> getMyPayslips(
            Authentication authentication) {

        return ResponseEntity.ok(
                payrollService.getMyPublishedPayslips(
                        authentication.getName()
                )
        );
    }

    // =====================================================
    // EMPLOYEE - VIEW MY PAYSLIP
    // =====================================================

    @GetMapping("/my-payslips/{payrollId}")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<PayrollResponse> getMyPayslip(
            @PathVariable Long payrollId,
            Authentication authentication) {

        return ResponseEntity.ok(
                payrollService.getMyPublishedPayslip(
                        payrollId,
                        authentication.getName()
                )
        );
    }

    // =====================================================
    // EMPLOYEE - GET MY PAYSLIP COMPONENTS
    // =====================================================

    @GetMapping("/my-payslips/{payrollId}/components")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<PayrollComponentResponse>> getMyPayslipComponents(
            @PathVariable Long payrollId,
            Authentication authentication) {

        // Verify employee ownership and payslip status first.
        payrollService.getMyPublishedPayslip(
                payrollId,
                authentication.getName()
        );

        return ResponseEntity.ok(
                payrollEditingService.getPayrollComponents(payrollId)
        );
    }

    // =====================================================
    // EMPLOYEE - DOWNLOAD MY PAYSLIP PDF
    // =====================================================

    @GetMapping("/my-payslips/{payrollId}/download")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<byte[]> downloadMyPayslip(
            @PathVariable Long payrollId,
            Authentication authentication) throws IOException {

        String employeeId = authentication.getName();

        // Verify ownership and PUBLISHED / PAID status.
        PayrollResponse payroll =
                payrollService.getMyPublishedPayslip(
                        payrollId,
                        employeeId
                );

        // Retrieve saved payroll components after verification.
        List<PayrollComponentResponse> components =
                payrollEditingService.getPayrollComponents(payrollId);

        byte[] pdf = payslipPdfService.generatePayslip(
                payroll,
                components
        );

        String filename = "Payslip-"
                + payroll.getEmployeeId()
                + "-"
                + payroll.getPayrollMonth()
                + ".pdf";

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + filename + "\""
                )
                .body(pdf);
    }

// =====================================================
// ADMIN / HR - EDIT PAYROLL COMPONENTS
// =====================================================

    @PutMapping("/{payrollId}/components")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<PayrollRecord> updatePayrollComponents(
        @PathVariable Long payrollId,
        @RequestBody PayrollEditRequest request) {

         return ResponseEntity.ok(
                 payrollEditingService.editPayroll(
                    payrollId,
                    request
            )
    );
    }

    // =====================================================
    // ADMIN / HR - GET PAYROLL COMPONENTS
    // =====================================================

    @GetMapping("/{payrollId}/components")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<PayrollComponentResponse>> getPayrollComponents(
            @PathVariable Long payrollId) {

        return ResponseEntity.ok(
                payrollEditingService.getPayrollComponents(payrollId)
        );
    }

    // =====================================================
    // ADMIN / HR - GET PAYROLL EDIT HISTORY
    // =====================================================

    @GetMapping("/{payrollId}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<PayrollEditHistoryResponse>> getPayrollEditHistory(
            @PathVariable Long payrollId) {

        return ResponseEntity.ok(
                payrollEditingService.getPayrollEditHistory(payrollId)
        );
    }
}