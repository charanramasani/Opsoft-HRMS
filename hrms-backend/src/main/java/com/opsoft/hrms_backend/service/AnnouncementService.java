
package com.opsoft.hrms_backend.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.opsoft.hrms_backend.dto.AnnouncementRequest;
import com.opsoft.hrms_backend.dto.AnnouncementResponse;
import com.opsoft.hrms_backend.entity.Announcement;
import com.opsoft.hrms_backend.entity.Employee;
import com.opsoft.hrms_backend.repository.AnnouncementRepository;
import com.opsoft.hrms_backend.repository.EmployeeRepository;

@Service
@Transactional
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final EmployeeRepository employeeRepository;

    public AnnouncementService(
            AnnouncementRepository announcementRepository,
            EmployeeRepository employeeRepository) {

        this.announcementRepository = announcementRepository;
        this.employeeRepository = employeeRepository;
    }

    // CREATE ANNOUNCEMENT
    public AnnouncementResponse createAnnouncement(
            AnnouncementRequest request,
            String createdBy) {

        validateRequest(request);

        Announcement announcement = new Announcement();

        announcement.setTitle(request.getTitle().trim());
        announcement.setContent(request.getContent().trim());
        announcement.setCategory(request.getCategory());
        announcement.setPriority(request.getPriority());
        announcement.setAudience(request.getAudience());
        announcement.setExpiryDate(request.getExpiryDate());

        announcement.setCreatedByEmployeeId(createdBy);
        announcement.setStatus("DRAFT");

        setSelectedEmployees(announcement, request);

        return convertToResponse(
                announcementRepository.save(announcement));
    }

    // GET ALL ANNOUNCEMENTS - HR/ADMIN
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getAllAnnouncements() {

        return announcementRepository
                .findAllByOrderByCreatedAtDesc()
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // GET ANNOUNCEMENTS VISIBLE TO EMPLOYEE
    @Transactional(readOnly = true)
    public List<AnnouncementResponse> getEmployeeAnnouncements(
            String employeeId) {

        return announcementRepository
                .findVisibleToEmployee(employeeId, LocalDate.now())
                .stream()
                .map(this::convertToResponse)
                .collect(Collectors.toList());
    }

    // UPDATE ANNOUNCEMENT
    public AnnouncementResponse updateAnnouncement(
            Long id,
            AnnouncementRequest request) {

        validateRequest(request);

        Announcement announcement = getAnnouncement(id);

        announcement.setTitle(request.getTitle().trim());
        announcement.setContent(request.getContent().trim());
        announcement.setCategory(request.getCategory());
        announcement.setPriority(request.getPriority());
        announcement.setAudience(request.getAudience());
        announcement.setExpiryDate(request.getExpiryDate());

        setSelectedEmployees(announcement, request);

        return convertToResponse(
                announcementRepository.save(announcement));
    }

    // PUBLISH ANNOUNCEMENT
    public AnnouncementResponse publishAnnouncement(Long id) {

        Announcement announcement = getAnnouncement(id);

        if (announcement.getExpiryDate() != null
                && announcement.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Cannot publish an expired announcement.");
        }

        if ("SELECTED_EMPLOYEES".equals(announcement.getAudience())
                && announcement.getSelectedEmployees().isEmpty()) {
            throw new IllegalArgumentException(
                    "Select at least one employee before publishing.");
        }

        announcement.setStatus("PUBLISHED");
        announcement.setPublishedAt(LocalDateTime.now());

        return convertToResponse(
                announcementRepository.save(announcement));
    }

    // UNPUBLISH ANNOUNCEMENT
    public AnnouncementResponse unpublishAnnouncement(Long id) {

        Announcement announcement = getAnnouncement(id);

        announcement.setStatus("UNPUBLISHED");

        return convertToResponse(
                announcementRepository.save(announcement));
    }

    // DELETE ANNOUNCEMENT
    public void deleteAnnouncement(Long id) {

        Announcement announcement = getAnnouncement(id);

        announcementRepository.delete(announcement);
    }

    // FIND ANNOUNCEMENT
    private Announcement getAnnouncement(Long id) {

        return announcementRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException(
                        "Announcement not found with ID: " + id));
    }

    // VALIDATE REQUEST
    private void validateRequest(AnnouncementRequest request) {

        if (request == null) {
            throw new IllegalArgumentException(
                    "Announcement details are required.");
        }

        if (request.getTitle() == null
                || request.getTitle().isBlank()) {
            throw new IllegalArgumentException(
                    "Announcement title is required.");
        }

        if (request.getTitle().length() > 200) {
            throw new IllegalArgumentException(
                    "Title cannot exceed 200 characters.");
        }

        if (request.getContent() == null
                || request.getContent().isBlank()) {
            throw new IllegalArgumentException(
                    "Announcement content is required.");
        }

        String audience = request.getAudience();

        if (!"ALL_EMPLOYEES".equals(audience)
                && !"SELECTED_EMPLOYEES".equals(audience)) {
            throw new IllegalArgumentException(
                    "Invalid announcement audience.");
        }

        String priority = request.getPriority();

        if (priority == null
                || !Set.of("NORMAL", "IMPORTANT", "URGENT")
                        .contains(priority)) {
            throw new IllegalArgumentException(
                    "Invalid announcement priority.");
        }

        if (request.getCategory() == null
                || request.getCategory().isBlank()
                || request.getCategory().length() > 50) {
            throw new IllegalArgumentException(
                    "Valid announcement category is required.");
        }

        if (request.getExpiryDate() != null
                && request.getExpiryDate().isBefore(LocalDate.now())) {
            throw new IllegalArgumentException(
                    "Expiry date cannot be in the past.");
        }

        if ("SELECTED_EMPLOYEES".equals(audience)
                && (request.getSelectedEmployeeIds() == null
                || request.getSelectedEmployeeIds().isEmpty())) {
            throw new IllegalArgumentException(
                    "Select at least one employee.");
        }
    }

    // SET SELECTED EMPLOYEES
    private void setSelectedEmployees(
            Announcement announcement,
            AnnouncementRequest request) {

        if ("ALL_EMPLOYEES".equals(request.getAudience())) {
            announcement.setSelectedEmployees(new HashSet<>());
            return;
        }

        Set<Employee> employees = new HashSet<>();

        for (String employeeId : request.getSelectedEmployeeIds()) {

            if (employeeId == null || employeeId.isBlank()) {
                throw new IllegalArgumentException(
                        "Invalid employee ID.");
            }

            Employee employee =
                    employeeRepository.findByEmployeeId(employeeId);

            if (employee == null) {
                throw new IllegalArgumentException(
                        "Employee not found: " + employeeId);
            }

            String status = employee.getStatus();

            if (!"ACTIVE".equalsIgnoreCase(status)
                    && !"ON_NOTICE".equalsIgnoreCase(status)) {
                throw new IllegalArgumentException(
                        "Employee is not active: " + employeeId);
            }

            employees.add(employee);
        }

        announcement.setSelectedEmployees(employees);
    }

    // CONVERT ENTITY TO RESPONSE DTO
    private AnnouncementResponse convertToResponse(
            Announcement announcement) {

        AnnouncementResponse response =
                new AnnouncementResponse();

        response.setId(announcement.getId());
        response.setTitle(announcement.getTitle());
        response.setContent(announcement.getContent());
        response.setCategory(announcement.getCategory());
        response.setPriority(announcement.getPriority());
        response.setAudience(announcement.getAudience());
        response.setStatus(announcement.getStatus());
        response.setCreatedByEmployeeId(
                announcement.getCreatedByEmployeeId());
        response.setCreatedAt(announcement.getCreatedAt());
        response.setPublishedAt(announcement.getPublishedAt());
        response.setExpiryDate(announcement.getExpiryDate());

        Set<String> employeeIds =
                announcement.getSelectedEmployees()
                        .stream()
                        .map(employee -> employee.getEmployeeId())
                        .collect(Collectors.toSet());

        response.setSelectedEmployeeIds(employeeIds);

        return response;
    }
}
