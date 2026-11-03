# Opsoft HRMS – Human Resource Management System

A web-based Human Resource Management System (HRMS) developed using React, Spring Boot, and MySQL to simplify and manage employee-related activities.

## Project Overview

Opsoft HRMS is designed to manage employee information, attendance, leave applications, payroll, and other HR operations through a centralized web application.

## Technologies Used

### Frontend
- React.js
- Vite
- JavaScript
- HTML5
- CSS3

### Backend
- Java 25
- Spring Boot
- Spring Data JPA
- Hibernate
- Maven
- REST APIs

### Database
- MySQL 8

### Development Tools
- Visual Studio Code
- Eclipse IDE
- Git and GitHub
- MySQL Workbench

## Main Modules

1. **Dashboard**
   - HRMS overview
   - Dashboard statistics and charts

2. **Employee Management**
   - Employee information management
   - Employee search and filtering

3. **Attendance Management**
   - Employee check-in and check-out
   - Attendance records

4. **Leave Management**
   - Leave applications
   - HR/Admin approval and rejection

5. **Payroll Management**
   - Salary structure management
   - Employee salary information

6. **Announcements and Holidays**
   - Company announcements
   - Company holiday information

7. **Authentication and User Management**
   - Login and role-based access
   - HR, Admin, and Employee access

## Project Structure

```text
Opsoft-HRMS/
│
├── frontend/
│   ├── src/
│   ├── public/
│   └── package.json
│
├── hrms-backend/
│   ├── src/
│   │   └── main/
│   │       ├── java/
│   │       └── resources/
│   └── pom.xml
│
└── README.md
```

## Prerequisites

Install the following before running the application:

- Java JDK 25
- Node.js and npm
- MySQL 8
- Maven

## Database Configuration

1. Start MySQL Server.
2. Create the database:

```sql
CREATE DATABASE opsoft_hrms;
```

3. Configure your database connection in:

`hrms-backend/src/main/resources/application.properties`

Keep your database username, password, JWT secrets, and other sensitive configuration private. Do not commit this file to GitHub.

## Run the Backend

Open a terminal in the `hrms-backend` directory.

```bash
cd hrms-backend
mvn spring-boot:run
```

Backend URL:

`http://localhost:8083`

## Run the Frontend

Open another terminal in the `frontend` directory.

```bash
cd frontend
npm install
npm run dev
```

Open the frontend URL displayed in your terminal (usually `http://localhost:5173`).

## GitHub Repository

[Opsoft-HRMS – GitHub](https://github.com/charanramasani/Opsoft-HRMS)

## Project Purpose

This project demonstrates full-stack application development using React, Spring Boot, REST APIs, Spring Data JPA, Hibernate, and MySQL.

---

**Developed as an HRMS project at Opsoft.**