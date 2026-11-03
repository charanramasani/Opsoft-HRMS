package com.opsoft.hrms_backend.dto;

public class LeaveReviewRequest {

    private String status;
    private String reviewerComment;

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getReviewerComment() {
        return reviewerComment;
    }

    public void setReviewerComment(String reviewerComment) {
        this.reviewerComment = reviewerComment;
    }
}