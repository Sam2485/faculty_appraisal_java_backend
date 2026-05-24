package com.faculty_appraisal.backend.service;

import com.faculty_appraisal.backend.exception.AppException;
import com.faculty_appraisal.backend.model.dto.admin.*;
import com.faculty_appraisal.backend.model.entity.core.*;
import com.faculty_appraisal.backend.model.entity.non_teaching.*;
import com.faculty_appraisal.backend.repository.core.*;
import com.faculty_appraisal.backend.repository.non_teaching.*;
import com.faculty_appraisal.backend.security.CurrentUser;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private static final Set<String> EDITABLE_ENV_KEYS = Set.of(
            "MAIL_USERNAME", "MAIL_PASSWORD", "MAIL_FROM", "MAIL_PORT",
            "MAIL_SERVER", "MAIL_TLS", "MAIL_SSL",
            "APP_URL", "FRONTEND_URL", "ALLOW_MOCK_USER",
            "USE_LOCAL_STORAGE", "GCP_STORAGE_BUCKET",
            "MAINTENANCE_MODE", "ALLOW_REGISTRATIONS", "EMAIL_NOTIFICATIONS",
            "DEBUG_LOGGING", "TWO_FACTOR_AUTH", "SESSION_TIMEOUT", "AUDIT_LOGGING"
    );

    private static final Set<String> VALID_ROLES = Set.of(
            "faculty", "non_teaching_staff", "staff", "hod", "reporting_officer",
            "section_head", "director", "center_head", "dean", "registrar", "vc",
            "admin", "hr", "super_admin"
    );

    private static final List<String> TEACHING_DELETE_TABLES = List.of(
            "declarations", "teaching_process", "course_files", "innovative_teaching",
            "projects_guided", "qualification_enhancement", "student_feedback",
            "department_activities", "university_activities", "social_contributions",
            "industry_connect", "acr_scores", "journal_publications", "popular_writings",
            "book_publications", "ict_pedagogy", "research_guidance", "research_projects",
            "external_research_projects", "ipr_records", "patents", "awards", "conferences",
            "research_proposals", "products_developed", "self_development", "industrial_training",
            "appraisal_documents", "appraisal_reviews", "appraisal_snapshots"
    );

    private final FacultyProfileRepository facultyRepo;
    private final DeclarationRepository declarationRepo;
    private final AppraisalConfigRepository appraisalConfigRepo;
    private final ModuleConfigRepository moduleConfigRepo;
    private final NTDesignationRepository designationRepo;
    private final NTWorkflowTemplateRepository templateRepo;
    private final NTWorkflowTemplateStepRepository stepRepo;
    private final NTWorkflowAssignmentRepository assignmentRepo;
    private final NonTeachingAppraisalRepository ntAppraisalRepo;
    private final PasswordEncoder passwordEncoder;
    private final EntityManager em;

    public void checkAdminPublic(CurrentUser user) {
        if (!user.getRoles().contains("admin") && !user.getRoles().contains("super_admin")) {
            throw new AppException(403, "Admin role required");
        }
    }

    // ── Stats ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getStats(String academicYear) {
        List<String> tYears = declarationRepo.findDistinctAcademicYearsOrderDesc();
        List<String> ntYears = ntAppraisalRepo.findDistinctAcademicYearsOrderDesc();
        TreeSet<String> yearSet = new TreeSet<>(Comparator.reverseOrder());
        yearSet.addAll(tYears);
        yearSet.addAll(ntYears);
        List<String> availableYears = new ArrayList<>(yearSet);

        if (academicYear == null && !availableYears.isEmpty()) {
            academicYear = availableYears.get(0);
        }

        List<FacultyProfile> allFaculty = facultyRepo.findAll();
        Map<String, Long> byRole = allFaculty.stream()
                .collect(Collectors.groupingBy(
                        fp -> fp.getAppraisalRole() != null ? fp.getAppraisalRole() : "unknown",
                        Collectors.counting()));
        Map<String, Long> bySchoolRegistered = allFaculty.stream()
                .filter(fp -> fp.getSchool() != null)
                .collect(Collectors.groupingBy(FacultyProfile::getSchool, Collectors.counting()));

        Map<String, Long> teachingPipeline = new HashMap<>();
        Map<String, Map<String, Long>> bySchoolSubmitted = new HashMap<>();
        Map<String, Map<String, Long>> byDeptSubmitted = new HashMap<>();
        Map<String, Long> ntPipeline = new HashMap<>();

        if (academicYear != null) {
            List<Declaration> declarations = declarationRepo.findByAcademicYear(academicYear);
            teachingPipeline = declarations.stream()
                    .collect(Collectors.groupingBy(Declaration::getStatus, Collectors.counting()));

            Map<String, FacultyProfile> profileMap = allFaculty.stream()
                    .collect(Collectors.toMap(FacultyProfile::getEmail, fp -> fp, (a, b) -> a));

            for (Declaration d : declarations) {
                FacultyProfile fp = profileMap.get(d.getFacultyEmail());
                String school = (fp != null && fp.getSchool() != null) ? fp.getSchool() : "Unknown";
                bySchoolSubmitted.computeIfAbsent(school, k -> new HashMap<>())
                        .merge(d.getStatus(), 1L, Long::sum);

                String dept = (fp != null && fp.getDepartment() != null) ? fp.getDepartment() : "Unknown";
                byDeptSubmitted.computeIfAbsent(dept, k -> new HashMap<>())
                        .merge(d.getStatus(), 1L, Long::sum);
            }

            ntPipeline = ntAppraisalRepo.findByAcademicYear(academicYear).stream()
                    .collect(Collectors.groupingBy(
                            a -> a.getStatus() != null ? a.getStatus() : "Unknown",
                            Collectors.counting()));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("academic_year", academicYear);
        result.put("available_years", availableYears);
        result.put("total_registered", (long) allFaculty.size());
        result.put("by_role", byRole);
        result.put("by_school_registered", bySchoolRegistered);
        result.put("teaching_submission_pipeline", teachingPipeline);
        result.put("by_school_submitted", bySchoolSubmitted);
        result.put("by_department_submitted", byDeptSubmitted);
        result.put("non_teaching_pipeline", ntPipeline);
        return result;
    }

    // ── Env Config ────────────────────────────────────────────────────────────

    public Map<String, String> getConfig() {
        Map<String, String> result = new LinkedHashMap<>();
        for (String key : EDITABLE_ENV_KEYS) {
            String val = System.getenv(key);
            if (val != null) result.put(key, val);
        }
        return result;
    }

    public Map<String, Object> updateConfig(Map<String, String> data) {
        Set<String> invalid = new HashSet<>(data.keySet());
        invalid.removeAll(EDITABLE_ENV_KEYS);
        if (!invalid.isEmpty()) {
            throw new AppException(400,
                    "These keys are not editable via the admin panel: " + new TreeSet<>(invalid));
        }
        writeEnvFile(data);
        return Map.<String, Object>of(
                "message", "Config updated. Changes to email/URL settings take effect immediately. Storage and auth settings require a server restart.",
                "updated", new ArrayList<>(data.keySet())
        );
    }

    private void writeEnvFile(Map<String, String> updates) {
        Path envPath = Path.of(".env");
        try {
            Map<String, String> existing = new LinkedHashMap<>();
            if (Files.exists(envPath)) {
                for (String line : Files.readAllLines(envPath)) {
                    if (line.contains("=") && !line.startsWith("#")) {
                        int idx = line.indexOf('=');
                        existing.put(line.substring(0, idx).trim(), line.substring(idx + 1).trim());
                    }
                }
            }
            existing.putAll(updates);
            List<String> lines = existing.entrySet().stream()
                    .map(e -> e.getKey() + "=" + e.getValue())
                    .collect(Collectors.toList());
            Files.write(envPath, lines, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException e) {
            log.warn("Could not write .env file: {}", e.getMessage());
        }
    }

    // ── Users ─────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listUsers(String school, String role, String search) {
        List<FacultyProfile> users = facultyRepo.findAll().stream()
                .sorted(Comparator.comparing((FacultyProfile u) -> u.getSchool() != null ? u.getSchool() : "")
                        .thenComparing(u -> u.getFullName() != null ? u.getFullName() : ""))
                .collect(Collectors.toList());

        if (school != null) users = users.stream()
                .filter(u -> school.equals(u.getSchool())).collect(Collectors.toList());
        if (role != null) users = users.stream()
                .filter(u -> role.equals(u.getAppraisalRole())).collect(Collectors.toList());
        if (search != null) {
            String lower = search.toLowerCase();
            users = users.stream()
                    .filter(u -> (u.getEmail() != null && u.getEmail().toLowerCase().contains(lower))
                            || (u.getFullName() != null && u.getFullName().toLowerCase().contains(lower)))
                    .collect(Collectors.toList());
        }

        return users.stream().map(this::userToMap).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createUser(UserCreateRequest req) {
        if (!VALID_ROLES.contains(req.getAppraisalRole())) {
            throw new AppException(400, "Invalid role '" + req.getAppraisalRole()
                    + "'. Valid roles: " + new TreeSet<>(VALID_ROLES));
        }
        if (facultyRepo.findByEmail(req.getEmail()).isPresent()) {
            throw new AppException(400, "Email already registered");
        }
        FacultyProfile user = new FacultyProfile();
        user.setEmail(req.getEmail());
        user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        user.setFullName(req.getFullName());
        user.setAppraisalRole(req.getAppraisalRole());
        user.setSchool(req.getSchool());
        user.setDepartment(req.getDepartment());
        user.setDesignation(req.getDesignation());
        user.setEmployeeId(req.getEmployeeId());
        user.setPhone(req.getPhone());
        user.setQualification(req.getQualification());
        user.setTeachingExperience(req.getTeachingExperience());
        user.setVerified(req.isVerified());
        user.setActive(req.isActive());
        user.setReportsToRegistrar(req.isReportsToRegistrar());
        user.setReportingOfficerEmail(req.getReportingOfficerEmail());
        user.setRegistrarEmail(req.getRegistrarEmail());
        user = facultyRepo.save(user);
        return Map.<String, Object>of("message", "User created", "email", user.getEmail(), "role", user.getAppraisalRole());
    }

    @Transactional
    public Map<String, Object> updateUser(String email, UserUpdateRequest req) {
        if (req.getAppraisalRole() != null && !VALID_ROLES.contains(req.getAppraisalRole())) {
            throw new AppException(400, "Invalid role '" + req.getAppraisalRole()
                    + "'. Valid roles: " + new TreeSet<>(VALID_ROLES));
        }
        FacultyProfile user = facultyRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(404, "User not found"));

        if (req.getPassword() != null) user.setPasswordHash(passwordEncoder.encode(req.getPassword()));
        if (req.getFullName() != null) user.setFullName(req.getFullName());
        if (req.getAppraisalRole() != null) user.setAppraisalRole(req.getAppraisalRole());
        if (req.getSchool() != null) user.setSchool(req.getSchool());
        if (req.getDepartment() != null) user.setDepartment(req.getDepartment());
        if (req.getDesignation() != null) user.setDesignation(req.getDesignation());
        if (req.getEmployeeId() != null) user.setEmployeeId(req.getEmployeeId());
        if (req.getPhone() != null) user.setPhone(req.getPhone());
        if (req.getQualification() != null) user.setQualification(req.getQualification());
        if (req.getTeachingExperience() != null) user.setTeachingExperience(req.getTeachingExperience());
        if (req.getIsVerified() != null) user.setVerified(req.getIsVerified().booleanValue());
        if (req.getIsActive() != null) user.setActive(req.getIsActive().booleanValue());
        if (req.getReportsToRegistrar() != null) user.setReportsToRegistrar(req.getReportsToRegistrar().booleanValue());
        if (req.getReportingOfficerEmail() != null) user.setReportingOfficerEmail(req.getReportingOfficerEmail());
        if (req.getRegistrarEmail() != null) user.setRegistrarEmail(req.getRegistrarEmail());

        user = facultyRepo.save(user);
        return Map.<String, Object>of("message", "User updated", "email", user.getEmail(), "role", user.getAppraisalRole());
    }

    @Transactional
    public Map<String, String> deleteUser(String email) {
        FacultyProfile user = facultyRepo.findByEmail(email)
                .orElseThrow(() -> new AppException(404, "User not found"));

        for (String table : TEACHING_DELETE_TABLES) {
            em.createNativeQuery("DELETE FROM " + table + " WHERE faculty_email = :email")
                    .setParameter("email", email).executeUpdate();
        }
        for (String table : List.of("non_teaching_part_a_items", "non_teaching_part_b_ratings", "non_teaching_appraisals")) {
            em.createNativeQuery("DELETE FROM " + table + " WHERE staff_email = :email")
                    .setParameter("email", email).executeUpdate();
        }
        em.createNativeQuery("DELETE FROM password_reset_tokens WHERE email = :email")
                .setParameter("email", email).executeUpdate();

        facultyRepo.delete(user);
        return Map.of("message", "User " + email + " deleted");
    }

    // ── Registrars / ROs ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listRegistrars() {
        return facultyRepo.findByAppraisalRole("registrar").stream()
                .filter(FacultyProfile::isActive)
                .sorted(Comparator.comparing(u -> u.getFullName() != null ? u.getFullName() : ""))
                .map(u -> Map.<String, Object>of("email", u.getEmail(), "full_name", u.getFullName(),
                        "school", u.getSchool() != null ? u.getSchool() : "",
                        "department", u.getDepartment() != null ? u.getDepartment() : ""))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listReportingOfficers() {
        return facultyRepo.findByAppraisalRole("reporting_officer").stream()
                .filter(FacultyProfile::isActive)
                .sorted(Comparator.comparing(u -> u.getFullName() != null ? u.getFullName() : ""))
                .map(u -> Map.<String, Object>of("email", u.getEmail(), "full_name", u.getFullName(),
                        "school", u.getSchool() != null ? u.getSchool() : "",
                        "department", u.getDepartment() != null ? u.getDepartment() : ""))
                .collect(Collectors.toList());
    }

    // ── NT Designations ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listDesignations() {
        return designationRepo.findAllByOrderByIsSystemDescNameAsc().stream()
                .map(this::designationToMap).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createDesignation(DesignationCreateRequest req) {
        String name = req.getName().trim();
        if (name.isEmpty()) throw new AppException(400, "Designation name is required");
        if (designationRepo.findByName(name).isPresent())
            throw new AppException(400, "Designation '" + name + "' already exists");
        NTDesignation d = new NTDesignation();
        d.setName(name);
        d.setDescription(req.getDescription());
        d = designationRepo.save(d);
        return designationToMap(d);
    }

    @Transactional
    public Map<String, Object> updateDesignation(UUID id, DesignationUpdateRequest req) {
        NTDesignation d = designationRepo.findById(id)
                .orElseThrow(() -> new AppException(404, "Designation not found"));
        if (req.getName() != null) {
            String name = req.getName().trim();
            if (name.isEmpty()) throw new AppException(400, "Designation name cannot be empty");
            designationRepo.findByName(name).ifPresent(existing -> {
                if (!existing.getId().equals(id))
                    throw new AppException(400, "Designation '" + name + "' already exists");
            });
            d.setName(name);
        }
        if (req.getDescription() != null) d.setDescription(req.getDescription());
        if (req.getIsActive() != null) d.setActive(req.getIsActive());
        d = designationRepo.save(d);
        return designationToMap(d);
    }

    @Transactional
    public Map<String, String> deleteDesignation(UUID id) {
        NTDesignation d = designationRepo.findById(id)
                .orElseThrow(() -> new AppException(404, "Designation not found"));
        if (d.isSystem()) throw new AppException(400, "System designations cannot be deleted");
        if (!stepRepo.findByDesignationIdInActiveTemplates(id).isEmpty()) {
            throw new AppException(400,
                    "Cannot delete: designation is used in an active workflow template. "
                            + "Remove it from all templates first, or deactivate instead.");
        }
        designationRepo.delete(d);
        return Map.of("message", "Designation '" + d.getName() + "' deleted");
    }

    // ── NT Workflow Templates ─────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listTemplates() {
        return templateRepo.findAllWithStepsOrdered().stream()
                .map(this::templateToMap).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createTemplate(TemplateCreateRequest req) {
        String name = req.getName().trim();
        if (name.isEmpty()) throw new AppException(400, "Template name is required");
        NTWorkflowTemplate t = new NTWorkflowTemplate();
        t.setName(name);
        t.setDescription(req.getDescription());
        t = templateRepo.save(t);
        return Map.of("id", t.getId().toString(), "name", t.getName(),
                "description", t.getDescription() != null ? t.getDescription() : "",
                "is_active", t.isActive(), "is_default", t.isDefault(), "steps", List.of());
    }

    @Transactional
    public Map<String, Object> updateTemplate(UUID id, TemplateUpdateRequest req) {
        NTWorkflowTemplate t = templateRepo.findById(id)
                .orElseThrow(() -> new AppException(404, "Template not found"));
        if (req.getName() != null) t.setName(req.getName());
        if (req.getDescription() != null) t.setDescription(req.getDescription());
        if (req.getIsActive() != null) t.setActive(req.getIsActive());
        t = templateRepo.save(t);
        return Map.of("id", t.getId().toString(), "name", t.getName(),
                "is_active", t.isActive(), "is_default", t.isDefault());
    }

    @Transactional
    public Map<String, String> deleteTemplate(UUID id) {
        NTWorkflowTemplate t = templateRepo.findById(id)
                .orElseThrow(() -> new AppException(404, "Template not found"));
        if (t.isDefault())
            throw new AppException(400, "Cannot delete the default template. Set another as default first.");
        templateRepo.delete(t);
        return Map.of("message", "Template '" + t.getName() + "' deleted");
    }

    @Transactional
    public Map<String, String> setDefaultTemplate(UUID id) {
        NTWorkflowTemplate t = templateRepo.findById(id)
                .orElseThrow(() -> new AppException(404, "Template not found"));
        templateRepo.clearDefaultExcept(id);
        t.setDefault(true);
        templateRepo.save(t);
        return Map.of("message", "'" + t.getName() + "' is now the default template");
    }

    // ── NT Workflow Template Steps ────────────────────────────────────────────

    @Transactional
    public Map<String, Object> addTemplateStep(UUID templateId, StepCreateRequest req) {
        if (templateRepo.findById(templateId).isEmpty())
            throw new AppException(404, "Template not found");

        UUID designationId = UUID.fromString(req.getDesignationId());
        NTDesignation desig = designationRepo.findById(designationId)
                .orElseThrow(() -> new AppException(404, "Designation not found"));
        if (!desig.isActive()) throw new AppException(400, "Cannot add an inactive designation as a step");

        int stepNo;
        if (req.getStepNo() != null) {
            if (stepRepo.findByTemplateIdAndStepNo(templateId, req.getStepNo()).isPresent())
                throw new AppException(400, "Step " + req.getStepNo() + " already exists in this template");
            stepNo = req.getStepNo();
        } else {
            stepNo = stepRepo.findMaxStepNoByTemplateId(templateId).orElse(0) + 1;
        }

        NTWorkflowTemplate template = templateRepo.findById(templateId).get();
        NTWorkflowTemplateStep step = new NTWorkflowTemplateStep();
        step.setTemplate(template);
        step.setStepNo(stepNo);
        step.setDesignation(desig);
        step.setRequired(req.isRequired());
        step = stepRepo.save(step);

        return Map.of("step", Map.of(
                "id", step.getId().toString(),
                "step_no", step.getStepNo(),
                "designation", desig.getName(),
                "is_required", step.isRequired()
        ));
    }

    @Transactional
    public Map<String, String> updateTemplateStep(UUID templateId, int stepNo, StepUpdateRequest req) {
        NTWorkflowTemplateStep step = stepRepo.findByTemplateIdAndStepNo(templateId, stepNo)
                .orElseThrow(() -> new AppException(404, "Step not found"));
        if (req.getDesignationId() != null) {
            NTDesignation desig = designationRepo.findById(UUID.fromString(req.getDesignationId()))
                    .orElseThrow(() -> new AppException(404, "Designation not found or inactive"));
            if (!desig.isActive()) throw new AppException(404, "Designation not found or inactive");
            step.setDesignation(desig);
        }
        if (req.getIsRequired() != null) step.setRequired(req.getIsRequired());
        stepRepo.save(step);
        return Map.of("message", "Step updated");
    }

    @Transactional
    public Map<String, String> removeTemplateStep(UUID templateId, int stepNo) {
        NTWorkflowTemplateStep step = stepRepo.findByTemplateIdAndStepNo(templateId, stepNo)
                .orElseThrow(() -> new AppException(404, "Step not found"));
        if (stepRepo.countByTemplateId(templateId) <= 1)
            throw new AppException(400, "A workflow template must have at least one step");
        stepRepo.delete(step);
        return Map.of("message", "Step removed");
    }

    @Transactional
    public Map<String, String> reorderTemplateSteps(UUID templateId, ReorderRequest req) {
        List<NTWorkflowTemplateStep> allSteps = stepRepo.findByTemplateIdOrderByStepNoAsc(templateId);
        if (allSteps.isEmpty()) throw new AppException(404, "Template not found or has no steps");

        Map<Integer, NTWorkflowTemplateStep> stepMap = allSteps.stream()
                .collect(Collectors.toMap(NTWorkflowTemplateStep::getStepNo, s -> s));
        List<Map<String, Integer>> newOrder = req.getSteps();

        // First pass: assign temp values to avoid unique constraint violations
        for (int i = 0; i < newOrder.size(); i++) {
            Integer oldNo = newOrder.get(i).get("step_no");
            if (oldNo != null && stepMap.containsKey(oldNo)) {
                stepMap.get(oldNo).setStepNo(i + 1001);
            }
        }
        stepRepo.saveAllAndFlush(allSteps);

        // Second pass: assign final values
        for (int i = 0; i < newOrder.size(); i++) {
            Integer oldNo = newOrder.get(i).get("step_no");
            if (oldNo != null && stepMap.containsKey(oldNo)) {
                stepMap.get(oldNo).setStepNo(i + 1);
            }
        }
        stepRepo.saveAll(allSteps);
        return Map.of("message", "Steps reordered");
    }

    // ── NT Workflow Assignments ───────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAssignments() {
        return assignmentRepo.findAllByOrderByCreatedAtDesc().stream().map(a -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", a.getId().toString());
            m.put("template_id", a.getTemplate().getId().toString());
            m.put("template_name", a.getTemplate().getName());
            m.put("staff_email", a.getStaffEmail());
            m.put("appraisal_role", a.getAppraisalRole());
            m.put("department", a.getDepartment());
            return m;
        }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createAssignment(AssignmentCreateRequest req) {
        long targetCount = (req.getStaffEmail() != null ? 1 : 0)
                + (req.getAppraisalRole() != null ? 1 : 0)
                + (req.getDepartment() != null ? 1 : 0);
        if (targetCount != 1)
            throw new AppException(400, "Provide exactly one of: staff_email, appraisal_role, department");

        UUID templateId = UUID.fromString(req.getTemplateId());
        NTWorkflowTemplate template = templateRepo.findById(templateId)
                .orElseThrow(() -> new AppException(404, "Template not found"));

        if (req.getAppraisalRole() != null
                && assignmentRepo.findByAppraisalRoleAndTemplate_Id(req.getAppraisalRole(), templateId).isPresent()) {
            throw new AppException(400, "An assignment for this target already exists");
        }
        if (req.getDepartment() != null
                && assignmentRepo.findByDepartmentAndTemplate_Id(req.getDepartment(), templateId).isPresent()) {
            throw new AppException(400, "An assignment for this target already exists");
        }

        NTWorkflowAssignment a = new NTWorkflowAssignment();
        a.setTemplate(template);
        a.setStaffEmail(req.getStaffEmail());
        a.setAppraisalRole(req.getAppraisalRole());
        a.setDepartment(req.getDepartment());
        a = assignmentRepo.save(a);
        return Map.<String, Object>of("id", a.getId().toString(), "template_id", a.getTemplate().getId().toString());
    }

    @Transactional
    public Map<String, String> deleteAssignment(UUID id) {
        NTWorkflowAssignment a = assignmentRepo.findById(id)
                .orElseThrow(() -> new AppException(404, "Assignment not found"));
        assignmentRepo.delete(a);
        return Map.of("message", "Assignment removed");
    }

    // ── Pending Faculty ───────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getPendingFaculty(String academicYear, String school) {
        Set<String> submitted = new HashSet<>(declarationRepo.findFacultyEmailsByAcademicYear(academicYear));
        List<String> teachingRoles = List.of("faculty", "hod", "director", "dean");
        return facultyRepo.findByAppraisalRoleIn(teachingRoles).stream()
                .filter(fp -> !submitted.contains(fp.getEmail()))
                .filter(fp -> school == null || school.equals(fp.getSchool()))
                .sorted(Comparator.comparing((FacultyProfile fp) -> fp.getSchool() != null ? fp.getSchool() : "")
                        .thenComparing(fp -> fp.getFullName() != null ? fp.getFullName() : ""))
                .map(fp -> Map.<String, Object>of(
                        "email", fp.getEmail(),
                        "full_name", fp.getFullName() != null ? fp.getFullName() : "",
                        "appraisal_role", fp.getAppraisalRole(),
                        "school", fp.getSchool() != null ? fp.getSchool() : "",
                        "department", fp.getDepartment() != null ? fp.getDepartment() : ""
                ))
                .collect(Collectors.toList());
    }

    // ── Submissions ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listSubmissions(String academicYear, String school) {
        if (academicYear == null) {
            List<String> years = declarationRepo.findDistinctAcademicYearsOrderDesc();
            if (years.isEmpty()) return List.of();
            academicYear = years.get(0);
        }
        List<Declaration> declarations = declarationRepo.findByAcademicYear(academicYear);
        if (school != null) {
            Set<String> schoolEmails = facultyRepo.findBySchool(school).stream()
                    .map(FacultyProfile::getEmail).collect(Collectors.toSet());
            declarations = declarations.stream()
                    .filter(d -> schoolEmails.contains(d.getFacultyEmail()))
                    .collect(Collectors.toList());
        }
        Map<String, FacultyProfile> profileMap = facultyRepo.findAll().stream()
                .collect(Collectors.toMap(FacultyProfile::getEmail, fp -> fp, (a, b) -> a));

        return declarations.stream()
                .sorted(Comparator.comparing((Declaration d) -> {
                    FacultyProfile fp = profileMap.get(d.getFacultyEmail());
                    return fp != null && fp.getSchool() != null ? fp.getSchool() : "";
                }).thenComparing(d -> {
                    FacultyProfile fp = profileMap.get(d.getFacultyEmail());
                    return fp != null && fp.getFullName() != null ? fp.getFullName() : "";
                }))
                .map(d -> {
                    FacultyProfile fp = profileMap.get(d.getFacultyEmail());
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("email", d.getFacultyEmail());
                    m.put("full_name", fp != null ? fp.getFullName() : "");
                    m.put("school", fp != null && fp.getSchool() != null ? fp.getSchool() : "");
                    m.put("department", fp != null && fp.getDepartment() != null ? fp.getDepartment() : "");
                    m.put("appraisal_role", fp != null ? fp.getAppraisalRole() : "");
                    m.put("designation", fp != null && fp.getDesignation() != null ? fp.getDesignation() : "");
                    m.put("academic_year", d.getAcademicYear());
                    m.put("status", d.getStatus());
                    m.put("submitted_at", d.getSubmittedAt() != null ? d.getSubmittedAt().toString() : null);
                    return m;
                }).collect(Collectors.toList());
    }

    // ── CSV Exports ───────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportSubmissions(String academicYear, String school) {
        if (academicYear == null) {
            List<String> years = declarationRepo.findDistinctAcademicYearsOrderDesc();
            if (years.isEmpty()) throw new AppException(404, "No submission data found");
            academicYear = years.get(0);
        }
        List<Declaration> declarations = declarationRepo.findByAcademicYear(academicYear);
        if (school != null) {
            Set<String> schoolEmails = facultyRepo.findBySchool(school).stream()
                    .map(FacultyProfile::getEmail).collect(Collectors.toSet());
            declarations = declarations.stream()
                    .filter(d -> schoolEmails.contains(d.getFacultyEmail()))
                    .collect(Collectors.toList());
        }
        if (declarations.isEmpty()) throw new AppException(404, "No submissions found for " + academicYear);

        Map<String, FacultyProfile> profileMap = facultyRepo.findAll().stream()
                .collect(Collectors.toMap(FacultyProfile::getEmail, fp -> fp, (a, b) -> a));

        String[] fields = {"faculty_email", "full_name", "school", "department", "appraisal_role",
                "designation", "academic_year", "status", "submitted_at", "part_a_total", "part_b_total", "grand_total"};

        StringWriter sw = new StringWriter();
        sw.write(String.join(",", fields) + "\n");
        for (Declaration d : declarations) {
            FacultyProfile fp = profileMap.get(d.getFacultyEmail());
            sw.write(csvRow(
                    d.getFacultyEmail(),
                    fp != null ? fp.getFullName() : "",
                    fp != null && fp.getSchool() != null ? fp.getSchool() : "",
                    fp != null && fp.getDepartment() != null ? fp.getDepartment() : "",
                    fp != null ? fp.getAppraisalRole() : "",
                    fp != null && fp.getDesignation() != null ? fp.getDesignation() : "",
                    d.getAcademicYear(),
                    d.getStatus(),
                    d.getSubmittedAt() != null ? d.getSubmittedAt().toString() : "",
                    d.getPartATotal() != null ? d.getPartATotal().toPlainString() : "0",
                    d.getPartBTotal() != null ? d.getPartBTotal().toPlainString() : "0",
                    d.getGrandTotal() != null ? d.getGrandTotal().toPlainString() : "0"
            ));
        }
        final String year = academicYear;
        return csvResponse(sw.toString(), "submissions-" + year + ".csv");
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> exportFaculty(String school, String role) {
        List<FacultyProfile> users = facultyRepo.findAll().stream()
                .sorted(Comparator.comparing((FacultyProfile u) -> u.getSchool() != null ? u.getSchool() : "")
                        .thenComparing(u -> u.getFullName() != null ? u.getFullName() : ""))
                .collect(Collectors.toList());
        if (school != null) users = users.stream()
                .filter(u -> school.equals(u.getSchool())).collect(Collectors.toList());
        if (role != null) users = users.stream()
                .filter(u -> role.equals(u.getAppraisalRole())).collect(Collectors.toList());

        String[] fields = {"email", "full_name", "appraisal_role", "school", "department",
                "designation", "phone", "qualification", "teaching_experience", "employee_id", "is_verified", "created_at"};
        StringWriter sw = new StringWriter();
        sw.write(String.join(",", fields) + "\n");
        for (FacultyProfile u : users) {
            sw.write(csvRow(
                    u.getEmail(),
                    u.getFullName() != null ? u.getFullName() : "",
                    u.getAppraisalRole(),
                    u.getSchool() != null ? u.getSchool() : "",
                    u.getDepartment() != null ? u.getDepartment() : "",
                    u.getDesignation() != null ? u.getDesignation() : "",
                    u.getPhone() != null ? u.getPhone() : "",
                    u.getQualification() != null ? u.getQualification() : "",
                    u.getTeachingExperience() != null ? u.getTeachingExperience() : "",
                    u.getEmployeeId() != null ? u.getEmployeeId() : "",
                    String.valueOf(u.isVerified()),
                    u.getCreatedAt() != null ? u.getCreatedAt().toString() : ""
            ));
        }
        return csvResponse(sw.toString(), "faculty-export.csv");
    }

    // ── Trends ────────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public Map<String, Object> getTrends(String academicYear) {
        if (academicYear == null) {
            List<String> years = declarationRepo.findDistinctAcademicYearsOrderDesc();
            academicYear = years.isEmpty() ? null : years.get(0);
        }
        if (academicYear == null) return Map.<String, Object>of("academic_year", (Object) null, "monthly", List.of());

        long totalRegistered = facultyRepo.findByAppraisalRoleIn(
                List.of("faculty", "hod", "director", "dean", "center_head")).size();

        List<Declaration> declarations = declarationRepo.findByAcademicYear(academicYear);
        declarations.sort(Comparator.comparing(d -> d.getSubmittedAt() != null ? d.getSubmittedAt() : d.getCreatedAt()));

        Map<String, Integer> monthCounts = new LinkedHashMap<>();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MMM");
        for (Declaration d : declarations) {
            if (d.getSubmittedAt() == null) continue;
            String key = d.getSubmittedAt().format(fmt);
            monthCounts.merge(key, 1, Integer::sum);
        }

        List<Map<String, Object>> monthly = new ArrayList<>();
        long cumulative = 0;
        for (Map.Entry<String, Integer> entry : monthCounts.entrySet()) {
            cumulative += entry.getValue();
            monthly.add(Map.<String, Object>of(
                    "month", entry.getKey(),
                    "submitted", cumulative,
                    "pending", Math.max(totalRegistered - cumulative, 0)
            ));
        }
        final String year = academicYear;
        return Map.<String, Object>of("academic_year", year, "monthly", monthly);
    }

    // ── Appraisal Config ──────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<Map<String, Object>> listAppraisalConfigs() {
        return appraisalConfigRepo.findAll().stream()
                .sorted(Comparator.comparing(AppraisalConfig::getAcademicYear, Comparator.reverseOrder()))
                .map(c -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", c.getId());
                    m.put("academic_year", c.getAcademicYear());
                    m.put("is_open", c.getIsOpen());
                    m.put("submission_start", c.getSubmissionStart());
                    m.put("submission_end", c.getSubmissionEnd());
                    m.put("updated_at", c.getUpdatedAt());
                    return m;
                }).collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> createAppraisalConfig(AppraisalConfigCreateRequest req) {
        if (appraisalConfigRepo.findByAcademicYear(req.getAcademicYear()).isPresent())
            throw new AppException(400, "Config for '" + req.getAcademicYear() + "' already exists");
        AppraisalConfig c = new AppraisalConfig();
        c.setAcademicYear(req.getAcademicYear());
        c.setIsOpen(req.isOpen());
        c.setSubmissionStart(req.getSubmissionStart());
        c.setSubmissionEnd(req.getSubmissionEnd());
        c = appraisalConfigRepo.save(c);
        return Map.<String, Object>of("message", "Appraisal config created",
                "academic_year", c.getAcademicYear(), "is_open", c.getIsOpen());
    }

    @Transactional
    public Map<String, Object> updateAppraisalConfig(String academicYear, AppraisalConfigUpdateRequest req) {
        AppraisalConfig c = appraisalConfigRepo.findByAcademicYear(academicYear)
                .orElseThrow(() -> new AppException(404, "No config found for '" + academicYear + "'"));
        if (req.getIsOpen() != null) c.setIsOpen(req.getIsOpen());
        if (req.getSubmissionStart() != null) c.setSubmissionStart(req.getSubmissionStart());
        if (req.getSubmissionEnd() != null) c.setSubmissionEnd(req.getSubmissionEnd());
        c = appraisalConfigRepo.save(c);
        return Map.<String, Object>of("message", "Config updated", "academic_year", c.getAcademicYear(), "is_open", c.getIsOpen());
    }

    @Transactional
    public Map<String, String> deleteAppraisalConfig(String academicYear) {
        AppraisalConfig c = appraisalConfigRepo.findByAcademicYear(academicYear)
                .orElseThrow(() -> new AppException(404, "No config found for '" + academicYear + "'"));
        appraisalConfigRepo.delete(c);
        return Map.of("message", "Config for '" + academicYear + "' deleted");
    }

    // ── Module Config ─────────────────────────────────────────────────────────

    @Transactional
    public Map<String, Object> getModuleConfig() {
        ModuleConfig c = moduleConfigRepo.findById(1).orElseGet(() -> {
            ModuleConfig def = new ModuleConfig();
            return moduleConfigRepo.save(def);
        });
        return Map.<String, Object>of(
                "appraisal_module_enabled", c.getAppraisalModuleEnabled(),
                "self_appraisal_enabled", c.getSelfAppraisalEnabled(),
                "peer_review_enabled", c.getPeerReviewEnabled()
        );
    }

    @Transactional
    public Map<String, String> updateModuleConfig(ModuleConfigUpdateRequest req) {
        ModuleConfig c = moduleConfigRepo.findById(1).orElseGet(() -> {
            ModuleConfig def = new ModuleConfig();
            return moduleConfigRepo.save(def);
        });
        if (req.getAppraisalModuleEnabled() != null) c.setAppraisalModuleEnabled(req.getAppraisalModuleEnabled());
        if (req.getSelfAppraisalEnabled() != null) c.setSelfAppraisalEnabled(req.getSelfAppraisalEnabled());
        if (req.getPeerReviewEnabled() != null) c.setPeerReviewEnabled(req.getPeerReviewEnabled());
        moduleConfigRepo.save(c);
        return Map.of("message", "Updated");
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private Map<String, Object> userToMap(FacultyProfile u) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("email", u.getEmail());
        m.put("full_name", u.getFullName());
        m.put("appraisal_role", u.getAppraisalRole());
        m.put("school", u.getSchool());
        m.put("department", u.getDepartment());
        m.put("designation", u.getDesignation());
        m.put("employee_id", u.getEmployeeId());
        m.put("phone", u.getPhone());
        m.put("qualification", u.getQualification());
        m.put("teaching_experience", u.getTeachingExperience());
        m.put("is_verified", u.isVerified());
        m.put("is_active", u.isActive());
        m.put("reports_to_registrar", u.isReportsToRegistrar());
        m.put("reporting_officer_email", u.getReportingOfficerEmail());
        m.put("registrar_email", u.getRegistrarEmail());
        m.put("created_at", u.getCreatedAt());
        return m;
    }

    private Map<String, Object> designationToMap(NTDesignation d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", d.getId().toString());
        m.put("name", d.getName());
        m.put("description", d.getDescription());
        m.put("is_system", d.isSystem());
        m.put("is_active", d.isActive());
        m.put("created_at", d.getCreatedAt());
        return m;
    }

    private Map<String, Object> templateToMap(NTWorkflowTemplate t) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", t.getId().toString());
        m.put("name", t.getName());
        m.put("description", t.getDescription());
        m.put("is_active", t.isActive());
        m.put("is_default", t.isDefault());
        m.put("created_at", t.getCreatedAt());
        m.put("steps", t.getSteps() == null ? List.of() : t.getSteps().stream().map(s -> {
            Map<String, Object> sm = new LinkedHashMap<>();
            sm.put("id", s.getId().toString());
            sm.put("step_no", s.getStepNo());
            sm.put("designation_id", s.getDesignation() != null ? s.getDesignation().getId().toString() : null);
            sm.put("designation", s.getDesignation() != null ? s.getDesignation().getName() : null);
            sm.put("is_required", s.isRequired());
            return sm;
        }).collect(Collectors.toList()));
        return m;
    }

    private ResponseEntity<byte[]> csvResponse(String content, String filename) {
        byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("text/csv"));
        headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"");
        return ResponseEntity.ok().headers(headers).body(bytes);
    }

    private String csvRow(String... values) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            if (i > 0) sb.append(',');
            String v = values[i] != null ? values[i] : "";
            if (v.contains(",") || v.contains("\"") || v.contains("\n")) {
                sb.append('"').append(v.replace("\"", "\"\"")).append('"');
            } else {
                sb.append(v);
            }
        }
        sb.append('\n');
        return sb.toString();
    }
}
