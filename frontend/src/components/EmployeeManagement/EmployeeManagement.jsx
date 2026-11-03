
import { useState } from "react";
import "./EmployeeManagement.css";
import StatusHistory from "./StatusHistory";

const API_URL = "http://localhost:8083";

// Common API function with JWT authentication
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

const initialForm = {
  employeeId: "",
  fullName: "",
  email: "",
  phone: "",
  department: "",
  designation: "",
  joiningDate: "",
  salary: "",
  role: "EMPLOYEE",
};

const employeeStatuses = [
  "ACTIVE",
  "ON_NOTICE",
  "RESIGNED",
  "TERMINATED",
  "INACTIVE",
];

function EmployeeManagement({
  employees = [],
  search = "",
  setSearch = () => {},
  onRefresh = () => {},
}) {
  const [showForm, setShowForm] = useState(false);
  const [form, setForm] = useState(initialForm);
  const [submitting, setSubmitting] = useState(false);
  const [formError, setFormError] = useState("");
  const [createdEmployee, setCreatedEmployee] = useState(null);

  // Initial Salary Structure is completed in the employee registration form.
  const [salaryForm, setSalaryForm] = useState({
    basicSalary: "",
    hra: "0",
    conveyanceAllowance: "0",
    medicalAllowance: "0",
    specialAllowance: "0",
    otherAllowances: "0",
    employeePf: "0",
    employerPf: "0",
    professionalTax: "0",
    otherDeductions: "0",
    effectiveFrom: "",
  });

  // Edit employee states
  const [showEditForm, setShowEditForm] = useState(false);
  const [editForm, setEditForm] = useState({});
  const [editingEmployeeId, setEditingEmployeeId] = useState("");
  const [editSubmitting, setEditSubmitting] = useState(false);
  const [editError, setEditError] = useState("");
  const [editSalaryLoading, setEditSalaryLoading] = useState(false);
  const [salaryStructureId, setSalaryStructureId] = useState(null);
  const [salaryHistory, setSalaryHistory] = useState([]);
  const [editSalaryForm, setEditSalaryForm] = useState({
    basicSalary: "",
    hra: "0",
    conveyanceAllowance: "0",
    medicalAllowance: "0",
    specialAllowance: "0",
    otherAllowances: "0",
    employeePf: "0",
    employerPf: "0",
    professionalTax: "0",
    otherDeductions: "0",
    effectiveFrom: "",
  });

  // Status management states
  const [showStatusForm, setShowStatusForm] = useState(false);
  const [statusEmployee, setStatusEmployee] = useState(null);
  const [statusForm, setStatusForm] = useState({
    status: "",
    reason: "",
  });
  const [statusSubmitting, setStatusSubmitting] = useState(false);
  const [statusError, setStatusError] = useState("");

  const [successMessage, setSuccessMessage] = useState("");

  // Employee status history state
  const [historyEmployeeId, setHistoryEmployeeId] = useState(null);

  // Employee directory filters
  const [departmentFilter, setDepartmentFilter] = useState("");
  const [designationFilter, setDesignationFilter] = useState("");

  // Build filter options dynamically from the employee records.
  const departmentOptions = [
    ...new Set(
      employees
        .map((employee) => employee.department?.trim())
        .filter(Boolean)
    ),
  ].sort((a, b) => a.localeCompare(b));

  const designationOptions = [
    ...new Set(
      employees
        .map((employee) => employee.designation?.trim())
        .filter(Boolean)
    ),
  ].sort((a, b) => a.localeCompare(b));

  // Search by employee ID/name and filter by department/designation.
  const filteredEmployees = employees.filter((employee) => {
    const searchText = (search || "").trim().toLowerCase();
    const employeeId = (employee.employeeId || "").toLowerCase();
    const fullName = (employee.fullName || "").toLowerCase();

    const matchesSearch =
      !searchText ||
      employeeId.includes(searchText) ||
      fullName.includes(searchText);

    const matchesDepartment =
      !departmentFilter || employee.department?.trim() === departmentFilter;

    const matchesDesignation =
      !designationFilter || employee.designation?.trim() === designationFilter;

    return matchesSearch && matchesDepartment && matchesDesignation;
  });

  // Common API response handling
  const getResponseData = async (response) => {
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
        data.detail ||
        data.message ||
        data.title ||
        `Request failed with status ${response.status}`
      );
    }

    return data;
  };

  // Add employee form changes
  const handleChange = (event) => {
    const { name, value } = event.target;

    setForm((previous) => ({
      ...previous,
      [name]: value,
    }));

    if (name === "joiningDate") {
      setSalaryForm((previous) => ({
        ...previous,
        effectiveFrom: value || previous.effectiveFrom || new Date().toLocaleDateString("en-CA"),
      }));
    }
  };

  // Initial Salary Structure form changes
  const handleSalaryChange = (event) => {
    const { name, value } = event.target;

    setSalaryForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const salaryGrossTotal = [
    "basicSalary",
    "hra",
    "conveyanceAllowance",
    "medicalAllowance",
    "specialAllowance",
    "otherAllowances",
  ].reduce((total, field) => total + (Number(salaryForm[field]) || 0), 0);

  const salaryDeductionsTotal = [
    "employeePf",
    "professionalTax",
    "otherDeductions",
  ].reduce((total, field) => total + (Number(salaryForm[field]) || 0), 0);

  // Create employee and initial Salary Structure in one backend request.
  const handleCreateEmployee = async (event) => {
    event.preventDefault();

    setFormError("");
    setSuccessMessage("");

    const expectedGross = Number(form.salary);

    if (!Number.isFinite(expectedGross) || expectedGross <= 0) {
      setFormError("Enter a valid monthly gross salary greater than zero.");
      return;
    }

    if (Number(salaryForm.basicSalary) <= 0) {
      setFormError("Basic salary must be greater than zero.");
      return;
    }

    if (Math.abs(salaryGrossTotal - expectedGross) > 0.009) {
      setFormError(
        `Total earnings (₹${salaryGrossTotal.toFixed(2)}) must equal monthly gross salary (₹${expectedGross.toFixed(2)}).`
      );
      return;
    }

    if (salaryDeductionsTotal > salaryGrossTotal) {
      setFormError("Employee deductions cannot exceed total monthly earnings.");
      return;
    }

    if (!salaryForm.effectiveFrom) {
      setFormError("Salary Structure effective date is required.");
      return;
    }

    setSubmitting(true);

    try {
      const payload = {
        employeeId: form.employeeId.trim(),
        fullName: form.fullName.trim(),
        email: form.email.trim(),
        phone: form.phone.trim() || null,
        department: form.department.trim() || null,
        designation: form.designation.trim() || null,
        joiningDate: form.joiningDate || null,
        salary: expectedGross,
        role: form.role,
        salaryStructure: {
          basicSalary: Number(salaryForm.basicSalary),
          hra: Number(salaryForm.hra),
          conveyanceAllowance: Number(salaryForm.conveyanceAllowance),
          medicalAllowance: Number(salaryForm.medicalAllowance),
          specialAllowance: Number(salaryForm.specialAllowance),
          otherAllowances: Number(salaryForm.otherAllowances),
          employeePf: Number(salaryForm.employeePf),
          employerPf: Number(salaryForm.employerPf),
          professionalTax: Number(salaryForm.professionalTax),
          otherDeductions: Number(salaryForm.otherDeductions),
          effectiveFrom: salaryForm.effectiveFrom,
        },
      };

      const response = await apiFetch(`${API_URL}/api/employees`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify(payload),
      });

      const data = await getResponseData(response);

      setCreatedEmployee(data);
      setForm(initialForm);
      setSalaryForm({
        basicSalary: "",
        hra: "0",
        conveyanceAllowance: "0",
        medicalAllowance: "0",
        specialAllowance: "0",
        otherAllowances: "0",
        employeePf: "0",
        employerPf: "0",
        professionalTax: "0",
        otherDeductions: "0",
        effectiveFrom: "",
      });
      setShowForm(false);

      setSuccessMessage(
        `Employee ${form.employeeId.trim()} and initial Salary Structure created successfully.`
      );

      await onRefresh();
    } catch (error) {
      setFormError(error.message || "Unable to create employee.");
    } finally {
      setSubmitting(false);
    }
  };

  // Load and display the employee's complete salary history.
  const loadSalaryIntoEditForm = (salary) => {
    setSalaryStructureId(salary?.id ?? null);
    setEditSalaryForm({
      basicSalary: String(salary?.basicSalary ?? ""),
      hra: String(salary?.hra ?? 0),
      conveyanceAllowance: String(salary?.conveyanceAllowance ?? 0),
      medicalAllowance: String(salary?.medicalAllowance ?? 0),
      specialAllowance: String(salary?.specialAllowance ?? 0),
      otherAllowances: String(salary?.otherAllowances ?? 0),
      employeePf: String(salary?.employeePf ?? 0),
      employerPf: String(salary?.employerPf ?? 0),
      professionalTax: String(salary?.professionalTax ?? 0),
      otherDeductions: String(salary?.otherDeductions ?? 0),
      // Keep the selected record effective date unchanged during direct editing.
      effectiveFrom: salary?.effectiveFrom || new Date().toLocaleDateString("en-CA"),
    });
  };

  const handleEditEmployee = async (employee) => {
    setEditingEmployeeId(employee.employeeId);

    setEditForm({
      fullName: employee.fullName || "",
      email: employee.email || "",
      phone: employee.phone || "",
      department: employee.department || "",
      designation: employee.designation || "",
      joiningDate: employee.joiningDate
        ? employee.joiningDate.substring(0, 10)
        : "",
    });

    setSalaryHistory([]);
    setSalaryStructureId(null);
    loadSalaryIntoEditForm(null);
    setEditError("");
    setSuccessMessage("");
    setShowEditForm(true);
    setShowStatusForm(false);
    setEditSalaryLoading(true);

    try {
      // This endpoint returns ALL salary records, not only the current one.
      const response = await apiFetch(
        `${API_URL}/api/salary-structures/employee/${encodeURIComponent(
          employee.employeeId
        )}`
      );
      const records = await getResponseData(response);

      if (!Array.isArray(records)) {
        throw new Error("The salary history API did not return a list.");
      }

      const sortedRecords = [...records].sort((a, b) =>
        String(b.effectiveFrom || "").localeCompare(String(a.effectiveFrom || ""))
      );

      setSalaryHistory(sortedRecords);

      if (sortedRecords.length > 0) {
        // Select the newest salary record initially; HR can select any older record.
        loadSalaryIntoEditForm(sortedRecords[0]);
      } else {
        // No salary exists yet. The form remains available to create the first one.
        loadSalaryIntoEditForm(null);
      }
    } catch (error) {
      setSalaryHistory([]);
      setSalaryStructureId(null);
      setEditError(error.message || "Unable to load employee salary history.");
    } finally {
      setEditSalaryLoading(false);
    }
  };

  const handleSelectSalaryHistory = (salary) => {
    loadSalaryIntoEditForm(salary);
    setEditError("");
  };

  // Employee form changes
  const handleEditChange = (event) => {
    const { name, value } = event.target;

    setEditForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  // Salary form changes
  const handleEditSalaryChange = (event) => {
    const { name, value } = event.target;

    setEditSalaryForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  const editSalaryGross = [
    "basicSalary",
    "hra",
    "conveyanceAllowance",
    "medicalAllowance",
    "specialAllowance",
    "otherAllowances",
  ].reduce((total, field) => total + (Number(editSalaryForm[field]) || 0), 0);

  const editSalaryDeductions = [
    "employeePf",
    "professionalTax",
    "otherDeductions",
  ].reduce((total, field) => total + (Number(editSalaryForm[field]) || 0), 0);

  const editSalaryNet = editSalaryGross - editSalaryDeductions;
  const editTotalEmployerCost = editSalaryGross + (Number(editSalaryForm.employerPf) || 0);

  // Update employee details and create a salary revision.
  const handleUpdateEmployee = async (event) => {
    event.preventDefault();

    setEditError("");
    setSuccessMessage("");

    if (editSalaryLoading) {
      setEditError("Please wait until the current salary structure finishes loading.");
      return;
    }

    if (Number(editSalaryForm.basicSalary) <= 0) {
      setEditError("Basic salary must be greater than zero.");
      return;
    }

    if (editSalaryDeductions > editSalaryGross) {
      setEditError("Employee deductions cannot exceed total monthly earnings.");
      return;
    }

    if (!salaryStructureId && !editSalaryForm.effectiveFrom) {
      setEditError("Salary effective date is required for the initial salary structure.");
      return;
    }

    setEditSubmitting(true);

    try {
      const employeePayload = {
        fullName: editForm.fullName.trim(),
        email: editForm.email.trim(),
        phone: editForm.phone.trim() || null,
        department: editForm.department.trim() || null,
        designation: editForm.designation.trim() || null,
        joiningDate: editForm.joiningDate || null,
      };

      const employeeResponse = await apiFetch(
        `${API_URL}/api/employees/${encodeURIComponent(editingEmployeeId)}`,
        {
          method: "PUT",
          headers: { "Content-Type": "application/json" },
          body: JSON.stringify(employeePayload),
        }
      );
      await getResponseData(employeeResponse);

      const salaryPayload = {
        basicSalary: Number(editSalaryForm.basicSalary),
        hra: Number(editSalaryForm.hra),
        conveyanceAllowance: Number(editSalaryForm.conveyanceAllowance),
        medicalAllowance: Number(editSalaryForm.medicalAllowance),
        specialAllowance: Number(editSalaryForm.specialAllowance),
        otherAllowances: Number(editSalaryForm.otherAllowances),
        employeePf: Number(editSalaryForm.employeePf),
        employerPf: Number(editSalaryForm.employerPf),
        professionalTax: Number(editSalaryForm.professionalTax),
        otherDeductions: Number(editSalaryForm.otherDeductions),
        effectiveFrom: editSalaryForm.effectiveFrom,
      };

      // Existing record: update that exact record without changing its ID or dates.
      // No existing record: create the employee's first salary structure.
      const salaryUrl = salaryStructureId
        ? `${API_URL}/api/salary-structures/${salaryStructureId}/edit`
        : `${API_URL}/api/salary-structures`;

      const salaryResponse = await apiFetch(salaryUrl, {
        method: salaryStructureId ? "PUT" : "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          ...salaryPayload,
          employeeId: editingEmployeeId,
        }),
      });
      await getResponseData(salaryResponse);

      setShowEditForm(false);
      setSuccessMessage("Employee details and salary structure updated successfully!");
      await onRefresh();
    } catch (error) {
      setEditError(error.message || "Unable to update employee and salary details.");
    } finally {
      setEditSubmitting(false);
    }
  };

  // Open manage status popup
  const handleManageStatus = (employee) => {
    setStatusEmployee(employee);

    setStatusForm({
      status: employee.status || "ACTIVE",
      reason: "",
    });

    setStatusError("");
    setSuccessMessage("");
    setShowStatusForm(true);
    setShowEditForm(false);
  };

  // Status form changes
  const handleStatusChange = (event) => {
    const { name, value } = event.target;

    setStatusForm((previous) => ({
      ...previous,
      [name]: value,
    }));
  };

  // Update employee status
  const handleUpdateStatus = async (event) => {
    event.preventDefault();

    if (!statusEmployee) {
      return;
    }

    setStatusError("");
    setStatusSubmitting(true);

    try {
      const payload = {
        status: statusForm.status,
        reason: statusForm.reason.trim(),
      };

      const response = await apiFetch(
        `${API_URL}/api/employees/${encodeURIComponent(
          statusEmployee.employeeId
        )}/status`,
        {
          method: "PATCH",
          headers: {
            "Content-Type": "application/json",
          },
          body: JSON.stringify(payload),
        }
      );

      await getResponseData(response);

      const updatedStatus = statusForm.status;

      setShowStatusForm(false);
      setStatusEmployee(null);

      setSuccessMessage(
        `Employee status updated to ${updatedStatus} successfully!`
      );

      await onRefresh();
    } catch (error) {
      setStatusError(
        error.message || "Unable to update employee status."
      );
    } finally {
      setStatusSubmitting(false);
    }
  };

  // Close employee creation success message
  const closeSuccess = () => {
    setCreatedEmployee(null);
  };

  // Status badge styling
  const getStatusClass = (status) => {
    switch (status?.toUpperCase()) {
      case "ACTIVE":
        return "status-active";

      case "ON_NOTICE":
        return "status-on-notice";

      case "RESIGNED":
        return "status-resigned";

      case "TERMINATED":
        return "status-terminated";

      case "INACTIVE":
        return "status-inactive";

      default:
        return "status-inactive";
    }
  };

  return (
    <div className="employee-management">

      {/* PAGE HEADER */}
      <div className="employee-page-heading">
        <div>
          <span className="employee-eyebrow">
            OPSOFT • PEOPLE MANAGEMENT
          </span>

          <h1>Employee Management</h1>

          <p>Manage, register and update employee records.</p>
        </div>

        <div className="employee-header-actions">
          <button
            className="employee-add-button"
            onClick={() => {
              setFormError("");
              setForm(initialForm);
              setSalaryForm({
                basicSalary: "",
                hra: "0",
                conveyanceAllowance: "0",
                medicalAllowance: "0",
                specialAllowance: "0",
                otherAllowances: "0",
                employeePf: "0",
                employerPf: "0",
                professionalTax: "0",
                otherDeductions: "0",
                effectiveFrom: new Date().toLocaleDateString("en-CA"),
              });
              setShowForm(true);
            }}
          >
            + Add Employee
          </button>

          <button
            className="employee-refresh-button"
            onClick={onRefresh}
          >
            ↻ Refresh
          </button>
        </div>
      </div>

      {/* SUMMARY */}
      <div className="employee-summary">
        <div className="employee-summary-icon">👥</div>

        <div>
          <span>Total Employees</span>
          <h2>{employees.length}</h2>
        </div>
      </div>

      {/* SUCCESS MESSAGE */}
      {successMessage && (
        <div className="employee-update-success">
          ✓ {successMessage}

          <button
            type="button"
            onClick={() => setSuccessMessage("")}
          >
            ×
          </button>
        </div>
      )}

      {/* EDIT EMPLOYEE FORM */}
      {showEditForm && (
        <div className="employee-form-card employee-edit-card">

          <div className="employee-form-heading">
            <div>
              <h2>Edit Employee Details</h2>
              <p>
                Update employee information for {editingEmployeeId}.
              </p>
            </div>

            <button
              className="employee-close-button"
              type="button"
              onClick={() => setShowEditForm(false)}
            >
              ×
            </button>
          </div>

          {editError && (
            <div className="employee-form-error">
              {editError}
            </div>
          )}

          <form onSubmit={handleUpdateEmployee}>
            <div className="employee-form-grid">

              <div className="employee-form-group">
                <label>Employee ID</label>
                <input value={editingEmployeeId} disabled />
              </div>

              <div className="employee-form-group">
                <label>Full Name *</label>
                <input
                  name="fullName"
                  value={editForm.fullName || ""}
                  onChange={handleEditChange}
                  placeholder="Enter full name"
                  maxLength={100}
                  required
                />
              </div>

              <div className="employee-form-group">
                <label>Email Address *</label>
                <input
                  type="email"
                  name="email"
                  value={editForm.email || ""}
                  onChange={handleEditChange}
                  placeholder="employee@opsoft.com"
                  maxLength={150}
                  required
                />
              </div>

              <div className="employee-form-group">
                <label>Phone Number</label>
                <input
                  type="tel"
                  name="phone"
                  value={editForm.phone || ""}
                  onChange={handleEditChange}
                  placeholder="Enter phone number"
                  maxLength={20}
                />
              </div>

              <div className="employee-form-group">
                <label>Department</label>
                <input
                  name="department"
                  value={editForm.department || ""}
                  onChange={handleEditChange}
                  placeholder="Example: IT"
                  maxLength={100}
                />
              </div>

              <div className="employee-form-group">
                <label>Designation</label>
                <input
                  name="designation"
                  value={editForm.designation || ""}
                  onChange={handleEditChange}
                  placeholder="Example: Java Developer"
                  maxLength={100}
                />
              </div>

              <div className="employee-form-group">
                <label>Joining Date</label>
                <input
                  type="date"
                  name="joiningDate"
                  value={editForm.joiningDate || ""}
                  onChange={handleEditChange}
                />
              </div>

            </div>

            <div className="employee-form-section">
              <h3>Edit Salary Structure</h3>
              <p>
                Select any previous salary record to edit its salary components directly.
                Its record ID and effective dates will remain unchanged.
              </p>

              {editSalaryLoading ? (
                <div className="employee-form-note">Loading employee salary history...</div>
              ) : (
                <>
                  <h3>Previous Salary Records</h3>
                  {salaryHistory.length > 0 ? (
                    <div className="employee-table-wrapper">
                      <table className="employee-data-table">
                        <thead>
                          <tr>
                            <th>Effective From</th>
                            <th>Effective To</th>
                            <th>Basic Salary</th>
                            <th>Gross Salary</th>
                            <th>Net Salary</th>
                            <th>Action</th>
                          </tr>
                        </thead>
                        <tbody>
                          {salaryHistory.map((record) => (
                            <tr key={record.id}>
                              <td>{record.effectiveFrom || "-"}</td>
                              <td>{record.effectiveTo || "Current / Open"}</td>
                              <td>₹{Number(record.basicSalary || 0).toLocaleString("en-IN")}</td>
                              <td>₹{Number(record.grossSalary || 0).toLocaleString("en-IN")}</td>
                              <td>₹{Number(record.netSalary || 0).toLocaleString("en-IN")}</td>
                              <td>
                                <button
                                  type="button"
                                  className="employee-edit-button"
                                  onClick={() => handleSelectSalaryHistory(record)}
                                >
                                  {salaryStructureId === record.id ? "Selected" : "View / Edit"}
                                </button>
                              </td>
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  ) : (
                    <div className="employee-form-note">
                      No previous salary records found. Enter the initial salary structure below.
                    </div>
                  )}

                  <h3>
                    {salaryStructureId
                      ? `Edit Selected Salary Record #${salaryStructureId}`
                      : "Create Initial Salary Structure"}
                  </h3>
                  <p>
                    {salaryStructureId
                      ? "The selected salary record will be updated directly. Its ID and effective dates will remain unchanged."
                      : "Enter the first salary structure for this employee."}
                  </p>

                  <div className="employee-form-grid">
                    {[
                      ["basicSalary", "Basic Salary *"],
                      ["hra", "HRA"],
                      ["conveyanceAllowance", "Conveyance Allowance"],
                      ["medicalAllowance", "Medical Allowance"],
                      ["specialAllowance", "Special Allowance"],
                      ["otherAllowances", "Other Allowances"],
                    ].map(([name, label]) => (
                      <div className="employee-form-group" key={name}>
                        <label>{label}</label>
                        <input
                          type="number"
                          name={name}
                          value={editSalaryForm[name]}
                          onChange={handleEditSalaryChange}
                          min={name === "basicSalary" ? "0.01" : "0"}
                          step="0.01"
                          required
                        />
                      </div>
                    ))}
                  </div>

                  <div className="employee-form-note">
                    Gross Monthly Salary: ₹{editSalaryGross.toLocaleString("en-IN", {
                      minimumFractionDigits: 2,
                      maximumFractionDigits: 2,
                    })}
                  </div>

                  <h3>Monthly Deductions and Employer Contribution</h3>
                  <div className="employee-form-grid">
                    {[
                      ["employeePf", "Employee PF"],
                      ["employerPf", "Employer PF"],
                      ["professionalTax", "Professional Tax"],
                      ["otherDeductions", "Other Deductions"],
                    ].map(([name, label]) => (
                      <div className="employee-form-group" key={name}>
                        <label>{label}</label>
                        <input
                          type="number"
                          name={name}
                          value={editSalaryForm[name]}
                          onChange={handleEditSalaryChange}
                          min="0"
                          step="0.01"
                          required
                        />
                      </div>
                    ))}

                    <div className="employee-form-group">
                      <label>{salaryStructureId ? "Effective From (unchanged)" : "Effective From *"}</label>
                      <input
                        type="date"
                        name="effectiveFrom"
                        value={editSalaryForm.effectiveFrom}
                        onChange={handleEditSalaryChange}
                        disabled={Boolean(salaryStructureId)}
                        required={!salaryStructureId}
                      />
                    </div>
                  </div>

                  <div className="employee-form-note">
                    <strong>Gross Salary:</strong> ₹{editSalaryGross.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    {" | "}<strong>Total Employee Deductions:</strong> ₹{editSalaryDeductions.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    {" | "}<strong>Net Salary:</strong> ₹{editSalaryNet.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                    {" | "}<strong>Total Employer Cost:</strong> ₹{editTotalEmployerCost.toLocaleString("en-IN", { minimumFractionDigits: 2, maximumFractionDigits: 2 })}
                  </div>

                  <div className="employee-form-note">
                    Employer PF is not deducted from employee net pay. Existing salary records are updated in place; use the separate salary revision feature to record a new salary period.
                  </div>
                </>
              )}
            </div>

            <div className="employee-form-note">
              Employee ID, role, status and password are not editable here.
            </div>

            <div className="employee-form-actions">
              <button
                type="button"
                className="employee-cancel-button"
                onClick={() => setShowEditForm(false)}
              >
                Cancel
              </button>

              <button
                type="submit"
                className="employee-submit-button"
                disabled={editSubmitting || editSalaryLoading}
              >
                {editSubmitting ? "Saving Changes..." : "Save Employee & Salary"}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* MANAGE EMPLOYEE STATUS */}
      {showStatusForm && statusEmployee && (
        <div className="employee-status-overlay">
          <div
            className="employee-status-modal"
            role="dialog"
            aria-modal="true"
            aria-labelledby="employee-status-title"
          >
            <div className="employee-form-heading">
              <div>
                <h2 id="employee-status-title">
                  Manage Employee Status
                </h2>

                <p>
                  {statusEmployee.fullName} ({statusEmployee.employeeId})
                </p>
              </div>

              <button
                className="employee-close-button"
                type="button"
                disabled={statusSubmitting}
                onClick={() => setShowStatusForm(false)}
              >
                ×
              </button>
            </div>

            {statusError && (
              <div className="employee-form-error">
                {statusError}
              </div>
            )}

            <form onSubmit={handleUpdateStatus}>
              <div className="employee-form-group">
                <label>Current Status</label>

                <input
                  value={statusEmployee.status || "Unknown"}
                  disabled
                />
              </div>

              <div className="employee-form-group employee-status-select-group">
                <label>New Employee Status *</label>

                <select
                  name="status"
                  value={statusForm.status}
                  onChange={handleStatusChange}
                  required
                >
                  {employeeStatuses.map((status) => (
                    <option key={status} value={status}>
                      {status.replace("_", " ")}
                    </option>
                  ))}
                </select>
              </div>

              <div className="employee-form-group employee-status-reason">
                <label>Reason for Status Change</label>

                <textarea
                  name="reason"
                  value={statusForm.reason}
                  onChange={handleStatusChange}
                  placeholder="Enter the reason for changing employee status..."
                  maxLength={500}
                  rows={4}
                />

                <small>Maximum 500 characters.</small>
              </div>

              <div className="employee-status-information">
                <strong>Important:</strong>
                <p>
                  Employees with ACTIVE or ON NOTICE status can log in.
                  RESIGNED, TERMINATED and INACTIVE employees cannot
                  log in after their next authentication.
                </p>
              </div>

              <div className="employee-form-actions">
                <button
                  type="button"
                  className="employee-cancel-button"
                  disabled={statusSubmitting}
                  onClick={() => setShowStatusForm(false)}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="employee-submit-button"
                  disabled={
                    statusSubmitting ||
                    statusForm.status === statusEmployee.status
                  }
                >
                  {statusSubmitting
                    ? "Updating Status..."
                    : "Save Status"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* EMPLOYEE REGISTRATION FORM */}
      {showForm && (
        <div className="employee-form-card">
          <div className="employee-form-heading">
            <div>
              <h2>Register New Employee</h2>
              <p>Enter employee details and complete the initial Salary Structure before saving.</p>
            </div>

            <button
              className="employee-close-button"
              type="button"
              onClick={() => setShowForm(false)}
            >
              ×
            </button>
          </div>

          {formError && (
            <div className="employee-form-error">
              {formError}
            </div>
          )}

          <form onSubmit={handleCreateEmployee}>
            <div className="employee-form-grid">

              <div className="employee-form-group">
                <label>Employee ID *</label>
                <input
                  name="employeeId"
                  value={form.employeeId}
                  onChange={handleChange}
                  placeholder="Example: OPS001"
                  maxLength={30}
                  required
                />
              </div>

              <div className="employee-form-group">
                <label>Full Name *</label>
                <input
                  name="fullName"
                  value={form.fullName}
                  onChange={handleChange}
                  placeholder="Enter full name"
                  maxLength={100}
                  required
                />
              </div>

              <div className="employee-form-group">
                <label>Email Address *</label>
                <input
                  type="email"
                  name="email"
                  value={form.email}
                  onChange={handleChange}
                  placeholder="employee@opsoft.com"
                  maxLength={150}
                  required
                />
              </div>

              <div className="employee-form-group">
                <label>Phone Number</label>
                <input
                  type="tel"
                  name="phone"
                  value={form.phone}
                  onChange={handleChange}
                  placeholder="Enter phone number"
                  maxLength={20}
                />
              </div>

              <div className="employee-form-group">
                <label>Department</label>
                <input
                  name="department"
                  value={form.department}
                  onChange={handleChange}
                  placeholder="Example: IT"
                  maxLength={100}
                />
              </div>

              <div className="employee-form-group">
                <label>Designation</label>
                <input
                  name="designation"
                  value={form.designation}
                  onChange={handleChange}
                  placeholder="Example: Java Developer"
                  maxLength={100}
                />
              </div>

              <div className="employee-form-group">
                <label>Joining Date</label>
                <input
                  type="date"
                  name="joiningDate"
                  value={form.joiningDate}
                  onChange={handleChange}
                />
              </div>

              <div className="employee-form-group">
                <label>Monthly Gross Salary *</label>
                <input
                  type="number"
                  name="salary"
                  value={form.salary}
                  onChange={handleChange}
                  placeholder="Enter monthly gross salary"
                  min="0.01"
                  step="0.01"
                  required
                />
              </div>

              <div className="employee-form-group">
                <label>Employee Role *</label>
                <select
                  name="role"
                  value={form.role}
                  onChange={handleChange}
                  required
                >
                  <option value="EMPLOYEE">Employee</option>
                  <option value="HR">HR</option>
                  <option value="ADMIN">Admin</option>
                </select>
              </div>
            </div>

            <div className="employee-form-section">
              <h3>Initial Salary Structure</h3>
              <p>
                Complete the salary components before registering the employee.
                The total monthly earnings must match the monthly gross salary above.
              </p>

              <div className="employee-form-grid">
                {[
                  ["basicSalary", "Basic Salary *"],
                  ["hra", "HRA"],
                  ["conveyanceAllowance", "Conveyance Allowance"],
                  ["medicalAllowance", "Medical Allowance"],
                  ["specialAllowance", "Special Allowance"],
                  ["otherAllowances", "Other Allowances"],
                ].map(([name, label]) => (
                  <div className="employee-form-group" key={name}>
                    <label>{label}</label>
                    <input
                      type="number"
                      name={name}
                      value={salaryForm[name]}
                      onChange={handleSalaryChange}
                      min={name === "basicSalary" ? "0.01" : "0"}
                      step="0.01"
                      required
                    />
                  </div>
                ))}
              </div>

              <div className="employee-form-note">
                <strong>
                  Total Monthly Earnings: ₹
                  {salaryGrossTotal.toLocaleString("en-IN", {
                    minimumFractionDigits: 2,
                    maximumFractionDigits: 2,
                  })}
                </strong>
                {" "} | Monthly Gross Salary: ₹
                {(Number(form.salary) || 0).toLocaleString("en-IN", {
                  minimumFractionDigits: 2,
                  maximumFractionDigits: 2,
                })}
              </div>

              <h3>Monthly Deductions and Employer Contribution</h3>
              <div className="employee-form-grid">
                {[
                  ["employeePf", "Employee PF"],
                  ["employerPf", "Employer PF"],
                  ["professionalTax", "Professional Tax"],
                  ["otherDeductions", "Other Deductions"],
                ].map(([name, label]) => (
                  <div className="employee-form-group" key={name}>
                    <label>{label}</label>
                    <input
                      type="number"
                      name={name}
                      value={salaryForm[name]}
                      onChange={handleSalaryChange}
                      min="0"
                      step="0.01"
                      required
                    />
                  </div>
                ))}

                <div className="employee-form-group">
                  <label>Effective From *</label>
                  <input
                    type="date"
                    name="effectiveFrom"
                    value={salaryForm.effectiveFrom || form.joiningDate}
                    onChange={handleSalaryChange}
                    required
                  />
                </div>
              </div>

              <div className="employee-form-note">
                Employee PF, professional tax and other deductions reduce
                employee net pay. Employer PF is recorded separately and is
                not deducted from employee net pay.
              </div>
            </div>

            <div className="employee-form-note">
              A random temporary password will be generated
              automatically. The employee must change it during
              their first login.
            </div>

            <div className="employee-form-actions">
              <button
                type="button"
                className="employee-cancel-button"
                onClick={() => setShowForm(false)}
              >
                Cancel
              </button>

              <button
                type="submit"
                className="employee-submit-button"
                disabled={submitting}
              >
                {submitting ? "Creating Employee..." : "Create Employee & Salary Structure"}
              </button>
            </div>
          </form>
        </div>
      )}

      {/* CREATED EMPLOYEE SUCCESS */}
      {createdEmployee && (
        <div className="employee-success-card">
          <div className="employee-success-icon">✓</div>

          <h2>Employee Created Successfully!</h2>

          <p>
            The new employee has been saved to the database.
          </p>

          <div className="employee-credential-row">
            <span>Employee ID</span>
            <strong>{createdEmployee.employeeId}</strong>
          </div>

          <div className="employee-credential-row">
            <span>Temporary Password</span>
            <strong className="temporary-password">
              {createdEmployee.temporaryPassword}
            </strong>
          </div>

          <button
            className="employee-copy-button"
            onClick={() =>
              navigator.clipboard.writeText(
                `Employee ID: ${createdEmployee.employeeId}\nTemporary Password: ${createdEmployee.temporaryPassword}`
              )
            }
          >
            Copy Credentials
          </button>

          <p className="employee-security-note">
            Save these credentials securely. This temporary password
            is displayed only once.
          </p>

          <button
            className="employee-submit-button"
            onClick={closeSuccess}
          >
            Done
          </button>
        </div>
      )}

      {/* EMPLOYEE DIRECTORY */}
      <div className="employee-table-card">
        <div className="employee-table-heading">
          <div>
            <h2>Employee Directory</h2>
            <p>Search and filter employees by name, ID, department or designation.</p>
          </div>

          <div
            className="employee-directory-filters"
            style={{
              display: "flex",
              gap: "10px",
              flexWrap: "wrap",
              alignItems: "center",
            }}
          >
            <input
              type="text"
              className="employee-search-input"
              placeholder="Search by ID or name..."
              value={search}
              onChange={(event) => setSearch(event.target.value)}
            />

            <select
              value={departmentFilter}
              onChange={(event) => setDepartmentFilter(event.target.value)}
              aria-label="Filter by department"
              style={{
                padding: "10px",
                border: "1px solid #dce2ea",
                borderRadius: "8px",
                minWidth: "150px",
                background: "#fff",
              }}
            >
              <option value="">All Departments</option>
              {departmentOptions.map((department) => (
                <option key={department} value={department}>
                  {department}
                </option>
              ))}
            </select>

            <select
              value={designationFilter}
              onChange={(event) => setDesignationFilter(event.target.value)}
              aria-label="Filter by designation"
              style={{
                padding: "10px",
                border: "1px solid #dce2ea",
                borderRadius: "8px",
                minWidth: "150px",
                background: "#fff",
              }}
            >
              <option value="">All Designations</option>
              {designationOptions.map((designation) => (
                <option key={designation} value={designation}>
                  {designation}
                </option>
              ))}
            </select>

            <button
              type="button"
              className="employee-refresh-button"
              onClick={() => {
                setSearch("");
                setDepartmentFilter("");
                setDesignationFilter("");
              }}
            >
              Clear Filters
            </button>
          </div>
        </div>

        <div className="employee-table-wrapper">
          <table className="employee-data-table">
            <thead>
              <tr>
                <th>Employee ID</th>
                <th>Full Name</th>
                <th>Email</th>
                <th>Department</th>
                <th>Designation</th>
                <th>Role</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              {filteredEmployees.length > 0 ? (
                filteredEmployees.map((employee) => (
                  <tr key={employee.employeeId}>
                    <td className="employee-id-cell">
                      {employee.employeeId}
                    </td>

                    <td>{employee.fullName}</td>

                    <td>{employee.email}</td>

                    <td>{employee.department || "-"}</td>

                    <td>{employee.designation || "-"}</td>

                    <td>
                      <span className="employee-role">
                        {employee.role}
                      </span>
                    </td>

                    <td>
                      <span
                        className={`employee-status ${getStatusClass(
                          employee.status
                        )}`}
                      >
                        {(employee.status || "UNKNOWN").replace("_", " ")}
                      </span>
                    </td>

                    <td>
                      <div className="employee-action-buttons">
                        <button
                          type="button"
                          className="employee-edit-button"
                          onClick={() => handleEditEmployee(employee)}
                        >
                          ✎ Edit
                        </button>

                        <button
                          type="button"
                          className="employee-manage-status-button"
                          onClick={() => handleManageStatus(employee)}
                        >
                          Manage Status
                        </button>

                        <button
                          type="button"
                          className="employee-history-button"
                          onClick={() =>
                            setHistoryEmployeeId(employee.employeeId)
                          }
                        >
                          View History
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              ) : (
                <tr>
                  <td colSpan="8" className="employee-empty">
                    {employees.length === 0
                      ? "No employee records found."
                      : "No matching employees found."}
                  </td>
                </tr>
              )}
            </tbody>
          </table>
        </div>

        <div className="employee-table-footer">
          Showing {filteredEmployees.length} of {employees.length} employees
        </div>
      </div>

      {/* EMPLOYEE STATUS HISTORY */}
      {historyEmployeeId && (
        <StatusHistory
          employeeId={historyEmployeeId}
          onClose={() => setHistoryEmployeeId(null)}
        />
      )}
    </div>
  );
}

export default EmployeeManagement;
