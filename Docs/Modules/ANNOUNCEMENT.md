# Announcement Module

The Announcement module provides a way for administrators to broadcast information, updates, and deadlines to specific groups of users or the entire university.

## Key Components
- **`AnnouncementController`**: REST endpoints for managing and viewing announcements.
- **`Announcement`**: Entity representing a broadcast message.
- **`AnnouncementRepository`**: Data access layer for announcement records.

## Features

### 1. Targeted Broadcasts
Announcements can be targeted to specific audiences using a comma-separated list of tokens. Valid audience tokens include:
- **Roles**: `all`, `faculty`, `hod`, `director`, `dean`, `registrar`, `non_teaching_staff`.
- **Schools**: `SoCSEA`, `SoBB`, `SoCE`, `SoEMR`, `SoC`, `SoMCS`, `SoD`, `SoAA`, `CISR`.

### 2. Visibility Management
- **Active State**: Announcements can be toggled `active` or `inactive`. Only active announcements are returned via the public API.
- **Public Feed**: A public endpoint allows anyone (or authenticated users, depending on security config) to view a feed of active announcements ordered by date.

### 3. Administrative Control
Administrators have full CRUD (Create, Read, Update, Delete) access to announcements:
- **Audit Tracking**: The system tracks which administrator created each announcement via the `createdBy` field.
- **Full History**: Admins can see both active and archived (inactive) announcements in a consolidated list.
- **Audience Validation**: The system strictly validates audience tokens to ensure messages reach the intended recipients.

## Security
- **Public Access**: `GET /api/v1/announcements` is typically permitted to show active broadcasts to all users.
- **Admin Access**: All endpoints under `/api/v1/admin/announcements` require the `ROLE_ADMIN` authority.

## Usage Example (Admin Create)
**Endpoint**: `POST /api/v1/admin/announcements`
**Payload**:
```json
{
  "title": "2026 Appraisal Cycle Open",
  "body": "The appraisal window is now open for all faculty members. Please submit your snapshots by June 30th.",
  "audience": "faculty, hod, director",
  "isActive": true
}
```

**Response**:
```json
{
  "message": "Announcement created",
  "id": 1
}
```
