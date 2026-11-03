
package com.opsoft.hrms_backend.repository;

import com.opsoft.hrms_backend.entity.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface SalaryStructureRepository
        extends JpaRepository<SalaryStructure, Long> {

    // Get all salary structures of an employee
    List<SalaryStructure> findByEmployeeId(String employeeId);

    // Get the latest salary structure of an employee
    Optional<SalaryStructure>
    findTopByEmployeeIdOrderByEffectiveFromDesc(String employeeId);

    // Get salary structures effective on a particular date
    List<SalaryStructure>
    findByEmployeeIdAndEffectiveFromLessThanEqualAndEffectiveToIsNull(
            String employeeId, LocalDate date);
}
