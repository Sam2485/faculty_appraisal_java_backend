# Database Schema & Entity Relationships

The system uses a PostgreSQL database. The schema is divided into core management tables and specific appraisal data tables.

**Note**: All schema changes are managed via [Flyway Migrations](MIGRATIONS.md). Hibernate's `ddl-auto` is disabled to ensure schema integrity.

## 1. Core Management
- **`faculty_profiles`**: Central user table storing names, emails, hashed passwords, roles, schools, and departments.
- **`declarations`**: Tracking table for final appraisal submissions, including status (Draft/Submitted) and total scores.
- **`appraisal_snapshots`**: Stores the full JSON payload (JSONB) of the appraisal form.
- **`appraisal_reviews`**: Stores evaluations and remarks from different reviewers.
- **`appraisal_documents`**: Metadata for uploaded files associated with an appraisal.

## 2. Part A: Teaching & Institutional Activities
Normalized tables storing specific data points for faculty teaching performance:
- `acr_scores`
- `course_files`
- `department_activities`
- `industry_connects`
- `innovative_teaching`
- `projects_guided`
- `qualification_enhancements`
- `social_contributions`
- `student_feedbacks`
- `teaching_processes`
- `university_activities`

## 3. Part B: Research & Development
Normalized tables for research and scholarly output:
- `awards`
- `book_publications`
- `conferences`
- `external_research_projects`
- `ict_pedagogies`
- `industrial_trainings`
- `ipr_records`
- `journal_publications`
- `patents`
- `popular_writings`
- `product_developments`
- `research_guidances`
- `research_projects`
- `research_proposals`
- `self_developments`

## 4. Non-Teaching Subsystem
A distinct set of tables for non-teaching staff workflows:
- `non_teaching_appraisals`
- `nt_workflow_templates` & `nt_workflow_template_steps`
- `nt_workflow_instances` & `nt_workflow_instance_steps`
- `nt_designations`

## Entity Inheritance
Most Part A and Part B entities extend `BaseAppraisalModel` (or `BasePartAModel`/`BasePartBModel`), which provides common fields like:
- `faculty_email`
- `academic_year`
- `row_no` (for multi-row entries)
- Scoring fields for HOD, Center Head, Director, Dean, and VC.
