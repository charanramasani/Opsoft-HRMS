
package com.opsoft.hrms_backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.*;

@Entity
@Table(name = "attendance_records")
public class AttendanceRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "employee_id", nullable = false, length = 30)
    private String employeeId;

    @Column(name = "attendance_date", nullable = false)
    private LocalDate attendanceDate;

    @Column(name = "check_in", nullable = false)
    private LocalDateTime checkIn;

    @Column(name = "check_out")
    private LocalDateTime checkOut;

    // Check-In Location
    @Column(name = "check_in_latitude", precision = 10, scale = 7)
    private BigDecimal checkInLatitude;

    @Column(name = "check_in_longitude", precision = 10, scale = 7)
    private BigDecimal checkInLongitude;

    @Column(name = "check_in_address", length = 500)
    private String checkInAddress;

    // Check-Out Location
    @Column(name = "check_out_latitude", precision = 10, scale = 7)
    private BigDecimal checkOutLatitude;

    @Column(name = "check_out_longitude", precision = 10, scale = 7)
    private BigDecimal checkOutLongitude;

    @Column(name = "check_out_address", length = 500)
    private String checkOutAddress;

    @Column(name = "created_at", insertable = false, updatable = false)
    private LocalDateTime createdAt;

    public AttendanceRecord() {
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getEmployeeId() {
        return employeeId;
    }

    public void setEmployeeId(String employeeId) {
        this.employeeId = employeeId;
    }

    public LocalDate getAttendanceDate() {
        return attendanceDate;
    }

    public void setAttendanceDate(LocalDate attendanceDate) {
        this.attendanceDate = attendanceDate;
    }

    public LocalDateTime getCheckIn() {
        return checkIn;
    }

    public void setCheckIn(LocalDateTime checkIn) {
        this.checkIn = checkIn;
    }

    public LocalDateTime getCheckOut() {
        return checkOut;
    }

    public void setCheckOut(LocalDateTime checkOut) {
        this.checkOut = checkOut;
    }

    public BigDecimal getCheckInLatitude() {
        return checkInLatitude;
    }

    public void setCheckInLatitude(BigDecimal checkInLatitude) {
        this.checkInLatitude = checkInLatitude;
    }

    public BigDecimal getCheckInLongitude() {
        return checkInLongitude;
    }

    public void setCheckInLongitude(BigDecimal checkInLongitude) {
        this.checkInLongitude = checkInLongitude;
    }

    public String getCheckInAddress() {
        return checkInAddress;
    }

    public void setCheckInAddress(String checkInAddress) {
        this.checkInAddress = checkInAddress;
    }

    public BigDecimal getCheckOutLatitude() {
        return checkOutLatitude;
    }

    public void setCheckOutLatitude(BigDecimal checkOutLatitude) {
        this.checkOutLatitude = checkOutLatitude;
    }

    public BigDecimal getCheckOutLongitude() {
        return checkOutLongitude;
    }

    public void setCheckOutLongitude(BigDecimal checkOutLongitude) {
        this.checkOutLongitude = checkOutLongitude;
    }

    public String getCheckOutAddress() {
        return checkOutAddress;
    }

    public void setCheckOutAddress(String checkOutAddress) {
        this.checkOutAddress = checkOutAddress;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
