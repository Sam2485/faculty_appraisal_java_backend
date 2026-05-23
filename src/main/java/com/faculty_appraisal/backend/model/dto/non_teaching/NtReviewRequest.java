package com.faculty_appraisal.backend.model.dto.non_teaching;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Map;

@Data
public class NtReviewRequest {

    @NotBlank(message = "academic_year is required")
    private String academicYear;

    private BigDecimal totalScore;
    private Map<String, Object> payload;
}
