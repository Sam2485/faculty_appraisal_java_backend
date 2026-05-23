-- Migration 021: widen reviewer_role CHECK on appraisal_reviews
-- The original constraint only allowed the 5 original review roles.
-- registrar, reporting_officer, and section_head are now valid reviewer roles.

ALTER TABLE public.appraisal_reviews
  DROP CONSTRAINT IF EXISTS appraisal_reviews_reviewer_role_check;

ALTER TABLE public.appraisal_reviews
  ADD CONSTRAINT appraisal_reviews_reviewer_role_check
  CHECK (reviewer_role IN (
    'hod', 'center_head', 'director', 'dean', 'vc',
    'registrar', 'reporting_officer', 'section_head'
  ));
