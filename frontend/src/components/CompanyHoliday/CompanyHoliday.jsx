
import React, { useCallback, useEffect, useMemo, useState } from "react";
import "./CompanyHoliday.css";

const API_URL = "http://localhost:8083/api/holidays";

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

function CompanyHoliday({ user }) {
  const isAdminOrHR =
    user?.role === "ADMIN" || user?.role === "HR";

  const isEmployee = user?.role === "EMPLOYEE";

  const [holidays, setHolidays] = useState([]);
  const [holidayDate, setHolidayDate] = useState("");
  const [holidayName, setHolidayName] = useState("");
  const [description, setDescription] = useState("");

  const [editingId, setEditingId] = useState(null);
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  const [filter, setFilter] = useState("ALL");

  const [currentMonth, setCurrentMonth] = useState(() => {
    const today = new Date();
    return new Date(today.getFullYear(), today.getMonth(), 1);
  });

  // Fetch holidays according to role
  const fetchHolidays = useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const endpoint = isEmployee
        ? `${API_URL}/active`
        : API_URL;

      const response = await apiFetch(endpoint, {
        method: "GET"
      });

      const data = await response.json().catch(() => []);

      if (!response.ok) {
        throw new Error(
          data.message ||
          data.error ||
          `Unable to load holidays. HTTP ${response.status}`
        );
      }

      setHolidays(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || "Unable to load holidays.");
    } finally {
      setLoading(false);
    }
  }, [isEmployee]);

  useEffect(() => {
    if (isAdminOrHR || isEmployee) {
      fetchHolidays();
    }
  }, [isAdminOrHR, isEmployee, fetchHolidays]);

  const resetForm = () => {
    setHolidayDate("");
    setHolidayName("");
    setDescription("");
    setEditingId(null);
  };

  // Add or update holiday
  const handleSubmit = async (event) => {
    event.preventDefault();

    setError("");
    setSuccess("");

    const payload = {
      holidayDate,
      holidayName: holidayName.trim(),
      description: description.trim()
    };

    setSaving(true);

    try {
      const response = await apiFetch(
        editingId
          ? `${API_URL}/${editingId}`
          : API_URL,
        {
          method: editingId ? "PUT" : "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify(payload)
        }
      );

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        throw new Error(
          data.message ||
          data.error ||
          `Unable to save holiday. HTTP ${response.status}`
        );
      }

      setSuccess(
        editingId
          ? "Holiday updated successfully."
          : "Holiday added successfully."
      );

      resetForm();
      await fetchHolidays();
    } catch (err) {
      setError(err.message || "Unable to save holiday.");
    } finally {
      setSaving(false);
    }
  };

  // Edit holiday
  const handleEdit = (holiday) => {
    setEditingId(holiday.id);
    setHolidayDate(holiday.holidayDate);
    setHolidayName(holiday.holidayName);
    setDescription(holiday.description || "");

    setError("");
    setSuccess("");

    window.scrollTo({
      top: 0,
      behavior: "smooth"
    });
  };

  // Deactivate holiday
  const handleDeactivate = async (holiday) => {
    const confirmed = window.confirm(
      `Are you sure you want to deactivate "${holiday.holidayName}"?`
    );

    if (!confirmed) return;

    setError("");
    setSuccess("");

    try {
      const response = await apiFetch(
        `${API_URL}/${holiday.id}/deactivate`,
        {
          method: "PATCH"
        }
      );

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        throw new Error(
          data.message ||
          data.error ||
          `Unable to deactivate holiday. HTTP ${response.status}`
        );
      }

      setSuccess("Holiday deactivated successfully.");
      await fetchHolidays();
    } catch (err) {
      setError(err.message || "Unable to deactivate holiday.");
    }
  };

  const activeCount = holidays.filter(
    (holiday) => holiday.isActive
  ).length;

  const today = new Date();
  today.setHours(0, 0, 0, 0);

  const upcomingHolidays = useMemo(() => {
    return holidays
      .filter((holiday) => {
        const date = new Date(`${holiday.holidayDate}T00:00:00`);
        return date >= today && holiday.isActive;
      })
      .sort((a, b) => a.holidayDate.localeCompare(b.holidayDate));
  }, [holidays]);

  const formatDate = (date) => {
    if (!date) return "-";

    return new Date(`${date}T00:00:00`).toLocaleDateString(
      "en-IN",
      {
        day: "2-digit",
        month: "short",
        year: "numeric"
      }
    );
  };

  // Calendar calculations
  const year = currentMonth.getFullYear();
  const month = currentMonth.getMonth();

  const firstDay = new Date(year, month, 1).getDay();
  const daysInMonth = new Date(year, month + 1, 0).getDate();

  // Monday-first calendar
  const calendarStartOffset = (firstDay + 6) % 7;

  const calendarDays = [
    ...Array(calendarStartOffset).fill(null),
    ...Array.from({ length: daysInMonth }, (_, index) => index + 1)
  ];

  while (calendarDays.length % 7 !== 0) {
    calendarDays.push(null);
  }

  const holidayMap = useMemo(() => {
    const map = {};

    holidays.forEach((holiday) => {
      if (holiday.isActive && holiday.holidayDate) {
        map[holiday.holidayDate] = holiday;
      }
    });

    return map;
  }, [holidays]);

  const changeMonth = (direction) => {
    setCurrentMonth(
      new Date(year, month + direction, 1)
    );
  };

  const monthTitle = currentMonth.toLocaleDateString("en-IN", {
    month: "long",
    year: "numeric"
  });

  const filteredHolidays = holidays.filter((holiday) => {
    if (filter === "ACTIVE") return holiday.isActive;
    if (filter === "INACTIVE") return !holiday.isActive;
    return true;
  });

  const inactiveCount = holidays.length - activeCount;

  if (!isAdminOrHR && !isEmployee) {
    return (
      <div className="holiday-page">
        <div className="holiday-alert holiday-error">
          You are not authorized to access company holidays.
        </div>
      </div>
    );
  }

  // Employee Holiday Calendar
  if (isEmployee) {
    return (
      <div className="holiday-page employee-holiday-page">

        <div className="holiday-heading">
          <div>
            <span className="holiday-eyebrow">
              EMPLOYEE SELF SERVICE
            </span>

            <h1>Company Holiday Calendar</h1>

            <p>
              View official company holidays and plan your time ahead.
            </p>
          </div>

          <div className="holiday-heading-icon">
            🗓️
          </div>
        </div>

        {error && (
          <div className="holiday-alert holiday-error">
            {error}
            <button onClick={() => setError("")}>×</button>
          </div>
        )}

        <div className="holiday-summary">

          <div className="holiday-summary-card">
            <span className="summary-icon">📅</span>
            <div>
              <p>Total Company Holidays</p>
              <h2>{holidays.length}</h2>
            </div>
          </div>

          <div className="holiday-summary-card">
            <span className="summary-icon">🎉</span>
            <div>
              <p>Upcoming Holidays</p>
              <h2>{upcomingHolidays.length}</h2>
            </div>
          </div>

          <div className="holiday-summary-card">
            <span className="summary-icon">⭐</span>
            <div>
              <p>Next Company Holiday</p>
              <h2 className="next-holiday-value">
                {upcomingHolidays.length > 0
                  ? formatDate(upcomingHolidays[0].holidayDate)
                  : "None"}
              </h2>
            </div>
          </div>

        </div>

        <section className="holiday-card employee-calendar-card">

          <div className="holiday-list-heading">
            <div>
              <h2>Monthly Holiday Calendar</h2>
              <p>Company holidays are highlighted in the calendar.</p>
            </div>

            <div className="calendar-navigation">
              <button
                type="button"
                onClick={() => changeMonth(-1)}
                aria-label="Previous month"
              >
                ‹
              </button>

              <strong>{monthTitle}</strong>

              <button
                type="button"
                onClick={() => changeMonth(1)}
                aria-label="Next month"
              >
                ›
              </button>
            </div>
          </div>

          <div className="holiday-calendar">

            <div className="calendar-weekdays">
              {["Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"].map(
                (day) => (
                  <div key={day}>{day}</div>
                )
              )}
            </div>

            <div className="calendar-days">
              {calendarDays.map((day, index) => {
                if (!day) {
                  return (
                    <div
                      key={`empty-${index}`}
                      className="calendar-day empty"
                    />
                  );
                }

                const dateString = [
                  year,
                  String(month + 1).padStart(2, "0"),
                  String(day).padStart(2, "0")
                ].join("-");

                const holiday = holidayMap[dateString];

                const isToday =
                  dateString === [
                    today.getFullYear(),
                    String(today.getMonth() + 1).padStart(2, "0"),
                    String(today.getDate()).padStart(2, "0")
                  ].join("-");

                return (
                  <div
                    key={dateString}
                    className={[
                      "calendar-day",
                      holiday ? "has-holiday" : "",
                      isToday ? "calendar-today" : ""
                    ].filter(Boolean).join(" ")}
                    title={holiday ? holiday.holidayName : ""}
                  >
                    <span className="calendar-date-number">
                      {day}
                    </span>

                    {holiday && (
                      <span className="calendar-holiday-name">
                        {holiday.holidayName}
                      </span>
                    )}

                    {isToday && (
                      <span className="calendar-today-label">
                        Today
                      </span>
                    )}
                  </div>
                );
              })}
            </div>

          </div>

          <div className="calendar-legend">
            <span className="legend-holiday-dot" />
            Company Holiday

            <span className="legend-today-dot" />
            Today
          </div>

        </section>

        <section className="holiday-card employee-upcoming-card">

          <div className="holiday-list-heading">
            <div>
              <h2>Upcoming Company Holidays</h2>
              <p>Plan your schedule around these holidays.</p>
            </div>

            <button
              className="holiday-refresh-button"
              onClick={fetchHolidays}
              disabled={loading}
            >
              ↻ Refresh
            </button>
          </div>

          {loading ? (
            <div className="holiday-empty">
              Loading company holidays...
            </div>
          ) : upcomingHolidays.length === 0 ? (
            <div className="holiday-empty">
              <span>📭</span>
              <h3>No upcoming holidays</h3>
              <p>There are currently no upcoming company holidays.</p>
            </div>
          ) : (
            <div className="employee-upcoming-list">
              {upcomingHolidays.map((holiday) => (
                <div
                  className="employee-upcoming-item"
                  key={holiday.id}
                >
                  <div className="upcoming-date-box">
                    <strong>
                      {new Date(
                        `${holiday.holidayDate}T00:00:00`
                      ).getDate()}
                    </strong>

                    <span>
                      {new Date(
                        `${holiday.holidayDate}T00:00:00`
                      ).toLocaleDateString("en-IN", {
                        month: "short"
                      })}
                    </span>
                  </div>

                  <div className="upcoming-holiday-details">
                    <h3>{holiday.holidayName}</h3>

                    <p>
                      {holiday.description || "Company Holiday"}
                    </p>

                    <span>
                      {formatDate(holiday.holidayDate)}
                    </span>
                  </div>

                  <span className="upcoming-holiday-badge">
                    COMPANY HOLIDAY
                  </span>
                </div>
              ))}
            </div>
          )}

        </section>

      </div>
    );
  }

  // HR/Admin Holiday Management
  return (
    <div className="holiday-page">

      <div className="holiday-heading">
        <div>
          <span className="holiday-eyebrow">
            HUMAN RESOURCE MANAGEMENT
          </span>

          <h1>Company Holiday Calendar</h1>

          <p>
            Manage official company holidays and maintain
            the holiday calendar for employees.
          </p>
        </div>

        <div className="holiday-heading-icon">
          🗓️
        </div>
      </div>

      {error && (
        <div className="holiday-alert holiday-error">
          {error}
          <button onClick={() => setError("")}>×</button>
        </div>
      )}

      {success && (
        <div className="holiday-alert holiday-success">
          {success}
          <button onClick={() => setSuccess("")}>×</button>
        </div>
      )}

      <div className="holiday-summary">

        <div className="holiday-summary-card">
          <span className="summary-icon">📅</span>
          <div>
            <p>Total Holidays</p>
            <h2>{holidays.length}</h2>
          </div>
        </div>

        <div className="holiday-summary-card">
          <span className="summary-icon">✅</span>
          <div>
            <p>Active Holidays</p>
            <h2>{activeCount}</h2>
          </div>
        </div>

        <div className="holiday-summary-card">
          <span className="summary-icon">⏸️</span>
          <div>
            <p>Inactive Holidays</p>
            <h2>{inactiveCount}</h2>
          </div>
        </div>

      </div>

      <section className="holiday-card">
        <div className="holiday-card-heading">
          <div>
            <h2>
              {editingId ? "Update Holiday" : "Add New Holiday"}
            </h2>
            <p>Enter the official company holiday details.</p>
          </div>

          <span>📌</span>
        </div>

        <form onSubmit={handleSubmit}>
          <div className="holiday-form-grid">

            <div className="holiday-field">
              <label>Holiday Date *</label>

              <input
                type="date"
                value={holidayDate}
                onChange={(event) =>
                  setHolidayDate(event.target.value)
                }
                required
              />
            </div>

            <div className="holiday-field">
              <label>Holiday Name *</label>

              <input
                type="text"
                value={holidayName}
                onChange={(event) =>
                  setHolidayName(event.target.value)
                }
                placeholder="Example: Independence Day"
                maxLength={150}
                required
              />
            </div>

            <div className="holiday-field holiday-description">
              <label>Description</label>

              <input
                type="text"
                value={description}
                onChange={(event) =>
                  setDescription(event.target.value)
                }
                placeholder="Enter holiday description"
                maxLength={500}
              />
            </div>

          </div>

          <div className="holiday-form-actions">
            <button
              type="submit"
              className="holiday-primary-button"
              disabled={saving}
            >
              {saving
                ? "Saving..."
                : editingId
                  ? "Update Holiday"
                  : "Add Holiday"}
            </button>

            {editingId && (
              <button
                type="button"
                className="holiday-cancel-button"
                onClick={resetForm}
              >
                Cancel
              </button>
            )}
          </div>
        </form>
      </section>

      <section className="holiday-card">
        <div className="holiday-list-heading">
          <div>
            <h2>Holiday Records</h2>
            <p>View and manage company holiday entries.</p>
          </div>

          <button
            className="holiday-refresh-button"
            onClick={fetchHolidays}
            disabled={loading}
          >
            ↻ Refresh
          </button>
        </div>

        <div className="holiday-filter">
          <label>Filter holidays</label>

          <select
            value={filter}
            onChange={(event) => setFilter(event.target.value)}
          >
            <option value="ALL">All Holidays</option>
            <option value="ACTIVE">Active Holidays</option>
            <option value="INACTIVE">Inactive Holidays</option>
          </select>
        </div>

        {loading ? (
          <div className="holiday-empty">
            Loading holidays...
          </div>
        ) : filteredHolidays.length === 0 ? (
          <div className="holiday-empty">
            <span>📭</span>
            <h3>No holidays found</h3>
            <p>Add a company holiday to get started.</p>
          </div>
        ) : (
          <div className="holiday-table-wrapper">
            <table className="holiday-table">
              <thead>
                <tr>
                  <th>#</th>
                  <th>Holiday Date</th>
                  <th>Holiday Name</th>
                  <th>Description</th>
                  <th>Status</th>
                  <th>Actions</th>
                </tr>
              </thead>

              <tbody>
                {filteredHolidays.map((holiday, index) => (
                  <tr key={holiday.id}>
                    <td>{index + 1}</td>

                    <td className="holiday-date-cell">
                      {formatDate(holiday.holidayDate)}
                    </td>

                    <td>
                      <strong>{holiday.holidayName}</strong>
                    </td>

                    <td>
                      {holiday.description || "-"}
                    </td>

                    <td>
                      <span
                        className={
                          holiday.isActive
                            ? "holiday-status active"
                            : "holiday-status inactive"
                        }
                      >
                        {holiday.isActive
                          ? "ACTIVE"
                          : "INACTIVE"}
                      </span>
                    </td>

                    <td>
                      <div className="holiday-actions">
                        {holiday.isActive && (
                          <>
                            <button
                              className="holiday-edit-button"
                              onClick={() => handleEdit(holiday)}
                            >
                              Edit
                            </button>

                            <button
                              className="holiday-deactivate-button"
                              onClick={() =>
                                handleDeactivate(holiday)
                              }
                            >
                              Deactivate
                            </button>
                          </>
                        )}

                        {!holiday.isActive && (
                          <span className="holiday-disabled-text">
                            No actions
                          </span>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </section>

    </div>
  );
}

export default CompanyHoliday;
