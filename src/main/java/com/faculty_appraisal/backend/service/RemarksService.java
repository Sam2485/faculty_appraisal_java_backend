package com.faculty_appraisal.backend.service;

import com.faculty_appraisal.backend.exception.AppException;
import com.faculty_appraisal.backend.model.dto.remarks.*;
import com.faculty_appraisal.backend.model.entity.core.*;
import com.faculty_appraisal.backend.repository.core.*;
import com.faculty_appraisal.backend.security.CurrentUser;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RemarksService {

    // ── Constants ────────────────────────────────────────────────────────────

    private static final Set<String> REJECTED_STATUSES = Set.of(
            "HOD Rejected", "Center Head Rejected", "Director Rejected",
            "Dean Rejected", "VC Rejected", "Registrar Rejected", "Reporting Officer Rejected"
    );

    private static final Set<String> VALID_REVIEWER_ROLES = Set.of(
            "hod", "center_head", "director", "dean", "vc", "registrar", "reporting_officer", "section_head"
    );

    private static final Map<String, Set<String>> FIRST_REVIEWER_FOR_ROLE = Map.of(
            "faculty",      Set.of("hod", "center_head"),
            "hod",          Set.of("director"),
            "section_head", Set.of("director", "center_head"),
            "director",     Set.of("dean"),
            "dean",         Set.of("vc"),
            "center_head",  Set.of("vc")
    );

    private static final Map<String, Set<String>> ACTIVE_REVIEWER_FOR_STATUS = Map.ofEntries(
            Map.entry("Pending HOD Review",         Set.of("hod")),
            Map.entry("Pending Center Head Review", Set.of("center_head")),
            Map.entry("Pending Director Review",    Set.of("director")),
            Map.entry("Pending Dean Review",        Set.of("dean")),
            Map.entry("Pending VC Review",          Set.of("vc")),
            Map.entry("Pending Registrar Review",   Set.of("registrar")),
            Map.entry("Pending RO Review",          Set.of("reporting_officer"))
    );

    private static final Map<String, String> STATUS_MAP = Map.of(
            "hod",         "Pending Director Review",
            "center_head", "Pending VC Review",
            "director",    "Pending Dean Review",
            "dean",        "Pending VC Review",
            "vc",          "Reviewed"
    );

    private static final Map<String, String> ROLE_DISPLAY = Map.of(
            "hod",                "HOD",
            "center_head",        "Center Head",
            "director",           "Director",
            "dean",               "Dean",
            "vc",                 "VC",
            "registrar",          "Registrar",
            "reporting_officer",  "Reporting Officer"
    );

    // Section key → JPQL entity class name
    private static final Map<String, String> SECTION_ENTITY_MAP = Map.ofEntries(
            Map.entry("lectures",         "TeachingProcess"),
            Map.entry("courseFile",       "CourseFile"),
            Map.entry("innovDetails",     "InnovativeTeaching"),
            Map.entry("projects",         "ProjectGuided"),
            Map.entry("quals",            "QualificationEnhancement"),
            Map.entry("feedback",         "StudentFeedback"),
            Map.entry("deptActs",         "DepartmentActivity"),
            Map.entry("uniActs",          "UniversityActivity"),
            Map.entry("society",          "SocialContribution"),
            Map.entry("industry",         "IndustryConnect"),
            Map.entry("acr",              "ACRScore"),
            Map.entry("journals",         "JournalPublication"),
            Map.entry("books",            "BookPublication"),
            Map.entry("ict",              "ICTPedagogy"),
            Map.entry("research",         "ResearchGuidance"),
            Map.entry("projects2",        "ResearchProject"),
            Map.entry("externalProjects", "ExternalResearchProject"),
            Map.entry("patents",          "Patent"),
            Map.entry("awards",           "Award"),
            Map.entry("confs",            "Conference"),
            Map.entry("proposals",        "ResearchProposal"),
            Map.entry("products",         "ProductDeveloped"),
            Map.entry("fdps",             "SelfDevelopment"),
            Map.entry("training",         "IndustrialTraining")
    );

    // Role → JPQL field name on BaseAppraisalModel
    // center_head writes to directorScore (same column as director — matches Python logic)
    private static final Map<String, String> ROLE_COLUMN_MAP = Map.of(
            "hod",         "hodScore",
            "center_head", "directorScore",
            "director",    "directorScore",
            "dean",        "deanScore",
            "vc",          "vcScore"
    );

    // ── Injected ─────────────────────────────────────────────────────────────

    private final FacultyProfileRepository facultyRepo;
    private final DeclarationRepository declarationRepo;
    private final AppraisalReviewRepository reviewRepo;
    private final ReviewerSnapshotRepository snapshotRepo;
    private final EntityManager em;

    // ── Draft endpoints ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ReviewDraftResponse getDraft(String email, String academicYear,
                                        String reviewerRole, CurrentUser currentUser) {
        String role = reviewerRole.trim().toLowerCase();

        if (!currentUser.getRoles().contains(role) && !currentUser.getRoles().contains("admin")) {
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                    "You do not have the '" + role + "' role.");
        }

        FacultyProfile target = facultyRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Faculty not found"));

        if (!currentUser.hasAuthorityOver(email, target.getAppraisalRole(),
                target.getDepartment(), target.getSchool())) {
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                    "Not authorized to read a review draft for this faculty");
        }

        return snapshotRepo.findByFacultyEmailAndAcademicYearAndReviewerRole(email, academicYear, role)
                .map(s -> new ReviewDraftResponse(s.getPayload(), s.getUpdatedAt()))
                .orElse(new ReviewDraftResponse(null, null));
    }

    @Transactional
    public Map<String, String> saveDraft(String email, SaveDraftRequest req, CurrentUser currentUser) {
        String role = req.getReviewerRole().trim().toLowerCase();

        if (!VALID_REVIEWER_ROLES.contains(role)) {
            throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY.value(),
                    "Invalid reviewer_role '" + role + "'");
        }
        if (!currentUser.getRoles().contains(role) && !currentUser.getRoles().contains("admin")) {
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                    "You do not have the '" + role + "' role.");
        }

        FacultyProfile target = facultyRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Faculty not found"));

        if (!currentUser.hasAuthorityOver(email, target.getAppraisalRole(),
                target.getDepartment(), target.getSchool())) {
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                    "Not authorized to save a review draft for this faculty");
        }

        ReviewerSnapshot snapshot = snapshotRepo
                .findByFacultyEmailAndAcademicYearAndReviewerRole(email, req.getAcademicYear(), role)
                .orElseGet(() -> {
                    ReviewerSnapshot s = new ReviewerSnapshot();
                    s.setFacultyEmail(email);
                    s.setAcademicYear(req.getAcademicYear());
                    s.setReviewerRole(role);
                    return s;
                });

        snapshot.setReviewerEmail(currentUser.getEmail());
        snapshot.setPayload(req.getPayload() != null ? req.getPayload() : Map.of());
        snapshotRepo.save(snapshot);

        return Map.of("message", "Draft saved");
    }

    // ── Final review ─────────────────────────────────────────────────────────

    @Transactional
    public ReviewResponse handleReview(String role, String email,
                                       ReviewRequest req, CurrentUser currentUser) {
        FacultyProfile target = facultyRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Faculty not found"));

        if (!currentUser.hasAuthorityOver(email, target.getAppraisalRole(),
                target.getDepartment(), target.getSchool())) {
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                    "Not authorized to update remarks for this faculty");
        }

        Declaration decl = declarationRepo
                .findByFacultyEmailAndAcademicYear(email, req.getAcademicYear())
                .orElse(null);

        // Lock: once VC finalises, only VC may re-edit
        if (!"vc".equals(role) && decl != null && "Reviewed".equals(decl.getStatus())) {
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                    "This appraisal has been finalised by the VC and can no longer be modified.");
        }

        // Lock: rejected appraisal is frozen until faculty resubmits
        if (decl != null && REJECTED_STATUSES.contains(decl.getStatus())
                && !req.isRejectionRequest()) {
            throw new AppException(HttpStatus.CONFLICT.value(),
                    "This appraisal has been rejected and is awaiting resubmission by the faculty.");
        }

        boolean isRejection = req.isRejectionRequest();

        if (isRejection) {
            String rejectionRemarks = req.getRejectionRemarks();
            if (rejectionRemarks.isBlank()) {
                throw new AppException(HttpStatus.UNPROCESSABLE_ENTITY.value(),
                        "Remarks are mandatory when rejecting an appraisal.");
            }
            if (decl == null) {
                throw new AppException(HttpStatus.BAD_REQUEST.value(),
                        "No submitted appraisal found for this faculty and year.");
            }
            if (REJECTED_STATUSES.contains(decl.getStatus())) {
                throw new AppException(HttpStatus.CONFLICT.value(),
                        "This appraisal has already been rejected and is awaiting resubmission.");
            }
            if (!isImmediateSuperior(role, target.getAppraisalRole(), decl.getStatus())) {
                throw new AppException(HttpStatus.FORBIDDEN.value(),
                        "Only the immediate superior may reject. Current workflow status is '"
                                + decl.getStatus() + "'.");
            }
        }

        // Upsert the AppraisalReview row
        AppraisalReview review = reviewRepo
                .findByFacultyEmailAndAcademicYearAndReviewerRole(email, req.getAcademicYear(), role)
                .orElseGet(() -> {
                    AppraisalReview r = new AppraisalReview();
                    r.setFacultyEmail(email);
                    r.setAcademicYear(req.getAcademicYear());
                    r.setReviewerRole(role);
                    return r;
                });

        review.setReviewerEmail(currentUser.getEmail());
        review.setPartAScore(BigDecimal.valueOf(req.getPartAScore()));
        review.setPartBScore(BigDecimal.valueOf(req.getPartBScore()));
        review.setTotalScore(BigDecimal.valueOf(req.getTotalScore()));
        review.setStatus(isRejection ? "Rejected" : "Reviewed");
        review.setReviewedAt(LocalDateTime.now());

        if (isRejection) {
            review.setRemarks(req.getRejectionRemarks());
        } else {
            review.setRemarks(req.getRemarks());
            if (req.getSectionScores() != null) {
                review.setSectionScores(req.getSectionScores());
            }
        }

        reviewRepo.save(review);

        // Update per-row scores on part_a / part_b tables
        if (req.getSectionScores() != null && !req.getSectionScores().isEmpty()) {
            updateItemScores(email, req.getAcademicYear(), role, req.getSectionScores());
        }

        // Advance (or reject) the declaration status
        if (decl != null) {
            if (isRejection) {
                String displayRole = ROLE_DISPLAY.getOrDefault(role,
                        role.replace("_", " "));
                // capitalise first letter of each word
                String rejectedStatus = Arrays.stream(displayRole.split(" "))
                        .map(w -> Character.toUpperCase(w.charAt(0)) + w.substring(1))
                        .collect(java.util.stream.Collectors.joining(" ")) + " Rejected";
                decl.setStatus(rejectedStatus);
            } else {
                decl.setStatus(STATUS_MAP.getOrDefault(role, decl.getStatus()));
            }
            declarationRepo.save(decl);
        }

        // Delete the reviewer's in-progress draft — it's now superseded
        snapshotRepo.deleteByFacultyEmailAndAcademicYearAndReviewerRole(
                email, req.getAcademicYear(), role);

        ReviewResponse.ReviewResponseBuilder builder = ReviewResponse.builder()
                .message(isRejection ? "Appraisal rejected" : "Review submitted")
                .status(decl != null ? decl.getStatus() : "unknown")
                .decision(isRejection ? "rejected" : "approved");

        if (isRejection) {
            builder.nextReviewer(null).nextReviewerRole(null).nextReviewerEmail(null);
        }

        return builder.build();
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private boolean isImmediateSuperior(String reviewerRole, String subjectRole, String currentStatus) {
        Set<String> allowed = ACTIVE_REVIEWER_FOR_STATUS.get(currentStatus);
        if (allowed != null) return allowed.contains(reviewerRole);

        if ("Submitted".equals(currentStatus)) {
            Set<String> firstReviewers = FIRST_REVIEWER_FOR_ROLE.getOrDefault(
                    subjectRole != null ? subjectRole.toLowerCase() : "faculty", Set.of());
            return firstReviewers.contains(reviewerRole);
        }
        return false;
    }

    private void updateItemScores(String email, String year, String role,
                                  Map<String, Object> sectionScores) {
        String column = ROLE_COLUMN_MAP.get(role);
        if (column == null) return;

        sectionScores.forEach((sectionKey, rawScore) -> {
            String entityName = SECTION_ENTITY_MAP.get(sectionKey);
            if (entityName == null) return;

            double numericScore = extractNumericScore(rawScore, role);
            try {
                em.createQuery(
                        "UPDATE " + entityName + " e SET e." + column + " = :score" +
                        " WHERE e.facultyEmail = :email AND e.academicYear = :year")
                        .setParameter("score", BigDecimal.valueOf(numericScore))
                        .setParameter("email", email)
                        .setParameter("year", year)
                        .executeUpdate();
            } catch (Exception ex) {
                log.warn("Could not update {} {} for section '{}': {}", entityName, column, sectionKey, ex.getMessage());
            }
        });
    }

    private double extractNumericScore(Object raw, String role) {
        if (raw instanceof Number n) return n.doubleValue();
        if (raw instanceof String s) {
            try { return Double.parseDouble(s); } catch (NumberFormatException e) { return 0; }
        }
        if (raw instanceof Map<?, ?> m) {
            Object val = m.get(role);
            if (val == null) val = m.get("score");
            if (val instanceof Number n) return n.doubleValue();
        }
        if (raw instanceof List<?> list) {
            double total = 0;
            for (Object item : list) {
                if (item instanceof Map<?, ?> m) {
                    Object val = m.get(role);
                    if (val == null) val = m.get("score");
                    if (val instanceof Number n) total += n.doubleValue();
                }
            }
            return total;
        }
        return 0;
    }
}
