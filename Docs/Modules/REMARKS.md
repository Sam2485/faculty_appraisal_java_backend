# Remarks & Review Module

The Remarks module provides functionality for reviewers (HODs, Directors, Deans, etc.) to evaluate, score, and provide feedback on faculty appraisals.

## Key Components
- **`RemarksController`**: REST endpoints for saving review drafts and submitting final reviews/rejections.
- **`RemarksService`**: Orchestrates the multi-level review workflow, score updates, and status transitions.
- **DTOs (`com.faculty_appraisal.backend.model.dto.remarks`)**:
    - `SaveDraftRequest`: Used to save a non-binding review draft.
    - `ReviewDraftResponse`: Returns the saved draft payload.
    - `ReviewRequest`: The main payload for finalising a review or rejecting an appraisal.
    - `ReviewResponse`: Response confirmation after a review action.

## Features

### 1. Review Drafts
- Reviewers can save their progress as a "draft" without advancing the appraisal workflow.
- Drafts are stored in the `ReviewerSnapshot` entity (JSONB).
- Saving a draft allows reviewers to work on large appraisals across multiple sessions.

### 2. Final Review Submission
- When a reviewer submits a final review:
    1. **Score Persistence**: Granular scores for each section (e.g., lectures, research) are updated directly on the normalized Part A/B tables using the `ROLE_COLUMN_MAP`.
    2. **Review Summary**: A record is created in `AppraisalReview` storing the total scores and remarks for that specific level.
    3. **Workflow Advancement**: The `Declaration` status is updated (e.g., "Pending HOD Review" -> "Pending Director Review").
    4. **Draft Cleanup**: The temporary review draft is deleted.

### 3. Appraisal Rejection
- Reviewers can reject an appraisal if corrections are needed.
- **Validation**: Only the immediate superior in the workflow chain can reject.
- **Effect**: The `Declaration` status changes to a "Rejected" state (e.g., "HOD Rejected").
- **Locking**: Rejected appraisals are frozen until the faculty member updates and resubmits their data.

### 4. Hierarchical Access & Locking
- **Authorization**: Reviewers can only review faculty members they have authority over (Department/School level).
- **Edit Locking**: 
    - Once a higher level (like the VC) has reviewed, lower levels can no longer modify their remarks.
    - Once "Reviewed" (VC final), the appraisal is considered complete.

## Workflow Status Mapping
The module manages the progression of an appraisal through various states:
- `Pending HOD Review`
- `Pending Director Review`
- `Pending Dean Review`
- `Pending VC Review`
- `Reviewed` (Final state)
