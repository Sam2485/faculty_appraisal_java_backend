package com.faculty_appraisal.backend.model.dto.non_teaching;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Data
public class NtAppraisalRequest {

    @NotBlank(message = "academic_year is required")
    private String academicYear;

    private String status;
    private BigDecimal selfTotal;
    private BigDecimal roTotal;
    private BigDecimal registrarTotal;
    private BigDecimal vcTotal;
    private Map<String, Object> payload = new HashMap<>();
}
