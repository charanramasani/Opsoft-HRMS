
import React, { useEffect, useMemo, useState } from "react";
import "./LeaveManagement.css";

const API_URL = "http://localhost:8083/api/leaves";

// Attach JWT token to every Leave Management API request
const apiFetch = (url, options = {}) => {
  const token = localStorage.getItem("hrmsToken");

  return fetch(url, {
    ...options,
    credentials: "omit",
    headers: {
      ...options.headers,
      ...(token ? { Authorization: `Bearer ${token}` } : {})
    }
  });
};

function LeaveManagement({ user }) {
  const isAdmin = user?.role === "ADMIN";
  const isHR = user?.role === "HR";
  const isAdminOrHR = isAdmin || isHR;
  const canApplyLeave = !isAdmin;

  const [form, setForm] = useState({
    leaveType: "CASUAL",
    startDate: "",
    endDate: "",
    reason: ""
  });

  const [leaveRecords, setLeaveRecords] = useState([]);
  const [loading, setLoading] = useState(false);
  const [historyLoading, setHistoryLoading] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [commentInputs, setCommentInputs] = useState({});
  const [reviewingId, setReviewingId] = useState(null);

  // Fetch requests according to role
  const fetchLeaveRequests = async () => {
    setHistoryLoading(true);
    setError("");

    try {
      const endpoint = isAdminOrHR ? "/all" : "/me";

      const response = await apiFetch(`${API_URL}${endpoint}`, {
        method: "GET"
      });

      const data = await response.json().catch(() => []);

      if (!response.ok) {
        throw new Error(
          data.message || "Unable to load leave requests."
        );
      }

      setLeaveRecords(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message);
    } finally {
      setHistoryLoading(false);
    }
  };

  useEffect(() => {
    fetchLeaveRequests();
  }, [isAdminOrHR]);

  // Form changes
  const handleChange = (e) => {
    setForm({
      ...form,
      [e.target.name]: e.target.value
    });
  };

  // Calculate leave duration
  const calculateDays = (startDate, endDate) => {
    if (!startDate || !endDate) return 0;

    const start = new Date(startDate + "T00:00:00");
    const end = new Date(endDate + "T00:00:00");

    return Math.round((end - start) / 86400000) + 1;
  };

  // Submit leave application (Employee and HR only)
  const handleSubmit = async (e) => {
    e.preventDefault();

    if (!canApplyLeave) return;

    setMessage("");
    setError("");

    if (!form.startDate || !form.endDate || !form.reason.trim()) {
      setError("Please complete all required fields.");
      return;
    }

    const today = new Date();
    const todayString = [
      today.getFullYear(),
      String(today.getMonth() + 1).padStart(2, "0"),
      String(today.getDate()).padStart(2, "0")
    ].join("-");

    if (form.startDate < todayString) {
      setError("Leave start date cannot be in the past.");
      return;
    }

    if (form.endDate < form.startDate) {
      setError("End date cannot be before start date.");
      return;
    }

    setLoading(true);

    try {
      const response = await apiFetch(`${API_URL}/apply`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify(form)
      });

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        throw new Error(
          data.message || "Leave application failed."
        );
      }

      setMessage("Leave application submitted successfully!");

      setForm({
        leaveType: "CASUAL",
        startDate: "",
        endDate: "",
        reason: ""
      });

      await fetchLeaveRequests();

    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // HR/Admin comment input
  const handleCommentChange = (leaveId, value) => {
    setCommentInputs((previous) => ({
      ...previous,
      [leaveId]: value
    }));
  };

  // HR/Admin review
  const handleReview = async (leaveId, status) => {
    setError("");
    setMessage("");

    const action = status === "APPROVED" ? "approve" : "reject";

    const confirmed = window.confirm(
      `Are you sure you want to ${action} this leave request?`
    );

    if (!confirmed) return;

    setReviewingId(leaveId);

    try {
      const response = await apiFetch(
        `${API_URL}/${leaveId}/review`,
        {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            status,
            reviewerComment: commentInputs[leaveId] || ""
          })
        }
      );

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        throw new Error(
          data.message || `Unable to ${action} leave request.`
        );
      }

      setMessage(
        `Leave request ${status.toLowerCase()} successfully.`
      );

      setCommentInputs((previous) => ({
        ...previous,
        [leaveId]: ""
      }));

      await fetchLeaveRequests();

    } catch (err) {
      setError(err.message);
    } finally {
      setReviewingId(null);
    }
  };

  // Search and filter
  const filteredRecords = useMemo(() => {
    return leaveRecords.filter((leave) => {
      const searchText = search.toLowerCase();

      const matchesSearch =
        (leave.employeeId || "").toLowerCase().includes(searchText) ||
        (leave.employeeName || "").toLowerCase().includes(searchText) ||
        (leave.department || "").toLowerCase().includes(searchText);

      const matchesStatus =
        statusFilter === "ALL" ||
        leave.status === statusFilter;

      return matchesSearch && matchesStatus;
    });
  }, [leaveRecords, search, statusFilter]);

  // Summary counts
  const pendingCount = leaveRecords.filter(
    (leave) => leave.status === "PENDING"
  ).length;

  const approvedCount = leaveRecords.filter(
    (leave) => leave.status === "APPROVED"
  ).length;

  const rejectedCount = leaveRecords.filter(
    (leave) => leave.status === "REJECTED"
  ).length;

  const getStatusClass = (status) => {
    if (status === "APPROVED") return "leave-approved";
    if (status === "REJECTED") return "leave-rejected";
    return "leave-pending";
  };

  // Reusable leave application form
  const renderApplicationForm = () => (
    <div className="leave-form-card">

      <div className="leave-card-heading">
        <h2>Apply for Leave</h2>
        <p>Enter your leave details below.</p>
      </div>

      <form onSubmit={handleSubmit}>

        <div className="leave-form-group">
          <label>Leave Type</label>

          <select
            name="leaveType"
            value={form.leaveType}
            onChange={handleChange}
            required
          >
            <option value="CASUAL">Casual Leave</option>
            <option value="SICK">Sick Leave</option>
            <option value="EARNED">Earned Leave</option>
            <option value="UNPAID">Unpaid Leave</option>
          </select>
        </div>

        <div className="leave-date-grid">

          <div className="leave-form-group">
            <label>Start Date</label>

            <input
              type="date"
              name="startDate"
              min={new Date().toLocaleDateString("en-CA")}
              value={form.startDate}
              onChange={handleChange}
              required
            />
          </div>

          <div className="leave-form-group">
            <label>End Date</label>

            <input
              type="date"
              name="endDate"
              min={
                form.startDate ||
                new Date().toLocaleDateString("en-CA")
              }
              value={form.endDate}
              onChange={handleChange}
              required
            />
          </div>

        </div>

        <div className="leave-days-preview">
          <span>Requested Leave Duration</span>

          <strong>
            {form.startDate && form.endDate
              ? `${calculateDays(form.startDate, form.endDate)} day(s)`
              : "Select dates"}
          </strong>
        </div>

        <div className="leave-form-group">
          <label>Reason for Leave</label>

          <textarea
            name="reason"
            rows="4"
            placeholder="Enter your reason for leave..."
            value={form.reason}
            onChange={handleChange}
            required
          />
        </div>

        <button
          type="submit"
          className="leave-submit-button"
          disabled={loading}
        >
          {loading ? "Submitting..." : "Submit Leave Request"}
        </button>

      </form>
    </div>
  );

  // Admin and HR dashboard
  const renderAdminDashboard = () => (
    <>
      <div className="leave-admin-summary">

        <div className="leave-admin-summary-card">
          <div className="leave-admin-icon">📋</div>
          <div>
            <p>Total Requests</p>
            <h2>{leaveRecords.length}</h2>
          </div>
        </div>

        <div className="leave-admin-summary-card">
          <div className="leave-admin-icon pending-icon">⏳</div>
          <div>
            <p>Pending Requests</p>
            <h2>{pendingCount}</h2>
          </div>
        </div>

        <div className="leave-admin-summary-card">
          <div className="leave-admin-icon approved-icon">✓</div>
          <div>
            <p>Approved</p>
            <h2>{approvedCount}</h2>
          </div>
        </div>

        <div className="leave-admin-summary-card">
          <div className="leave-admin-icon rejected-icon">✕</div>
          <div>
            <p>Rejected</p>
            <h2>{rejectedCount}</h2>
          </div>
        </div>

      </div>

      <div className="leave-history-card">

        <div className="leave-card-heading leave-history-title">
          <div>
            <h2>Employee Leave Requests</h2>
            <p>Review employee applications and take action.</p>
          </div>

          <button
            type="button"
            className="leave-refresh-button"
            onClick={fetchLeaveRequests}
            disabled={historyLoading}
          >
            {historyLoading ? "Loading..." : "↻ Refresh"}
          </button>
        </div>

        <div className="leave-admin-filters">

          <input
            type="text"
            placeholder="Search by employee ID, name or department..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
          />

          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
          >
            <option value="ALL">All Requests</option>
            <option value="PENDING">Pending</option>
            <option value="APPROVED">Approved</option>
            <option value="REJECTED">Rejected</option>
          </select>

        </div>

        <div className="leave-table-wrapper">
          <table className="leave-table leave-admin-table">

            <thead>
              <tr>
                <th>Employee</th>
                <th>Leave Type</th>
                <th>Start Date</th>
                <th>End Date</th>
                <th>Days</th>
                <th>Reason</th>
                <th>Status</th>
                <th>HR Comment</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>

              {historyLoading ? (
                <tr>
                  <td colSpan="9" className="leave-empty">
                    Loading employee leave requests...
                  </td>
                </tr>
              ) : filteredRecords.length === 0 ? (
                <tr>
                  <td colSpan="9" className="leave-empty">
                    No leave requests found.
                  </td>
                </tr>
              ) : (
                filteredRecords.map((leave) => (
                  <tr key={leave.id}>

                    <td>
                      <strong>{leave.employeeName || "Employee"}</strong>
                      <small className="leave-employee-id">
                        {leave.employeeId}
                      </small>
                      <small className="leave-employee-id">
                        {leave.department || "-"}
                      </small>
                    </td>

                    <td>{leave.leaveType}</td>
                    <td>{leave.startDate}</td>
                    <td>{leave.endDate}</td>

                    <td>
                      {calculateDays(
                        leave.startDate,
                        leave.endDate
                      )}
                    </td>

                    <td className="leave-reason-cell">
                      {leave.reason}
                    </td>

                    <td>
                      <span
                        className={`leave-status ${getStatusClass(leave.status)}`}
                      >
                        {leave.status}
                      </span>
                    </td>

                    <td>{leave.reviewerComment || "-"}</td>

                    <td className="leave-action-cell">

                      {leave.status === "PENDING" ? (
                        <div className="leave-review-actions">

                          <textarea
                            placeholder="HR comment (optional)"
                            rows="2"
                            value={commentInputs[leave.id] || ""}
                            onChange={(e) =>
                              handleCommentChange(
                                leave.id,
                                e.target.value
                              )
                            }
                            disabled={reviewingId === leave.id}
                          />

                          <div className="leave-action-buttons">

                            <button
                              type="button"
                              className="leave-approve-button"
                              onClick={() =>
                                handleReview(leave.id, "APPROVED")
                              }
                              disabled={reviewingId === leave.id}
                            >
                              {reviewingId === leave.id
                                ? "Processing..."
                                : "✓ Approve"}
                            </button>

                            <button
                              type="button"
                              className="leave-reject-button"
                              onClick={() =>
                                handleReview(leave.id, "REJECTED")
                              }
                              disabled={reviewingId === leave.id}
                            >
                              ✕ Reject
                            </button>

                          </div>
                        </div>
                      ) : (
                        <span className="leave-reviewed-label">
                          Reviewed by {leave.reviewedBy || "HR"}
                        </span>
                      )}

                    </td>
                  </tr>
                ))
              )}

            </tbody>
          </table>
        </div>

        <div className="leave-table-footer">
          Showing {filteredRecords.length} of {leaveRecords.length} requests
        </div>

      </div>
    </>
  );

  // Employee and HR dashboard
  return (
    <div className="leave-page">

      <div className="leave-heading">
        <div>
          <span className="leave-eyebrow">
            {isAdminOrHR
              ? "HUMAN RESOURCE MANAGEMENT"
              : "EMPLOYEE SELF SERVICE"}
          </span>

          <h1>Leave Management</h1>

          <p>
            {isAdmin
              ? "Review and manage employee leave applications."
              : isHR
                ? "Apply for leave and manage employee leave requests."
                : "Apply for leave and track your leave requests."}
          </p>
        </div>
      </div>

      {message && (
        <div className="leave-success-message">
          {message}
        </div>
      )}

      {error && (
        <div className="leave-error-message">
          {error}
        </div>
      )}

      {/* HR and Employee can apply. Admin cannot. */}
      {canApplyLeave && renderApplicationForm()}

      {/* Admin and HR can view all employee requests. */}
      {isAdminOrHR && renderAdminDashboard()}

      {/* Employee can view only their own history. */}
      {!isAdminOrHR && (
        <div className="leave-history-card">

          <div className="leave-card-heading leave-history-title">
            <div>
              <h2>My Leave History</h2>
              <p>Track your submitted applications.</p>
            </div>

            <button
              type="button"
              onClick={fetchLeaveRequests}
              disabled={historyLoading}
              className="leave-refresh-button"
            >
              {historyLoading ? "Loading..." : "Refresh"}
            </button>
          </div>

          <div className="leave-table-wrapper">
            <table className="leave-table">

              <thead>
                <tr>
                  <th>Leave Type</th>
                  <th>Start Date</th>
                  <th>End Date</th>
                  <th>Days</th>
                  <th>Status</th>
                  <th>HR Comment</th>
                </tr>
              </thead>

              <tbody>

                {historyLoading ? (
                  <tr>
                    <td colSpan="6" className="leave-empty">
                      Loading leave history...
                    </td>
                  </tr>
                ) : leaveRecords.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="leave-empty">
                      No leave applications found.
                    </td>
                  </tr>
                ) : (
                  leaveRecords.map((leave) => (
                    <tr key={leave.id}>
                      <td>{leave.leaveType}</td>
                      <td>{leave.startDate}</td>
                      <td>{leave.endDate}</td>

                      <td>
                        {calculateDays(
                          leave.startDate,
                          leave.endDate
                        )}
                      </td>

                      <td>
                        <span
                          className={`leave-status ${getStatusClass(leave.status)}`}
                        >
                          {leave.status}
                        </span>
                      </td>

                      <td>{leave.reviewerComment || "-"}</td>
                    </tr>
                  ))
                )}

              </tbody>
            </table>
          </div>
        </div>
      )}

    </div>
  );
}

export default LeaveManagement;
