
import React from "react";
import "./Login.css";

function Login({
  employeeId,
  setEmployeeId,
  password,
  setPassword,
  showPassword,
  setShowPassword,
  handleLogin,
  loading,
  error
}) {
  return (
    <div className="auth-page">

      {/* Animated background decorations */}
      <div className="login-orb login-orb-one"></div>
      <div className="login-orb login-orb-two"></div>
      <div className="login-orb login-orb-three"></div>
      <div className="login-glow"></div>

      {/* Login card */}
      <div className="login-card">

        {/* Company logo */}
        <div className="login-logo-container">
          <img
            src={`${import.meta.env.BASE_URL}opsoft-logo.png`}
            alt="OpSoft Technologies Logo"
            className="login-company-logo"
          />
        </div>

        {/* Branding */}
        <div className="login-brand-tag">
          OPSOFT • PEOPLE MANAGEMENT
        </div>

        <h1>OpsoftHRMS</h1>

        <p className="login-subtitle">
          Employee Management Portal
        </p>

        {/* Welcome section */}
        <div className="login-welcome-mark">
          <span className="login-welcome-line"></span>
          <span>SECURE WORKSPACE</span>
          <span className="login-welcome-line"></span>
        </div>

        <h2>Welcome Back!</h2>

        <p className="login-description">
          Sign in to your employee workspace
        </p>

        {/* Login form */}
        <form onSubmit={handleLogin}>

          <label htmlFor="employeeId">
            Employee ID
          </label>

          <input
            id="employeeId"
            type="text"
            placeholder="Enter your Employee ID"
            value={employeeId}
            onChange={(event) =>
              setEmployeeId(event.target.value)
            }
            autoComplete="username"
            required
          />

          <label htmlFor="password">
            Password
          </label>

          <div className="login-password-wrap">

            <input
              id="password"
              type={showPassword ? "text" : "password"}
              placeholder="Enter your password"
              value={password}
              onChange={(event) =>
                setPassword(event.target.value)
              }
              autoComplete="current-password"
              required
            />

            <button
              type="button"
              className="password-toggle"
              onClick={() =>
                setShowPassword((visible) => !visible)
              }
              aria-label={
                showPassword ? "Hide password" : "Show password"
              }
            >
              {showPassword ? "Hide" : "Show"}
            </button>

          </div>

          {/* Error message */}
          {error && (
            <div className="error-message" role="alert">
              {error}
            </div>
          )}

          {/* Login button */}
          <button
            className="login-button"
            type="submit"
            disabled={loading}
          >
            <span>
              {loading ? "Signing In..." : "Sign In"}
            </span>
            {!loading && <span className="login-button-arrow">→</span>}
            {loading && <span className="login-spinner"></span>}
          </button>

        </form>

        {/* Footer */}
        <p className="login-footer">
          <span className="footer-dot"></span>
          Secure Employee Access
        </p>

        <p className="login-copyright">
          OpsoftHRMS © 2026
        </p>

      </div>
    </div>
  );
}

export default Login;
