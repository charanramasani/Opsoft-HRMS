
package com.opsoft.hrms_backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.opsoft.hrms_backend.entity.EmployeeStatusHistory;

@Repository
public interface EmployeeStatusHistoryRepository
        extends JpaRepository<EmployeeStatusHistory, Integer> {

    List<EmployeeStatusHistory>
        findByEmployeeIdOrderByChangedAtDesc(String employeeId);

}
