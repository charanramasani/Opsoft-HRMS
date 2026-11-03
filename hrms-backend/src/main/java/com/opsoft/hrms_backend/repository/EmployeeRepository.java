
package com.opsoft.hrms_backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.opsoft.hrms_backend.entity.Employee;

@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Integer> {

    Employee findByEmployeeId(String employeeId);

    Employee findByEmail(String email);

    boolean existsByEmailAndEmployeeIdNot(String email, String employeeId);
}
