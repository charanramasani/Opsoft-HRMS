import React, { useEffect, useMemo, useState } from "react";
import "./DashboardAnalytics.css";

const API_URL = "http://localhost:8083";

const apiFetch = (url) => {
  const token = localStorage.getItem("hrmsToken");
  return fetch(url, {
    method: "GET",
    credentials: "omit",
    headers: token ? { Authorization: `Bearer ${token}` } : {}
  });
};

const asArray = (value) => Array.isArray(value) ? value : [];

const dateKey = (value) => {
  if (!value) return "";
  return String(value).slice(0, 10);
};

const monthKey = (date) =>
  `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, "0")}`;

const monthLabel = (key) => {
  const [year, month] = key.split("-").map(Number);
  return new Date(year, month - 1, 1).toLocaleString("en-IN", {
    month: "short"
  });
};

function DashboardAnalytics({ user }) {
  const isAdminOrHR = ["ADMIN", "HR"].includes(user?.role);
  const [employees, setEmployees] = useState([]);
  const [attendance, setAttendance] = useState([]);
  const [leaves, setLeaves] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!isAdminOrHR) {
      setLoading(false);
      return;
    }

    let active = true;

    const loadAnalytics = async () => {
      setLoading(true);
      setError("");

      try {
        const [employeeResponse, attendanceResponse, leaveResponse] =
          await Promise.all([
            apiFetch(`${API_URL}/api/employees`),
            apiFetch(`${API_URL}/api/attendance/all`),
            apiFetch(`${API_URL}/api/leaves/all`)
          ]);

        if (!employeeResponse.ok || !attendanceResponse.ok || !leaveResponse.ok) {
          throw new Error("Unable to load analytics data. Check API access and try again.");
        }

        const [employeeData, attendanceData, leaveData] = await Promise.all([
          employeeResponse.json(),
          attendanceResponse.json(),
          leaveResponse.json()
        ]);

        if (active) {
          setEmployees(asArray(employeeData));
          setAttendance(asArray(attendanceData));
          setLeaves(asArray(leaveData));
        }
      } catch (err) {
        if (active) setError(err.message || "Unable to load dashboard analytics.");
      } finally {
        if (active) setLoading(false);
      }
    };

    loadAnalytics();
    return () => { active = false; };
  }, [isAdminOrHR]);

  const today = new Date();
  const todayKey = [
    today.getFullYear(),
    String(today.getMonth() + 1).padStart(2, "0"),
    String(today.getDate()).padStart(2, "0")
  ].join("-");

  const attendanceToday = useMemo(
    () => attendance.filter((row) => dateKey(row.attendanceDate) === todayKey),
    [attendance, todayKey]
  );

  const attendanceCounts = useMemo(() => {
    const present = new Set(
      attendanceToday.filter((row) => row.checkIn).map((row) => row.employeeId)
    ).size;
    const completed = new Set(
      attendanceToday.filter((row) => row.checkIn && row.checkOut).map((row) => row.employeeId)
    ).size;
    const working = new Set(
      attendanceToday.filter((row) => row.checkIn && !row.checkOut).map((row) => row.employeeId)
    ).size;
    const absent = Math.max(0, employees.length - present);
    return { present, absent, working, completed };
  }, [attendanceToday, employees.length]);

  const attendanceMonths = useMemo(() => {
    const months = [];
    for (let offset = 5; offset >= 0; offset--) {
      const date = new Date(today.getFullYear(), today.getMonth() - offset, 1);
      const key = monthKey(date);
      const rows = attendance.filter((row) => dateKey(row.attendanceDate).startsWith(key));
      const present = new Set(rows.filter((row) => row.checkIn).map((row) => row.employeeId)).size;
      const completed = new Set(rows.filter((row) => row.checkIn && row.checkOut).map((row) => row.employeeId)).size;
      months.push({ key, label: monthLabel(key), present, completed });
    }
    return months;
  }, [attendance]);

  const leaveCounts = useMemo(() => {
    const counts = { PENDING: 0, APPROVED: 0, REJECTED: 0 };
    leaves.forEach((leave) => {
      const status = String(leave.status || "").toUpperCase();
      if (Object.prototype.hasOwnProperty.call(counts, status)) counts[status] += 1;
    });
    return counts;
  }, [leaves]);

  const departmentCounts = useMemo(() => {
    const counts = {};
    employees.forEach((employee) => {
      const department = employee.department?.trim() || "Unassigned";
      counts[department] = (counts[department] || 0) + 1;
    });
    return Object.entries(counts)
      .map(([name, count]) => ({ name, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 6);
  }, [employees]);

  const totalAttendance = attendanceCounts.present + attendanceCounts.absent;
  const presentPercent = totalAttendance
    ? Math.round((attendanceCounts.present / totalAttendance) * 100)
    : 0;

  if (!isAdminOrHR) return null;

  return (
    <section className="dashboard-analytics">
      <div className="dashboard-analytics-heading">
        <div>
          <span className="dashboard-analytics-eyebrow">WORKFORCE INSIGHTS</span>
          <h2>HR Analytics</h2>
          <p>Attendance, leave and department overview</p>
        </div>
        <span className="dashboard-analytics-date">
          {today.toLocaleDateString("en-IN", { day: "2-digit", month: "short", year: "numeric" })}
        </span>
      </div>

      {loading ? (
        <div className="analytics-message">Loading analytics...</div>
      ) : error ? (
        <div className="analytics-message analytics-error">{error}</div>
      ) : (
        <div className="dashboard-analytics-grid">
          <article className="analytics-card attendance-overview-card">
            <div className="analytics-card-heading">
              <div>
                <h3>Today's Attendance</h3>
                <p>Employees who checked in today</p>
              </div>
              <span className="analytics-card-icon">◷</span>
            </div>

            <div className="attendance-chart-content">
              <div
                className="attendance-donut"
                style={{
                  background: `conic-gradient(#16a36a 0% ${presentPercent}%, #e9edf3 ${presentPercent}% 100%)`
                }}
              >
                <div className="attendance-donut-center">
                  <strong>{presentPercent}%</strong>
                  <span>Present</span>
                </div>
              </div>
              <div className="attendance-legend">
                <div><i className="legend-dot present-dot" /><span>Present</span><strong>{attendanceCounts.present}</strong></div>
                <div><i className="legend-dot absent-dot" /><span>Not checked in</span><strong>{attendanceCounts.absent}</strong></div>
                <div><i className="legend-dot working-dot" /><span>Currently working</span><strong>{attendanceCounts.working}</strong></div>
                <div><i className="legend-dot completed-dot" /><span>Checked out</span><strong>{attendanceCounts.completed}</strong></div>
              </div>
            </div>
          </article>

          <article className="analytics-card attendance-trend-card">
            <div className="analytics-card-heading">
              <div>
                <h3>Monthly Attendance Trend</h3>
                <p>Unique employees with attendance records</p>
              </div>
              <span className="analytics-card-icon">▥</span>
            </div>
            <div className="analytics-bar-chart">
              {attendanceMonths.map((month) => {
                const max = Math.max(1, ...attendanceMonths.map((item) => item.present));
                return (
                  <div className="analytics-bar-column" key={month.key}>
                    <div className="analytics-bar-track">
                      <div
                        className="analytics-bar-fill"
                        style={{ height: `${(month.present / max) * 100}%` }}
                        title={`${month.present} employees`}
                      />
                    </div>
                    <strong>{month.present}</strong>
                    <span>{month.label}</span>
                  </div>
                );
              })}
            </div>
            <div className="analytics-chart-note">Monthly distinct employee count; not total attendance days.</div>
          </article>

          <article className="analytics-card leave-overview-card">
            <div className="analytics-card-heading">
              <div>
                <h3>Leave Overview</h3>
                <p>Leave requests by current status</p>
              </div>
              <span className="analytics-card-icon">▤</span>
            </div>
            <div className="leave-status-list">
              {[
                { label: "Approved", value: leaveCounts.APPROVED, color: "approved" },
                { label: "Pending", value: leaveCounts.PENDING, color: "pending" },
                { label: "Rejected", value: leaveCounts.REJECTED, color: "rejected" }
              ].map((item) => {
                const total = leaves.length || 1;
                return (
                  <div className="leave-status-row" key={item.color}>
                    <div className="leave-status-label">
                      <span>{item.label}</span><strong>{item.value}</strong>
                    </div>
                    <div className="leave-status-track">
                      <div className={`leave-status-fill ${item.color}`} style={{ width: `${(item.value / total) * 100}%` }} />
                    </div>
                  </div>
                );
              })}
            </div>
            <div className="analytics-chart-note">{leaves.length} total leave request(s)</div>
          </article>

          <article className="analytics-card department-overview-card">
            <div className="analytics-card-heading">
              <div>
                <h3>Department-wise Employees</h3>
                <p>Employee distribution by department</p>
              </div>
              <span className="analytics-card-icon">▦</span>
            </div>
            {departmentCounts.length === 0 ? (
              <div className="analytics-empty">No department data available.</div>
            ) : (
              <div className="department-chart">
                {departmentCounts.map((department) => {
                  const max = Math.max(1, ...departmentCounts.map((item) => item.count));
                  return (
                    <div className="department-row" key={department.name}>
                      <span title={department.name}>{department.name}</span>
                      <div className="department-track">
                        <div className="department-fill" style={{ width: `${(department.count / max) * 100}%` }} />
                      </div>
                      <strong>{department.count}</strong>
                    </div>
                  );
                })}
              </div>
            )}
          </article>
        </div>
      )}
    </section>
  );
}

export default DashboardAnalytics;
