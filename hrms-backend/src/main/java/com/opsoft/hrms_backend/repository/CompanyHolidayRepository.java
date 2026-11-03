
package com.opsoft.hrms_backend.repository;

import com.opsoft.hrms_backend.entity.CompanyHoliday;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyHolidayRepository
        extends JpaRepository<CompanyHoliday, Long> {

    // Find a holiday by its date
    Optional<CompanyHoliday> findByHolidayDate(LocalDate holidayDate);

    // Get all active holidays
    List<CompanyHoliday> findByIsActiveTrueOrderByHolidayDateAsc();

    // Get active holidays within a date range
    List<CompanyHoliday> findByIsActiveTrueAndHolidayDateBetweenOrderByHolidayDateAsc(
            LocalDate startDate,
            LocalDate endDate
    );

    // Check whether a holiday already exists on a date
    boolean existsByHolidayDate(LocalDate holidayDate);
}
