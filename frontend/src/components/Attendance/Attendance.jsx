
import React, { useMemo, useState } from "react";
import "./Attendance.css";

function Attendance({
  records = [],
  onCheckIn,
  onCheckOut,
  loading = false,
  isAdminOrHR = false,
  currentUser
}) {
  const [searchTerm, setSearchTerm] = useState("");
  const [selectedDate, setSelectedDate] = useState("");
  const [selectedDepartment, setSelectedDepartment] = useState("ALL");

  const todayKey = new Date().toLocaleDateString("en-CA");

  const formatDate = (dateValue) => {
    if (!dateValue) return "-";

    const date = new Date(dateValue + "T00:00:00");

    return date.toLocaleDateString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric"
    });
  };

  const parseAttendanceDateTime = (value) => {
    if (!value) return null;
    return new Date(value);
  };

  const formatTime = (value) => {
    if (!value) return "-";

    const date = parseAttendanceDateTime(value);

    if (!date || Number.isNaN(date.getTime())) return "-";

    return date.toLocaleTimeString("en-IN", {
      hour: "2-digit",
      minute: "2-digit",
      second: "2-digit",
      hour12: true
    });
  };

  const calculateHours = (checkIn, checkOut) => {
    if (!checkIn) return "-";

    const start = parseAttendanceDateTime(checkIn);
    const end = checkOut
      ? parseAttendanceDateTime(checkOut)
      : new Date();

    if (!start || !end) return "-";

    let difference = Math.floor((end - start) / 1000);

    if (difference < 0) return "-";

    const hours = Math.floor(difference / 3600);
    const minutes = Math.floor((difference % 3600) / 60);
    const seconds = difference % 60;

    return `${String(hours).padStart(2, "0")}h ${String(
      minutes
    ).padStart(2, "0")}m ${String(seconds).padStart(2, "0")}s`;
  };

  const getAttendanceStatus = (record) => {
    if (record.attendanceStatus) {
      return record.attendanceStatus;
    }

    return record.checkOut ? "COMPLETED" : "CHECKED_IN";
  };

  // Display GPS address and Google Maps location
  const renderLocation = (address, latitude, longitude) => {
    const hasCoordinates =
      latitude !== null &&
      latitude !== undefined &&
      longitude !== null &&
      longitude !== undefined;

    if (!address && !hasCoordinates) {
      return (
        <span style={{ color: "#999", fontSize: "13px" }}>
          Location not available
        </span>
      );
    }

    const mapUrl = hasCoordinates
      ? `https://www.google.com/maps?q=${latitude},${longitude}`
      : null;

    return (
      <div style={{ minWidth: "150px", maxWidth: "230px" }}>
        <div
          style={{
            fontSize: "13px",
            color: "#334155",
            lineHeight: "1.5",
            overflowWrap: "anywhere"
          }}
        >
          {address || "Address not available"}
        </div>

        {hasCoordinates && (
          <a
            href={mapUrl}
            target="_blank"
            rel="noopener noreferrer"
            style={{
              display: "inline-block",
              marginTop: "5px",
              color: "#2563eb",
              fontSize: "12px",
              fontWeight: "600",
              textDecoration: "none"
            }}
          >
            📍 View Map
          </a>
        )}
      </div>
    );
  };

  // Today's attendance belonging to the logged-in user.
  // Admin/HR receives all employees' records, so match by the logged-in
  // user's employeeId instead of taking the first employee's record.
  const todayRecord = records.find(
    (record) =>
      record.attendanceDate === todayKey &&
      String(record.employeeId || "").toLowerCase() ===
        String(currentUser?.employeeId || "").toLowerCase()
  );

  const hasCheckedIn = Boolean(todayRecord?.checkIn);
  const hasCheckedOut = Boolean(todayRecord?.checkOut);

  // Unique departments for Admin/HR filter
  const departments = useMemo(() => {
    return [
      ...new Set(
        records
          .map((record) => record.department)
          .filter(Boolean)
      )
    ].sort();
  }, [records]);

  // Admin/HR filtering
  const filteredRecords = useMemo(() => {
    return records.filter((record) => {
      const search = searchTerm.trim().toLowerCase();

      const matchesSearch =
        !search ||
        record.employeeId?.toLowerCase().includes(search) ||
        record.employeeName?.toLowerCase().includes(search);

      const matchesDate =
        !selectedDate || record.attendanceDate === selectedDate;

      const matchesDepartment =
        selectedDepartment === "ALL" ||
        record.department === selectedDepartment;

      return matchesSearch && matchesDate && matchesDepartment;
    });
  }, [records, searchTerm, selectedDate, selectedDepartment]);

  // Admin/HR dashboard summary
  const totalRecords = filteredRecords.length;

  const currentlyWorking = filteredRecords.filter(
    (record) => !record.checkOut
  ).length;

  const completedRecords = filteredRecords.filter(
    (record) => Boolean(record.checkOut)
  ).length;

  const uniqueEmployees = new Set(
    filteredRecords.map((record) => record.employeeId)
  ).size;

  // Employee summary
  const employeeTotalDays = records.length;

  const employeeCompletedDays = records.filter(
    (record) => Boolean(record.checkOut)
  ).length;

  const employeeWorkingDays = records.filter(
    (record) => !record.checkOut
  ).length;

  // ADMIN / HR VIEW
  if (isAdminOrHR) {
    return (
      <div className="attendance-page">
        <div className="attendance-heading">
          <div>
            <span className="attendance-eyebrow">
              HUMAN RESOURCE MANAGEMENT
            </span>

            <h1>Attendance Management</h1>

            <p>
              Monitor and manage employee attendance records.
            </p>
          </div>

          <div className="attendance-date">
            <span>SELECTED DATE</span>
            <strong>
              {selectedDate ? formatDate(selectedDate) : "All Dates"}
            </strong>
          </div>
        </div>

        {/* HR/Admin personal Check-In / Check-Out */}
        <div className="attendance-action-card">
          <div className="attendance-action-info">
            <div className="attendance-clock-icon">🕒</div>

            <div>
              <h2>
                {hasCheckedOut
                  ? "Today's Attendance Completed"
                  : hasCheckedIn
                  ? "You are currently working"
                  : "Ready to start your day?"}
              </h2>

              <p>
                {hasCheckedIn
                  ? `Check-in time: ${formatTime(todayRecord.checkIn)}`
                  : "Check in to record your own attendance."}
              </p>
            </div>
          </div>

          <div className="attendance-actions">
            <button
              type="button"
              className="attendance-checkin"
              onClick={onCheckIn}
              disabled={loading || hasCheckedIn}
            >
              {loading ? "Please wait..." : "Check In"}
            </button>

            <button
              type="button"
              className="attendance-checkout"
              onClick={onCheckOut}
              disabled={loading || !hasCheckedIn || hasCheckedOut}
            >
              {loading ? "Please wait..." : "Check Out"}
            </button>
          </div>
        </div>

        {/* Admin Summary Cards */}
        <div className="attendance-summary admin-attendance-summary">
          <div className="attendance-summary-card">
            <div className="summary-icon">📋</div>
            <div>
              <p>Total Records</p>
              <h3>{totalRecords}</h3>
            </div>
          </div>

          <div className="attendance-summary-card">
            <div className="summary-icon">👥</div>
            <div>
              <p>Employees</p>
              <h3>{uniqueEmployees}</h3>
            </div>
          </div>

          <div className="attendance-summary-card">
            <div className="summary-icon">🟢</div>
            <div>
              <p>Currently Working</p>
              <h3>{currentlyWorking}</h3>
            </div>
          </div>

          <div className="attendance-summary-card">
            <div className="summary-icon">✅</div>
            <div>
              <p>Completed</p>
              <h3>{completedRecords}</h3>
            </div>
          </div>
        </div>

        {/* Search and Filters */}
        <div className="attendance-filter-card">
          <div className="attendance-filter-heading">
            <div>
              <h2>Employee Attendance</h2>
              <p>Search and filter attendance records.</p>
            </div>
          </div>

          <div className="attendance-filters">
            <div className="attendance-search">
              <label>Search Employee</label>

              <input
                type="text"
                placeholder="Search by employee ID or name"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
              />
            </div>

            <div className="attendance-filter">
              <label>Filter by Date</label>

              <input
                type="date"
                value={selectedDate}
                onChange={(e) => setSelectedDate(e.target.value)}
              />
            </div>

            <div className="attendance-filter">
              <label>Department</label>

              <select
                value={selectedDepartment}
                onChange={(e) =>
                  setSelectedDepartment(e.target.value)
                }
              >
                <option value="ALL">All Departments</option>

                {departments.map((department) => (
                  <option key={department} value={department}>
                    {department}
                  </option>
                ))}
              </select>
            </div>

            <button
              type="button"
              className="attendance-reset-button"
              onClick={() => {
                setSearchTerm("");
                setSelectedDate("");
                setSelectedDepartment("ALL");
              }}
            >
              Reset Filters
            </button>
          </div>
        </div>

        {/* All Employee Attendance Table */}
        <div className="attendance-history-card">
          <div className="attendance-history-heading">
            <h2>Attendance History</h2>

            <p>
              Showing {filteredRecords.length} attendance record(s).
            </p>
          </div>

          <div className="attendance-table-wrapper">
            <table className="attendance-table admin-attendance-table">
              <thead>
                <tr>
                  <th>Employee ID</th>
                  <th>Employee Name</th>
                  <th>Department</th>
                  <th>Designation</th>
                  <th>Date</th>
                  <th>Check In</th>
                  <th>Check In Location</th>
                  <th>Check Out</th>
                  <th>Check Out Location</th>
                  <th>Working Hours</th>
                  <th>Status</th>
                </tr>
              </thead>

              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan="11" className="attendance-empty">
                      Loading attendance records...
                    </td>
                  </tr>
                ) : filteredRecords.length === 0 ? (
                  <tr>
                    <td colSpan="11" className="attendance-empty">
                      No attendance records found.
                    </td>
                  </tr>
                ) : (
                  filteredRecords.map((record, index) => {
                    const status = getAttendanceStatus(record);

                    return (
                      <tr
                        key={`${record.employeeId}-${record.attendanceDate}-${index}`}
                      >
                        <td>
                          <strong>{record.employeeId}</strong>
                        </td>

                        <td>{record.employeeName || "-"}</td>

                        <td>{record.department || "-"}</td>

                        <td>{record.designation || "-"}</td>

                        <td>{formatDate(record.attendanceDate)}</td>

                        <td>{formatTime(record.checkIn)}</td>

                        <td>
                          {renderLocation(
                            record.checkInAddress,
                            record.checkInLatitude,
                            record.checkInLongitude
                          )}
                        </td>

                        <td>{formatTime(record.checkOut)}</td>

                        <td>
                          {renderLocation(
                            record.checkOutAddress,
                            record.checkOutLatitude,
                            record.checkOutLongitude
                          )}
                        </td>

                        <td>
                          {calculateHours(
                            record.checkIn,
                            record.checkOut
                          )}
                        </td>

                        <td>
                          <span
                            className={`attendance-status ${
                              status === "COMPLETED"
                                ? "attendance-completed"
                                : "attendance-working"
                            }`}
                          >
                            {status === "COMPLETED"
                              ? "Completed"
                              : "Checked In"}
                          </span>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>
      </div>
    );
  }

  // EMPLOYEE VIEW
  return (
    <div className="attendance-page">
      <div className="attendance-heading">
        <div>
          <span className="attendance-eyebrow">
            EMPLOYEE SELF SERVICE
          </span>

          <h1>My Attendance</h1>

          <p>
            Track your daily attendance and working hours.
          </p>
        </div>

        <div className="attendance-date">
          <span>TODAY'S DATE</span>
          <strong>{formatDate(todayKey)}</strong>
        </div>
      </div>

      {/* Check-in / Check-out */}
      <div className="attendance-action-card">
        <div className="attendance-action-info">
          <div className="attendance-clock-icon">🕒</div>

          <div>
            <h2>
              {hasCheckedOut
                ? "Today's Attendance Completed"
                : hasCheckedIn
                ? "You are currently working"
                : "Ready to start your day?"}
            </h2>

            <p>
              {hasCheckedIn
                ? `Check-in time: ${formatTime(todayRecord.checkIn)}`
                : "Please check in when you start your work."}
            </p>
          </div>
        </div>

        <div className="attendance-actions">
          <button
            type="button"
            className="attendance-checkin"
            onClick={onCheckIn}
            disabled={loading || hasCheckedIn}
          >
            {loading ? "Please wait..." : "Check In"}
          </button>

          <button
            type="button"
            className="attendance-checkout"
            onClick={onCheckOut}
            disabled={loading || !hasCheckedIn || hasCheckedOut}
          >
            {loading ? "Please wait..." : "Check Out"}
          </button>
        </div>
      </div>

      {/* Employee Summary */}
      <div className="attendance-summary">
        <div className="attendance-summary-card">
          <div className="summary-icon">📅</div>

          <div>
            <p>Total Attendance Records</p>
            <h3>{employeeTotalDays}</h3>
          </div>
        </div>

        <div className="attendance-summary-card">
          <div className="summary-icon">✅</div>

          <div>
            <p>Completed Days</p>
            <h3>{employeeCompletedDays}</h3>
          </div>
        </div>

        <div className="attendance-summary-card">
          <div className="summary-icon">🟢</div>

          <div>
            <p>Currently Working</p>
            <h3>{employeeWorkingDays}</h3>
          </div>
        </div>

        <div className="attendance-summary-card">
          <div className="summary-icon">⏱️</div>

          <div>
            <p>Today's Working Hours</p>

            <h3>
              {todayRecord
                ? calculateHours(
                    todayRecord.checkIn,
                    todayRecord.checkOut
                  )
                : "00h 00m 00s"}
            </h3>
          </div>
        </div>
      </div>

      {/* Employee Attendance History */}
      <div className="attendance-history-card">
        <div className="attendance-history-heading">
          <h2>My Attendance History</h2>

          <p>
            View your previous check-in and check-out records.
          </p>
        </div>

        <div className="attendance-table-wrapper">
          <table className="attendance-table">
            <thead>
              <tr>
                <th>Date</th>
                <th>Check In</th>
                <th>Check In Location</th>
                <th>Check Out</th>
                <th>Check Out Location</th>
                <th>Working Hours</th>
                <th>Status</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="7" className="attendance-empty">
                    Loading attendance records...
                  </td>
                </tr>
              ) : records.length === 0 ? (
                <tr>
                  <td colSpan="7" className="attendance-empty">
                    No attendance records found.
                  </td>
                </tr>
              ) : (
                records.map((record, index) => {
                  const status = getAttendanceStatus(record);

                  return (
                    <tr
                      key={`${record.attendanceDate}-${record.employeeId}-${index}`}
                    >
                      <td>{formatDate(record.attendanceDate)}</td>

                      <td>{formatTime(record.checkIn)}</td>

                      <td>
                        {renderLocation(
                          record.checkInAddress,
                          record.checkInLatitude,
                          record.checkInLongitude
                        )}
                      </td>

                      <td>{formatTime(record.checkOut)}</td>

                      <td>
                        {renderLocation(
                          record.checkOutAddress,
                          record.checkOutLatitude,
                          record.checkOutLongitude
                        )}
                      </td>

                      <td>
                        {calculateHours(
                          record.checkIn,
                          record.checkOut
                        )}
                      </td>

                      <td>
                        <span
                          className={`attendance-status ${
                            status === "COMPLETED"
                              ? "attendance-completed"
                              : "attendance-working"
                          }`}
                        >
                          {status === "COMPLETED"
                            ? "Completed"
                            : "Checked In"}
                        </span>
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

export default Attendance;
