
import React, { useEffect, useMemo, useState } from "react";
import "./Dashboard.css";

const API_URL = "http://localhost:8083";

const apiFetch = (url) => {
  const token = localStorage.getItem("hrmsToken");

  return fetch(url, {
    method: "GET",
    credentials: "omit",
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
    },
  });
};

const getTodayKey = () => {
  const today = new Date();
  const year = today.getFullYear();
  const month = String(today.getMonth() + 1).padStart(2, "0");
  const day = String(today.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
};

async function fetchArray(endpoint) {
  const response = await apiFetch(`${API_URL}${endpoint}`);
  const data = await response.json().catch(() => []);

  if (!response.ok) {
    throw new Error(data.message || `Unable to load ${endpoint}.`);
  }

  if (!Array.isArray(data)) {
    throw new Error(`Unexpected response received from ${endpoint}.`);
  }

  return data;
}

function Dashboard({ user, onOpenModule, onOpenProfile }) {
  const isAdminOrHR = user?.role === "ADMIN" || user?.role === "HR";

  const [stats, setStats] = useState({
    totalEmployees: null,
    presentToday: null,
    onLeaveToday: null,
    pendingLeaves: null,
  });

  const [analytics, setAnalytics] = useState({
    employees: [],
    attendance: [],
    leaves: [],
  });

  const [statsLoading, setStatsLoading] = useState(false);
  const [statsError, setStatsError] = useState("");

  const loadDashboardStats = async () => {
    if (!isAdminOrHR) return;

    setStatsLoading(true);
    setStatsError("");

    const [employeesResult, attendanceResult, leavesResult] =
      await Promise.allSettled([
        fetchArray("/api/employees"),
        fetchArray("/api/attendance/all"),
        fetchArray("/api/leaves/all"),
      ]);

    const today = getTodayKey();

    const nextStats = {
      totalEmployees: null,
      presentToday: null,
      onLeaveToday: null,
      pendingLeaves: null,
    };

    const failedItems = [];

    const employees =
      employeesResult.status === "fulfilled"
        ? employeesResult.value
        : [];

    const attendance =
      attendanceResult.status === "fulfilled"
        ? attendanceResult.value
        : [];

    const leaves =
      leavesResult.status === "fulfilled"
        ? leavesResult.value
        : [];

    setAnalytics({ employees, attendance, leaves });

    if (employeesResult.status === "fulfilled") {
      nextStats.totalEmployees = employees.length;
    } else {
      failedItems.push("employee");
    }

    if (attendanceResult.status === "fulfilled") {
      const presentIds = new Set(
        attendance
          .filter(
            (record) =>
              record.attendanceDate?.toString().slice(0, 10) === today &&
              record.checkIn &&
              record.employeeId
          )
          .map((record) => record.employeeId)
      );

      nextStats.presentToday = presentIds.size;
    } else {
      failedItems.push("attendance");
    }

    if (leavesResult.status === "fulfilled") {
      const onLeaveIds = new Set(
        leaves
          .filter(
            (leave) =>
              leave.status === "APPROVED" &&
              leave.startDate <= today &&
              leave.endDate >= today &&
              leave.employeeId
          )
          .map((leave) => leave.employeeId)
      );

      nextStats.onLeaveToday = onLeaveIds.size;

      nextStats.pendingLeaves = leaves.filter(
        (leave) => leave.status === "PENDING"
      ).length;
    } else {
      failedItems.push("leave");
    }

    setStats(nextStats);

    if (failedItems.length > 0) {
      setStatsError(
        `Could not load ${failedItems.join(
          ", "
        )} statistics. Check the related API access and try refreshing.`
      );
    }

    setStatsLoading(false);
  };

  useEffect(() => {
    loadDashboardStats();
  }, [user?.role]);

  // Today's attendance analytics
  const attendanceAnalytics = useMemo(() => {
    const today = getTodayKey();

    const todayRecords = analytics.attendance.filter(
      (record) =>
        record.attendanceDate?.toString().slice(0, 10) === today
    );

    const presentIds = new Set(
      todayRecords
        .filter((record) => record.checkIn && record.employeeId)
        .map((record) => record.employeeId)
    );

    const checkedOutIds = new Set(
      todayRecords
        .filter((record) => record.checkOut && record.employeeId)
        .map((record) => record.employeeId)
    );

    const total = analytics.employees.length;
    const present = presentIds.size;
    const notCheckedIn = Math.max(total - present, 0);
    const working = Math.max(present - checkedOutIds.size, 0);

    return {
      total,
      present,
      notCheckedIn,
      working,
      checkedOut: checkedOutIds.size,
      percentage: total > 0 ? (present / total) * 100 : 0,
    };
  }, [analytics]);

  // Last six months attendance trend
  const monthlyAttendance = useMemo(() => {
    const months = [];
    const now = new Date();

    for (let i = 5; i >= 0; i--) {
      const date = new Date(now.getFullYear(), now.getMonth() - i, 1);

      const year = date.getFullYear();
      const month = String(date.getMonth() + 1).padStart(2, "0");
      const key = `${year}-${month}`;

      const employeeIds = new Set(
        analytics.attendance
          .filter((record) => {
            const attendanceMonth = record.attendanceDate
              ?.toString()
              .slice(0, 7);

            return (
              attendanceMonth === key &&
              record.checkIn &&
              record.employeeId
            );
          })
          .map((record) => record.employeeId)
      );

      months.push({
        key,
        label: date.toLocaleString("en-US", { month: "short" }),
        count: employeeIds.size,
      });
    }

    return months;
  }, [analytics.attendance]);

  // Leave request status overview
  const leaveAnalytics = useMemo(() => {
    return {
      pending: analytics.leaves.filter(
        (leave) => leave.status === "PENDING"
      ).length,
      approved: analytics.leaves.filter(
        (leave) => leave.status === "APPROVED"
      ).length,
      rejected: analytics.leaves.filter(
        (leave) => leave.status === "REJECTED"
      ).length,
    };
  }, [analytics.leaves]);

  // Department-wise employee count
  const departmentAnalytics = useMemo(() => {
    const departmentMap = {};

    analytics.employees.forEach((employee) => {
      const department = employee.department?.trim() || "Not Assigned";

      departmentMap[department] =
        (departmentMap[department] || 0) + 1;
    });

    return Object.entries(departmentMap)
      .map(([department, count]) => ({ department, count }))
      .sort((a, b) => b.count - a.count)
      .slice(0, 6);
  }, [analytics.employees]);

  const maxMonthlyCount = Math.max(
    ...monthlyAttendance.map((item) => item.count),
    1
  );

  const maxDepartmentCount = Math.max(
    ...departmentAnalytics.map((item) => item.count),
    1
  );

  const modules = [
    ...(isAdminOrHR
      ? [
          {
            icon: "👥",
            title: "Employee Management",
            description: "View and manage employee records.",
            action: () => onOpenModule?.("Employee Management"),
          },
        ]
      : []),
    {
      icon: "🕒",
      title: "Attendance Management",
      description: "Review your attendance and working hours.",
      action: () => onOpenModule?.("Attendance Management"),
    },
    {
      icon: "📅",
      title: "Leave Management",
      description: "Apply for leave and track your requests.",
      action: () => onOpenModule?.("Leave Management"),
    },
    {
      icon: "🎉",
      title: "Company Holidays",
      description: "View upcoming company holidays and dates.",
      action: () => onOpenModule?.("Company Holidays"),
    },
    ...(isAdminOrHR
      ? [
          {
            icon: "💰",
            title: "Payroll Management",
            description: "Manage payroll and salary information.",
            action: () => onOpenModule?.("Payroll Management"),
          },
        ]
      : []),
    {
      icon: "👤",
      title: "My Profile",
      description: "View your personal and employment details.",
      action: onOpenProfile,
    },
  ];

  const displayName = user?.fullName || user?.employeeId || "Employee";

  const summaryCards = useMemo(
    () => [
      {
        icon: "👥",
        label: "Total Employees",
        value: stats.totalEmployees,
        tone: "employees",
      },
      {
        icon: "🟢",
        label: "Present Today",
        value: stats.presentToday,
        tone: "present",
      },
      {
        icon: "📅",
        label: "On Leave Today",
        value: stats.onLeaveToday,
        tone: "leave",
      },
      {
        icon: "⏳",
        label: "Pending Leave Requests",
        value: stats.pendingLeaves,
        tone: "pending",
      },
    ],
    [stats]
  );

  return (
    <main className="hrms-dashboard">
      <style>{`
        .dashboard-analytics {
          margin-top: 30px;
        }

        .analytics-grid {
          display: grid;
          grid-template-columns: repeat(2, minmax(0, 1fr));
          gap: 20px;
          margin-top: 18px;
        }

        .analytics-card {
          background: #fff;
          border: 1px solid #e8edf3;
          border-radius: 14px;
          padding: 22px;
          min-width: 0;
          box-shadow: 0 3px 12px rgba(15, 23, 42, 0.04);
        }

        .analytics-card h3 {
          margin: 0 0 6px;
          font-size: 16px;
          color: #172033;
        }

        .analytics-card-subtitle {
          margin: 0 0 22px;
          color: #8490a3;
          font-size: 12px;
        }

        .attendance-chart-content {
          display: flex;
          align-items: center;
          justify-content: center;
          gap: 30px;
          flex-wrap: wrap;
        }

        .attendance-donut {
          width: 155px;
          height: 155px;
          border-radius: 50%;
          display: grid;
          place-items: center;
          flex-shrink: 0;
        }

        .attendance-donut-inner {
          width: 112px;
          height: 112px;
          border-radius: 50%;
          background: white;
          display: flex;
          flex-direction: column;
          justify-content: center;
          align-items: center;
        }

        .attendance-donut-inner strong {
          font-size: 27px;
          color: #172033;
        }

        .attendance-donut-inner span {
          font-size: 11px;
          color: #8490a3;
        }

        .analytics-legend {
          display: grid;
          gap: 13px;
        }

        .analytics-legend-item {
          display: flex;
          align-items: center;
          gap: 9px;
          font-size: 13px;
          color: #586477;
        }

        .analytics-legend-item strong {
          margin-left: auto;
          color: #172033;
        }

        .analytics-dot {
          width: 10px;
          height: 10px;
          border-radius: 50%;
          flex-shrink: 0;
        }

        .monthly-chart {
          height: 190px;
          display: flex;
          align-items: flex-end;
          justify-content: space-around;
          gap: 12px;
          border-bottom: 1px solid #e8edf3;
          padding: 0 5px;
        }

        .monthly-chart-column {
          height: 100%;
          flex: 1;
          display: flex;
          flex-direction: column;
          justify-content: flex-end;
          align-items: center;
          gap: 9px;
          min-width: 0;
        }

        .monthly-chart-bar-area {
          height: 150px;
          width: 100%;
          display: flex;
          align-items: flex-end;
          justify-content: center;
        }

        .monthly-chart-bar {
          width: min(35px, 75%);
          min-height: 3px;
          border-radius: 6px 6px 0 0;
          background: linear-gradient(180deg, #6488f5, #4263d8);
          transition: height 0.3s ease;
        }

        .monthly-chart-column strong {
          font-size: 11px;
          color: #586477;
        }

        .monthly-chart-column span {
          font-size: 12px;
          color: #8490a3;
        }

        .leave-chart-list,
        .department-chart-list {
          display: grid;
          gap: 20px;
        }

        .leave-chart-item,
        .department-chart-item {
          display: grid;
          gap: 8px;
        }

        .leave-chart-label,
        .department-chart-label {
          display: flex;
          justify-content: space-between;
          gap: 10px;
          font-size: 13px;
          color: #586477;
        }

        .leave-chart-label strong,
        .department-chart-label strong {
          color: #172033;
        }

        .analytics-progress-track {
          width: 100%;
          height: 9px;
          background: #edf1f6;
          border-radius: 20px;
          overflow: hidden;
        }

        .analytics-progress-fill {
          height: 100%;
          border-radius: 20px;
          transition: width 0.3s ease;
        }

        .analytics-empty {
          color: #8490a3;
          text-align: center;
          padding: 30px 10px;
          font-size: 13px;
        }

        @media (max-width: 768px) {
          .analytics-grid {
            grid-template-columns: 1fr;
          }
        }
      `}</style>

      <header className="dashboard-header">
        <div>
          <p className="dashboard-eyebrow">OPSOFT HRMS</p>
          <h1>Welcome, {displayName}</h1>
          <p className="dashboard-subtitle">
            Here’s your workspace. Choose a section to get started.
          </p>
        </div>

        <div className="dashboard-user-info">
          <span className="dashboard-role">{user?.role || "EMPLOYEE"}</span>
          {user?.employeeId && (
            <span className="dashboard-employee-id">
              ID: {user.employeeId}
            </span>
          )}
        </div>
      </header>

      {isAdminOrHR && (
        <>
          <section className="dashboard-section dashboard-overview-section">
            <div className="dashboard-section-heading">
              <div>
                <h2>Workforce overview</h2>
                <p>Today's HRMS summary</p>
              </div>

              <button
                type="button"
                className="dashboard-refresh-button"
                onClick={loadDashboardStats}
                disabled={statsLoading}
              >
                {statsLoading ? "Refreshing..." : "↻ Refresh"}
              </button>
            </div>

            <div className="dashboard-stats-grid">
              {summaryCards.map((card) => (
                <article
                  className={`dashboard-stat-card dashboard-stat-${card.tone}`}
                  key={card.label}
                >
                  <span className="dashboard-stat-icon" aria-hidden="true">
                    {card.icon}
                  </span>

                  <div className="dashboard-stat-content">
                    <p>{card.label}</p>
                    <strong>
                      {card.value === null
                        ? statsLoading
                          ? "..."
                          : "—"
                        : card.value}
                    </strong>
                  </div>
                </article>
              ))}
            </div>

            {statsError && (
              <p className="dashboard-stats-error" role="status">
                {statsError}
              </p>
            )}
          </section>

          {/* Analytics charts */}
          <section className="dashboard-section dashboard-analytics">
            <div className="dashboard-section-heading">
              <div>
                <h2>HR Analytics</h2>
                <p>Workforce insights based on HRMS records</p>
              </div>
            </div>

            {statsLoading && analytics.employees.length === 0 ? (
              <div className="analytics-card analytics-empty">
                Loading analytics...
              </div>
            ) : (
              <div className="analytics-grid">
                {/* Today's Attendance */}
                <article className="analytics-card">
                  <h3>Today's Attendance</h3>
                  <p className="analytics-card-subtitle">
                    Employee check-in overview
                  </p>

                  <div className="attendance-chart-content">
                    <div
                      className="attendance-donut"
                      style={{
                        background: `conic-gradient(#16a36a ${
                          attendanceAnalytics.percentage
                        }%, #e9edf3 0)`,
                      }}
                    >
                      <div className="attendance-donut-inner">
                        <strong>
                          {Math.round(attendanceAnalytics.percentage)}%
                        </strong>
                        <span>Present</span>
                      </div>
                    </div>

                    <div className="analytics-legend">
                      <div className="analytics-legend-item">
                        <span
                          className="analytics-dot"
                          style={{ background: "#16a36a" }}
                        />
                        Present
                        <strong>{attendanceAnalytics.present}</strong>
                      </div>

                      <div className="analytics-legend-item">
                        <span
                          className="analytics-dot"
                          style={{ background: "#e9edf3" }}
                        />
                        Not checked in
                        <strong>{attendanceAnalytics.notCheckedIn}</strong>
                      </div>

                      <div className="analytics-legend-item">
                        <span
                          className="analytics-dot"
                          style={{ background: "#6488f5" }}
                        />
                        Currently working
                        <strong>{attendanceAnalytics.working}</strong>
                      </div>

                      <div className="analytics-legend-item">
                        <span
                          className="analytics-dot"
                          style={{ background: "#f0a33a" }}
                        />
                        Checked out
                        <strong>{attendanceAnalytics.checkedOut}</strong>
                      </div>
                    </div>
                  </div>
                </article>

                {/* Monthly Attendance Trend */}
                <article className="analytics-card">
                  <h3>Monthly Attendance Trend</h3>
                  <p className="analytics-card-subtitle">
                    Number of distinct employees with check-ins per month
                  </p>

                  <div className="monthly-chart">
                    {monthlyAttendance.map((item) => (
                      <div
                        className="monthly-chart-column"
                        key={item.key}
                      >
                        <div className="monthly-chart-bar-area">
                          <div
                            className="monthly-chart-bar"
                            style={{
                              height: `${
                                (item.count / maxMonthlyCount) * 100
                              }%`,
                            }}
                            title={`${item.count} employees`}
                          />
                        </div>

                        <strong>{item.count}</strong>
                        <span>{item.label}</span>
                      </div>
                    ))}
                  </div>
                </article>

                {/* Leave Overview */}
                <article className="analytics-card">
                  <h3>Leave Overview</h3>
                  <p className="analytics-card-subtitle">
                    Leave requests by current status
                  </p>

                  {analytics.leaves.length === 0 ? (
                    <div className="analytics-empty">
                      No leave records available.
                    </div>
                  ) : (
                    <div className="leave-chart-list">
                      {[
                        {
                          label: "Pending",
                          count: leaveAnalytics.pending,
                          color: "#e9a23b",
                        },
                        {
                          label: "Approved",
                          count: leaveAnalytics.approved,
                          color: "#16a36a",
                        },
                        {
                          label: "Rejected",
                          count: leaveAnalytics.rejected,
                          color: "#e05252",
                        },
                      ].map((item) => (
                        <div className="leave-chart-item" key={item.label}>
                          <div className="leave-chart-label">
                            <span>{item.label}</span>
                            <strong>{item.count}</strong>
                          </div>

                          <div className="analytics-progress-track">
                            <div
                              className="analytics-progress-fill"
                              style={{
                                width: `${
                                  (item.count / analytics.leaves.length) *
                                  100
                                }%`,
                                background: item.color,
                              }}
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </article>

                {/* Department-wise Employees */}
                <article className="analytics-card">
                  <h3>Department-wise Employees</h3>
                  <p className="analytics-card-subtitle">
                    Employee distribution by department
                  </p>

                  {departmentAnalytics.length === 0 ? (
                    <div className="analytics-empty">
                      No department records available.
                    </div>
                  ) : (
                    <div className="department-chart-list">
                      {departmentAnalytics.map((item) => (
                        <div
                          className="department-chart-item"
                          key={item.department}
                        >
                          <div className="department-chart-label">
                            <span>{item.department}</span>
                            <strong>{item.count}</strong>
                          </div>

                          <div className="analytics-progress-track">
                            <div
                              className="analytics-progress-fill"
                              style={{
                                width: `${
                                  (item.count / maxDepartmentCount) * 100
                                }%`,
                                background: "#6488f5",
                              }}
                            />
                          </div>
                        </div>
                      ))}
                    </div>
                  )}
                </article>
              </div>
            )}
          </section>
        </>
      )}

      <section className="dashboard-section">
        <div className="dashboard-section-heading">
          <div>
            <h2>Quick access</h2>
            <p>Frequently used modules</p>
          </div>

          <span className="dashboard-count">
            {modules.length} {modules.length === 1 ? "module" : "modules"}
          </span>
        </div>

        <div className="dashboard-module-grid">
          {modules.map((module) => (
            <button
              type="button"
              className="dashboard-module-card"
              key={module.title}
              onClick={module.action}
            >
              <span className="dashboard-module-icon" aria-hidden="true">
                {module.icon}
              </span>

              <span className="dashboard-card-content">
                <span className="dashboard-card-title">
                  {module.title}
                </span>

                <span className="dashboard-card-description">
                  {module.description}
                </span>
              </span>

              <span className="dashboard-card-arrow" aria-hidden="true">
                →
              </span>
            </button>
          ))}
        </div>
      </section>

      <footer className="dashboard-footer">
        <span>© {new Date().getFullYear()} OpsoftHRMS</span>
        <span>OpSoft Technologies Pvt Ltd</span>
      </footer>
    </main>
  );
}

export default Dashboard;
