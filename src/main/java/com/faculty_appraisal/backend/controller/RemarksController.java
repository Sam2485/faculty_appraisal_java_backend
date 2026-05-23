package com.faculty_appraisal.backend.controller;

import com.faculty_appraisal.backend.exception.AppException;
import com.faculty_appraisal.backend.model.dto.remarks.*;
import com.faculty_appraisal.backend.security.CurrentUser;
import com.faculty_appraisal.backend.service.RemarksService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/appraisal-remarks")
@RequiredArgsConstructor
public class RemarksController extends BaseController {

    private final RemarksService remarksService;

    // ── Draft save / load ────────────────────────────────────────────────────

    @GetMapping("/draft/{email}")
    public ReviewDraftResponse getDraft(
            @PathVariable String email,
            @RequestParam String academicYear,
            @RequestParam String reviewerRole
    ) {
        return remarksService.getDraft(email, academicYear, reviewerRole, currentUser());
    }

    @PutMapping("/draft/{email}")
    public Map<String, String> saveDraft(
            @PathVariable String email,
            @Valid @RequestBody SaveDraftRequest req
    ) {
        return remarksService.saveDraft(email, req, currentUser());
    }

    // ── Final review endpoints ───────────────────────────────────────────────

    @PutMapping("/hod/{email}")
    public ReviewResponse reviewHod(
            @PathVariable String email,
            @Valid @RequestBody ReviewRequest req
    ) {
        CurrentUser user = currentUser();
        if (!user.getRoles().contains("hod") && !user.getRoles().contains("admin")) {
            throw new AppException(HttpStatus.FORBIDDEN.value(), "HOD role required");
        }
        return remarksService.handleReview("hod", email, req, user);
    }

    @PutMapping("/center-head/{email}")
    public ReviewResponse reviewCenterHead(
            @PathVariable String email,
            @Valid @RequestBody ReviewRequest req
    ) {
        CurrentUser user = currentUser();
        if (!user.getRoles().contains("center_head") && !user.getRoles().contains("admin")) {
            throw new AppException(HttpStatus.FORBIDDEN.value(), "Center Head role required");
        }
        return remarksService.handleReview("center_head", email, req, user);
    }

    @PutMapping("/director/{email}")
    public ReviewResponse reviewDirector(
            @PathVariable String email,
            @Valid @RequestBody ReviewRequest req
    ) {
        CurrentUser user = currentUser();
        if (!user.getRoles().contains("director") && !user.getRoles().contains("admin")) {
            throw new AppException(HttpStatus.FORBIDDEN.value(), "Director role required");
        }
        return remarksService.handleReview("director", email, req, user);
    }

    @PutMapping("/dean/{email}")
    public ReviewResponse reviewDean(
            @PathVariable String email,
            @Valid @RequestBody ReviewRequest req
    ) {
        CurrentUser user = currentUser();
        if (!user.getRoles().contains("dean") && !user.getRoles().contains("admin")) {
            throw new AppException(HttpStatus.FORBIDDEN.value(), "Dean role required");
        }
        return remarksService.handleReview("dean", email, req, user);
    }

    @PutMapping("/final/{email}")
    public ReviewResponse reviewFinal(
            @PathVariable String email,
            @Valid @RequestBody ReviewRequest req
    ) {
        CurrentUser user = currentUser();
        if (!user.getRoles().contains("vc") && !user.getRoles().contains("admin")) {
            throw new AppException(HttpStatus.FORBIDDEN.value(), "VC role required");
        }
        return remarksService.handleReview("vc", email, req, user);
    }
}
