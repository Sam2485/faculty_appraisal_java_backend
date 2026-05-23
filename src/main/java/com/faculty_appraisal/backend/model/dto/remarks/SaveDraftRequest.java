package com.faculty_appraisal.backend.model.dto.remarks;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class SaveDraftRequest {

    @NotBlank(message = "reviewer_role is required")
    private String reviewerRole;

    @NotBlank(message = "academic_year is required")
    private String academicYear;

    private Map<String, Object> payload = new HashMap<>();
}
