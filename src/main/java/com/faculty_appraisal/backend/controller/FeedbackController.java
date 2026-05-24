package com.faculty_appraisal.backend.controller;

import com.faculty_appraisal.backend.exception.AppException;
import com.faculty_appraisal.backend.model.dto.FeedbackRequest;
import com.faculty_appraisal.backend.model.entity.core.Feedback;
import com.faculty_appraisal.backend.repository.core.FeedbackRepository;
import com.faculty_appraisal.backend.security.CurrentUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/v1/feedback")
@RequiredArgsConstructor
public class FeedbackController extends BaseController {

    private static final Set<String> VALID_CATEGORIES =
            Set.of("query", "feedback", "bug", "suggestion", "other");

    private final FeedbackRepository feedbackRepo;

    // Public — no auth required (security config must permit POST /api/v1/feedback)
    @PostMapping
    public Map<String, Object> createFeedback(
            @Valid @RequestBody FeedbackRequest req,
            HttpServletRequest httpRequest
    ) {
        if (!VALID_CATEGORIES.contains(req.getCategory())) {
            throw new AppException(422, "Category must be one of: "
                    + String.join(", ", new TreeSet<>(VALID_CATEGORIES)));
        }

        Feedback feedback = new Feedback();
        feedback.setName(req.getName() != null && !req.getName().isBlank() ? req.getName().strip() : null);
        feedback.setEmail(req.getEmail().strip().toLowerCase());
        feedback.setCategory(req.getCategory().strip());
        feedback.setSubject(req.getSubject().strip());
        feedback.setMessage(req.getMessage().strip());
        feedback.setIpAddress(resolveClientIp(httpRequest));
        String ua = httpRequest.getHeader("User-Agent");
        feedback.setUserAgent(ua != null ? ua.substring(0, Math.min(ua.length(), 512)) : null);
        feedback = feedbackRepo.save(feedback);

        return Map.<String, Object>of(
                "success", true,
                "message", "Feedback saved.",
                "feedback", Map.<String, Object>of(
                        "id", feedback.getId().toString(),
                        "status", feedback.getStatus(),
                        "submitted_at", feedback.getSubmittedAt().toString()
                )
        );
    }

    // Admin only
    @GetMapping
    public List<Map<String, Object>> listFeedback(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String status
    ) {
        requireAdmin(currentUser());
        limit = Math.max(1, Math.min(limit, 100));

        List<Feedback> items;
        if (category != null && status != null) {
            items = feedbackRepo.findByCategoryAndStatus(category, status);
        } else if (category != null) {
            items = feedbackRepo.findByCategory(category);
        } else if (status != null) {
            items = feedbackRepo.findByStatus(status);
        } else {
            items = feedbackRepo.findAll(Sort.by(Sort.Direction.DESC, "submittedAt"));
        }

        final int cap = limit;
        return items.stream().limit(cap).map(f -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", f.getId().toString());
            m.put("name", f.getName());
            m.put("email", f.getEmail());
            m.put("category", f.getCategory());
            m.put("subject", f.getSubject());
            m.put("message", f.getMessage());
            m.put("status", f.getStatus());
            m.put("ip_address", f.getIpAddress());
            m.put("submitted_at", f.getSubmittedAt() != null ? f.getSubmittedAt().toString() : null);
            return m;
        }).collect(java.util.stream.Collectors.toList());
    }

    // Admin only
    @GetMapping("/{feedbackId}")
    public Map<String, Object> getFeedback(@PathVariable UUID feedbackId) {
        requireAdmin(currentUser());
        Feedback f = feedbackRepo.findById(feedbackId)
                .orElseThrow(() -> new AppException(404, "Feedback not found"));
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", f.getId().toString());
        m.put("name", f.getName());
        m.put("email", f.getEmail());
        m.put("category", f.getCategory());
        m.put("subject", f.getSubject());
        m.put("message", f.getMessage());
        m.put("status", f.getStatus());
        m.put("ip_address", f.getIpAddress());
        m.put("user_agent", f.getUserAgent());
        m.put("submitted_at", f.getSubmittedAt() != null ? f.getSubmittedAt().toString() : null);
        return m;
    }

    private void requireAdmin(CurrentUser user) {
        if (!user.getRoles().contains("admin") && !user.getRoles().contains("super_admin")) {
            throw new AppException(403, "Admin role required");
        }
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].strip();
        }
        return request.getRemoteAddr();
    }
}
