
import React, { useEffect, useState } from "react";
import "./AnnouncementManagement.css";

const API_URL = "http://localhost:8083";

const initialForm = {
  title: "",
  content: "",
  category: "GENERAL",
  priority: "NORMAL",
  audience: "ALL_EMPLOYEES",
  selectedEmployeeIds: [],
  expiryDate: ""
};

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

async function readResponse(response) {
  const text = await response.text();
  let data = {};

  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = { message: text };
    }
  }

  if (!response.ok) {
    throw new Error(
      data.message || data.error || "Announcement request failed."
    );
  }

  return data;
}

function AnnouncementManagement({ user, employees = [] }) {
  const isAdminOrHR =
    user?.role === "ADMIN" || user?.role === "HR";

  const [announcements, setAnnouncements] = useState([]);
  const [form, setForm] = useState(initialForm);
  const [editingId, setEditingId] = useState(null);
  const [showForm, setShowForm] = useState(false);
  const [selectedAnnouncement, setSelectedAnnouncement] = useState(null);

  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const fetchAnnouncements = async () => {
    setLoading(true);
    setError("");

    try {
      const endpoint = isAdminOrHR
        ? "/api/announcements/admin"
        : "/api/announcements/employee";

      const response = await apiFetch(`${API_URL}${endpoint}`);
      const data = await readResponse(response);

      setAnnouncements(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchAnnouncements();
  }, [user?.employeeId, user?.role]);

  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value
    }));
  };

  const handleEmployeeSelection = (employeeId) => {
    setForm((previous) => {
      const selected = previous.selectedEmployeeIds;

      return {
        ...previous,
        selectedEmployeeIds: selected.includes(employeeId)
          ? selected.filter((id) => id !== employeeId)
          : [...selected, employeeId]
      };
    });
  };

  const openCreateForm = () => {
    setEditingId(null);
    setForm(initialForm);
    setShowForm(true);
    setError("");
    setSuccess("");
  };

  const openEditForm = (announcement) => {
    setEditingId(announcement.id);

    setForm({
      title: announcement.title || "",
      content: announcement.content || "",
      category: announcement.category || "GENERAL",
      priority: announcement.priority || "NORMAL",
      audience: announcement.audience || "ALL_EMPLOYEES",
      selectedEmployeeIds: announcement.selectedEmployeeIds || [],
      expiryDate: announcement.expiryDate || ""
    });

    setShowForm(true);
    setError("");
    setSuccess("");
  };

  const handleSubmit = async (event) => {
    event.preventDefault();

    if (
      form.audience === "SELECTED_EMPLOYEES" &&
      form.selectedEmployeeIds.length === 0
    ) {
      setError("Please select at least one employee.");
      return;
    }

    setSaving(true);
    setError("");
    setSuccess("");

    try {
      const url = editingId
        ? `${API_URL}/api/announcements/${editingId}`
        : `${API_URL}/api/announcements`;

      const response = await apiFetch(url, {
        method: editingId ? "PUT" : "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          ...form,
          selectedEmployeeIds:
            form.audience === "ALL_EMPLOYEES"
              ? []
              : form.selectedEmployeeIds,
          expiryDate: form.expiryDate || null
        })
      });

      await readResponse(response);

      setShowForm(false);
      setEditingId(null);
      setForm(initialForm);

      setSuccess(
        editingId
          ? "Announcement updated successfully."
          : "Announcement created successfully as a draft."
      );

      await fetchAnnouncements();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  const handlePublish = async (announcement) => {
    const action = announcement.status === "PUBLISHED"
      ? "unpublish"
      : "publish";

    const confirmed = window.confirm(
      `Are you sure you want to ${action} this announcement?`
    );

    if (!confirmed) return;

    try {
      setError("");
      setSuccess("");

      const response = await apiFetch(
        `${API_URL}/api/announcements/${announcement.id}/${action}`,
        { method: "PATCH" }
      );

      await readResponse(response);

      setSuccess(`Announcement ${action}ed successfully.`);
      await fetchAnnouncements();
    } catch (err) {
      setError(err.message);
    }
  };

  const handleDelete = async (id) => {
    if (!window.confirm("Delete this announcement permanently?")) {
      return;
    }

    try {
      setError("");

      const response = await apiFetch(
        `${API_URL}/api/announcements/${id}`,
        { method: "DELETE" }
      );

      await readResponse(response);

      setSuccess("Announcement deleted successfully.");
      setSelectedAnnouncement(null);

      await fetchAnnouncements();
    } catch (err) {
      setError(err.message);
    }
  };

  const formatDate = (date) => {
    if (!date) return "Not published";

    return new Date(date).toLocaleDateString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric"
    });
  };

  return (
    <div className="announcement-page">

      <div className="announcement-header">
        <div>
          <span className="announcement-eyebrow">
            OPSOFT HRMS
          </span>

          <h1>Announcements</h1>

          <p>
            {isAdminOrHR
              ? "Create and manage organization announcements."
              : "Stay informed with the latest company updates."}
          </p>
        </div>

        {isAdminOrHR && (
          <button
            className="announcement-primary-btn"
            onClick={openCreateForm}
          >
            + Create Announcement
          </button>
        )}
      </div>

      {error && (
        <div className="announcement-alert error">
          {error}
        </div>
      )}

      {success && (
        <div className="announcement-alert success">
          {success}
        </div>
      )}

      {showForm && (
        <form
          className="announcement-form"
          onSubmit={handleSubmit}
        >
          <h2>
            {editingId ? "Edit Announcement" : "Create Announcement"}
          </h2>

          <label>Announcement Title *</label>

          <input
            name="title"
            value={form.title}
            onChange={handleChange}
            maxLength={200}
            required
          />

          <label>Announcement Content *</label>

          <textarea
            name="content"
            value={form.content}
            onChange={handleChange}
            rows={5}
            required
          />

          <div className="announcement-form-grid">

            <div>
              <label>Category</label>

              <select
                name="category"
                value={form.category}
                onChange={handleChange}
              >
                <option value="GENERAL">General</option>
                <option value="HR">HR</option>
                <option value="POLICY">Policy</option>
                <option value="EVENT">Event</option>
                <option value="HOLIDAY">Holiday</option>
              </select>
            </div>

            <div>
              <label>Priority</label>

              <select
                name="priority"
                value={form.priority}
                onChange={handleChange}
              >
                <option value="NORMAL">Normal</option>
                <option value="IMPORTANT">Important</option>
                <option value="URGENT">Urgent</option>
              </select>
            </div>

            <div>
              <label>Audience</label>

              <select
                name="audience"
                value={form.audience}
                onChange={handleChange}
              >
                <option value="ALL_EMPLOYEES">
                  All Employees
                </option>
                <option value="SELECTED_EMPLOYEES">
                  Selected Employees
                </option>
              </select>
            </div>

            <div>
              <label>Expiry Date</label>

              <input
                type="date"
                name="expiryDate"
                value={form.expiryDate}
                onChange={handleChange}
              />
            </div>

          </div>

          {form.audience === "SELECTED_EMPLOYEES" && (
            <div className="announcement-employee-selection">
              <h3>Select Employees</h3>

              {employees
                .filter((employee) => employee.role === "EMPLOYEE")
                .map((employee) => (
                  <label
                    className="announcement-employee-option"
                    key={employee.employeeId}
                  >
                    <input
                      type="checkbox"
                      checked={form.selectedEmployeeIds.includes(
                        employee.employeeId
                      )}
                      onChange={() =>
                        handleEmployeeSelection(employee.employeeId)
                      }
                    />

                    <span>
                      {employee.fullName} ({employee.employeeId})
                    </span>
                  </label>
                ))}

              {employees.filter(
                (employee) => employee.role === "EMPLOYEE"
              ).length === 0 && (
                <p>No employee records available.</p>
              )}
            </div>
          )}

          <div className="announcement-form-actions">
            <button
              type="button"
              className="announcement-secondary-btn"
              onClick={() => setShowForm(false)}
            >
              Cancel
            </button>

            <button
              type="submit"
              className="announcement-primary-btn"
              disabled={saving}
            >
              {saving
                ? "Saving..."
                : editingId
                  ? "Update Announcement"
                  : "Save as Draft"}
            </button>
          </div>
        </form>
      )}

      {loading ? (
        <p className="announcement-empty">Loading announcements...</p>
      ) : announcements.length === 0 ? (
        <div className="announcement-empty">
          <span>📢</span>
          <h3>No announcements available</h3>
          <p>New company announcements will appear here.</p>
        </div>
      ) : (
        <div className="announcement-list">

          {announcements.map((announcement) => (
            <article
              className="announcement-card"
              key={announcement.id}
            >
              <div className="announcement-card-top">
                <span className="announcement-category">
                  {announcement.category}
                </span>

                {isAdminOrHR && (
                  <span
                    className={`announcement-status ${
                      announcement.status?.toLowerCase()
                    }`}
                  >
                    {announcement.status}
                  </span>
                )}
              </div>

              <h2>{announcement.title}</h2>

              <p className="announcement-preview">
                {announcement.content}
              </p>

              <div className="announcement-meta">
                <span>Priority: {announcement.priority}</span>

                <span>
                  {announcement.publishedAt
                    ? `Published: ${formatDate(announcement.publishedAt)}`
                    : `Created: ${formatDate(announcement.createdAt)}`}
                </span>
              </div>

              <div className="announcement-card-actions">

                <button
                  className="announcement-secondary-btn"
                  onClick={() => setSelectedAnnouncement(announcement)}
                >
                  Read More
                </button>

                {isAdminOrHR && (
                  <>
                    <button
                      className="announcement-secondary-btn"
                      onClick={() => openEditForm(announcement)}
                    >
                      Edit
                    </button>

                    <button
                      className="announcement-primary-btn"
                      onClick={() => handlePublish(announcement)}
                    >
                      {announcement.status === "PUBLISHED"
                        ? "Unpublish"
                        : "Publish"}
                    </button>

                    <button
                      className="announcement-delete-btn"
                      onClick={() => handleDelete(announcement.id)}
                    >
                      Delete
                    </button>
                  </>
                )}

              </div>
            </article>
          ))}

        </div>
      )}

      {selectedAnnouncement && (
        <div className="announcement-modal-backdrop">
          <div className="announcement-modal">

            <button
              className="announcement-modal-close"
              onClick={() => setSelectedAnnouncement(null)}
            >
              ×
            </button>

            <span className="announcement-category">
              {selectedAnnouncement.category}
            </span>

            <h2>{selectedAnnouncement.title}</h2>

            <p className="announcement-full-content">
              {selectedAnnouncement.content}
            </p>

            <div className="announcement-meta">
              <span>
                Priority: {selectedAnnouncement.priority}
              </span>

              <span>
                Published: {formatDate(selectedAnnouncement.publishedAt)}
              </span>
            </div>

          </div>
        </div>
      )}

    </div>
  );
}

export default AnnouncementManagement;
