package com.faculty_appraisal.backend.model.dto.non_teaching;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;

@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class NtSubordinateDto {
    private String staffEmail;
    private String name;
    private String designation;
    private String department;
    private String appraisalRole;
    private String status;
    private LocalDate submittedOn;
    private BigDecimal selfTotal;
    private BigDecimal roTotal;
    private BigDecimal registrarTotal;
    private BigDecimal vcTotal;
    private Map<String, Object> payload;
}
