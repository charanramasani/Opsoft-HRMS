
package com.opsoft.hrms_backend.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import com.opsoft.hrms_backend.dto.AnnouncementRequest;
import com.opsoft.hrms_backend.dto.AnnouncementResponse;
import com.opsoft.hrms_backend.service.AnnouncementService;

@RestController
@RequestMapping("/api/announcements")
@CrossOrigin(origins = "http://localhost:5173")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(
            AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    // HR/ADMIN: CREATE ANNOUNCEMENT
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<AnnouncementResponse> createAnnouncement(
            @RequestBody AnnouncementRequest request,
            Authentication authentication) {

        String createdBy = authentication.getName();

        AnnouncementResponse response =
                announcementService.createAnnouncement(
                        request, createdBy);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // HR/ADMIN: VIEW ALL ANNOUNCEMENTS
    @GetMapping("/admin")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<List<AnnouncementResponse>> getAllAnnouncements() {

        return ResponseEntity.ok(
                announcementService.getAllAnnouncements());
    }

    // EMPLOYEE: VIEW VISIBLE ANNOUNCEMENTS
    @GetMapping("/employee")
    @PreAuthorize("hasRole('EMPLOYEE')")
    public ResponseEntity<List<AnnouncementResponse>> getEmployeeAnnouncements(
            Authentication authentication) {

        String employeeId = authentication.getName();

        return ResponseEntity.ok(
                announcementService.getEmployeeAnnouncements(employeeId));
    }

    // HR/ADMIN: UPDATE ANNOUNCEMENT
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<AnnouncementResponse> updateAnnouncement(
            @PathVariable Long id,
            @RequestBody AnnouncementRequest request) {

        return ResponseEntity.ok(
                announcementService.updateAnnouncement(id, request));
    }

    // HR/ADMIN: PUBLISH ANNOUNCEMENT
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<AnnouncementResponse> publishAnnouncement(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                announcementService.publishAnnouncement(id));
    }

    // HR/ADMIN: UNPUBLISH ANNOUNCEMENT
    @PatchMapping("/{id}/unpublish")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<AnnouncementResponse> unpublishAnnouncement(
            @PathVariable Long id) {

        return ResponseEntity.ok(
                announcementService.unpublishAnnouncement(id));
    }

    // HR/ADMIN: DELETE ANNOUNCEMENT
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<Void> deleteAnnouncement(
            @PathVariable Long id) {

        announcementService.deleteAnnouncement(id);

        return ResponseEntity.noContent().build();
    }
}
