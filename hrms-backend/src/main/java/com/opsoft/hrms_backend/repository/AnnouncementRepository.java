
package com.opsoft.hrms_backend.repository;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.opsoft.hrms_backend.entity.Announcement;

@Repository
public interface AnnouncementRepository
        extends JpaRepository<Announcement, Long> {

    // HR/Admin: View all announcements
    List<Announcement> findAllByOrderByCreatedAtDesc();

    // HR/Admin: Filter announcements by status
    List<Announcement> findByStatusOrderByCreatedAtDesc(
            String status);

    // Employee: View published announcements for all employees
    // or specifically selected for the logged-in employee
    @Query("""
        SELECT DISTINCT a
        FROM Announcement a
        LEFT JOIN a.selectedEmployees e
        WHERE a.status = 'PUBLISHED'
          AND (a.expiryDate IS NULL OR a.expiryDate >= :today)
          AND (
                a.audience = 'ALL_EMPLOYEES'
                OR e.employeeId = :employeeId
              )
        ORDER BY a.publishedAt DESC
        """)
    List<Announcement> findVisibleToEmployee(
            @Param("employeeId") String employeeId,
            @Param("today") LocalDate today
    );
}
