-- Migration 004: Add missing indexes
-- All Part A/B tables were being full-scanned on every form load and review write.
-- faculty_profiles had no index on school/department, hitting full scans on every
-- dashboard subordinates query.
--
-- Safe to run on a live DB. IF NOT EXISTS makes every statement idempotent.
-- Note: migrations/001 unique constraints already create implicit indexes on:
--   declarations(faculty_email, academic_year)
--   appraisal_snapshots(faculty_email, academic_year)
--   appraisal_reviews(faculty_email, academic_year, reviewer_role)
--   non_teaching_appraisals(staff_email, academic_year)
-- Those are NOT repeated here.

-- faculty_profiles
CREATE INDEX IF NOT EXISTS idx_faculty_profiles_school
    ON faculty_profiles (school);

CREATE INDEX IF NOT EXISTS idx_faculty_profiles_school_dept
    ON faculty_profiles (school, department);

CREATE INDEX IF NOT EXISTS idx_faculty_profiles_role
    ON faculty_profiles (appraisal_role);

-- declarations
CREATE INDEX IF NOT EXISTS idx_declarations_academic_year
    ON declarations (academic_year);

CREATE INDEX IF NOT EXISTS idx_declarations_status
    ON declarations (status);

-- appraisal_documents
CREATE INDEX IF NOT EXISTS idx_appraisal_documents_email_year
    ON appraisal_documents (faculty_email, academic_year);

-- Part A tables
CREATE INDEX IF NOT EXISTS idx_teaching_process_email_year
    ON teaching_process (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_course_files_email_year
    ON course_files (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_innovative_teaching_email_year
    ON innovative_teaching (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_projects_guided_email_year
    ON projects_guided (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_qualification_enhancement_email_year
    ON qualification_enhancement (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_student_feedback_email_year
    ON student_feedback (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_department_activities_email_year
    ON department_activities (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_university_activities_email_year
    ON university_activities (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_social_contributions_email_year
    ON social_contributions (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_industry_connect_email_year
    ON industry_connect (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_acr_scores_email_year
    ON acr_scores (faculty_email, academic_year);

-- Part B tables
CREATE INDEX IF NOT EXISTS idx_journal_publications_email_year
    ON journal_publications (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_popular_writings_email_year
    ON popular_writings (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_book_publications_email_year
    ON book_publications (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_ict_pedagogy_email_year
    ON ict_pedagogy (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_research_guidance_email_year
    ON research_guidance (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_research_projects_email_year
    ON research_projects (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_external_research_projects_email_year
    ON external_research_projects (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_ipr_records_email_year
    ON ipr_records (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_patents_email_year
    ON patents (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_awards_email_year
    ON awards (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_conferences_email_year
    ON conferences (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_research_proposals_email_year
    ON research_proposals (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_products_developed_email_year
    ON products_developed (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_self_development_email_year
    ON self_development (faculty_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_industrial_training_email_year
    ON industrial_training (faculty_email, academic_year);

-- Non-teaching detail tables
CREATE INDEX IF NOT EXISTS idx_nt_part_a_items_email_year
    ON non_teaching_part_a_items (staff_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_nt_part_b_ratings_email_year
    ON non_teaching_part_b_ratings (staff_email, academic_year);

CREATE INDEX IF NOT EXISTS idx_non_teaching_academic_year
    ON non_teaching_appraisals (academic_year);

CREATE INDEX IF NOT EXISTS idx_non_teaching_status
    ON non_teaching_appraisals (status);
