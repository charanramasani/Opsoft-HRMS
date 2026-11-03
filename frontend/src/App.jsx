
import { useEffect, useState } from "react";
import "./App.css";

import Login from "./components/Login/Login";
import Dashboard from "./components/Dashboard/Dashboard";
import EmployeeManagement from "./components/EmployeeManagement/EmployeeManagement";
import Profile from "./components/Profile/Profile";
import Attendance from "./components/Attendance/Attendance";
import LeaveManagement from "./components/LeaveManagement/LeaveManagement";
import Payroll from "./components/Payroll/Payroll";
import CompanyHoliday from "./components/CompanyHoliday/CompanyHoliday";
import AnnouncementManagement from "./components/AnnouncementManagement/AnnouncementManagement";

const API_URL = "http://localhost:8083";

const getSavedUser = () => {
  try {
    const savedUser = localStorage.getItem("hrmsUser");
    return savedUser ? JSON.parse(savedUser) : null;
  } catch {
    localStorage.removeItem("hrmsUser");
    localStorage.removeItem("hrmsToken");
    return null;
  }
};

// Common authenticated request helper
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

// Capture GPS coordinates and convert them into a readable address
const getCurrentLocationWithAddress = () => {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(
        new Error(
          "Your browser does not support GPS location."
        )
      );
      return;
    }

    navigator.geolocation.getCurrentPosition(
      async (position) => {
        try {
          const latitude = position.coords.latitude;
          const longitude = position.coords.longitude;

          const params = new URLSearchParams({
            format: "jsonv2",
            lat: latitude.toString(),
            lon: longitude.toString()
          });

          const response = await fetch(
            `https://nominatim.openstreetmap.org/reverse?${params.toString()}`,
            {
              method: "GET",
              headers: {
                Accept: "application/json"
              }
            }
          );

          if (!response.ok) {
            throw new Error(
              "Unable to retrieve your address. Please try again."
            );
          }

          const data = await response.json();

          if (!data.display_name) {
            throw new Error(
              "Address could not be identified for your GPS location."
            );
          }

          resolve({
            latitude,
            longitude,
            address: data.display_name
          });

        } catch (error) {
          reject(
            new Error(
              error.message ||
              "Unable to retrieve your location address."
            )
          );
        }
      },

      (error) => {
        let message = "Unable to get your GPS location.";

        if (error.code === 1) {
          message =
            "Location permission denied. Please allow location access in your browser.";
        } else if (error.code === 2) {
          message =
            "Your current location is unavailable. Please enable GPS and try again.";
        } else if (error.code === 3) {
          message =
            "Location request timed out. Please try again.";
        }

        reject(new Error(message));
      },

      {
        enableHighAccuracy: true,
        timeout: 20000,
        maximumAge: 0
      }
    );
  });
};

function App() {

  const [employeeId, setEmployeeId] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);

  const [user, setUser] = useState(getSavedUser);
  const [currentPage, setCurrentPage] = useState("Dashboard");

  const [currentPassword, setCurrentPassword] = useState("");
  const [newPassword, setNewPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");

  const [employees, setEmployees] = useState([]);
  const [profile, setProfile] = useState(null);
  const [attendanceRecords, setAttendanceRecords] = useState([]);

  const [search, setSearch] = useState("");

  const [loading, setLoading] = useState(false);
  const [attendanceLoading, setAttendanceLoading] = useState(false);

  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");

  const isAdminOrHR =
    user?.role === "ADMIN" || user?.role === "HR";

  // LOGIN
  const handleLogin = async (event) => {
    event.preventDefault();

    setError("");
    setSuccess("");
    setLoading(true);

    try {
      const response = await fetch(`${API_URL}/api/auth/login`, {
        method: "POST",
        credentials: "omit",
        headers: {
          "Content-Type": "application/json"
        },
        body: JSON.stringify({
          employeeId: employeeId.trim(),
          password
        })
      });

      const data = await response.json();

      if (!response.ok) {
        throw new Error(
          data.message || "Invalid employee ID or password."
        );
      }

      if (!data.token) {
        throw new Error(
          "Login response did not contain a JWT token. Please check the backend login response."
        );
      }

      localStorage.setItem("hrmsToken", data.token);

      const userData = {
        employeeId: data.employeeId,
        fullName: data.fullName,
        role: data.role,
        mustChangePassword: data.mustChangePassword
      };

      localStorage.setItem(
        "hrmsUser",
        JSON.stringify(userData)
      );

      setUser(userData);
      setPassword("");
      setProfile(null);
      setAttendanceRecords([]);

      setCurrentPage(
        data.mustChangePassword
          ? "ChangePassword"
          : "Dashboard"
      );

    } catch (err) {
      setError(err.message || "Unable to login.");
    } finally {
      setLoading(false);
    }
  };

  // CHANGE PASSWORD
  const handleChangePassword = async (event) => {
    event.preventDefault();

    setError("");
    setSuccess("");

    if (newPassword.length < 12) {
      setError(
        "New password must contain at least 12 characters."
      );
      return;
    }

    if (newPassword !== confirmPassword) {
      setError(
        "New password and confirm password do not match."
      );
      return;
    }

    setLoading(true);

    try {
      const response = await apiFetch(
        `${API_URL}/api/auth/change-password`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            currentPassword,
            newPassword,
            confirmPassword
          })
        }
      );

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        throw new Error(
          data.message || "Password change failed."
        );
      }

      const updatedUser = {
        ...user,
        mustChangePassword: false
      };

      setUser(updatedUser);

      localStorage.setItem(
        "hrmsUser",
        JSON.stringify(updatedUser)
      );

      setCurrentPassword("");
      setNewPassword("");
      setConfirmPassword("");

      setCurrentPage("Dashboard");

      setSuccess("Password changed successfully.");

    } catch (err) {
      setError(err.message || "Unable to change password.");
    } finally {
      setLoading(false);
    }
  };

  // FETCH EMPLOYEES
  const fetchEmployees = async () => {
    try {
      const response = await apiFetch(
        `${API_URL}/api/employees`,
        {
          method: "GET"
        }
      );

      if (!response.ok) {
        throw new Error("Unable to fetch employees.");
      }

      const data = await response.json();

      setEmployees(data);

    } catch (err) {
      setError(err.message);
    }
  };

  // FETCH MY PROFILE
  const fetchMyProfile = async () => {
    try {
      const response = await apiFetch(
        `${API_URL}/api/employees/me`,
        {
          method: "GET"
        }
      );

      if (!response.ok) {
        throw new Error("Unable to fetch your profile.");
      }

      const data = await response.json();

      setProfile(data);

    } catch (err) {
      setProfile(null);
      setError(err.message);
    }
  };

  // FETCH ATTENDANCE BASED ON ROLE
  const fetchAttendance = async () => {
    setAttendanceLoading(true);

    try {
      const attendanceEndpoint = isAdminOrHR
        ? "/api/attendance/all"
        : "/api/attendance/me";

      const response = await apiFetch(
        `${API_URL}${attendanceEndpoint}`,
        {
          method: "GET"
        }
      );

      const data = await response.json().catch(() => []);

      if (!response.ok) {
        throw new Error(
          data.message ||
          "Unable to fetch attendance records."
        );
      }

      setAttendanceRecords(
        Array.isArray(data) ? data : []
      );

    } catch (err) {
      setError(
        err.message || "Unable to load attendance."
      );
    } finally {
      setAttendanceLoading(false);
    }
  };

  // COMMON FUNCTION TO SUBMIT ATTENDANCE LOCATION
  const submitAttendanceLocation = async (action) => {

    setError("");
    setSuccess("");
    setAttendanceLoading(true);

    try {

      setSuccess("Getting your current GPS location...");

      const location = await getCurrentLocationWithAddress();

      const response = await apiFetch(
        `${API_URL}/api/attendance/${action}`,
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            latitude: location.latitude,
            longitude: location.longitude,
            address: location.address
          })
        }
      );

      const data = await response.json().catch(() => ({}));

      if (!response.ok) {
        throw new Error(
          data.message ||
          `Unable to complete ${action.replace("-", " ")}.`
        );
      }

      const actionLabel =
        action === "check-in" ? "Check-in" : "Check-out";

      setSuccess(
        `${actionLabel} successful! Location: ${location.address}`
      );

      await fetchAttendance();

    } catch (err) {
      setSuccess("");
      setError(
        err.message || "Attendance operation failed."
      );
    } finally {
      setAttendanceLoading(false);
    }
  };

  // CHECK-IN
  const handleCheckIn = async () => {
    await submitAttendanceLocation("check-in");
  };

  // CHECK-OUT
  const handleCheckOut = async () => {
    await submitAttendanceLocation("check-out");
  };

  // LOAD DATA WHEN OPENING A PAGE
  useEffect(() => {

    if (!user) return;

    if (
      currentPage === "Employee Management" &&
      isAdminOrHR
    ) {
      fetchEmployees();
    }

    if (currentPage === "Announcements" && isAdminOrHR) {
      fetchEmployees();
    }

    if (currentPage === "My Profile") {
      fetchMyProfile();
    }

    if (currentPage === "Attendance Management") {
      fetchAttendance();
    }

  }, [currentPage, user]);

  // OPEN MODULE
  const openModule = (moduleName) => {

    setError("");
    setSuccess("");

    // Employees cannot access Employee Management
    // Employees CAN access their own payslips
    if (
      moduleName === "Employee Management" &&
      !isAdminOrHR
    ) {
      setError(
        "You are not authorized to access this module."
      );
      return;
    }

    setCurrentPage(moduleName);
  };

  // OPEN PROFILE
  const openProfile = () => {
    setError("");
    setSuccess("");
    setCurrentPage("My Profile");
  };

  // LOGOUT
  const handleLogout = async () => {

    try {
      await apiFetch(
        `${API_URL}/api/auth/logout`,
        {
          method: "POST"
        }
      );
    } catch (err) {
      console.error("Logout error:", err);
    } finally {

      localStorage.removeItem("hrmsToken");
      localStorage.removeItem("hrmsUser");

      setUser(null);
      setProfile(null);
      setEmployees([]);
      setAttendanceRecords([]);

      setEmployeeId("");
      setPassword("");
      setSearch("");

      setCurrentPage("Dashboard");

      setError("");
      setSuccess("");
    }
  };

  // LOGIN PAGE
  if (!user) {
    return (
      <Login
        employeeId={employeeId}
        setEmployeeId={setEmployeeId}
        password={password}
        setPassword={setPassword}
        showPassword={showPassword}
        setShowPassword={setShowPassword}
        handleLogin={handleLogin}
        loading={loading}
        error={error}
      />
    );
  }

  // FIRST LOGIN PASSWORD CHANGE
  if (
    user.mustChangePassword ||
    currentPage === "ChangePassword"
  ) {
    return (
      <div className="auth-page">
        <div className="login-card password-card">

          <div className="login-icon">🔐</div>

          <div className="login-brand-tag">
            OPSOFT • ACCOUNT SECURITY
          </div>

          <h1>Change Password</h1>

          <p className="login-subtitle">
            Create a new password to continue.
          </p>

          <form onSubmit={handleChangePassword}>

            <label>Current Password</label>

            <input
              type="password"
              value={currentPassword}
              onChange={(event) =>
                setCurrentPassword(event.target.value)
              }
              placeholder="Enter current password"
              required
            />

            <label>New Password</label>

            <input
              type="password"
              value={newPassword}
              onChange={(event) =>
                setNewPassword(event.target.value)
              }
              placeholder="Enter new password"
              minLength={12}
              required
            />

            <label>Confirm New Password</label>

            <input
              type="password"
              value={confirmPassword}
              onChange={(event) =>
                setConfirmPassword(event.target.value)
              }
              placeholder="Confirm new password"
              minLength={12}
              required
            />

            {error && (
              <div className="error-message">
                {error}
              </div>
            )}

            <button
              className="login-button"
              type="submit"
              disabled={loading}
            >
              {loading
                ? "Updating..."
                : "Update Password"}
            </button>

          </form>
        </div>
      </div>
    );
  }

  // MAIN APPLICATION
  return (
    <div className="app-container">

      <header className="app-header">

        <div className="app-brand">

          <span className="app-brand-icon">
            🏢
          </span>

          <div>
            <h2>OpsoftHRMS</h2>
            <p>People Management Portal</p>
          </div>

        </div>

        <div className="header-user">

          <div className="header-user-info">
            <strong>
              {user.fullName || user.employeeId}
            </strong>

            <span>{user.role}</span>
          </div>

          <button
            className="logout-button"
            onClick={handleLogout}
          >
            Logout
          </button>

        </div>

      </header>

      <div className="app-body">

        <aside className="sidebar">

          <p className="sidebar-heading">
            MAIN MENU
          </p>

          <button
            className={`sidebar-link ${
              currentPage === "Dashboard" ? "active" : ""
            }`}
            onClick={() => setCurrentPage("Dashboard")}
          >
            <span>🏠</span>
            Dashboard
          </button>

          {isAdminOrHR && (
            <button
              className={`sidebar-link ${
                currentPage === "Employee Management"
                  ? "active"
                  : ""
              }`}
              onClick={() =>
                openModule("Employee Management")
              }
            >
              <span>👥</span>
              Employee Management
            </button>
          )}

          <button
            className={`sidebar-link ${
              currentPage === "Attendance Management"
                ? "active"
                : ""
            }`}
            onClick={() =>
              openModule("Attendance Management")
            }
          >
            <span>🕒</span>
            Attendance
          </button>

          <button
            className={`sidebar-link ${
              currentPage === "Leave Management"
                ? "active"
                : ""
            }`}
            onClick={() =>
              openModule("Leave Management")
            }
          >
            <span>🗓️</span>
            Leave Management
          </button>

          {/* COMPANY HOLIDAYS */}
          <button
            className={`sidebar-link ${
              currentPage === "Company Holidays"
                ? "active"
                : ""
            }`}
            onClick={() =>
              openModule("Company Holidays")
            }
          >
            <span>🎉</span>
            Company Holidays
          </button>

          {/* PAYROLL - ADMIN, HR AND EMPLOYEE */}
          <button
            className={`sidebar-link ${
              currentPage === "Payroll Management"
                ? "active"
                : ""
            }`}
            onClick={() =>
              openModule("Payroll Management")
            }
          >
            <span>💰</span>
            {isAdminOrHR ? "Payroll Management" : "My Payslips"}
          </button>

          {/* ANNOUNCEMENTS */}
          <button
            className={`sidebar-link ${
              currentPage === "Announcements"
                ? "active"
                : ""
            }`}
            onClick={() =>
              openModule("Announcements")
            }
          >
            <span>📢</span>
            Announcements
          </button>

          <p className="sidebar-heading account-heading">
            ACCOUNT
          </p>

          <button
            className={`sidebar-link ${
              currentPage === "My Profile"
                ? "active"
                : ""
            }`}
            onClick={openProfile}
          >
            <span>👤</span>
            My Profile
          </button>

        </aside>

        <main
          key={currentPage}
          className="main-content"
        >

          {error && (
            <div className="app-alert error-message">
              {error}

              <button onClick={() => setError("")}>
                ×
              </button>
            </div>
          )}

          {success && (
            <div className="app-alert success-message">
              {success}

              <button onClick={() => setSuccess("")}>
                ×
              </button>
            </div>
          )}

          {/* DASHBOARD */}
          {currentPage === "Dashboard" && (
            <Dashboard
              user={user}
              onOpenModule={openModule}
              onOpenProfile={openProfile}
            />
          )}

          {/* EMPLOYEE MANAGEMENT */}
          {currentPage === "Employee Management" &&
            isAdminOrHR && (
              <EmployeeManagement
                employees={employees}
                search={search}
                setSearch={setSearch}
                onRefresh={fetchEmployees}
              />
            )}

          {/* PROFILE */}
          {currentPage === "My Profile" && (
            <Profile
              profile={profile}
              onRefresh={fetchMyProfile}
            />
          )}

          {/* ATTENDANCE MANAGEMENT */}
          {currentPage === "Attendance Management" && (
            <Attendance
              records={attendanceRecords}
              onCheckIn={handleCheckIn}
              onCheckOut={handleCheckOut}
              loading={attendanceLoading}
              isAdminOrHR={isAdminOrHR}
              currentUser={user}
            />
          )}

          {/* LEAVE MANAGEMENT */}
          {currentPage === "Leave Management" && (
            <LeaveManagement user={user} />
          )}

          {/* COMPANY HOLIDAYS */}
          {currentPage === "Company Holidays" && (
            <CompanyHoliday user={user} />
          )}

          {/* PAYROLL AND EMPLOYEE PAYSLIPS */}
          {currentPage === "Payroll Management" && (
            <Payroll user={user} />
          )}

          {/* ANNOUNCEMENTS */}
          {currentPage === "Announcements" && (
            <AnnouncementManagement
              user={user}
              employees={employees}
            />
          )}

        </main>

      </div>

    </div>
  );
}

export default App;
