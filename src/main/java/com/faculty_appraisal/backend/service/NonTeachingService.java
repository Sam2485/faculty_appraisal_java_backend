package com.faculty_appraisal.backend.service;

import com.faculty_appraisal.backend.exception.AppException;
import com.faculty_appraisal.backend.model.dto.non_teaching.*;
import com.faculty_appraisal.backend.model.entity.core.FacultyProfile;
import com.faculty_appraisal.backend.model.entity.non_teaching.*;
import com.faculty_appraisal.backend.repository.core.FacultyProfileRepository;
import com.faculty_appraisal.backend.repository.non_teaching.*;
import com.faculty_appraisal.backend.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NonTeachingService {

    // ── Part A section definitions (key → [title, maxMarks]) ─────────────────
    private static final Map<String, Object[]> PART_A_SECTIONS = Map.of(
            "selfResp",    new Object[]{"Current Responsibilities",   10},
            "selfContrib", new Object[]{"Other Useful Contributions", 10},
            "selfAchieve", new Object[]{"Achievements",                5}
    );

    // ── Part B section definitions (key → [title, paramCount]) ───────────────
    private static final Map<String, Object[]> PART_B_SECTIONS = Map.ofEntries(
            Map.entry("profComp", new Object[]{"Professional Competence",  5}),
            Map.entry("quality",  new Object[]{"Quality of Work",          5}),
            Map.entry("personal", new Object[]{"Personal Characteristics", 6}),
            Map.entry("regular",  new Object[]{"Regularity",               5})
    );

    private static final Set<String> DRAFT_STATUSES = Set.of("Draft", "Pending Registrar Review");
    private static final Set<String> SUBMITTED_STATUSES = Set.of("Pending RO Review", "Pending Registrar Review");

    // ── Injected ─────────────────────────────────────────────────────────────
    private final FacultyProfileRepository facultyRepo;
    private final NonTeachingAppraisalRepository appraisalRepo;
    private final NonTeachingPartAItemRepository partARepo;
    private final NonTeachingPartBRatingRepository partBRepo;
    private final NTWorkflowTemplateRepository templateRepo;
    private final NTWorkflowAssignmentRepository assignmentRepo;
    private final NTWorkflowInstanceRepository instanceRepo;
    private final NTWorkflowInstanceStepRepository instanceStepRepo;

    // ── Template resolution: individual > dept > role > default ──────────────

    private Optional<NTWorkflowTemplate> resolveTemplate(
            String staffEmail, String appraisalRole, String department) {

        if (staffEmail != null) {
            Optional<NTWorkflowAssignment> a = assignmentRepo.findByStaffEmail(staffEmail);
            if (a.isPresent()) return Optional.of(a.get().getTemplate());
        }
        if (department != null) {
            Optional<NTWorkflowAssignment> a = assignmentRepo.findByDepartment(department);
            if (a.isPresent()) return Optional.of(a.get().getTemplate());
        }
        if (appraisalRole != null) {
            Optional<NTWorkflowAssignment> a = assignmentRepo.findByAppraisalRole(appraisalRole);
            if (a.isPresent()) return Optional.of(a.get().getTemplate());
        }
        return templateRepo.findFirstByIsDefaultTrueAndIsActiveTrue();
    }

    // ── GET /workflow-template ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public WorkflowTemplateResponse getWorkflowTemplate(String role) {
        Optional<NTWorkflowTemplate> templateOpt =
                resolveTemplate(null, role, null);

        if (templateOpt.isEmpty()) {
            return new WorkflowTemplateResponse("Non Teaching Approval Flow", List.of());
        }
        NTWorkflowTemplate t = templateOpt.get();
        List<WorkflowTemplateResponse.StepDto> steps = t.getSteps().stream()
                .map(s -> new WorkflowTemplateResponse.StepDto(
                        s.getStepNo(),
                        s.getDesignation() != null ? s.getDesignation().getName() : "Unknown",
                        "WAITING"))
                .collect(Collectors.toList());
        return new WorkflowTemplateResponse(t.getName(), steps);
    }

    // ── GET /workflow/{email} ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public WorkflowStatusResponse getWorkflowForStaff(
            String email, String academicYear, CurrentUser currentUser) {

        FacultyProfile profile = facultyRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Staff not found"));

        Optional<NTWorkflowInstance> instanceOpt =
                instanceRepo.findByStaffEmailAndAcademicYear(email, academicYear);

        WorkflowStatusResponse resp = new WorkflowStatusResponse();

        if (instanceOpt.isEmpty()) {
            Optional<NTWorkflowTemplate> t =
                    resolveTemplate(email, profile.getAppraisalRole(), profile.getDepartment());
            resp.setWorkflowId(null);
            resp.setWorkflowName(t.map(NTWorkflowTemplate::getName).orElse("Non Teaching Approval Flow"));
            resp.setCurrentStep(null);
            resp.setStatus("NOT_STARTED");
            resp.setSteps(t.map(tmpl -> tmpl.getSteps().stream()
                    .map(s -> {
                        WorkflowStatusResponse.StepStatusDto dto = new WorkflowStatusResponse.StepStatusDto();
                        dto.setStepNo(s.getStepNo());
                        dto.setDesignation(s.getDesignation() != null ? s.getDesignation().getName() : "?");
                        dto.setStatus("WAITING");
                        return dto;
                    }).collect(Collectors.toList())).orElse(List.of()));
            return resp;
        }

        NTWorkflowInstance instance = instanceOpt.get();
        resp.setWorkflowId(instance.getId().toString());
        resp.setWorkflowName("Non Teaching Approval Flow");
        resp.setCurrentStep(instance.getCurrentStep());
        resp.setStatus(instance.getStatus());
        resp.setSteps(instance.getInstanceSteps().stream()
                .sorted(Comparator.comparingInt(NTWorkflowInstanceStep::getStepNo))
                .map(s -> {
                    WorkflowStatusResponse.StepStatusDto dto = new WorkflowStatusResponse.StepStatusDto();
                    dto.setStepNo(s.getStepNo());
                    dto.setDesignation(s.getDesignation());
                    dto.setStatus(s.getStatus());
                    return dto;
                }).collect(Collectors.toList()));
        return resp;
    }

    // ── GET /appraisal ────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Optional<NonTeachingAppraisal> getMyAppraisal(String academicYear, CurrentUser currentUser) {
        return appraisalRepo.findByStaffEmailAndAcademicYear(currentUser.getEmail(), academicYear);
    }

    // ── PUT /appraisal ────────────────────────────────────────────────────────

    @Transactional
    public NonTeachingAppraisal upsertMyAppraisal(NtAppraisalRequest req, CurrentUser currentUser) {
        String email = currentUser.getEmail();
        String year = req.getAcademicYear();

        FacultyProfile profile = facultyRepo.findByEmail(email).orElse(null);

        // direct-to-registrar staff skip RO step → route to Pending Registrar Review
        String status = req.getStatus();
        if (status == null || status.equals("Draft") || status.equals("Pending RO Review")) {
            if (profile != null && profile.isReportsToRegistrar()) {
                Optional<NonTeachingAppraisal> existing =
                        appraisalRepo.findByStaffEmailAndAcademicYear(email, year);
                if (existing.isEmpty()
                        || DRAFT_STATUSES.contains(existing.get().getStatus())) {
                    status = "Pending Registrar Review";
                }
            }
        }

        NonTeachingAppraisal appraisal =
                appraisalRepo.findByStaffEmailAndAcademicYear(email, year)
                        .orElseGet(() -> {
                            NonTeachingAppraisal a = new NonTeachingAppraisal();
                            a.setStaffEmail(email);
                            a.setAcademicYear(year);
                            return a;
                        });

        if (status != null) appraisal.setStatus(status);
        if (req.getSelfTotal() != null) appraisal.setSelfTotal(req.getSelfTotal());
        if (req.getRoTotal() != null) appraisal.setRoTotal(req.getRoTotal());
        if (req.getRegistrarTotal() != null) appraisal.setRegistrarTotal(req.getRegistrarTotal());
        if (req.getVcTotal() != null) appraisal.setVcTotal(req.getVcTotal());
        if (req.getPayload() != null && !req.getPayload().isEmpty()) {
            appraisal.setPayload(req.getPayload());
        }
        if (appraisal.getSubmittedAt() == null
                && SUBMITTED_STATUSES.contains(appraisal.getStatus())) {
            appraisal.setSubmittedAt(LocalDateTime.now());
        }

        appraisal = appraisalRepo.save(appraisal);

        // Shred Part A from payload
        if (req.getPayload() != null && !req.getPayload().isEmpty()) {
            shredPartA(email, year, req.getPayload());
        }

        // Create workflow instance on first submission (idempotent)
        final String finalStatus = appraisal.getStatus();
        if (SUBMITTED_STATUSES.contains(finalStatus)) {
            boolean instanceExists =
                    instanceRepo.findByStaffEmailAndAcademicYear(email, year).isPresent();
            if (!instanceExists) {
                Optional<NTWorkflowTemplate> templateOpt = resolveTemplate(
                        email,
                        profile != null ? profile.getAppraisalRole() : null,
                        profile != null ? profile.getDepartment() : null);

                if (templateOpt.isPresent()) {
                    NTWorkflowTemplate template = templateOpt.get();
                    List<NTWorkflowTemplateStep> steps = template.getSteps();
                    if (!steps.isEmpty()) {
                        NTWorkflowInstance instance = new NTWorkflowInstance();
                        instance.setAppraisalId(appraisal.getId());
                        instance.setTemplate(template);
                        instance.setStaffEmail(email);
                        instance.setAcademicYear(year);
                        instance.setCurrentStep(steps.get(0).getStepNo());
                        instance.setStatus("PENDING");
                        instance = instanceRepo.save(instance);

                        for (NTWorkflowTemplateStep step : steps) {
                            NTWorkflowInstanceStep iStep = new NTWorkflowInstanceStep();
                            iStep.setInstance(instance);
                            iStep.setStepNo(step.getStepNo());
                            iStep.setDesignation(step.getDesignation() != null
                                    ? step.getDesignation().getName() : "?");
                            iStep.setStatus(step.getStepNo() == steps.get(0).getStepNo()
                                    ? "PENDING" : "WAITING");
                            instanceStepRepo.save(iStep);
                        }
                    }
                }
            }
        }

        return appraisal;
    }

    // ── GET /subordinates ─────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<NtSubordinateDto> getNtSubordinates(String academicYear, CurrentUser currentUser) {
        List<String> roles = currentUser.getRoles();
        boolean isAllowed = roles.stream().anyMatch(r ->
                Set.of("registrar", "vc", "reporting_officer", "admin", "super_admin").contains(r));
        if (!isAllowed) return List.of();

        // Collect eligible staff emails based on role
        List<String> eligibleEmails;
        if (roles.contains("vc") || roles.contains("admin") || roles.contains("super_admin")) {
            eligibleEmails = null; // all
        } else if (roles.contains("registrar")) {
            eligibleEmails = facultyRepo.findByRegistrarEmail(currentUser.getEmail())
                    .stream().map(FacultyProfile::getEmail).collect(Collectors.toList());
        } else { // reporting_officer
            eligibleEmails = facultyRepo
                    .findByReportingOfficerEmailAndReportsToRegistrarFalse(currentUser.getEmail())
                    .stream().map(FacultyProfile::getEmail).collect(Collectors.toList());
        }

        // Load NT appraisals for the year
        List<NonTeachingAppraisal> appraisals;
        if (eligibleEmails == null) {
            appraisals = appraisalRepo.findByAcademicYear(academicYear);
        } else if (eligibleEmails.isEmpty()) {
            return List.of();
        } else {
            appraisals = appraisalRepo.findByStaffEmailInAndAcademicYear(eligibleEmails, academicYear);
        }

        // Load faculty profiles
        List<String> staffEmails = appraisals.stream()
                .map(NonTeachingAppraisal::getStaffEmail).collect(Collectors.toList());
        Map<String, FacultyProfile> profileMap = facultyRepo.findAll().stream()
                .filter(p -> staffEmails.contains(p.getEmail()))
                .collect(Collectors.toMap(FacultyProfile::getEmail, p -> p));

        // Reviewer's designation for workflow-step matching
        String reviewerDesignation = facultyRepo.findByEmail(currentUser.getEmail())
                .map(FacultyProfile::getDesignation).orElse(null);

        List<NtSubordinateDto> result = new ArrayList<>();
        for (NonTeachingAppraisal appr : appraisals) {
            FacultyProfile profile = profileMap.get(appr.getStaffEmail());

            // Workflow step filter — only show if this reviewer's designation is the pending step
            if (reviewerDesignation != null) {
                Optional<NTWorkflowInstance> instOpt =
                        instanceRepo.findByStaffEmailAndAcademicYear(
                                appr.getStaffEmail(), academicYear);
                if (instOpt.isPresent()) {
                    NTWorkflowInstance instance = instOpt.get();
                    Optional<NTWorkflowInstanceStep> pending = instance.getInstanceSteps()
                            .stream()
                            .filter(s -> "PENDING".equals(s.getStatus()))
                            .findFirst();
                    if (pending.isEmpty()
                            || !reviewerDesignation.equals(pending.get().getDesignation())) {
                        continue;
                    }
                }
            }

            NtSubordinateDto dto = new NtSubordinateDto();
            dto.setStaffEmail(appr.getStaffEmail());
            dto.setStatus(appr.getStatus());
            dto.setSubmittedOn(appr.getSubmittedAt() != null
                    ? appr.getSubmittedAt().toLocalDate() : null);
            dto.setSelfTotal(appr.getSelfTotal());
            dto.setRoTotal(appr.getRoTotal());
            dto.setRegistrarTotal(appr.getRegistrarTotal());
            dto.setVcTotal(appr.getVcTotal());
            dto.setPayload(appr.getPayload());

            if (profile != null) {
                dto.setName(profile.getFullName());
                dto.setDesignation(profile.getDesignation());
                dto.setDepartment(profile.getDepartment());
                dto.setAppraisalRole(profile.getAppraisalRole());
            }
            result.add(dto);
        }
        return result;
    }

    // ── PUT /review/{email} ───────────────────────────────────────────────────

    @Transactional
    public NonTeachingAppraisal reviewNonTeaching(
            String email, NtReviewRequest req, CurrentUser currentUser) {

        FacultyProfile target = facultyRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Staff profile not found"));

        boolean isAssignedRo = currentUser.getRoles().contains("reporting_officer")
                && email.equals(target.getReportingOfficerEmail());
        boolean isAssignedRegistrar = currentUser.getRoles().contains("registrar")
                && email.equals(target.getRegistrarEmail());

        if (!isAssignedRo && !isAssignedRegistrar
                && !currentUser.hasAuthorityOver(email, target.getAppraisalRole(),
                target.getDepartment(), target.getSchool())) {
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                    "Not authorized to review this staff member");
        }

        NonTeachingAppraisal appr = appraisalRepo
                .findByStaffEmailAndAcademicYear(email, req.getAcademicYear())
                .orElseThrow(() -> new AppException(HttpStatus.NOT_FOUND.value(), "Appraisal not found"));

        // Determine effective reviewer role
        Map<String, String[]> roleConfig = Map.of(
                "reporting_officer", new String[]{"roTotal",         "Reporting Officer Reviewed", "roReviewedAt"},
                "registrar",         new String[]{"registrarTotal",  "Registrar Reviewed",         "registrarReviewedAt"},
                "vc",                new String[]{"vcTotal",         "VC Approved",                "vcReviewedAt"}
        );

        String primaryRole = currentUser.getRoles().stream()
                .filter(roleConfig::containsKey).findFirst().orElse(null);
        if (primaryRole == null && currentUser.getRoles().contains("admin")) {
            primaryRole = "registrar";
        }
        if (primaryRole == null) {
            throw new AppException(HttpStatus.FORBIDDEN.value(), "Invalid reviewer role");
        }
        if ("reporting_officer".equals(primaryRole) && target.isReportsToRegistrar()) {
            throw new AppException(HttpStatus.FORBIDDEN.value(),
                    "This staff member reports directly to the Registrar. RO review does not apply.");
        }

        String[] config = roleConfig.get(primaryRole);
        appr.setStatus(config[1]);

        switch (primaryRole) {
            case "reporting_officer" -> appr.setRoReviewedAt(LocalDateTime.now());
            case "registrar"         -> appr.setRegistrarReviewedAt(LocalDateTime.now());
            case "vc"                -> appr.setVcReviewedAt(LocalDateTime.now());
        }

        if (req.getPayload() != null) appr.setPayload(req.getPayload());
        if (req.getTotalScore() != null) {
            switch (primaryRole) {
                case "reporting_officer" -> appr.setRoTotal(req.getTotalScore());
                case "registrar"         -> appr.setRegistrarTotal(req.getTotalScore());
                case "vc"                -> appr.setVcTotal(req.getTotalScore());
            }
        }

        // Write reviewer marks into normalized tables
        if (req.getPayload() != null) {
            updateReviewerMarks(email, req.getAcademicYear(), req.getPayload(), primaryRole);
        }

        // Advance workflow instance
        instanceRepo.findByStaffEmailAndAcademicYear(email, req.getAcademicYear())
                .ifPresent(instance -> advanceWorkflow(instance, currentUser.getEmail(),
                        req.getTotalScore()));

        return appraisalRepo.save(appr);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private void shredPartA(String email, String year, Map<String, Object> payload) {
        PART_A_SECTIONS.forEach((itemKey, meta) -> {
            String title    = (String) meta[0];
            int    maxMarks = (Integer) meta[1];

            Object sectionRaw = payload.get(itemKey);
            if (!(sectionRaw instanceof Map<?, ?> section)) return;

            BigDecimal selfMarks = parseDecimal(section.get("marks"));
            String details = section.get("text") instanceof String s ? s : null;

            List<NonTeachingPartAItem> existing =
                    partARepo.findByStaffEmailAndAcademicYearAndItemKey(email, year, itemKey);

            if (!existing.isEmpty()) {
                NonTeachingPartAItem item = existing.get(0);
                item.setSelfMarks(selfMarks);
                item.setDetails(details);
                partARepo.save(item);
            } else {
                NonTeachingPartAItem item = new NonTeachingPartAItem();
                item.setStaffEmail(email);
                item.setAcademicYear(year);
                item.setItemKey(itemKey);
                item.setTitle(title);
                item.setMaxMarks(BigDecimal.valueOf(maxMarks));
                item.setDetails(details);
                item.setSelfMarks(selfMarks);
                partARepo.save(item);
            }
        });
    }

    private void updateReviewerMarks(String email, String year,
                                     Map<String, Object> payload, String role) {
        // Part A
        String partAField = switch (role) {
            case "reporting_officer" -> "roMarks";
            case "registrar"         -> "regMarks";
            case "vc"                -> "vcMarks";
            default -> null;
        };

        if (partAField != null) {
            for (String itemKey : PART_A_SECTIONS.keySet()) {
                Object sectionRaw = payload.get(itemKey);
                if (!(sectionRaw instanceof Map<?, ?> section)) continue;
                BigDecimal marks = parseDecimal(section.get(partAField));

                partARepo.findByStaffEmailAndAcademicYearAndItemKey(email, year, itemKey)
                        .stream().findFirst().ifPresent(item -> {
                            switch (role) {
                                case "reporting_officer" -> item.setRoMarks(marks);
                                case "registrar"         -> item.setRegistrarMarks(marks);
                                case "vc"                -> item.setVcMarks(marks);
                            }
                            partARepo.save(item);
                        });
            }
        }

        // Part B
        String partBSuffix = switch (role) {
            case "reporting_officer" -> "ro";
            case "registrar"         -> "reg";
            case "vc"                -> "vc";
            default -> null;
        };

        if (partBSuffix == null) return;

        Object partBRaw = payload.get("partB");
        if (partBRaw == null) partBRaw = payload.get("part_b");
        if (!(partBRaw instanceof Map<?, ?> partBData)) return;

        PART_B_SECTIONS.forEach((sectionKey, meta) -> {
            String sectionTitle = (String) meta[0];
            int    paramCount   = (Integer) meta[1];

            Object sectionRaw = partBData.get(sectionKey);
            if (!(sectionRaw instanceof Map<?, ?> section)) return;

            for (int paramNo = 0; paramNo < paramCount; paramNo++) {
                final int pNo = paramNo;
                BigDecimal rating = parseDecimal(section.get("p" + paramNo + "_" + partBSuffix));

                Optional<NonTeachingPartBRating> existing =
                        partBRepo.findByStaffEmailAndAcademicYearAndSectionKeyAndParameterNo(
                                email, year, sectionKey, paramNo);

                if (existing.isPresent()) {
                    NonTeachingPartBRating r = existing.get();
                    switch (role) {
                        case "reporting_officer" -> r.setRoRating(rating);
                        case "registrar"         -> r.setRegistrarRating(rating);
                        case "vc"                -> r.setVcRating(rating);
                    }
                    partBRepo.save(r);
                } else {
                    NonTeachingPartBRating r = new NonTeachingPartBRating();
                    r.setStaffEmail(email);
                    r.setAcademicYear(year);
                    r.setSectionKey(sectionKey);
                    r.setSectionTitle(sectionTitle);
                    r.setMaxMarks(BigDecimal.valueOf(5));
                    r.setParameterNo(pNo);
                    r.setParameterLabel("Parameter " + (pNo + 1));
                    switch (role) {
                        case "reporting_officer" -> r.setRoRating(rating);
                        case "registrar"         -> r.setRegistrarRating(rating);
                        case "vc"                -> r.setVcRating(rating);
                    }
                    partBRepo.save(r);
                }
            }
        });
    }

    private void advanceWorkflow(NTWorkflowInstance instance, String reviewerEmail,
                                 BigDecimal score) {
        List<NTWorkflowInstanceStep> steps = instance.getInstanceSteps().stream()
                .sorted(Comparator.comparingInt(NTWorkflowInstanceStep::getStepNo))
                .collect(Collectors.toList());

        for (NTWorkflowInstanceStep step : steps) {
            if (step.getStepNo() == Optional.ofNullable(instance.getCurrentStep()).orElse(-1)) {
                step.setStatus("APPROVED");
                step.setReviewerEmail(reviewerEmail);
                step.setReviewedAt(LocalDateTime.now());
                if (score != null) step.setScore(score);
                instanceStepRepo.save(step);
                break;
            }
        }

        List<NTWorkflowInstanceStep> waiting = steps.stream()
                .filter(s -> "WAITING".equals(s.getStatus()))
                .sorted(Comparator.comparingInt(NTWorkflowInstanceStep::getStepNo))
                .collect(Collectors.toList());

        if (!waiting.isEmpty()) {
            waiting.get(0).setStatus("PENDING");
            instance.setCurrentStep(waiting.get(0).getStepNo());
            instanceStepRepo.save(waiting.get(0));
        } else {
            instance.setCurrentStep(null);
            instance.setStatus("COMPLETED");
        }
        instance.setUpdatedAt(LocalDateTime.now());
        instanceRepo.save(instance);
    }

    private BigDecimal parseDecimal(Object raw) {
        if (raw == null) return null;
        String s = raw.toString().trim();
        if (s.isEmpty()) return null;
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return null; }
    }
}
