# Feedback Module

The Feedback module allows users (authenticated or otherwise) to submit queries, bug reports, suggestions, or general feedback about the system. It also provides administrative tools for reviewing and managing these submissions.

## Key Components
- **`FeedbackController`**: REST endpoints for submitting and listing feedback.
- **`Feedback`**: Entity representing a single feedback submission.
- **`FeedbackRequest`**: DTO for incoming feedback data.
- **`FeedbackRepository`**: Data access layer for feedback records.

## Features

### 1. Feedback Submission
- **Public Access**: Submission of feedback is typically permitted without authentication, allowing users to report issues even if they cannot log in.
- **Categorization**: Submissions must fall into one of the following valid categories:
    - `query`
    - `feedback`
    - `bug`
    - `suggestion`
    - `other`
- **Metadata Capture**: The system automatically captures the user's **IP Address** and **User-Agent** string to assist in technical debugging.

### 2. Administrative Review
Administrators can use the `GET` endpoints to monitor submissions:
- **Filtering**: Feedback can be filtered by `category` and/or `status`.
- **Pagination/Limiting**: The list endpoint supports a `limit` parameter (default 50, max 100) to keep response sizes manageable.
- **Detailed View**: Admins can drill down into a specific feedback item to see the full message and technical metadata.

## Security
- **Submission**: Protected by category validation and basic string sanitization.
- **Review**: The `listFeedback` and `getFeedback` endpoints are strictly restricted to users with `admin` or `super_admin` roles.

## Usage Example
**Endpoint**: `POST /api/v1/feedback`
**Payload**:
```json
{
  "name": "Jane Doe",
  "email": "jane@example.com",
  "category": "bug",
  "subject": "Login Timeout",
  "message": "I'm being logged out every 5 minutes."
}
```

**Response**:
```json
{
  "success": true,
  "message": "Feedback saved.",
  "feedback": {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "status": "New",
    "submitted_at": "2026-05-23T16:00:00"
  }
}
```
