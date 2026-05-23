# Faculty Appraisal Backend Documentation

Welcome to the documentation for the Faculty Appraisal Java Backend. This project is a Spring Boot-based REST API designed to manage the appraisal lifecycle for both faculty and non-teaching staff.

## Table of Contents

1. [Architecture Overview](Architecture/OVERVIEW.md) - High-level system design.
2. [Modules](Modules/)
    - [Authentication & User Management](Modules/AUTH.md)
    - [Appraisal Lifecycle](Modules/APPRAISAL.md)
    - [Remarks & Review](Modules/REMARKS.md)
    - [Non-Teaching Appraisal](Modules/NON_TEACHING.md)
    - [Document Management](Modules/DOCUMENT.md)
    - [Upload & Storage](Modules/UPLOAD_STORAGE.md)
    - [Admin Management](Modules/ADMIN.md)
    - [User Feedback](Modules/FEEDBACK.md)
    - [Announcements](Modules/ANNOUNCEMENT.md)
    - [Dashboard & Reporting](Modules/DASHBOARD.md)
3. [Database Schema](Database/SCHEMA.md) - Entity relationships and data storage.
4. [Security & Authorization](Security/SECURITY.md) - JWT and role-based access control.
5. [Maintenance & Bug Fixes](Maintenance/DASHBOARD_SERVICE_FIX.md) - Log of important fixes.

## Project Structure

- `src/main/java/com/faculty_appraisal/backend/`
    - `config/`: Application configuration.
    - `controller/`: REST API endpoints.
    - `service/`: Business logic.
    - `repository/`: Data access layer (Spring Data JPA).
    - `model/`: Entities and DTOs.
    - `security/`: Security configuration and utilities.
