package com.faculty_appraisal.backend.controller;

import com.faculty_appraisal.backend.exception.AppException;
import com.faculty_appraisal.backend.model.entity.core.Announcement;
import com.faculty_appraisal.backend.repository.core.AnnouncementRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AnnouncementController extends BaseController {

    private static final Set<String> VALID_AUDIENCES = Set.of(
            "all", "faculty", "hod", "director", "dean", "registrar", "non_teaching_staff",
            "SoCSEA", "SoBB", "SoCE", "SoEMR", "SoC", "SoMCS", "SoD", "SoAA", "CISR"
    );

    private final AnnouncementRepository announcementRepo;

    // ── DTOs ──────────────────────────────────────────────────────────────────

    @Data
    public static class AnnouncementCreateRequest {
        @NotBlank private String title;
        @NotBlank private String body;
        private String audience = "all";
        private boolean isActive = true;
    }

    @Data
    public static class AnnouncementUpdateRequest {
        private String title;
        private String body;
        private String audience;
        private Boolean isActive;
    }

    // ── Public ────────────────────────────────────────────────────────────────

    // Security config must permitAll GET /api/v1/announcements
    @GetMapping("/announcements")
    public List<Map<String, Object>> listAnnouncements() {
        return announcementRepo.findByIsActiveTrueOrderByCreatedAtDesc().stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("title", a.getTitle());
                    m.put("body", a.getBody());
                    m.put("audience", a.getAudience());
                    m.put("created_at", a.getCreatedAt());
                    return m;
                }).collect(Collectors.toList());
    }

    // ── Admin ─────────────────────────────────────────────────────────────────

    @GetMapping("/admin/announcements")
    public List<Map<String, Object>> listAllAnnouncements() {
        requireAdmin();
        return announcementRepo.findAllByOrderByCreatedAtDesc().stream()
                .map(a -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("id", a.getId());
                    m.put("title", a.getTitle());
                    m.put("body", a.getBody());
                    m.put("audience", a.getAudience());
                    m.put("is_active", a.getIsActive());
                    m.put("created_by", a.getCreatedBy());
                    m.put("created_at", a.getCreatedAt());
                    m.put("updated_at", a.getUpdatedAt());
                    return m;
                }).collect(Collectors.toList());
    }

    @PostMapping("/admin/announcements")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, Object> createAnnouncement(@Valid @RequestBody AnnouncementCreateRequest req) {
        requireAdmin();
        validateAudience(req.getAudience());
        Announcement a = new Announcement();
        a.setTitle(req.getTitle());
        a.setBody(req.getBody());
        a.setAudience(req.getAudience());
        a.setIsActive(req.isActive());
        a.setCreatedBy(currentUser().getEmail());
        a = announcementRepo.save(a);
        return Map.of("message", "Announcement created", "id", a.getId());
    }

    @PutMapping("/admin/announcements/{id}")
    public Map<String, Object> updateAnnouncement(
            @PathVariable Integer id,
            @RequestBody AnnouncementUpdateRequest req
    ) {
        requireAdmin();
        if (req.getAudience() != null) validateAudience(req.getAudience());
        Announcement a = announcementRepo.findById(id)
                .orElseThrow(() -> new AppException(404, "Announcement not found"));
        if (req.getTitle() != null) a.setTitle(req.getTitle());
        if (req.getBody() != null) a.setBody(req.getBody());
        if (req.getAudience() != null) a.setAudience(req.getAudience());
        if (req.getIsActive() != null) a.setIsActive(req.getIsActive());
        a = announcementRepo.save(a);
        return Map.of("message", "Announcement updated", "id", a.getId());
    }

    @DeleteMapping("/admin/announcements/{id}")
    public Map<String, String> deleteAnnouncement(@PathVariable Integer id) {
        requireAdmin();
        Announcement a = announcementRepo.findById(id)
                .orElseThrow(() -> new AppException(404, "Announcement not found"));
        announcementRepo.delete(a);
        return Map.of("message", "Announcement " + id + " deleted");
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void requireAdmin() {
        if (!currentUser().getRoles().contains("admin")) {
            throw new AppException(403, "Admin role required");
        }
    }

    private void validateAudience(String audience) {
        List<String> tokens = Arrays.stream(audience.split(","))
                .map(String::strip).filter(s -> !s.isEmpty()).collect(Collectors.toList());
        if (tokens.isEmpty()) throw new AppException(400, "audience cannot be empty");
        List<String> invalid = tokens.stream()
                .filter(t -> !VALID_AUDIENCES.contains(t)).collect(Collectors.toList());
        if (!invalid.isEmpty()) {
            throw new AppException(400, "Invalid audience value(s): " + invalid
                    + ". Each token must be one of: " + new TreeSet<>(VALID_AUDIENCES));
        }
    }
}
