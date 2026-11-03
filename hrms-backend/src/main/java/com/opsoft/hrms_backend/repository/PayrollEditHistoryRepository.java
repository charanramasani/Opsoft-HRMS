
package com.opsoft.hrms_backend.repository;

import com.opsoft.hrms_backend.entity.PayrollEditHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PayrollEditHistoryRepository
        extends JpaRepository<PayrollEditHistory, Long> {

    List<PayrollEditHistory> findByPayrollIdOrderByChangedAtDesc(
            Long payrollId);
}
