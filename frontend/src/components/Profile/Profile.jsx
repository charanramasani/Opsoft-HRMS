
import React from "react";
import "./Profile.css";

function Profile({ profile, onRefresh }) {
  if (!profile) {
    return (
      <div className="profile-loading">
        <div className="profile-loader"></div>
        <p>Loading your profile...</p>
      </div>
    );
  }

  const details = [
    { label: "Employee ID", value: profile.employeeId },
    { label: "Email Address", value: profile.email },
    { label: "Phone Number", value: profile.phone },
    { label: "Department", value: profile.department },
    { label: "Designation", value: profile.designation },
    { label: "Role", value: profile.role },
    { label: "Employment Status", value: profile.status },
    { label: "Joining Date", value: profile.joiningDate },
  ];

  return (
    <div className="profile-page">
      <div className="profile-page-heading">
        <div>
          <span className="profile-eyebrow">
            OPSOFT • ACCOUNT INFORMATION
          </span>
          <h1>My Profile</h1>
          <p>View your personal and employment information.</p>
        </div>

        <button
          className="profile-refresh-button"
          onClick={onRefresh}
        >
          ↻ Refresh
        </button>
      </div>

      <div className="profile-main-card">
        <div className="profile-top">
          <div className="profile-avatar">👤</div>

          <div>
            <h2>{profile.fullName}</h2>
            <p>{profile.designation || profile.role}</p>
            <span className="profile-status">
              {profile.status || "Unknown"}
            </span>
          </div>
        </div>

        <div className="profile-divider"></div>

        <h3>Employee Information</h3>

        <div className="profile-information-grid">
          {details.map((item) => (
            <div className="profile-information-item" key={item.label}>
              <span>{item.label}</span>
              <strong>{item.value || "Not available"}</strong>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

export default Profile;
