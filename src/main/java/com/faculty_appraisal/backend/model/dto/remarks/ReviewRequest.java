package com.faculty_appraisal.backend.model.dto.remarks;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.Map;

@Data
public class ReviewRequest {

    @NotBlank(message = "academic_year is required")
    private String academicYear;

    private double partAScore;
    private double partBScore;
    private double totalScore;
    private String remarks;
    private String rejectionReason;

    private Map<String, Object> sectionScores;

    // Rejection detection — frontend may send any of these
    private String decision;
    private String action;
    private String reviewDecision;
    private Boolean rejected;

    @JsonProperty("is_rejected")
    private Boolean isRejected;

    public boolean isRejectionRequest() {
        if ("rejected".equalsIgnoreCase(trimmed(decision))) return true;
        if ("reject".equalsIgnoreCase(trimmed(action))) return true;
        if ("rejected".equalsIgnoreCase(trimmed(reviewDecision))) return true;
        if (Boolean.TRUE.equals(rejected)) return true;
        if (Boolean.TRUE.equals(isRejected)) return true;
        return false;
    }

    public String getRejectionRemarks() {
        String r = remarks != null ? remarks.trim() : "";
        if (!r.isEmpty()) return r;
        return rejectionReason != null ? rejectionReason.trim() : "";
    }

    private String trimmed(String s) {
        return s != null ? s.trim() : "";
    }
}
