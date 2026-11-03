package com.opsoft.hrms_backend.repository;

import com.opsoft.hrms_backend.entity.PayrollComponent;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollComponentRepository
        extends JpaRepository<PayrollComponent, Long> {

    List<PayrollComponent> findByPayrollId(Long payrollId);

    @Modifying(flushAutomatically = true)
    @Query("DELETE FROM PayrollComponent p WHERE p.payrollId = :payrollId")
    void deleteByPayrollId(@Param("payrollId") Long payrollId);
}