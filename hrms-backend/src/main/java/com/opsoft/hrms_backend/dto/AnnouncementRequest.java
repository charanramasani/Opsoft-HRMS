
package com.opsoft.hrms_backend.dto;

import java.time.LocalDate;
import java.util.Set;

public class AnnouncementRequest {

    private String title;

    private String content;

    private String category;

    private String priority;

    // ALL_EMPLOYEES or SELECTED_EMPLOYEES
    private String audience;

    // Employee company IDs, for example 122615
    private Set<String> selectedEmployeeIds;

    private LocalDate expiryDate;

    public AnnouncementRequest() {
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

    public Set<String> getSelectedEmployeeIds() {
        return selectedEmployeeIds;
    }

    public void setSelectedEmployeeIds(Set<String> selectedEmployeeIds) {
        this.selectedEmployeeIds = selectedEmployeeIds;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }
}
