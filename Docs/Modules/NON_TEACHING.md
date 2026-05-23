# Non-Teaching Appraisal Module

The Non-Teaching module manages the appraisal lifecycle for non-teaching staff, which differs from the faculty workflow by using a template-driven, step-by-step approval process.

## Key Components
- **`NonTeachingController`**: REST endpoints for staff appraisals, subordinate management, and workflow tracking.
- **`NonTeachingService`**: Orchestrates the appraisal lifecycle, including "shredding" data into normalized tables and managing workflow transitions.
- **Entities (`com.faculty_appraisal.backend.model.entity.non_teaching`)**:
    - `NonTeachingAppraisal`: The main entity storing submission status, totals, and the full JSON payload.
    - `NonTeachingPartAItem`: Normalized records for responsibilities, contributions, and achievements.
    - `NonTeachingPartBRating`: Granular ratings for professional competence, quality of work, and personal characteristics.
    - **Workflow System**: `NTWorkflowTemplate`, `NTWorkflowInstance`, and `NTWorkflowInstanceStep` manage the approval chain.

## Features

### 1. Template-Driven Workflows
Unlike faculty appraisals which have a fixed HOD -> Director -> Dean -> VC chain, non-teaching appraisals use flexible templates.
- Templates are resolved based on: Individual Assignment > Department > Appraisal Role > Global Default.
- A template defines a sequence of steps, each associated with a specific designation (e.g., Registrar, VC, Director).

### 2. Appraisal Submission
- Staff members can save drafts or submit their final appraisal.
- **Shredding**: Upon submission, the JSON payload is "shredded" into:
    - **Part A**: Qualitative items (Responsibilities, Contributions, Achievements).
    - **Part B**: Quantitative ratings across 4 sections (Professional Competence, Quality, Personal, Regularity).
- **Workflow Initialization**: On the first submission, a `NTWorkflowInstance` is created based on the resolved template.

### 3. Review Process
- Reviewers (Reporting Officers, Registrars, etc.) can see their pending tasks via the `/subordinates` endpoint.
- **Hierarchical Access**: A reviewer only sees a subordinate if it is their turn in the workflow sequence (matching the current step's designation).
- **Status Advancement**: When a reviewer submits their scores/remarks:
    1. Their ratings are updated in the normalized Part A and Part B tables.
    2. The current `NTWorkflowInstanceStep` is marked as `APPROVED`.
    3. The next step in the sequence is marked as `PENDING`.
    4. If no steps remain, the appraisal status becomes `COMPLETED`.

### 4. Dynamic Routing
- Some staff members report directly to the Registrar, bypassing the standard Reporting Officer (RO) step.
- The system automatically detects this via the `reportsToRegistrar` flag in the `FacultyProfile` and adjusts the initial status to `Pending Registrar Review`.

## Part B Sections
Ratings are collected across several categories with 5-point scales:
1. **Professional Competence** (5 parameters)
2. **Quality of Work** (5 parameters)
3. **Personal Characteristics** (6 parameters)
4. **Regularity** (5 parameters)
