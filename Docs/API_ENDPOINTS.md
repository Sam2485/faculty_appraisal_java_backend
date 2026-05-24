# API Documentation

This document lists all the API endpoints available in the Faculty Appraisal Java Backend.

## Base URL
The base URL for all API endpoints is `/api/v1` (except for the health check).

---

## Health Check

### Health
- **URL:** `/`
- **Method:** `GET`
- **Description:** Checks if the API is running.
- **Response:**
  ```json
  {
    "message": "Faculty Appraisal API is running!",
    "version": "2.0.0"
  }
  ```

---

## Auth Module
**Base URL:** `/api/v1/auth`

### Login
- **URL:** `/login`
- **Method:** `POST`
- **Request Body:** `LoginRequest`
  - `email` (String, required)
  - `password` (String, required)
- **Response:** `LoginResponse`
  - `token` (String)
  - `profile` (ProfileResponse)

### Register
- **URL:** `/register`
- **Method:** `POST`
- **Request Body:** `RegisterRequest`
  - `email` (String, required)
  - `password` (String, required)
  - `fullName` (String, required)
  - `appraisalRole` (String, required)
  - `school` (String)
  - `department` (String)
  - `designation` (String)
  - `employeeId` (String)
  - `phone` (String)
  - `qualification` (String)
  - `teachingExperience` (String)
- **Response:** Map with success message.

### Verify Email
- **URL:** `/verify-email`
- **Method:** `GET`
- **Query Params:** `token` (String, required)
- **Description:** Verifies user's email. Redirects to a configured URL.

### Get Current Profile
- **URL:** `/me`
- **Method:** `GET`
- **Auth:** Required
- **Response:** `ProfileResponse`

### Update Current Profile
- **URL:** `/me`
- **Method:** `PUT`
- **Auth:** Required
- **Request Body:** `UpdateProfileRequest`
- **Response:** `ProfileResponse`

### Change Password
- **URL:** `/change-password`
- **Method:** `POST`
- **Auth:** Required
- **Request Body:** `ChangePasswordRequest`
- **Response:** Map with success message.

### Forgot Password
- **URL:** `/forgot-password`
- **Method:** `POST`
- **Request Body:** `ForgotPasswordRequest`
- **Response:** Map with success message.

### Reset Password
- **URL:** `/reset-password`
- **Method:** `POST`
- **Request Body:** `ResetPasswordRequest`
- **Response:** Map with success message.

---

## Admin Module
**Base URL:** `/api/v1/admin`
**Auth:** Admin role required for all endpoints.

### Get Stats
- **URL:** `/stats`
- **Method:** `GET`
- **Query Params:** `academic_year` (String, optional)
- **Auth:** Admin/Super Admin

### Get Environment Config
- **URL:** `/config`
- **Method:** `GET`
- **Auth:** Admin/Super Admin

### Update Environment Config
- **URL:** `/config`
- **Method:** `PUT`
- **Request Body:** Map of config key-value pairs.
- **Auth:** Admin/Super Admin

### List Users
- **URL:** `/users`
- **Method:** `GET`
- **Query Params:** 
  - `school` (optional)
  - `role` (optional)
  - `search` (optional)
- **Auth:** Admin/Super Admin

### Create User
- **URL:** `/users`
- **Method:** `POST`
- **Request Body:** `UserCreateRequest`
- **Auth:** Admin/Super Admin

### Update User
- **URL:** `/users/{email}`
- **Method:** `PUT`
- **Request Body:** `UserUpdateRequest`
- **Auth:** Admin/Super Admin

### Delete User
- **URL:** `/users/{email}`
- **Method:** `DELETE`
- **Auth:** Admin/Super Admin

### List Registrars
- **URL:** `/registrars`
- **Method:** `GET`
- **Auth:** Admin/Super Admin

### List Reporting Officers
- **URL:** `/reporting-officers`
- **Method:** `GET`
- **Auth:** Admin/Super Admin

### List NT Designations
- **URL:** `/nt-designations`
- **Method:** `GET`
- **Auth:** Admin/Super Admin

### Create NT Designation
- **URL:** `/nt-designations`
- **Method:** `POST`
- **Request Body:** `DesignationCreateRequest`

### Update NT Designation
- **URL:** `/nt-designations/{id}`
- **Method:** `PUT`
- **Request Body:** `DesignationUpdateRequest`

### Delete NT Designation
- **URL:** `/nt-designations/{id}`
- **Method:** `DELETE`

### List NT Workflow Templates
- **URL:** `/nt-workflow-templates`
- **Method:** `GET`

### Create NT Workflow Template
- **URL:** `/nt-workflow-templates`
- **Method:** `POST`
- **Request Body:** `TemplateCreateRequest`

### Update NT Workflow Template
- **URL:** `/nt-workflow-templates/{id}`
- **Method:** `PUT`
- **Request Body:** `TemplateUpdateRequest`

### Delete NT Workflow Template
- **URL:** `/nt-workflow-templates/{id}`
- **Method:** `DELETE`

### Set Default NT Template
- **URL:** `/nt-workflow-templates/{id}/set-default`
- **Method:** `PUT`

### Add NT Template Step
- **URL:** `/nt-workflow-templates/{templateId}/steps`
- **Method:** `POST`
- **Request Body:** `StepCreateRequest`

### Update NT Template Step
- **URL:** `/nt-workflow-templates/{templateId}/steps/{stepNo}`
- **Method:** `PUT`
- **Request Body:** `StepUpdateRequest`

### Remove NT Template Step
- **URL:** `/nt-workflow-templates/{templateId}/steps/{stepNo}`
- **Method:** `DELETE`

### Reorder NT Template Steps
- **URL:** `/nt-workflow-templates/{templateId}/reorder`
- **Method:** `PUT`
- **Request Body:** `ReorderRequest`

### List NT Workflow Assignments
- **URL:** `/nt-workflow-assignments`
- **Method:** `GET`

### Create NT Workflow Assignment
- **URL:** `/nt-workflow-assignments`
- **Method:** `POST`
- **Request Body:** `AssignmentCreateRequest`

### Delete NT Workflow Assignment
- **URL:** `/nt-workflow-assignments/{id}`
- **Method:** `DELETE`

### Get Pending Faculty
- **URL:** `/pending-faculty`
- **Method:** `GET`
- **Query Params:** `academic_year` (required), `school` (optional)

### List Submissions
- **URL:** `/submissions`
- **Method:** `GET`
- **Query Params:** `academic_year`, `school` (optional)

### Export Submissions (CSV)
- **URL:** `/export/submissions`
- **Method:** `GET`
- **Query Params:** `academic_year`, `school` (optional)

### Export Faculty (CSV)
- **URL:** `/export/faculty`
- **Method:** `GET`
- **Query Params:** `school`, `role` (optional)

### Get Trends
- **URL:** `/trends`
- **Method:** `GET`
- **Query Params:** `academic_year` (optional)

### List Appraisal Configs
- **URL:** `/appraisal-config`
- **Method:** `GET`

### Create Appraisal Config
- **URL:** `/appraisal-config`
- **Method:** `POST`
- **Request Body:** `AppraisalConfigCreateRequest`

### Update Appraisal Config
- **URL:** `/appraisal-config/{academicYear}`
- **Method:** `PUT`
- **Request Body:** `AppraisalConfigUpdateRequest`

### Delete Appraisal Config
- **URL:** `/appraisal-config/{academicYear}`
- **Method:** `DELETE`

### Get Module Config
- **URL:** `/module-config`
- **Method:** `GET`

### Update Module Config
- **URL:** `/module-config`
- **Method:** `PUT`
- **Request Body:** `ModuleConfigUpdateRequest`

---

## Announcements Module
**Base URL:** `/api/v1`

### List Active Announcements (Public)
- **URL:** `/announcements`
- **Method:** `GET`
- **Description:** Security config must permitAll.

### List All Announcements (Admin)
- **URL:** `/admin/announcements`
- **Method:** `GET`
- **Auth:** Admin role required.

### Create Announcement (Admin)
- **URL:** `/admin/announcements`
- **Method:** `POST`
- **Auth:** Admin role required.
- **Request Body:** `AnnouncementCreateRequest`
  - `title` (String, required)
  - `body` (String, required)
  - `audience` (String, default "all")
  - `isActive` (boolean, default true)

### Update Announcement (Admin)
- **URL:** `/admin/announcements/{id}`
- **Method:** `PUT`
- **Auth:** Admin role required.
- **Request Body:** `AnnouncementUpdateRequest`

### Delete Announcement (Admin)
- **URL:** `/admin/announcements/{id}`
- **Method:** `DELETE`
- **Auth:** Admin role required.

---

## Appraisal Module
**Base URL:** `/api/v1/appraisal`
**Auth:** Required

### Get Snapshot
- **URL:** `/snapshot`
- **Method:** `GET`
- **Query Params:** `academicYear` (required)

### Upsert Snapshot
- **URL:** `/snapshot`
- **Method:** `PUT`
- **Request Body:** `SnapshotRequest`
  - `academicYear` (String, required)
  - `payload` (Map)
  - `docs` (Map)

### Submit Appraisal
- **URL:** `/submit`
- **Method:** `POST`
- **Request Body:** `SubmitAppraisalRequest`

### Get Status
- **URL:** `/status`
- **Method:** `GET`
- **Query Params:** `academicYear` (required)

---

## Dashboard Module
**Base URL:** `/api/v1/dashboard`
**Auth:** Required (Reviewer roles)

### Get Subordinates
- **URL:** `/subordinates`
- **Method:** `GET`
- **Query Params:** `academicYear` (required), `reviewerSchool`, `reviewerDepartment`, `schools` (optional)

### Get Faculty Snapshot
- **URL:** `/faculty/{email}`
- **Method:** `GET`
- **Query Params:** `academicYear` (required)

---

## Documents Module
**Base URL:** `/api/v1/appraisal-documents`
**Auth:** Required

### Get My Documents
- **URL:** `/`
- **Method:** `GET`
- **Query Params:** `academic_year` (required)

---

## Feedback Module
**Base URL:** `/api/v1/feedback`

### Create Feedback (Public)
- **URL:** `/`
- **Method:** `POST`
- **Request Body:** `FeedbackRequest`
  - `name` (String, optional)
  - `email` (String, required)
  - `category` (String, required: "query", "feedback", "bug", "suggestion", "other")
  - `subject` (String, required)
  - `message` (String, required)

### List Feedback (Admin)
- **URL:** `/`
- **Method:** `GET`
- **Auth:** Admin role required.
- **Query Params:** `limit` (default 50), `category`, `status` (optional)

### Get Feedback Details (Admin)
- **URL:** `/ {feedbackId}`
- **Method:** `GET`
- **Auth:** Admin role required.

---

## Non-Teaching Module
**Base URL:** `/api/v1/non-teaching`
**Auth:** Required

### Get Workflow Template
- **URL:** `/workflow-template`
- **Method:** `GET`
- **Query Params:** `role` (optional)

### Get Workflow for Staff
- **URL:** `/workflow/{email}`
- **Method:** `GET`
- **Query Params:** `academicYear` (required)

### Get My Appraisal
- **URL:** `/appraisal`
- **Method:** `GET`
- **Query Params:** `academic_year` (required)

### Upsert My Appraisal
- **URL:** `/appraisal`
- **Method:** `PUT`
- **Request Body:** `NtAppraisalRequest`

### Get Subordinates
- **URL:** `/subordinates`
- **Method:** `GET`
- **Query Params:** `academic_year` (required)

### Review Non-Teaching Appraisal
- **URL:** `/review/{email}`
- **Method:** `PUT`
- **Request Body:** `NtReviewRequest`

---

## Remarks Module
**Base URL:** `/api/v1/appraisal-remarks`
**Auth:** Required (Reviewer roles)

### Get Review Draft
- **URL:** `/draft/{email}`
- **Method:** `GET`
- **Query Params:** `academicYear`, `reviewerRole` (required)

### Save Review Draft
- **URL:** `/draft/{email}`
- **Method:** `PUT`
- **Request Body:** `SaveDraftRequest`

### HOD Review
- **URL:** `/hod/{email}`
- **Method:** `PUT`
- **Request Body:** `ReviewRequest`

### Center Head Review
- **URL:** `/center-head/{email}`
- **Method:** `PUT`
- **Request Body:** `ReviewRequest`

### Director Review
- **URL:** `/director/{email}`
- **Method:** `PUT`
- **Request Body:** `ReviewRequest`

### Dean Review
- **URL:** `/dean/{email}`
- **Method:** `PUT`
- **Request Body:** `ReviewRequest`

### Final (VC) Review
- **URL:** `/final/{email}`
- **Method:** `PUT`
- **Request Body:** `ReviewRequest`

---

## Upload Module
**Base URL:** `/api/v1`
**Auth:** Required

### Upload File
- **URL:** `/upload`
- **Method:** `POST`
- **Request Type:** `multipart/form-data`
- **Parts:**
  - `file` (MultipartFile, required)
- **Query Params:** `folder` (optional)
- **Response:** `UploadResponse`
  - `url` (String)
  - `fileName` (String)
  - `fileSize` (long)
  - `contentType` (String)
