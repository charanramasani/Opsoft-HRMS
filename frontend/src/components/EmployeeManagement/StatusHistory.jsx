
import React, { useCallback, useEffect, useState } from "react";
import "./StatusHistory.css";

const API_URL = "http://localhost:8083";

// API request with JWT authentication
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

function StatusHistory({ employeeId, onClose }) {
    const [history, setHistory] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState("");

    // Fetch employee status history
    const fetchHistory = useCallback(async () => {
        try {
            setLoading(true);
            setError("");

            const response = await apiFetch(
                `${API_URL}/api/employees/${encodeURIComponent(employeeId)}/status-history`,
                {
                    method: "GET"
                }
            );

            const data = await response.json().catch(() => ({}));

            if (!response.ok) {
                throw new Error(
                    data.message ||
                    data.detail ||
                    `Unable to load status history. Server returned ${response.status}.`
                );
            }

            setHistory(Array.isArray(data) ? data : []);

        } catch (err) {
            setError(err.message || "Something went wrong.");
        } finally {
            setLoading(false);
        }
    }, [employeeId]);

    useEffect(() => {
        fetchHistory();
    }, [fetchHistory]);

    const formatDate = (dateValue) => {
        if (!dateValue) return "Date unavailable";

        const date = new Date(dateValue);

        if (Number.isNaN(date.getTime())) {
            return dateValue;
        }

        return date.toLocaleString("en-IN", {
            day: "2-digit",
            month: "short",
            year: "numeric",
            hour: "2-digit",
            minute: "2-digit"
        });
    };

    const formatStatus = (status) => {
        if (!status) return "Unknown";

        return status
            .replaceAll("_", " ")
            .toLowerCase()
            .replace(/\b\w/g, (letter) => letter.toUpperCase());
    };

    return (
        <div className="history-overlay" onClick={onClose}>
            <div
                className="history-modal"
                onClick={(event) => event.stopPropagation()}
            >
                <div className="history-header">
                    <div>
                        <h2>Employee Status History</h2>
                        <p>Employee ID: {employeeId}</p>
                    </div>

                    <button
                        className="history-close"
                        onClick={onClose}
                        aria-label="Close"
                    >
                        &times;
                    </button>
                </div>

                <div className="history-content">
                    {loading && (
                        <div className="history-message">
                            Loading status history...
                        </div>
                    )}

                    {!loading && error && (
                        <div className="history-error">
                            {error}
                            <button onClick={fetchHistory}>
                                Retry
                            </button>
                        </div>
                    )}

                    {!loading && !error && history.length === 0 && (
                        <div className="history-empty">
                            <div className="history-empty-icon">📋</div>
                            <h3>No Status History Found</h3>
                            <p>
                                Status changes for this employee will appear
                                here.
                            </p>
                        </div>
                    )}

                    {!loading && !error && history.length > 0 && (
                        <div className="history-timeline">
                            {history.map((item) => (
                                <div className="history-item" key={item.id}>
                                    <div className="history-marker"></div>

                                    <div className="history-card">
                                        <div className="history-status-row">
                                            <span
                                                className={`history-status status-${(item.newStatus || "").toLowerCase()}`}
                                            >
                                                {formatStatus(item.newStatus)}
                                            </span>

                                            <span className="history-date">
                                                {formatDate(item.changedAt)}
                                            </span>
                                        </div>

                                        <p className="history-transition">
                                            {formatStatus(item.previousStatus)}
                                            <span> → </span>
                                            {formatStatus(item.newStatus)}
                                        </p>

                                        <div className="history-details">
                                            <p>
                                                <strong>Reason:</strong>{" "}
                                                {item.reason || "Not provided"}
                                            </p>

                                            <p>
                                                <strong>Changed By:</strong>{" "}
                                                {item.changedByEmployeeId || "System"}
                                            </p>
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    )}
                </div>

                <div className="history-footer">
                    <button
                        className="history-refresh"
                        onClick={fetchHistory}
                        disabled={loading}
                    >
                        Refresh History
                    </button>

                    <button
                        className="history-done"
                        onClick={onClose}
                    >
                        Close
                    </button>
                </div>
            </div>
        </div>
    );
}

export default StatusHistory;
