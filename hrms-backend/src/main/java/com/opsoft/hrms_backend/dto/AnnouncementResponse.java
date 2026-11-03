
package com.opsoft.hrms_backend.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Set;

public class AnnouncementResponse {

    private Long id;
    private String title;
    private String content;
    private String category;
    private String priority;
    private String audience;
    private String status;
    private String createdByEmployeeId;
    private LocalDateTime createdAt;
    private LocalDateTime publishedAt;
    private LocalDate expiryDate;
    private Set<String> selectedEmployeeIds;

    public AnnouncementResponse() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getPriority() {
        return priority;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public String getAudience() {
        return audience;
    }

    public void setAudience(String audience) {
        this.audience = audience;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getCreatedByEmployeeId() {
        return createdByEmployeeId;
    }

    public void setCreatedByEmployeeId(String createdByEmployeeId) {
        this.createdByEmployeeId = createdByEmployeeId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(LocalDateTime publishedAt) {
        this.publishedAt = publishedAt;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public Set<String> getSelectedEmployeeIds() {
        return selectedEmployeeIds;
    }

    public void setSelectedEmployeeIds(Set<String> selectedEmployeeIds) {
        this.selectedEmployeeIds = selectedEmployeeIds;
    }
}
