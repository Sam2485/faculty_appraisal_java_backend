package com.faculty_appraisal.backend.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.faculty_appraisal.backend.exception.AppException;
import com.faculty_appraisal.backend.model.BaseAppraisalModel;
import com.faculty_appraisal.backend.model.HasRowNo;
import com.faculty_appraisal.backend.model.dto.appraisal.AppraisalStatusResponse;
import com.faculty_appraisal.backend.model.dto.appraisal.SnapshotRequest;
import com.faculty_appraisal.backend.model.dto.appraisal.SubmitAppraisalRequest;
import com.faculty_appraisal.backend.model.entity.core.AppraisalDocument;
import com.faculty_appraisal.backend.model.entity.core.AppraisalReview;
import com.faculty_appraisal.backend.model.entity.core.AppraisalSnapshot;
import com.faculty_appraisal.backend.model.entity.core.Declaration;
import com.faculty_appraisal.backend.model.entity.part_a.*;
import com.faculty_appraisal.backend.model.entity.part_b.*;
import com.faculty_appraisal.backend.repository.core.*;
import com.faculty_appraisal.backend.repository.part_a.*;
import com.faculty_appraisal.backend.repository.part_b.*;
import com.faculty_appraisal.backend.security.RoleUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppraisalService {

    private static final Set<String> REJECTED_STATUSES = Set.of(
            "HOD Rejected", "Center Head Rejected", "Director Rejected",
            "Dean Rejected", "VC Rejected", "Registrar Rejected",
            "Reporting Officer Rejected"
    );

    private final AppraisalSnapshotRepository snapshotRepo;
    private final DeclarationRepository declarationRepo;
    private final AppraisalDocumentRepository documentRepo;
    private final AppraisalReviewRepository reviewRepo;
    private final AppraisalConfigRepository configRepo;
    private final ObjectMapper objectMapper;

    // All Part A repositories
    private final TeachingProcessRepository teachingProcessRepo;
    private final CourseFileRepository courseFileRepo;
    private final InnovativeTeachingRepository innovativeTeachingRepo;
    private final ProjectGuidedRepository projectGuidedRepo;
    private final QualificationEnhancementRepository qualificationEnhancementRepo;
    private final StudentFeedbackRepository studentFeedbackRepo;
    private final DepartmentActivityRepository departmentActivityRepo;
    private final UniversityActivityRepository universityActivityRepo;
    private final SocialContributionRepository socialContributionRepo;
    private final IndustryConnectRepository industryConnectRepo;
    private final ACRScoreRepository acrScoreRepo;

    // All Part B repositories
    private final JournalPublicationRepository journalRepo;
    private final BookPublicationRepository bookRepo;
    private final ICTPedagogyRepository ictRepo;
    private final ResearchGuidanceRepository researchGuidanceRepo;
    private final ResearchProjectRepository researchProjectRepo;
    private final ExternalResearchProjectRepository externalResearchProjectRepo;
    private final PatentRepository patentRepo;
    private final AwardRepository awardRepo;
    private final ConferenceRepository conferenceRepo;
    private final ResearchProposalRepository researchProposalRepo;
    private final ProductDevelopedRepository productDevelopedRepo;
    private final SelfDevelopmentRepository selfDevelopmentRepo;
    private final IndustrialTrainingRepository industrialTrainingRepo;

    // ── Get Snapshot ───────────────────────────────────────────────────────
    public Optional<AppraisalSnapshot> getSnapshot(String email, String academicYear) {
        return snapshotRepo.findByFacultyEmailAndAcademicYear(email, academicYear);
    }

    // ── Upsert Snapshot (Save Draft) ───────────────────────────────────────
    @Transactional
    public Map<String, String> upsertSnapshot(String email, SnapshotRequest request) {
        String year = request.academicYear();

        // Block edit if already submitted, unless the appraisal was rejected (allow resubmission)
        declarationRepo.findByFacultyEmailAndAcademicYear(email, year).ifPresent(d -> {
            if (!REJECTED_STATUSES.contains(d.getStatus())) {
                throw new AppException(403,
                        "Your appraisal has already been submitted and cannot be modified.");
            }
        });

        AppraisalSnapshot snapshot = snapshotRepo
                .findByFacultyEmailAndAcademicYear(email, year)
                .orElseGet(() -> {
                    AppraisalSnapshot s = new AppraisalSnapshot();
                    s.setFacultyEmail(email);
                    s.setAcademicYear(year);
                    return s;
                });

        try {
            snapshot.setPayload(objectMapper.writeValueAsString(request.payload()));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new AppException(500, "Error serializing snapshot payload: " + e.getMessage());
        }
        snapshotRepo.save(snapshot);

        if (request.docs() != null && !request.docs().isEmpty()) {
            documentRepo.deleteByFacultyEmailAndAcademicYear(email, year);
            saveDocuments(email, year, request.docs());
        }

        return Map.of("message", "Saved");
    }

    // ── Submit Appraisal ───────────────────────────────────────────────────
    @Transactional
    public Map<String, String> submitAppraisal(String email, String school,
                                               SubmitAppraisalRequest request) {
        String year = request.academicYear();

        // Resolve form data — frontend can send it as top-level 'form' or nested in 'payload.form'
        Map<String, Object> form = request.form();
        Map<String, Object> totals = request.totals();
        if (form == null && request.payload() != null) {
            form = (Map<String, Object>) request.payload().get("form");
            if (totals == null)
                totals = (Map<String, Object>) request.payload().get("totals");
        }
        if (form == null)
            throw new AppException(400,
                    "Form data is missing. Ensure 'form' or 'payload.form' key is present.");
        if (totals == null) totals = Map.of();

        // Cycle gate
        configRepo.findByAcademicYear(year).ifPresent(config -> {
            if (!config.getIsOpen())
                throw new AppException(403,
                        "Appraisal submissions for " + year + " are currently closed.");
        });

        String formFamily = RoleUtils.getFormFamily(school);

        // 1. Shred form JSON into normalized tables
        shredForm(email, year, form, formFamily);

        // 2. Save Declaration
        Declaration decl = declarationRepo
                .findByFacultyEmailAndAcademicYear(email, year)
                .orElseGet(() -> {
                    Declaration d = new Declaration();
                    d.setFacultyEmail(email);
                    d.setAcademicYear(year);
                    return d;
                });

        decl.setPartATotal(safeNum(totals.get("partATotal")));
        decl.setPartBTotal(safeNum(totals.get("partBTotal")));
        decl.setGrandTotal(safeNum(totals.get("grandTotal")));
        decl.setStatus(request.status() != null ? request.status() : "Submitted");
        declarationRepo.save(decl);

        // 3. Save documents
        if (request.docs() != null && !request.docs().isEmpty()) {
            documentRepo.deleteByFacultyEmailAndAcademicYear(email, year);
            saveDocuments(email, year, request.docs());
        }

        // 4. Update snapshot to latest submitted state
        AppraisalSnapshot snapshot = snapshotRepo
                .findByFacultyEmailAndAcademicYear(email, year)
                .orElseGet(() -> {
                    AppraisalSnapshot s = new AppraisalSnapshot();
                    s.setFacultyEmail(email);
                    s.setAcademicYear(year);
                    return s;
                });
        // Store the full request as the snapshot payload
        Map<String, Object> fullPayload = new HashMap<>();
        fullPayload.put("form", form);
        fullPayload.put("totals", totals);
        try {
            snapshot.setPayload(objectMapper.writeValueAsString(fullPayload));
        } catch (com.fasterxml.jackson.core.JsonProcessingException e) {
            throw new AppException(500, "Error serializing snapshot payload: " + e.getMessage());
        }
        snapshotRepo.save(snapshot);

        return Map.of(
                "message", "Submitted successfully",
                "submitted_at", java.time.Instant.now().toString()
        );
    }

    // ── Get Status ─────────────────────────────────────────────────────────
    public AppraisalStatusResponse getStatus(String email, String academicYear) {
        Optional<Declaration> declaration =
                declarationRepo.findByFacultyEmailAndAcademicYear(email, academicYear);

        List<AppraisalReview> reviews =
                reviewRepo.findByFacultyEmailAndAcademicYear(email, academicYear);

        List<Map<String, Object>> reviewsData = reviews.stream().map(r -> {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("reviewer_role",  r.getReviewerRole());
            map.put("reviewer_email", r.getReviewerEmail());
            map.put("part_a_score",   r.getPartAScore() != null ? r.getPartAScore().doubleValue() : 0);
            map.put("part_b_score",   r.getPartBScore() != null ? r.getPartBScore().doubleValue() : 0);
            map.put("total_score",    r.getTotalScore() != null ? r.getTotalScore().doubleValue() : 0);
            map.put("section_scores", r.getSectionScores() != null ? r.getSectionScores() : Map.of());
            map.put("remarks",        r.getRemarks());
            map.put("status",         r.getStatus());
            map.put("reviewed_at",    r.getReviewedAt() != null ? r.getReviewedAt().toString() : null);
            return map;
        }).toList();

        return new AppraisalStatusResponse(declaration.orElse(null), reviewsData);
    }

    // ── Shred Form ─────────────────────────────────────────────────────────
    // Converts the frontend JSON form into normalized DB rows
    private void shredForm(String email, String year,
                           Map<String, Object> formData, String formFamily) {

        // Field name aliases — frontend keys → entity field names
        Map<String, String> aliases = Map.ofEntries(
                Map.entry("title_with_page_nos",  "title"),
                Map.entry("journal_details",      "journal"),
                Map.entry("issn_isbn_no",         "issn"),
                Map.entry("issn_isbn",            "issn"),
                Map.entry("course_code_name",     "courseCode"),
                Map.entry("course_paper",         "course"),
                Map.entry("nature_of_activity",   "nature"),
                Map.entry("activity_type",        "activity"),
                Map.entry("details_of_activity",  "details"),
                Map.entry("project_type",         "label"),
                Map.entry("qualification_type",   "label"),
                Map.entry("short_description",    "details"),
                Map.entry("title_and_pages",      "title"),
                Map.entry("book_title_editor",    "book"),
                Map.entry("event_title",          "title"),
                Map.entry("hosting_organization", "organization"),
                Map.entry("event_level",          "level"),
                Map.entry("pedagogy_type",        "type"),
                Map.entry("company_industry",     "company"),
                Map.entry("duration_days",        "duration"),
                Map.entry("nature_of_training",   "nature"),
                Map.entry("hod",                  "hodScore"),
                Map.entry("director",             "directorScore"),
                Map.entry("dean",                 "deanScore"),
                Map.entry("vc",                   "vcScore"),
                Map.entry("maxMarks",             "maxMarks")
        );

        // InnovativeTeaching — scalar, not a list
        innovativeTeachingRepo.deleteByFacultyEmailAndAcademicYear(email, year);
        Object innovDetails = formData.get("innovDetails");
        Object innovScoreRaw = formData.get("innovScore");
        if (innovDetails != null || innovScoreRaw != null) {
            InnovativeTeaching innov = new InnovativeTeaching();
            innov.setFacultyEmail(email);
            innov.setAcademicYear(year);
            innov.setFormFamily(formFamily);
            innov.setSectionTitle("A3. Innovative Teaching-Learning");
            if (innovDetails instanceof Map) {
                innov.setDetails((String) ((Map<?,?>) innovDetails).get("details"));
            } else if (innovDetails != null) {
                innov.setDetails(innovDetails.toString());
            }
            if (innovScoreRaw != null) innov.setScore(safeNum(innovScoreRaw));
            innovativeTeachingRepo.save(innov);
        }

        // Section mappings: formKey → (sectionTitle, saveFunction)
        shredSection(email, year, formFamily, formData, aliases,
                "lectures",         "A1. Lectures / Tutorials / Practicals",   teachingProcessRepo,        TeachingProcess::new);
        shredSection(email, year, formFamily, formData, aliases,
                "courseFile",       "A2. Course File",                          courseFileRepo,             CourseFile::new);
        shredSection(email, year, formFamily, formData, aliases,
                "projects",         "A4. Projects",                             projectGuidedRepo,          ProjectGuided::new);
        shredSection(email, year, formFamily, formData, aliases,
                "quals",            "A5. Qualification Enhancement",            qualificationEnhancementRepo, QualificationEnhancement::new);
        shredSection(email, year, formFamily, formData, aliases,
                "feedback",         "Student Feedback",                         studentFeedbackRepo,        StudentFeedback::new);
        shredSection(email, year, formFamily, formData, aliases,
                "deptActs",         "Departmental / School Activities",         departmentActivityRepo,     DepartmentActivity::new);
        shredSection(email, year, formFamily, formData, aliases,
                "uniActs",          "University Level Activities",              universityActivityRepo,     UniversityActivity::new);
        shredSection(email, year, formFamily, formData, aliases,
                "society",          "Contribution to Society",                  socialContributionRepo,     SocialContribution::new);
        shredSection(email, year, formFamily, formData, aliases,
                "industry",         "Industry Connect",                         industryConnectRepo,        IndustryConnect::new);
        shredSection(email, year, formFamily, formData, aliases,
                "acr",              "Annual Confidential Report - School Level", acrScoreRepo,              ACRScore::new);
        shredSection(email, year, formFamily, formData, aliases,
                "journals",         "B1. Research Papers / Journal Publications", journalRepo,              JournalPublication::new);
        shredSection(email, year, formFamily, formData, aliases,
                "books",            "B2. Books / Book Chapters",                bookRepo,                   BookPublication::new);
        shredSection(email, year, formFamily, formData, aliases,
                "ict",              "B3. ICT / E-Content / Pedagogy",           ictRepo,                    ICTPedagogy::new);
        shredSection(email, year, formFamily, formData, aliases,
                "research",         "B4(a). Research Guidance - PhD / PG",      researchGuidanceRepo,       ResearchGuidance::new);
        shredSection(email, year, formFamily, formData, aliases,
                "projects2",        "B4(b). Research / Consultancy Internal",   researchProjectRepo,        ResearchProject::new);
        shredSection(email, year, formFamily, formData, aliases,
                "externalProjects", "B4(c). Research / Consultancy External",   externalResearchProjectRepo, ExternalResearchProject::new);
        shredSection(email, year, formFamily, formData, aliases,
                "patents",          "B5(a). Patents (IPR)",                     patentRepo,                 Patent::new);
        shredSection(email, year, formFamily, formData, aliases,
                "awards",           "B5(b). Awards",                            awardRepo,                  Award::new);
        shredSection(email, year, formFamily, formData, aliases,
                "confs",            "B6. Invited Lectures / Resource Person",   conferenceRepo,             Conference::new);
        shredSection(email, year, formFamily, formData, aliases,
                "proposals",        "B7(a). Submitted Research Proposals",      researchProposalRepo,       ResearchProposal::new);
        shredSection(email, year, formFamily, formData, aliases,
                "products",         "B7(b). Product Developed",                 productDevelopedRepo,       ProductDeveloped::new);
        shredSection(email, year, formFamily, formData, aliases,
                "fdps",             "B8(a). FDP / Workshops",                   selfDevelopmentRepo,        SelfDevelopment::new);
        shredSection(email, year, formFamily, formData, aliases,
                "training",         "B8(b). Industrial Training",               industrialTrainingRepo,     IndustrialTraining::new);
    }

    private <T extends BaseAppraisalModel> void shredSection(
            String email, String year, String formFamily,
            Map<String, Object> formData, Map<String, String> aliases,
            String formKey, String sectionTitle,
            BaseAppraisalRepository<T, UUID> repo,
            java.util.function.Supplier<T> constructor) {

        // Delete existing rows for this section
        repo.deleteByFacultyEmailAndAcademicYear(email, year);

        Object sectionData = formData.get(formKey);
        if (sectionData == null) {
            log.warn("shredSection: skipping '{}' — no data in submitted form", formKey);
            return;
        }

        List<Map<String, Object>> items = sectionData instanceof List
                ? (List<Map<String, Object>>) sectionData
                : List.of((Map<String, Object>) sectionData);

        List<T> toSave = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            if (item == null) continue;

            T entity = constructor.get();
            entity.setFacultyEmail(email);
            entity.setAcademicYear(year);
            entity.setFormFamily(formFamily);
            entity.setSectionTitle(sectionTitle);
            if (entity instanceof HasRowNo r) r.setRowNo(i + 1);

            // Map fields using aliases + reflection
            for (Map.Entry<String, Object> entry : item.entrySet()) {
                String targetField = aliases.getOrDefault(entry.getKey(), entry.getKey());
                setFieldSafely(entity, targetField, entry.getValue());
            }
            toSave.add(entity);
        }
        repo.saveAll(toSave);
        log.info("shredSection: '{}' → {} row(s) saved", formKey, toSave.size());
    }

    private void setFieldSafely(Object entity, String fieldName, Object value) {
        if (value == null) return;
        try {
            java.lang.reflect.Field field = findField(entity.getClass(), fieldName);
            if (field == null) {
                log.warn("setFieldSafely: field '{}' not found in {}", fieldName, entity.getClass().getSimpleName());
                return;
            }
            field.setAccessible(true);
            field.set(entity, coerce(value, field.getType()));
        } catch (Exception e) {
            log.warn("setFieldSafely: could not set '{}' — {}", fieldName, e.getMessage());
        }
    }

    private java.lang.reflect.Field findField(Class<?> clazz, String name) {
        while (clazz != null) {
            try { return clazz.getDeclaredField(name); }
            catch (NoSuchFieldException e) { clazz = clazz.getSuperclass(); }
        }
        return null;
    }

    private Object coerce(Object value, Class<?> targetType) {
        if (value == null) return null;
        String str = value.toString().trim();
        if (str.isEmpty()) return null;
        try {
            if (targetType == Integer.class || targetType == int.class)
                return (int) Double.parseDouble(str);
            if (targetType == Double.class || targetType == double.class)
                return Double.parseDouble(str);
            if (targetType == java.math.BigDecimal.class)
                return new java.math.BigDecimal(str);
            if (targetType == java.time.LocalDate.class) {
                for (String fmt : new String[]{
                        "dd/MM/yyyy", "yyyy-MM-dd", "dd-MM-yyyy"}) {
                    try {
                        return java.time.LocalDate.parse(str,
                                java.time.format.DateTimeFormatter.ofPattern(fmt));
                    } catch (Exception ignored) {}
                }
                return null;
            }
            if (targetType == String.class)
                return str;
        } catch (Exception e) {
            log.warn("coerce: cannot coerce '{}' to {}", str, targetType.getSimpleName());
        }
        return value;
    }

    // ── Document helper ────────────────────────────────────────────────────
    private void saveDocuments(String email, String year,
                               Map<String, List<Map<String, Object>>> docs) {
        for (Map.Entry<String, List<Map<String, Object>>> entry : docs.entrySet()) {
            String docKey = entry.getKey();
            String section = docKey.replaceAll("\\d+$", "");
            if (section.isEmpty()) section = docKey;
            List<Map<String, Object>> fileList = entry.getValue();
            if (fileList == null) continue;

            for (int i = 0; i < fileList.size(); i++) {
                Map<String, Object> fileObj = fileList.get(i);
                if (fileObj == null || fileObj.get("name") == null) continue;

                AppraisalDocument doc = new AppraisalDocument();
                doc.setFacultyEmail(email);
                doc.setAcademicYear(year);
                doc.setSection(section);
                doc.setDocKey(docKey);
                doc.setRowNo(i + 1);
                doc.setFileName(fileObj.get("name").toString());
                doc.setFileType(fileObj.get("type") != null ? fileObj.get("type").toString() : null);
                doc.setFileUrl(fileObj.get("url") != null ? fileObj.get("url").toString() : null);
                doc.setStoragePath(fileObj.get("publicId") != null ? fileObj.get("publicId").toString() : null);
                documentRepo.save(doc);
            }
        }
    }

    private BigDecimal safeNum(Object value) {
        if (value == null) return BigDecimal.ZERO;
        try {
            String s = value.toString();
            if (s.isEmpty()) return BigDecimal.ZERO;
            return new BigDecimal(s);
        } catch (Exception e) {
            return BigDecimal.ZERO;
        }
    }
}

