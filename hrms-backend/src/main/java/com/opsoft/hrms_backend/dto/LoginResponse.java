package com.opsoft.hrms_backend.dto;

public class LoginResponse {

    private String message;
    private String employeeId;
    private String fullName;
    private String role;
    private boolean mustChangePassword;
    private String token;

    public LoginResponse() {
    }

    public LoginResponse(
            String message,
            String employeeId,
            String fullName,
            String role,
            boolean mustChangePassword,
            String token) {

        this.message = message;
        this.employeeId = employeeId;
        this.fullName = fullName;
        this.role = role;
        this.mustChangePassword = mustChangePassword;
        this.token = token;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isMustChangePassword() {
        return mustChangePassword;
    }

    public void setMustChangePassword(boolean mustChangePassword) {
        this.mustChangePassword = mustChangePassword;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }
}
