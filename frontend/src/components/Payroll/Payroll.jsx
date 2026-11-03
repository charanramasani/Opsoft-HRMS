
import React, { useEffect, useMemo, useState } from "react";
import "./Payroll.css";

const API_URL = "http://localhost:8083/api/payroll";

function Payroll({ user }) {
  const isAdminOrHR =
    user?.role === "ADMIN" || user?.role === "HR";

  const isEmployee = user?.role === "EMPLOYEE";

  const [payrollRecords, setPayrollRecords] = useState([]);
  const [components, setComponents] = useState([]);
  const [history, setHistory] = useState([]);
  const [myPayslips, setMyPayslips] = useState([]);

  const [selectedPayroll, setSelectedPayroll] = useState(null);
  const [search, setSearch] = useState("");
  const [statusFilter, setStatusFilter] = useState("ALL");
  const [month, setMonth] = useState("");

  const [changeReason, setChangeReason] = useState("");

  const [loading, setLoading] = useState(false);
  const [generating, setGenerating] = useState(false);
  const [generationReport, setGenerationReport] = useState(null);
  const [saving, setSaving] = useState(false);
  const [publishingId, setPublishingId] = useState(null);
  const [publishingAll, setPublishingAll] = useState(false);
  const [payingId, setPayingId] = useState(null);
  const [downloadingId, setDownloadingId] = useState(null);

  const [message, setMessage] = useState("");
  const [error, setError] = useState("");

  const [showEditor, setShowEditor] = useState(false);
  const [showHistory, setShowHistory] = useState(false);
  const [showComponents, setShowComponents] = useState(false);

  const [editComponents, setEditComponents] = useState([]);

  // =====================================================
  // JWT AUTHENTICATED API REQUEST
  // =====================================================

  const apiFetch = async (url, options = {}) => {
    const token = localStorage.getItem("hrmsToken");

    if (!token) {
      throw new Error("Your login session has expired. Please log in again.");
    }

    const headers = {
      ...options.headers,
      Authorization: `Bearer ${token}`
    };

    const response = await fetch(url, {
      ...options,
      headers,
      credentials: "omit"
    });

    if (response.status === 401) {
      throw new Error("Session expired or authentication failed. Please log in again.");
    }

    if (response.status === 403) {
      throw new Error("You do not have permission to perform this action.");
    }

    return response;
  };

  const readResponse = async (response, fallback) => {
    const data = await response.json().catch(() => fallback);

    if (!response.ok) {
      throw new Error(
        data?.message ||
        data?.error ||
        `Request failed with status ${response.status}.`
      );
    }

    return data;
  };

  // =====================================================
  // FORMATTING
  // =====================================================

  const formatCurrency = (value) =>
    new Intl.NumberFormat("en-IN", {
      style: "currency",
      currency: "INR",
      maximumFractionDigits: 2
    }).format(Number(value || 0));

  const formatDate = (value) => {
    if (!value) return "-";

    const date = new Date(`${value.toString().slice(0, 10)}T00:00:00`);

    if (Number.isNaN(date.getTime())) return value;

    return date.toLocaleDateString("en-IN", {
      day: "2-digit",
      month: "short",
      year: "numeric"
    });
  };

  const formatMonth = (value) => {
    if (!value) return "-";

    const date = new Date(`${value.toString().slice(0, 10)}T00:00:00`);

    if (Number.isNaN(date.getTime())) return value;

    return date.toLocaleDateString("en-IN", {
      month: "long",
      year: "numeric"
    });
  };

  const getStatusClass = (status) => {
    if (status === "PAID") return "payroll-paid";
    if (status === "PUBLISHED") return "payroll-published";
    return "payroll-generated";
  };

  // =====================================================
  // LOAD HR / ADMIN PAYROLL
  // =====================================================

  const fetchPayroll = async () => {
    if (!isAdminOrHR) return;

    setLoading(true);
    setError("");

    try {
      const response = await apiFetch(API_URL);
      const data = await readResponse(response, []);

      setPayrollRecords(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // =====================================================
  // LOAD EMPLOYEE'S OWN PUBLISHED PAYSLIPS
  // =====================================================

  const fetchMyPayslips = async () => {
    if (!isEmployee) return;

    setLoading(true);
    setError("");

    try {
      const response = await apiFetch(`${API_URL}/my-payslips`);
      const data = await readResponse(response, []);

      setMyPayslips(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // =====================================================
  // EMPLOYEE: DOWNLOAD OWN PAYSLIP AS PDF
  // =====================================================

  const handleDownloadPayslip = async (record) => {
    setError("");
    setMessage("");
    setDownloadingId(record.id);

    try {
      const response = await apiFetch(
        `${API_URL}/my-payslips/${record.id}/download`
      );

      if (!response.ok) {
        let errorMessage = `Payslip download failed (${response.status}).`;

        try {
          const errorData = await response.json();
          errorMessage =
            errorData?.message ||
            errorData?.error ||
            errorMessage;
        } catch {
          // The server may return a non-JSON error response.
        }

        throw new Error(errorMessage);
      }

      const pdfBlob = await response.blob();

      if (!pdfBlob.size) {
        throw new Error("The server returned an empty PDF file.");
      }

      const downloadUrl = window.URL.createObjectURL(pdfBlob);
      const link = document.createElement("a");

      const employeeId = String(record.employeeId || "Employee")
        .replace(/[^a-zA-Z0-9_-]/g, "_");
      const payrollMonth = String(record.payrollMonth || "Payslip")
        .slice(0, 10)
        .replace(/[^a-zA-Z0-9_-]/g, "_");

      link.href = downloadUrl;
      link.download = `Payslip-${employeeId}-${payrollMonth}.pdf`;

      document.body.appendChild(link);
      link.click();
      link.remove();

      window.setTimeout(() => {
        window.URL.revokeObjectURL(downloadUrl);
      }, 1000);

      setMessage("Payslip PDF downloaded successfully.");
    } catch (err) {
      setError(err.message || "Unable to download payslip.");
    } finally {
      setDownloadingId(null);
    }
  };

  useEffect(() => {
    if (isAdminOrHR) {
      fetchPayroll();
    } else if (isEmployee) {
      fetchMyPayslips();
    }
  }, [isAdminOrHR, isEmployee]);

  // =====================================================
  // GENERATE MONTHLY PAYROLL
  // =====================================================

  const handleGeneratePayroll = async (event) => {
    event.preventDefault();

    setMessage("");
    setError("");

    if (!month) {
      setError("Please select a payroll month.");
      return;
    }

    const confirmed = window.confirm(
      `Generate payroll for ${month}? Existing payroll records will be skipped.`
    );

    if (!confirmed) return;

    setGenerating(true);

    try {
      const response = await apiFetch(`${API_URL}/generate`, {
        method: "POST",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          payrollMonth: `${month}-01`
        })
      });

      const data = await readResponse(response, {});

      // New backend response: generatedCount, skippedCount,
      // generatedPayrolls and skippedEmployees.
      // The Array.isArray fallback keeps compatibility with the old API response.
      const generatedPayrolls = Array.isArray(data)
        ? data
        : Array.isArray(data.generatedPayrolls)
          ? data.generatedPayrolls
          : [];

      const skippedEmployees = Array.isArray(data.skippedEmployees)
        ? data.skippedEmployees
        : [];

      const generatedCount = Number(
        data.generatedCount ?? generatedPayrolls.length
      );

      const skippedCount = Number(
        data.skippedCount ?? skippedEmployees.length
      );

      setGenerationReport({
        generatedCount,
        skippedCount,
        generatedPayrolls,
        skippedEmployees
      });

      setMessage(
        `Payroll generation completed: ${generatedCount} generated, ${skippedCount} skipped.`
      );

      await fetchPayroll();
    } catch (err) {
      setError(err.message);
    } finally {
      setGenerating(false);
    }
  };

  // =====================================================
  // PUBLISH ONE PAYSLIP
  // =====================================================

  const handlePublish = async (record) => {
    setError("");
    setMessage("");

    const confirmed = window.confirm(
      `Publish the payslip for ${record.employeeName || record.employeeId} for ${formatMonth(record.payrollMonth)}?`
    );

    if (!confirmed) return;

    setPublishingId(record.id);

    try {
      const response = await apiFetch(
        `${API_URL}/${record.id}/publish`,
        {
          method: "PATCH"
        }
      );

      await readResponse(response, {});

      setMessage(
        `Payslip published successfully for ${record.employeeName || record.employeeId}.`
      );

      await fetchPayroll();
    } catch (err) {
      setError(err.message);
    } finally {
      setPublishingId(null);
    }
  };

  // =====================================================
  // PUBLISH ALL PAYSLIPS FOR SELECTED MONTH
  // =====================================================

  const handlePublishAll = async () => {
    setError("");
    setMessage("");

    if (!month) {
      setError("Please select the payroll month before publishing.");
      return;
    }

    const confirmed = window.confirm(
      `Publish all GENERATED payslips for ${month}?`
    );

    if (!confirmed) return;

    setPublishingAll(true);

    try {
      const response = await apiFetch(
        `${API_URL}/publish-all?payrollMonth=${encodeURIComponent(`${month}-01`)}`,
        {
          method: "PATCH"
        }
      );

      const data = await readResponse(response, []);

      setMessage(
        Array.isArray(data)
          ? `${data.length} payslip(s) published successfully.`
          : "Publish All operation completed."
      );

      await fetchPayroll();
    } catch (err) {
      setError(err.message);
    } finally {
      setPublishingAll(false);
    }
  };

  // =====================================================
  // MARK PAYROLL AS PAID
  // =====================================================

  const handleMarkAsPaid = async (record) => {
    setError("");
    setMessage("");

    if (record.paymentStatus !== "PUBLISHED") {
      setError("Only published payslips can be marked as paid.");
      return;
    }

    const confirmed = window.confirm(
      `Mark the payroll for ${record.employeeName || record.employeeId} as PAID?`
    );

    if (!confirmed) return;

    setPayingId(record.id);

    try {
      const response = await apiFetch(
        `${API_URL}/${record.id}/pay`,
        {
          method: "PATCH"
        }
      );

      await readResponse(response, {});

      setMessage("Payroll marked as PAID successfully.");

      await fetchPayroll();
    } catch (err) {
      setError(err.message);
    } finally {
      setPayingId(null);
    }
  };

  // =====================================================
  // OPEN PAYROLL EDITOR
  // =====================================================

  const openEditor = async (record) => {
    setError("");
    setMessage("");

    if (record.paymentStatus !== "GENERATED") {
      setError("Only GENERATED payroll records can be edited.");
      return;
    }

    setSelectedPayroll(record);
    setShowHistory(false);
    setShowComponents(false);
    setShowEditor(false);
    setLoading(true);

    try {
      const response = await apiFetch(
        `${API_URL}/${record.id}/components`
      );

      const data = await readResponse(response, []);

      setComponents(Array.isArray(data) ? data : []);

      setEditComponents(
        (Array.isArray(data) ? data : []).map((item) => ({
          componentName: item.componentName,
          componentType: item.componentType,
          amount: Number(item.amount)
        }))
      );

      setChangeReason("");
      setShowEditor(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  const handleComponentChange = (index, field, value) => {
    setEditComponents((previous) =>
      previous.map((item, i) =>
        i === index
          ? {
              ...item,
              [field]: value
            }
          : item
      )
    );
  };

  const addComponent = (type) => {
    setEditComponents((previous) => [
      ...previous,
      {
        componentName: "",
        componentType: type,
        amount: 0
      }
    ]);
  };

  const removeComponent = (index) => {
    setEditComponents((previous) =>
      previous.filter((_, i) => i !== index)
    );
  };

  const totals = useMemo(() => {
    const earnings = editComponents
      .filter((item) => item.componentType === "EARNING")
      .reduce(
        (sum, item) => sum + (Number(item.amount) || 0),
        0
      );

    const deductions = editComponents
      .filter((item) => item.componentType === "DEDUCTION")
      .reduce(
        (sum, item) => sum + (Number(item.amount) || 0),
        0
      );

    return {
      grossSalary: earnings,
      deductions,
      netSalary: earnings - deductions
    };
  }, [editComponents]);

  // =====================================================
  // SAVE PAYROLL COMPONENT CHANGES
  // =====================================================

  const handleSaveChanges = async (event) => {
    event.preventDefault();

    setError("");
    setMessage("");

    if (!selectedPayroll) return;

    if (selectedPayroll.paymentStatus !== "GENERATED") {
      setError("Only GENERATED payroll records can be edited.");
      return;
    }

    if (!changeReason.trim()) {
      setError("Please enter a reason for editing payroll.");
      return;
    }

    if (
      editComponents.some(
        (item) =>
          !item.componentName.trim() ||
          !Number.isFinite(Number(item.amount)) ||
          Number(item.amount) < 0
      )
    ) {
      setError(
        "Enter a valid component name and non-negative amount for every component."
      );
      return;
    }

    if (totals.grossSalary <= 0) {
      setError("Gross salary must be greater than zero.");
      return;
    }

    if (totals.deductions > totals.grossSalary) {
      setError("Total deductions cannot exceed gross salary.");
      return;
    }

    if (
      !editComponents.some(
        (item) =>
          item.componentType === "EARNING" &&
          item.componentName.trim().toUpperCase() === "BASIC_SALARY"
      )
    ) {
      setError("BASIC_SALARY earning component is required.");
      return;
    }

    setSaving(true);

    try {
      const payload = {
        changeReason: changeReason.trim(),
        components: editComponents.map((item) => ({
          componentName: item.componentName.trim(),
          componentType: item.componentType,
          amount: Number(item.amount)
        }))
      };

      const response = await apiFetch(
        `${API_URL}/${selectedPayroll.id}/components`,
        {
          method: "PUT",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify(payload)
        }
      );

      await readResponse(response, {});

      setMessage("Payroll components updated successfully.");
      setShowEditor(false);
      setSelectedPayroll(null);

      await fetchPayroll();
    } catch (err) {
      setError(err.message);
    } finally {
      setSaving(false);
    }
  };

  // =====================================================
  // VIEW PAYROLL COMPONENTS
  // =====================================================

  const openComponents = async (record) => {
    setError("");
    setMessage("");

    setSelectedPayroll(record);
    setShowEditor(false);
    setShowHistory(false);
    setShowComponents(false);
    setLoading(true);

    try {
      const response = await apiFetch(
        `${API_URL}/${record.id}/components`
      );

      const data = await readResponse(response, []);

      setComponents(Array.isArray(data) ? data : []);
      setShowComponents(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // =====================================================
  // VIEW PAYROLL EDIT HISTORY
  // =====================================================

  const openHistory = async (record) => {
    setError("");
    setMessage("");

    setSelectedPayroll(record);
    setShowEditor(false);
    setShowComponents(false);
    setShowHistory(false);
    setLoading(true);

    try {
      const response = await apiFetch(
        `${API_URL}/${record.id}/history`
      );

      const data = await readResponse(response, []);

      setHistory(Array.isArray(data) ? data : []);
      setShowHistory(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // =====================================================
  // EMPLOYEE: OPEN OWN PUBLISHED PAYSLIP
  // =====================================================

  const openMyPayslip = async (record) => {
    setError("");
    setMessage("");
    setLoading(true);

    try {
      // Load the payslip summary and its component breakdown together.
      const [payslipResponse, componentsResponse] = await Promise.all([
        apiFetch(`${API_URL}/my-payslips/${record.id}`),
        apiFetch(`${API_URL}/my-payslips/${record.id}/components`)
      ]);

      const [payslipData, componentData] = await Promise.all([
        readResponse(payslipResponse, {}),
        readResponse(componentsResponse, [])
      ]);

      setSelectedPayroll(payslipData);
      setComponents(Array.isArray(componentData) ? componentData : []);

      setShowEditor(false);
      setShowHistory(false);
      setShowComponents(true);
    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  // =====================================================
  // FILTERS AND SUMMARY
  // =====================================================

  const filteredRecords = useMemo(() => {
    return payrollRecords.filter((record) => {
      const searchText = search.toLowerCase();

      const matchesSearch =
        (record.employeeId || "").toLowerCase().includes(searchText) ||
        (record.employeeName || "").toLowerCase().includes(searchText) ||
        (record.department || "").toLowerCase().includes(searchText);

      const matchesStatus =
        statusFilter === "ALL" ||
        record.paymentStatus === statusFilter;

      const matchesMonth =
        !month ||
        record.payrollMonth?.toString().startsWith(month);

      return matchesSearch && matchesStatus && matchesMonth;
    });
  }, [payrollRecords, search, statusFilter, month]);

  const totalGross = payrollRecords.reduce(
    (sum, record) => sum + Number(record.grossSalary || 0),
    0
  );

  const totalNet = payrollRecords.reduce(
    (sum, record) => sum + Number(record.netSalary || 0),
    0
  );

  const paidCount = payrollRecords.filter(
    (record) => record.paymentStatus === "PAID"
  ).length;

  const generatedCount = payrollRecords.filter(
    (record) => record.paymentStatus === "GENERATED"
  ).length;

  const publishedCount = payrollRecords.filter(
    (record) => record.paymentStatus === "PUBLISHED"
  ).length;

  const employeePublishedTotal = myPayslips.reduce(
    (sum, record) => sum + Number(record.netSalary || 0),
    0
  );

  // =====================================================
  // EMPLOYEE VIEW
  // =====================================================

  if (isEmployee) {
    return (
      <div className="payroll-page">
        <div className="payroll-heading">
          <div>
            <span className="payroll-eyebrow">
              EMPLOYEE SELF SERVICE
            </span>

            <h1>My Payslips</h1>

            <p>
              View your published salary statements.
            </p>
          </div>

          <button
            className="payroll-refresh-button"
            onClick={fetchMyPayslips}
            disabled={loading}
          >
            ↻ Refresh
          </button>
        </div>

        {message && (
          <div className="payroll-success">{message}</div>
        )}

        {error && (
          <div className="payroll-error">{error}</div>
        )}

        <div className="payroll-summary">
          <div className="payroll-summary-card">
            <span className="payroll-summary-icon">📄</span>
            <div>
              <p>Published Payslips</p>
              <h2>{myPayslips.length}</h2>
            </div>
          </div>

          <div className="payroll-summary-card">
            <span className="payroll-summary-icon">💰</span>
            <div>
              <p>Total Net Salary</p>
              <h2>{formatCurrency(employeePublishedTotal)}</h2>
            </div>
          </div>
        </div>

        <div className="payroll-history-card">
          <div className="payroll-card-heading">
            <h2>Published Payslip History</h2>
            <p>
              Only payslips published by HR/Admin are displayed here.
            </p>
          </div>

          <div className="payroll-table-wrapper">
            <table className="payroll-table">
              <thead>
                <tr>
                  <th>Payroll Month</th>
                  <th>Gross Salary</th>
                  <th>Deductions</th>
                  <th>Net Salary</th>
                  <th>Status</th>
                  <th>Action</th>
                </tr>
              </thead>

              <tbody>
                {loading ? (
                  <tr>
                    <td colSpan="6" className="payroll-empty">
                      Loading your payslips...
                    </td>
                  </tr>
                ) : myPayslips.length === 0 ? (
                  <tr>
                    <td colSpan="6" className="payroll-empty">
                      No published payslips are available yet.
                    </td>
                  </tr>
                ) : (
                  myPayslips.map((record) => (
                    <tr key={record.id}>
                      <td>{formatMonth(record.payrollMonth)}</td>

                      <td>{formatCurrency(record.grossSalary)}</td>

                      <td>{formatCurrency(record.deductions)}</td>

                      <td className="payroll-net">
                        {formatCurrency(record.netSalary)}
                      </td>

                      <td>
                        <span
                          className={`payroll-status ${getStatusClass(record.paymentStatus)}`}
                        >
                          {record.paymentStatus}
                        </span>
                      </td>

                      <td>
                        <button
                          type="button"
                          className="payroll-view-button"
                          onClick={() => openMyPayslip(record)}
                          disabled={loading}
                        >
                          View Payslip
                        </button>
                        <button
                          type="button"
                          className="payroll-view-button"
                          onClick={() => handleDownloadPayslip(record)}
                          disabled={downloadingId === record.id || loading}
                          title="Download payslip as PDF"
                        >
                          {downloadingId === record.id
                            ? "Downloading..."
                            : "↓ Download PDF"}
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* EMPLOYEE PAYSLIP DETAILS */}
        {showComponents && selectedPayroll && (
          <div className="payroll-modal-overlay">
            <div className="payroll-modal payroll-components-modal">
              <div className="payroll-modal-heading">
                <div>
                  <h2>My Payslip</h2>
                  <p>
                    Payroll Month: {formatMonth(selectedPayroll.payrollMonth)}
                  </p>
                  <p>
                    Employee ID: {selectedPayroll.employeeId || user?.employeeId || user?.username || "-"}
                  </p>
                </div>

                <button
                  type="button"
                  onClick={() => setShowComponents(false)}
                  className="payroll-close-button"
                >
                  ×
                </button>
              </div>

              <div className="payroll-view-list">
                <div className="payroll-view-row">
                  <span>Employee Name</span>
                  <strong>
                    {selectedPayroll.employeeName || user?.fullName || "-"}
                  </strong>
                </div>

                <div className="payroll-view-row">
                  <span>Gross Salary</span>
                  <strong>
                    {formatCurrency(selectedPayroll.grossSalary)}
                  </strong>
                </div>

                <div className="payroll-view-row">
                  <span>Total Deductions</span>
                  <strong>
                    {formatCurrency(selectedPayroll.deductions)}
                  </strong>
                </div>

                <div className="payroll-view-row">
                  <span>Net Salary</span>
                  <strong>
                    {formatCurrency(selectedPayroll.netSalary)}
                  </strong>
                </div>

                <div className="payroll-view-row">
                  <span>Payment Status</span>
                  <strong>{selectedPayroll.paymentStatus}</strong>
                </div>
              </div>

              <div className="payroll-view-list" style={{ marginTop: "20px" }}>
                <h3 style={{ marginBottom: "12px" }}>Earnings</h3>

                {components.filter((item) => item.componentType === "EARNING").length === 0 ? (
                  <p className="payroll-view-empty">No earning components found.</p>
                ) : (
                  components
                    .filter((item) => item.componentType === "EARNING")
                    .map((item) => (
                      <div className="payroll-view-row" key={item.id || item.componentName}>
                        <span>{String(item.componentName || "").replaceAll("_", " ")}</span>
                        <strong>{formatCurrency(item.amount)}</strong>
                      </div>
                    ))
                )}

                <h3 style={{ marginTop: "20px", marginBottom: "12px" }}>Deductions</h3>

                {components.filter((item) => item.componentType === "DEDUCTION").length === 0 ? (
                  <p className="payroll-view-empty">No deduction components found.</p>
                ) : (
                  components
                    .filter((item) => item.componentType === "DEDUCTION")
                    .map((item) => (
                      <div className="payroll-view-row" key={item.id || item.componentName}>
                        <span>{String(item.componentName || "").replaceAll("_", " ")}</span>
                        <strong>{formatCurrency(item.amount)}</strong>
                      </div>
                    ))
                )}
              </div>

              <div className="payroll-modal-actions">
                <button
                  type="button"
                  className="payroll-cancel-button"
                  onClick={() => setShowComponents(false)}
                >
                  Close
                </button>
              </div>
            </div>
          </div>
        )}
      </div>
    );
  }

  // =====================================================
  // ACCESS CONTROL
  // =====================================================

  if (!isAdminOrHR) {
    return (
      <div className="payroll-page">
        <div className="payroll-error">
          You are not authorized to access Payroll Management.
        </div>
      </div>
    );
  }

  // =====================================================
  // ADMIN / HR VIEW
  // =====================================================

  return (
    <div className="payroll-page">

      <div className="payroll-heading">
        <div>
          <span className="payroll-eyebrow">
            HUMAN RESOURCE MANAGEMENT
          </span>

          <h1>Payroll Management</h1>

          <p>
            Generate payroll, review salary components and publish payslips.
          </p>
        </div>

        <button
          className="payroll-refresh-button"
          onClick={fetchPayroll}
          disabled={loading}
        >
          ↻ Refresh
        </button>
      </div>

      {message && (
        <div className="payroll-success">{message}</div>
      )}

      {error && (
        <div className="payroll-error">{error}</div>
      )}

      {/* SUMMARY CARDS */}
      <div className="payroll-summary">
        <div className="payroll-summary-card">
          <span className="payroll-summary-icon">📋</span>
          <div>
            <p>Total Records</p>
            <h2>{payrollRecords.length}</h2>
          </div>
        </div>

        <div className="payroll-summary-card">
          <span className="payroll-summary-icon">💰</span>
          <div>
            <p>Total Gross Salary</p>
            <h2>{formatCurrency(totalGross)}</h2>
          </div>
        </div>

        <div className="payroll-summary-card">
          <span className="payroll-summary-icon">✓</span>
          <div>
            <p>Paid Records</p>
            <h2>{paidCount}</h2>
          </div>
        </div>

        <div className="payroll-summary-card">
          <span className="payroll-summary-icon">📢</span>
          <div>
            <p>Published Records</p>
            <h2>{publishedCount}</h2>
          </div>
        </div>
      </div>

      {/* GENERATE PAYROLL */}
      <div className="payroll-generation-card">
        <div className="payroll-card-heading">
          <h2>Generate Monthly Payroll</h2>
          <p>
            Generate payroll using applicable employee salary structures.
          </p>
        </div>

        <form
          className="payroll-generation-form"
          onSubmit={handleGeneratePayroll}
        >
          <div>
            <label>Select Payroll Month</label>

            <input
              type="month"
              value={month}
              onChange={(event) => setMonth(event.target.value)}
              required
            />
          </div>

          <button type="submit" disabled={generating}>
            {generating ? "Generating..." : "Generate Payroll"}
          </button>
        </form>
      </div>

      {/* PAYROLL GENERATION RESULT */}
      {generationReport && (
        <div
          className="payroll-history-card"
          style={{ marginTop: "20px", padding: "22px" }}
        >
          <div className="payroll-card-heading">
            <h2>Payroll Generation Report</h2>
            <p>
              Results for the most recent payroll generation request.
            </p>
          </div>

          <div className="payroll-summary">
            <div className="payroll-summary-card">
              <span className="payroll-summary-icon">✓</span>
              <div>
                <p>Generated Successfully</p>
                <h2>{generationReport.generatedCount}</h2>
              </div>
            </div>

            <div className="payroll-summary-card">
              <span className="payroll-summary-icon">⚠</span>
              <div>
                <p>Skipped Employees</p>
                <h2>{generationReport.skippedCount}</h2>
              </div>
            </div>
          </div>

          {generationReport.generatedPayrolls.length > 0 && (
            <div style={{ marginTop: "18px" }}>
              <h3>Generated Payroll Records</h3>
              <div className="payroll-table-wrapper">
                <table className="payroll-table">
                  <thead>
                    <tr>
                      <th>Employee ID</th>
                      <th>Employee Name</th>
                      <th>Payroll Month</th>
                      <th>Net Salary</th>
                      <th>Status</th>
                    </tr>
                  </thead>
                  <tbody>
                    {generationReport.generatedPayrolls.map((record, index) => (
                      <tr key={record.id || `${record.employeeId}-${index}`}>
                        <td>{record.employeeId || "-"}</td>
                        <td>{record.employeeName || "-"}</td>
                        <td>{formatMonth(record.payrollMonth)}</td>
                        <td>{formatCurrency(record.netSalary)}</td>
                        <td>{record.paymentStatus || "GENERATED"}</td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>
          )}

          {generationReport.skippedEmployees.length > 0 && (
            <div style={{ marginTop: "20px" }}>
              <h3>Skipped Employees and Reasons</h3>
              <ul style={{ paddingLeft: "22px", lineHeight: 1.8 }}>
                {generationReport.skippedEmployees.map((reason, index) => (
                  <li key={`${index}-${reason}`}>{reason}</li>
                ))}
              </ul>
            </div>
          )}

          {generationReport.generatedCount === 0 &&
            generationReport.skippedCount === 0 && (
              <p className="payroll-view-empty">
                No payroll records were generated and no employees were reported as skipped.
              </p>
            )}
        </div>
      )}

      {/* PAYROLL RECORDS */}
      <div className="payroll-history-card">
        <div className="payroll-card-heading payroll-list-heading">
          <div>
            <h2>Payroll Records</h2>
            <p>
              Review, edit, publish and track employee payroll.
            </p>
          </div>

          <button
            type="button"
            className="payroll-save-button"
            onClick={handlePublishAll}
            disabled={publishingAll || !month}
          >
            {publishingAll ? "Publishing..." : "Publish All for Selected Month"}
          </button>
        </div>

        <div className="payroll-filters">
          <input
            type="text"
            placeholder="Search employee ID, name or department..."
            value={search}
            onChange={(event) => setSearch(event.target.value)}
          />

          <select
            value={statusFilter}
            onChange={(event) => setStatusFilter(event.target.value)}
          >
            <option value="ALL">All Status</option>
            <option value="GENERATED">Generated</option>
            <option value="PUBLISHED">Published</option>
            <option value="PAID">Paid</option>
          </select>
        </div>

        <div className="payroll-table-wrapper">
          <table className="payroll-table">
            <thead>
              <tr>
                <th>Employee</th>
                <th>Department</th>
                <th>Payroll Month</th>
                <th>Gross Salary</th>
                <th>Deductions</th>
                <th>Net Salary</th>
                <th>Status</th>
                <th>Actions</th>
              </tr>
            </thead>

            <tbody>
              {loading ? (
                <tr>
                  <td colSpan="8" className="payroll-empty">
                    Loading payroll records...
                  </td>
                </tr>
              ) : filteredRecords.length === 0 ? (
                <tr>
                  <td colSpan="8" className="payroll-empty">
                    No payroll records found.
                  </td>
                </tr>
              ) : (
                filteredRecords.map((record) => (
                  <tr key={record.id}>
                    <td>
                      <strong>{record.employeeName || "Employee"}</strong>
                      <small>{record.employeeId}</small>
                    </td>

                    <td>{record.department || "-"}</td>

                    <td>{formatMonth(record.payrollMonth)}</td>

                    <td>{formatCurrency(record.grossSalary)}</td>

                    <td>{formatCurrency(record.deductions)}</td>

                    <td className="payroll-net">
                      {formatCurrency(record.netSalary)}
                    </td>

                    <td>
                      <span
                        className={`payroll-status ${getStatusClass(record.paymentStatus)}`}
                      >
                        {record.paymentStatus}
                      </span>
                    </td>

                    <td>
                      <div className="payroll-actions">
                        <button
                          type="button"
                          className="payroll-view-button"
                          onClick={() => openComponents(record)}
                          disabled={loading}
                        >
                          View
                        </button>

                        {record.paymentStatus === "GENERATED" && (
                          <>
                            <button
                              type="button"
                              className="payroll-edit-button"
                              onClick={() => openEditor(record)}
                              disabled={loading}
                            >
                              Edit
                            </button>

                            <button
                              type="button"
                              className="payroll-save-button"
                              onClick={() => handlePublish(record)}
                              disabled={publishingId === record.id}
                            >
                              {publishingId === record.id
                                ? "Publishing..."
                                : "Publish"}
                            </button>
                          </>
                        )}

                        {record.paymentStatus === "PUBLISHED" && (
                          <button
                            type="button"
                            className="payroll-save-button"
                            onClick={() => handleMarkAsPaid(record)}
                            disabled={payingId === record.id}
                          >
                            {payingId === record.id
                              ? "Updating..."
                              : "Mark Paid"}
                          </button>
                        )}

                        {record.paymentStatus === "PAID" && (
                          <span className="payroll-locked">
                            🔒 Locked
                          </span>
                        )}

                        <button
                          type="button"
                          className="payroll-history-button"
                          onClick={() => openHistory(record)}
                          disabled={loading}
                        >
                          History
                        </button>
                      </div>
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>

        <div className="payroll-table-footer">
          Showing {filteredRecords.length} of {payrollRecords.length} records
          {" · "}
          Total net payable: {formatCurrency(totalNet)}
          {" · "}
          Generated: {generatedCount}
        </div>
      </div>

      {/* EDIT PAYROLL COMPONENTS MODAL */}
      {showEditor && selectedPayroll && (
        <div className="payroll-modal-overlay">
          <div className="payroll-modal">
            <div className="payroll-modal-heading">
              <div>
                <h2>Edit Payroll Components</h2>
                <p>
                  {selectedPayroll.employeeName} ({selectedPayroll.employeeId})
                </p>
              </div>

              <button
                type="button"
                onClick={() => setShowEditor(false)}
                className="payroll-close-button"
              >
                ×
              </button>
            </div>

            <form onSubmit={handleSaveChanges}>
              <div className="payroll-component-section">
                <div className="payroll-component-heading">
                  <h3>Earnings</h3>

                  <button
                    type="button"
                    onClick={() => addComponent("EARNING")}
                  >
                    + Add Earning
                  </button>
                </div>

                {editComponents.map((item, index) =>
                  item.componentType === "EARNING" ? (
                    <div
                      className="payroll-component-row"
                      key={`${index}-${item.componentName}`}
                    >
                      <input
                        value={item.componentName}
                        onChange={(event) =>
                          handleComponentChange(
                            index,
                            "componentName",
                            event.target.value
                          )
                        }
                        placeholder="Component name"
                        required
                      />

                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        value={item.amount}
                        onChange={(event) =>
                          handleComponentChange(
                            index,
                            "amount",
                            event.target.value
                          )
                        }
                        required
                      />

                      <button
                        type="button"
                        onClick={() => removeComponent(index)}
                        aria-label="Remove earning"
                      >
                        ×
                      </button>
                    </div>
                  ) : null
                )}
              </div>

              <div className="payroll-component-section">
                <div className="payroll-component-heading">
                  <h3>Deductions</h3>

                  <button
                    type="button"
                    onClick={() => addComponent("DEDUCTION")}
                  >
                    + Add Deduction
                  </button>
                </div>

                {editComponents.map((item, index) =>
                  item.componentType === "DEDUCTION" ? (
                    <div
                      className="payroll-component-row"
                      key={`${index}-${item.componentName}`}
                    >
                      <input
                        value={item.componentName}
                        onChange={(event) =>
                          handleComponentChange(
                            index,
                            "componentName",
                            event.target.value
                          )
                        }
                        placeholder="Component name"
                        required
                      />

                      <input
                        type="number"
                        min="0"
                        step="0.01"
                        value={item.amount}
                        onChange={(event) =>
                          handleComponentChange(
                            index,
                            "amount",
                            event.target.value
                          )
                        }
                        required
                      />

                      <button
                        type="button"
                        onClick={() => removeComponent(index)}
                        aria-label="Remove deduction"
                      >
                        ×
                      </button>
                    </div>
                  ) : null
                )}
              </div>

              <div className="payroll-calculation">
                <div>
                  <span>Gross Earnings</span>
                  <strong>{formatCurrency(totals.grossSalary)}</strong>
                </div>

                <div>
                  <span>Total Deductions</span>
                  <strong>{formatCurrency(totals.deductions)}</strong>
                </div>

                <div className="payroll-net-total">
                  <span>Net Salary</span>
                  <strong>{formatCurrency(totals.netSalary)}</strong>
                </div>
              </div>

              <div className="payroll-reason">
                <label>Reason for Payroll Change *</label>

                <textarea
                  value={changeReason}
                  onChange={(event) => setChangeReason(event.target.value)}
                  placeholder="Explain why this payroll is being changed..."
                  maxLength={500}
                  rows="3"
                  required
                />
              </div>

              <div className="payroll-modal-actions">
                <button
                  type="button"
                  className="payroll-cancel-button"
                  onClick={() => setShowEditor(false)}
                  disabled={saving}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="payroll-save-button"
                  disabled={saving}
                >
                  {saving ? "Saving..." : "Save Changes"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* VIEW PAYROLL COMPONENTS MODAL */}
      {showComponents && selectedPayroll && (
        <div className="payroll-modal-overlay">
          <div className="payroll-modal payroll-components-modal">
            <div className="payroll-modal-heading">
              <div>
                <h2>Payroll Components</h2>

                <p>
                  {selectedPayroll.employeeName} ({selectedPayroll.employeeId})
                </p>

                <p>
                  Payroll Month: {formatMonth(selectedPayroll.payrollMonth)}
                </p>
              </div>

              <button
                type="button"
                onClick={() => setShowComponents(false)}
                className="payroll-close-button"
              >
                ×
              </button>
            </div>

            <div className="payroll-component-section">
              <h3>Earnings</h3>

              <div className="payroll-view-list">
                {components
                  .filter((item) => item.componentType === "EARNING")
                  .map((item, index) => (
                    <div
                      className="payroll-view-row"
                      key={item.id || index}
                    >
                      <span>{item.componentName}</span>
                      <strong>{formatCurrency(item.amount)}</strong>
                    </div>
                  ))}

                {components.filter(
                  (item) => item.componentType === "EARNING"
                ).length === 0 && (
                  <p className="payroll-view-empty">
                    No earning components available.
                  </p>
                )}
              </div>
            </div>

            <div className="payroll-component-section">
              <h3>Deductions</h3>

              <div className="payroll-view-list">
                {components
                  .filter((item) => item.componentType === "DEDUCTION")
                  .map((item, index) => (
                    <div
                      className="payroll-view-row"
                      key={item.id || index}
                    >
                      <span>{item.componentName}</span>
                      <strong>{formatCurrency(item.amount)}</strong>
                    </div>
                  ))}

                {components.filter(
                  (item) => item.componentType === "DEDUCTION"
                ).length === 0 && (
                  <p className="payroll-view-empty">
                    No deduction components available.
                  </p>
                )}
              </div>
            </div>

            <div className="payroll-calculation">
              <div>
                <span>Gross Salary</span>
                <strong>
                  {formatCurrency(selectedPayroll.grossSalary)}
                </strong>
              </div>

              <div>
                <span>Total Deductions</span>
                <strong>
                  {formatCurrency(selectedPayroll.deductions)}
                </strong>
              </div>

              <div className="payroll-net-total">
                <span>Net Salary</span>
                <strong>
                  {formatCurrency(selectedPayroll.netSalary)}
                </strong>
              </div>
            </div>

            <div className="payroll-modal-actions">
              <button
                type="button"
                className="payroll-cancel-button"
                onClick={() => setShowComponents(false)}
              >
                Close
              </button>
            </div>
          </div>
        </div>
      )}

      {/* PAYROLL HISTORY MODAL */}
      {showHistory && selectedPayroll && (
        <div className="payroll-modal-overlay">
          <div className="payroll-modal payroll-history-modal">
            <div className="payroll-modal-heading">
              <div>
                <h2>Payroll Edit History</h2>

                <p>
                  {selectedPayroll.employeeName} ({selectedPayroll.employeeId})
                </p>
              </div>

              <button
                type="button"
                onClick={() => setShowHistory(false)}
                className="payroll-close-button"
              >
                ×
              </button>
            </div>

            {history.length === 0 ? (
              <div className="payroll-empty-history">
                No payroll edit history found.
              </div>
            ) : (
              <div className="payroll-audit-list">
                {history.map((item) => (
                  <div className="payroll-audit-card" key={item.id}>
                    <div className="payroll-audit-top">
                      <strong>
                        {item.changedByEmployeeId || "User"}
                      </strong>

                      <span>{formatDate(item.changedAt)}</span>
                    </div>

                    <p>{item.changeReason}</p>

                    <div className="payroll-audit-totals">
                      <div>
                        <span>Old Gross</span>
                        <strong>
                          {formatCurrency(item.oldGrossSalary)}
                        </strong>
                      </div>

                      <div>
                        <span>New Gross</span>
                        <strong>
                          {formatCurrency(item.newGrossSalary)}
                        </strong>
                      </div>

                      <div>
                        <span>Old Net</span>
                        <strong>
                          {formatCurrency(item.oldNetSalary)}
                        </strong>
                      </div>

                      <div>
                        <span>New Net</span>
                        <strong>
                          {formatCurrency(item.newNetSalary)}
                        </strong>
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}

export default Payroll;
